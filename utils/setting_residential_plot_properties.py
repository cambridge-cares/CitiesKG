import numpy as np
import pandas as pd
import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon
from rdflib.namespace import XSD
from GFAOntoManager import *
from SPARQLWrapper import SPARQLWrapper, JSON
from envelope_conversion import envelopeStringToPolygon
from shapely.geometry.polygon import Polygon
from rdflib import Dataset, Literal, URIRef
from shapely.geometry import LineString

cur_dir = 'C://Users/AydaGrisiute/Desktop'

def get_plots(endpoint):
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery(""" PREFIX ocgml:<http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    SELECT ?plots ?geom ?zone
    WHERE { 
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/>
     { ?plots ocgml:id ?obj_id .
     BIND(IRI(REPLACE(STR(?plots), "cityobject", "genericcityobject")) AS ?gen_obj) }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/> {
      ?s ocgml:cityObjectId ?gen_obj ;
        ocgml:GeometryType ?geom . 
        hint:Prior hint:runLast "true" . }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
        ?attr ocgml:cityObjectId ?plots ;
                ocgml:attrName 'LU_DESC' ;
                ocgml:strVal ?zone . } 
    } """)
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()

    queryResults = pd.DataFrame(results['results']['bindings'])
    queryResults = queryResults.applymap(lambda cell: cell['value'])
    geometries = gpd.GeoSeries(queryResults['geom'].map(lambda geo: envelopeStringToPolygon(geo, geodetic=True)),
                               crs='EPSG:4326')
    geometries = geometries.to_crs(epsg=3857)
    plots = gpd.GeoDataFrame(queryResults, geometry=geometries).drop(columns=['geom'])
    plots = process_plots(plots)
    return plots


'''Filter and clean queried plot result.'''


def process_plots(plots):
    plots.geometry = plots.geometry.simplify(0.1)
    plots = plots[~(plots.geometry.type == "MultiPolygon")]
    plots.loc[:,'geometry'] = plots.loc[:,'geometry'].buffer(0)
    plots = plots.loc[plots.area >= 50]
    return plots


'''Generate an abstract residential areas boundary by merging residential plot buffered polygons. '''


def get_residential_area(res_plots, buffer, hole_size):
    res_area = res_plots.buffer(buffer).unary_union
    multipolygon_parts = []
    for polygon in res_area.geoms:
        list_interiors = []
        for interior in polygon.interiors:
            if Polygon(interior).area > hole_size:
                list_interiors.append(interior)
        multipolygon_parts.append(Polygon(polygon.exterior.coords, holes=list_interiors))
    res_area = MultiPolygon(multipolygon_parts).simplify(0.5).buffer(-abs(buffer))
    res_area_df = gpd.GeoDataFrame(geometry=gpd.GeoSeries(res_area), crs=3857)
    res_area_df = res_area_df.explode('geometry', ignore_index=True)
    return res_area_df


'''Check if a residential plot is in a residential plot fringe and add boolean to new column 'fringe'. '''


def check_fringe(res_plots, residential_area, intersection_buffer):
    res_plots_buffered = res_plots.copy()
    res_plots_buffered['geometry'] = res_plots_buffered.buffer(intersection_buffer)
    res_plots_buffered['area'] = res_plots_buffered.area
    intersection = gpd.overlay(residential_area, res_plots_buffered, how='intersection')
    intersection['intersection_area'] = intersection.area
    intersection_fringe = intersection[round(intersection['intersection_area']) < round(intersection['area'])]
    res_plots['fringe'] = res_plots['plots'].isin(intersection_fringe['plots'])
    return res_plots


'''Find residential plot neighbors and add neighbor ids into new column 'neighbor_list'. '''


def find_neighbours(plots, all_plots):
    buffered_plots = plots[['plots', 'geometry']].copy()
    buffered_plots['geometry'] = buffered_plots.buffer(2, cap_style=3)
    intersection = gpd.overlay(buffered_plots, all_plots, how='intersection', keep_geom_type=True)
    intersection['area'] = intersection.area
    intersection = intersection.loc[intersection['area'] > 1,:].drop(columns={'area', 'geometry'})
    neighbors = intersection.groupby('plots')['context_plots'].unique()
    plots['neighbor_list'] = neighbors.loc[plots['plots']].to_numpy()
    return plots


'''Get the edges of any geometry.'''


def get_edges(polygon):
    curve = polygon.exterior.simplify(0.1)
    return list(map(LineString, zip(curve.coords[:-1], curve.coords[1:])))


'''Create edge dataframe with min_rect_edges exploaded and buffered.'''


def get_min_rect_edge_df(res_plots):
    edges = res_plots.loc[:,['plots', 'min_rect_edges']].copy().sort_values('plots').explode('min_rect_edges', ignore_index=True)
    edges["order"] = list(range(4)) * res_plots['plots'].nunique()
    edges = gpd.GeoDataFrame(edges, geometry='min_rect_edges', crs=3857)
    edges['length'] = edges.length.round(decimals=3)
    edges = edges.sort_values("plots")
    edges['min_rect_edges'] = edges.buffer(3, single_sided=False, cap_style=3)
    edges['buffered_area'] = edges.area
    return edges


'''Intersect buffered edge dataframe with road plots and filter out intersections with not neighbor roads. '''


def intersect_roads_with_edges(edges, plots, res_plots):
    intersection = gpd.overlay(edges, plots[plots['zone'] == 'ROAD'], how='intersection')
    neighbor = (intersection.merge(res_plots.loc[:, ['plots','neighbor_list']], how="left", left_on="plots_1", right_on='plots')
                .drop(columns=["plots"])
                .apply(lambda row: row["plots_2"] in row["neighbor_list"], axis=1))
    intersection = intersection.loc[neighbor, :]
    intersection['intersection_area'] = intersection.area
    return intersection


'''Set min rectangle edge types and add edge indices in corresponding df columns.'''


def set_min_rect_edge_types(intersection, edges, plots):
    # define a front edge indice based on largest intersection with the longest edge.
    front_edge = intersection.sort_values(by=['plots_1', 'intersection_area','length'], ascending=False).groupby(['plots_1'])['order'].first()
    front_edge.name = 'min_rect_front_edge'
    plots = plots.merge(front_edge, left_on='plots', right_index=True, how='left')
    not_front_edge = edges["order"] != plots.sort_values('plots')['min_rect_front_edge'].repeat(4).to_numpy()
    front_edge_length = edges.loc[~not_front_edge, ["plots", 'length']].set_index("plots")['length']
    front_edge_length_missing = list(set(plots["plots"].unique()).difference(front_edge_length.index))
    front_edge_length = pd.concat([front_edge_length, pd.Series([0]*len(front_edge_length_missing), index=front_edge_length_missing)])
    front_edge_length = front_edge_length.sort_index().repeat(4)
    # define a rear edge indice which is not a front edge indice but the same length.
    rear_edge = not_front_edge & (edges["length"] == front_edge_length.to_numpy())
    rear_edge = edges.loc[rear_edge, :].groupby("plots")["order"].first()
    rear_edge.name = 'min_rect_rear_edge'
    plots = plots.merge(rear_edge, left_on='plots', right_index=True, how='left')
    # define side edge indices which are the remaining indices that are not front or rear edge indices.
    edge_indices = [list(set([0.0, 1.0, 2.0, 3.0]).difference([plots.loc[x, 'min_rect_front_edge'], plots.loc[x,'min_rect_rear_edge']])) for x in plots.index]
    plots['min_rect_side_edges'] = [edge_indices[i] for i in range(len(edge_indices))]
    plots.loc[plots['min_rect_front_edge'].isna(), 'min_rect_side_edges'] = np.nan
    return plots


def is_corner_plot_helper(road_edge_row):
    return len(set(road_edge_row.loc['order']).difference([road_edge_row.loc['min_rect_rear_edge']])) > 1


'''Determining booleans if it is a corner plot by checking if plots min rect front and side edge intersecs with the road at least 30%.'''


def is_corner_plot(intersection, min_rect_plots, overlap_ratio):
    road_edges = intersection.loc[(intersection['intersection_area'] / intersection['buffered_area']) > overlap_ratio].groupby('plots_1')['order'].unique()
    road_edges = pd.merge(road_edges, min_rect_plots.loc[:,['plots', 'min_rect_rear_edge']].set_index('plots'), how='left',
                      right_index=True, left_index=True)
    road_edges.loc[:,'min_rect_rear_edge'] = road_edges['min_rect_rear_edge'].astype(int)
    min_rect_plots = min_rect_plots.merge(road_edges.apply(is_corner_plot_helper, axis=1).rename('is_corner_plot'), how='left', left_on='plots', right_index=True)
    return min_rect_plots


'''Calculate average width or depth of a plot using min rect front and side edges and stores it in corresponding columns.'''


def find_average_width_or_depth(plot_row, width):
    cur_front_edge = plot_row.loc['min_rect_edges'][int(plot_row.loc['min_rect_front_edge'])]
    cur_side_edge = plot_row.loc['min_rect_edges'][int(plot_row.loc[ 'min_rect_side_edges'][0])]
    if width == 'average_width':
        offset_distances = np.linspace(0, cur_side_edge.length, 12)[1:-1]
        lines = [cur_front_edge.parallel_offset(cur_offset, 'left') for cur_offset in offset_distances]
    else:
        offset_distances = np.linspace(0, cur_front_edge.length, 12)[1:-1]
        lines = [cur_side_edge.parallel_offset(cur_offset, 'left') for cur_offset in offset_distances]
    average_length = round(np.median([plot_row.loc['geometry'].intersection(line).length for line in lines]),3)
    return average_length


'''Set residential plot properties, e.g. if it is a fringe plot, corner plot, plot's average depth and width.'''


def set_residential_plot_properties(plots):
    res_plots = plots[plots['zone'].isin(
        ['RESIDENTIAL', 'COMMERCIAL & RESIDENTIAL', 'RESIDENTIAL WITH COMMERCIAL AT 1ST STOREY',
         'RESIDENTIAL / INSTITUTION'])]

    residential_area = get_residential_area(res_plots, 200, 120000)
    res_plots = check_fringe(res_plots.copy(), residential_area, 10)
    print('fringe plots set.')
    all_plots = plots.loc[:, ['plots', 'geometry']]
    all_plots.rename(columns={'plots': 'context_plots'}, inplace=True)
    res_plots = find_neighbours(res_plots, all_plots)
    print('residential plot neighbors set.')
    res_plots['min_rect_edges'] = [get_edges(x) for x in [y.minimum_rotated_rectangle for y in res_plots.geometry]]
    edges = get_min_rect_edge_df(res_plots)
    intersection = intersect_roads_with_edges(edges, plots, res_plots)
    res_plots = set_min_rect_edge_types(intersection, edges, res_plots)
    res_plots = is_corner_plot(intersection, res_plots, 0.3)
    print('corner plots set.')
    filtered_res_plots = res_plots[~res_plots['min_rect_front_edge'].isna()].set_index('plots')
    filtered_res_plots.loc[:, 'average_width'] = filtered_res_plots.apply(find_average_width_or_depth, axis=1, args=('average_width',)).to_numpy()
    filtered_res_plots.loc[:, 'average_depth'] = filtered_res_plots.apply(find_average_width_or_depth, axis=1, args=('average_depth',)).to_numpy()
    res_plots = res_plots.merge(filtered_res_plots.loc[:, ['average_width', 'average_depth']], how='left', left_on='plots', right_index=True)
    print('Average plot width and depth set.')
    return res_plots


'''Set road plot properties e.g. road type and road category'''


def set_road_plot_properties(road_network, plots):
    road_plots = plots[plots['zone'] == 'ROAD']
    invalid_road_types = ['Cross Junction', 'T-Junction', 'Expunged', 'Other Junction', 'Pedestrian Mall',
                          '2 T-Junction opposite each other', 'Unknown', 'Y-Junction', 'Imaginary Line']
    road_network = road_network[~road_network['RD_TYP_CD'].isin(invalid_road_types)]
    road_cat_dict = {'Expressway': '1',
                     'Semi Expressway': '2-3',
                     'Major Arterials/Minor Arterials': '2-3',
                     'Local Collector/Primary Access': '4',
                     'Local Access': '5',
                     'Slip Road': '5',
                     'Service Road': '5',
                     'no category': 'unknown'}
    road_plots = assign_road_category(road_plots, road_network)
    road_plots['road_category'] = [road_cat_dict[x] for x in road_plots["RD_TYP_CD"]]
    return road_plots


'''Overlap road network data and road plots and links road network attributes to road plots.'''


def assign_road_category(road_plots, road_network):
    road_network.loc[:,'geometry'] = road_network.buffer(5)
    intersection = gpd.overlay(road_plots,  road_network, how='intersection', keep_geom_type=True)
    intersection['intersection_area'] = intersection.area
    grouped_intersection = intersection.groupby(['plots']).apply(lambda x: x.sort_values(['intersection_area'], ascending=False).iloc[0, :]).drop(columns=['plots'])
    grouped_intersection = grouped_intersection.reset_index()
    grouped_intersection = grouped_intersection[['plots', 'RD_TYP_CD']].copy()
    road_plots = road_plots.merge(grouped_intersection, on='plots', how='left')
    road_plots.loc[road_plots["RD_TYP_CD"].isna(), "RD_TYP_CD"] = "no category"
    return road_plots


class TripleDataset:

    def __init__(self):
        self.dataset = Dataset()


    '''A method to generate necessary triples to represent residential plot properties.'''


    def create_plot_property_triples(self, res_plots):
        for i in res_plots.index:
            city_obj_uri = URIRef(res_plots.loc[i, 'plots'])
            if not pd.isna(res_plots.loc[i, 'fringe']):
                fringe_bool = Literal(str(res_plots.loc[i, 'fringe']), datatype=XSD.boolean)
                self.dataset.add((city_obj_uri, GFAOntoManager.IS_AT_RESIDENTIAL_FRINGE, fringe_bool, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            if not pd.isna(res_plots.loc[i, 'average_width']):
                average_width = Literal(str(res_plots.loc[i, 'average_width']), datatype=XSD.decimal)
                self.dataset.add((city_obj_uri, GFAOntoManager.HAS_WIDTH, average_width, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            if not pd.isna(res_plots.loc[i, 'average_depth']):
                average_depth = Literal(str(res_plots.loc[i, 'average_depth']), datatype=XSD.decimal)
                self.dataset.add((city_obj_uri, GFAOntoManager.HAS_DEPTH, average_depth, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            if not pd.isna(res_plots.loc[i, 'is_corner_plot']):
                corner_plot_bool = Literal(str(res_plots.loc[i, 'is_corner_plot']), datatype=XSD.boolean)
                self.dataset.add((city_obj_uri, GFAOntoManager.IS_CORNER_PLOT, corner_plot_bool, GFAOntoManager.BUILDABLE_SPACE_GRAPH))


    '''A method to generate necessary triples to represent road plot properties.'''


    def create_road_plot_property_triples(self, road_plots):
        for i in road_plots.index:
            city_obj_uri = URIRef(road_plots.loc[i, 'plots'])
            if not pd.isna(road_plots.loc[i, 'RD_TYP_CD']):
                road_type = Literal(str(road_plots.loc[i, 'RD_TYP_CD']), datatype=XSD.string)
                self.dataset.add((city_obj_uri, GFAOntoManager.HAS_ROAD_TYPE, road_type, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            # currently skipped as mapping between types and categories is arbitrary and not confirmed by URA.
            #if not pd.isna(road_plots.loc[i, 'road_category']):
            #    road_category = Literal(str(road_plots.loc[i, 'road_category']), datatype=XSD.string)
            #    self.dataset.add((city_obj_uri, GFAOntoManager.HAS_ROAD_CATEGORY, road_category, GFAOntoManager.BUILDABLE_SPACE_GRAPH))


    '''A method to write the aggregated triple dataset into a nquad(text) file.'''


    def write_triples(self, triple_type):
        with open(cur_dir + "/output_" + triple_type + ".nq", mode="wb") as file:
            file.write(self.dataset.serialize(format='nquads'))


'''A method to write generated residential plot property triples into a nquad file'''


def instantiate_residential_plot_property_triples(plots_df):
    plot_properties = TripleDataset()
    plot_properties.create_plot_property_triples(plots_df)
    plot_properties.write_triples("residential_plot_properties_triples")
    print("plot properties nquads written.")


'''A method to write generated road plot property triples into a nquad file'''


def instantiate_road_plot_properties(plots_df):
    road_plot_properties = TripleDataset()
    road_plot_properties.create_road_plot_property_triples(plots_df)
    road_plot_properties.write_triples("road_plot_properties_triples")
    print("road plot properties nquads written.")


if __name__ == "__main__":
    plots = get_plots("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql")
    print('plots retrieved')
    #residential_plots_df = set_residential_plot_properties(plots)
    #print('Residential plot properties set.')
    #instantiate_residential_plot_property_triples(residential_plots_df)
    #print('residential plot property triples written.')

    road_network = gpd.read_file("C:/Users/AydaGrisiute/Desktop/demonstrator/roads/roads_whole_SG/roads.shp").to_crs(3857)
    road_network = road_network[['RD_NAME', 'RD_TYP_CD', 'LVL_OF_RD', 'UNIQUE_ID', 'geometry']].copy()
    print('Road network retrieved')
    road_plots_df = set_road_plot_properties(road_network, plots)
    print('Road plot properties set.')
    instantiate_road_plot_properties(road_plots_df)
    print('Road plot property triples written')

