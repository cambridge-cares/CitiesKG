import numpy as np
import pandas as pd
import geopandas as gpd
from shapely.geometry import Polygon, MultiPolygon
from rdflib.namespace import XSD
from rdflib.namespace import RDF
from GFAOntoManager import *
from SPARQLWrapper import SPARQLWrapper, JSON
from envelope_conversion import envelopeStringToPolygon
from shapely.geometry.polygon import Polygon
from rdflib import Dataset, Literal, URIRef
from shapely.geometry import LineString
import uuid

cur_dir = 'C://Users/AydaGrisiute/Desktop'

def get_plots(endpoint):
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX ocgml:<http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    SELECT ?plots ?geom ?zone ?gpr
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/>
    { ?plots ocgml:id ?obj_id .
    BIND(IRI(REPLACE(STR(?plots), "cityobject", "genericcityobject")) AS ?gen_obj) }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/> {
    ?s ocgml:cityObjectId ?gen_obj ;
    ocgml:GeometryType ?geom . 
    hint:Prior hint:runLast "true" . }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
    ?attr ocgml:cityObjectId ?plots ;
    ocgml:attrName 'LU_DESC' ;
    ocgml:strVal ?zone . 
    ?attr1 ocgml:cityObjectId ?plots ;
    ocgml:attrName 'GPR' ;
    ocgml:strVal ?gpr . } } """)
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()

    queryResults = pd.DataFrame(results['results']['bindings'])
    queryResults = queryResults.applymap(lambda cell: cell['value'])
    geometries = gpd.GeoSeries(queryResults['geom'].map(lambda geo: envelopeStringToPolygon(geo, geodetic=True)), crs='EPSG:4326')
    geometries = geometries.to_crs(epsg=3857)
    plots = gpd.GeoDataFrame(queryResults, geometry=geometries).drop(columns=['geom'])
    plots = process_plots(plots)
    return plots


'''Filter and clean queried plot result.'''


def process_plots(plots):
    plots.geometry = plots.geometry.simplify(0.1)
    plots = plots[~(plots.geometry.type == "MultiPolygon")]
    plots.loc[:,'geometry'] = plots.loc[:, 'geometry'].buffer(0)
    plots = plots.loc[plots.area >= 50]
    plots['plot_area'] = plots.area
    plots['gpr'] = pd.to_numeric(plots['gpr'], errors='coerce')
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
    intersection = intersection.loc[intersection['area'] > 1, :].drop(columns={'area', 'geometry'})
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


'''Determining booleans if it is a corner plot by checking if plots min rect front and side edge intersecs with the road at least 30%.'''


def is_corner_plot(intersection, min_rect_plots, overlap_ratio):
    road_edges = intersection.loc[(intersection['intersection_area'] / intersection['buffered_area']) > overlap_ratio].groupby('plots_1')['order'].unique()
    road_edges = pd.merge(road_edges, min_rect_plots.loc[:,['plots', 'min_rect_rear_edge']].set_index('plots'), how='left',
                      right_index=True, left_index=True)
    road_edges.loc[:,'min_rect_rear_edge'] = road_edges['min_rect_rear_edge'].astype(int)
    min_rect_plots = min_rect_plots.merge(road_edges.apply(is_corner_plot_helper, axis=1).rename('is_corner_plot'), how='left', left_on='plots', right_index=True)
    return min_rect_plots


def is_corner_plot_helper(road_edge_row):
    return len(set(road_edge_row.loc['order']).difference([road_edge_row.loc['min_rect_rear_edge']])) > 1


'''Calculate average width or depth of a plot using min rect front and side edges and stores it in corresponding columns.'''


def find_average_width_or_depth(plot_row, width):
    cur_front_edge = plot_row.loc['min_rect_edges'][int(plot_row.loc['min_rect_front_edge'])]
    cur_side_edge = plot_row.loc['min_rect_edges'][int(plot_row.loc['min_rect_side_edges'][0])]
    if width == 'average_width':
        offset_distances = np.linspace(0, cur_side_edge.length, 12)[1:-1]
        lines = [cur_front_edge.parallel_offset(cur_offset, 'left') for cur_offset in offset_distances]
    else:
        offset_distances = np.linspace(0, cur_front_edge.length, 12)[1:-1]
        lines = [cur_side_edge.parallel_offset(cur_offset, 'left') for cur_offset in offset_distances]
    average_length = round(np.median([plot_row.loc['geometry'].intersection(line).length for line in lines]), 3)
    return average_length


'''Set residential plot properties, e.g. if it is a fringe plot, corner plot, plot's average depth and width.'''


def set_residential_plot_properties(res_plots, plots):
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


def set_road_plot_properties(road_network, road_plots):
    invalid_road_types = ['Cross Junction', 'T-Junction', 'Expunged', 'Other Junction', 'Pedestrian Mall',
                          '2 T-Junction opposite each other', 'Unknown', 'Y-Junction', 'Imaginary Line']
    road_network_valid = road_network[~road_network['RD_TYP_CD'].isin(invalid_road_types)]
    road_cat_dict = {'Expressway': '1',
                     'Semi Expressway': '2-3',
                     'Major Arterials/Minor Arterials': '2-3',
                     'Local Collector/Primary Access': '4',
                     'Local Access': '5',
                     'Slip Road': '5',
                     'Service Road': '5',
                     'no category': 'unknown'}
    road_plots_cat = assign_road_category(road_plots, road_network_valid.copy())
    road_plots_cat['road_category'] = [road_cat_dict[x] for x in road_plots_cat["RD_TYP_CD"]]
    print('road properties set.')
    return road_plots_cat


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


'''Queries plot properties.'''


def get_plot_properties(plots, endpoint):
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
        PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
        SELECT ?plots ?avg_width
        WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace/> {
        OPTIONAL { ?plots obs:hasWidth/om:hasValue/om:hasNumericValue ?avg_width . } } }""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    plot_properties = pd.DataFrame(results['results']['bindings'])
    plot_properties = plot_properties.applymap(lambda cell: cell['value'], na_action='ignore')
    plots = plots.merge(plot_properties, how='left', on='plots')

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
        PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
        SELECT ?plots ?avg_depth
        WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace/> {
        OPTIONAL { ?plots obs:hasDepth/om:hasValue/om:hasNumericValue ?avg_depth . } } }""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    plot_properties = pd.DataFrame(results['results']['bindings'])
    plot_properties = plot_properties.applymap(lambda cell: cell['value'], na_action='ignore')
    plots = plots.merge(plot_properties, how='left', on='plots')

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
    PREFIX oz: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>
    SELECT ?plots ?zone ?corner_plot ?fringe_plot
    WHERE {?plots oz:hasZone ?zone_uri . 
        BIND(STRAFTER(STR(?zone_uri), '#') AS ?zone)
        OPTIONAL { ?plots obs:isCornerPlot ?corner_plot . }
        OPTIONAL { ?plots obs:atResidentialFringe ?fringe_plot . } }""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    plot_properties = pd.DataFrame(results['results']['bindings'])
    plot_properties = plot_properties.applymap(lambda cell: cell['value'], na_action='ignore')
    plots = plots.merge(plot_properties, how='left', on='plots')
    plots['avg_width'] = pd.to_numeric(plots['avg_width'], errors='coerce')
    plots['avg_depth'] = pd.to_numeric(plots['avg_depth'], errors='coerce')
    return plots


'''Queries residential plot neighbour types: road type, zone type and whether neighbour is in a god class bungalow area.'''


def get_plot_neighbour_types(plots, endpoint):
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX oz: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>
    SELECT ?plots (GROUP_CONCAT(?type; separator=",") as ?neighbour_road_type) (GROUP_CONCAT(DISTINCT ?zone; separator=",") as ?neighbour_zones) (COUNT(?gcba) AS ?abuts_gcba)
    WHERE { ?plots obs:hasNeighbour ?neighbour .
    ?neighbour oz:hasZone ?zone_uri .
    BIND(STRAFTER(STR(?zone_uri), '#') AS ?zone)
    OPTIONAL { ?neighbour obs:hasRoadType ?type . }
    OPTIONAL { ?gcba opr:appliesTo ?neighbour ;
    rdf:type opr:GoodClassBungalowArea . }} 
    GROUP By ?plots""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    neighbours = pd.DataFrame(results['results']['bindings'])
    neighbours = neighbours.applymap(lambda cell: cell['value'], na_action='ignore')
    plots = plots.merge(neighbours, how='left', on='plots')
    plots['neighbour_road_type'] = [plots.loc[i, 'neighbour_road_type'].split(',') if not pd.isnull(plots.loc[i, 'neighbour_road_type']) else [] for i in plots.index]
    plots['neighbour_zones'] = [plots.loc[i, 'neighbour_zones'].split(',') if not pd.isnull(plots.loc[i, 'neighbour_zones']) else [] for i in plots.index]
    plots['abuts_gcba'] = pd.to_numeric(plots['abuts_gcba'], errors='ignore')
    return plots


'''Queries allowed development types on residential plots based on regulations.'''


def get_plot_allowed_programmes(plots, endpoint):
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX oz: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>
    SELECT ?plots (GROUP_CONCAT(DISTINCT ?pb; separator=',') AS ?in_pb) (GROUP_CONCAT(DISTINCT ?sbp_programme_name; separator=",") as ?sbp_programmes) (GROUP_CONCAT(DISTINCT ?lha) AS ?in_lha) (GROUP_CONCAT(DISTINCT ?lha_programme_name; separator=",") as ?lha_programmes) (COUNT(?gcba) as ?in_gcba)
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/> 
              { ?plots oz:hasZone ?zone . }
    OPTIONAL  { ?sbp rdf:type opr:StreetBlockPlan ;
                    opr:appliesTo ?plots ;
                    oz:allowsProgramme ?sbp_programme . 
    BIND(STRAFTER(STR(?sbp_programme), '#') as ?sbp_programme_name)}
    OPTIONAL {?lha rdf:type opr:LandedHousingArea ;
                    opr:appliesTo ?plots ;
                    oz:allowsProgramme ?lha_programme .
    BIND(STRAFTER(STR(?lha_programme), '#') as ?lha_programme_name)}
    OPTIONAL  {?gcba rdf:type opr:GoodClassBungalowArea ;
                    opr:appliesTo ?plots . }
    OPTIONAL {?pb opr:appliesTo ?plots;
                  rdf:type opr:PlanningBoundary.  }  }
    GROUP BY ?plots""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    applicable_regulations = pd.DataFrame(results['results']['bindings'])
    applicable_regulations = applicable_regulations.applymap(lambda cell: cell['value'])
    plots = plots.merge(applicable_regulations, how='left', on='plots')
    plots['sbp_programmes'] = [plots.loc[i, 'sbp_programmes'].split(',') if not pd.isnull(plots.loc[i, 'sbp_programmes']) else [] for i in plots.index]
    plots['lha_programmes'] = [plots.loc[i, 'lha_programmes'].split(',') if not pd.isnull(plots.loc[i, 'lha_programmes']) else [] for i in plots.index]
    plots['in_pb'] = [plots.loc[i, 'in_pb'].split(',') if not pd.isnull(plots.loc[i, 'in_pb']) else [] for i in plots.index]
    plots['in_lha'] = [plots.loc[i, 'in_lha'].split(',') if not pd.isnull(plots.loc[i, 'in_lha']) else [] for i in plots.index]
    plots['in_gcba'] = pd.to_numeric(plots['in_gcba'], errors='ignore')
    return plots


'''Finds allowed residential development types on plots.'''


def find_allowed_residential_types(plots, road_list):
    zone_list = ['Residential', 'ResidentialWithCommercialAtFirstStorey', 'CommercialAndResidential',
                 'ResidentialOrInstitution', 'White', 'BusinesParkWhite', 'Business1White', 'Business2White']
    mixed_zone_list = ['ResidentialWithCommercialAtFirstStorey', 'CommercialAndResidential', 'White',
                       'BusinesParkWhite', 'Business1White', 'Business2White']
    allowed_types = []
    for i in plots.index:
        zone = plots.loc[i, 'zone']
        cur_allowed_types = []
        if zone in zone_list:
            lha_programmes = plots.loc[i, 'lha_programmes']
            in_lha = '' not in lha_programmes
            in_gcba = plots.loc[i, 'in_gcba'] > 0
            sbp_programmes = plots.loc[i, 'sbp_programmes']
            area = plots.loc[i, 'plot_area']
            width = plots.loc[i, 'avg_width'] if not pd.isnull(plots.loc[i, 'avg_width']) else 0
            depth = plots.loc[i, 'avg_depth'] if not pd.isnull(plots.loc[i, 'avg_depth']) else 0
            is_corner = plots.loc[i, 'corner_plot'] == 'true'
            at_fringe = plots.loc[i, 'fringe_plot'] == 'true'
            road_condition = len(set(plots.loc[i, 'neighbour_road_type']).intersection(road_list)) > 0
            allowed_programmes = sbp_programmes + lha_programmes

            # filter for Bungalow
            b_geo_condition = (area >= 400) and (width >= 10)
            if b_geo_condition and ((zone == 'Residential') or ('Bungalow' in (sbp_programmes or lha_programmes))) and (not in_gcba):
                cur_allowed_types.append('Bungalow')
            # filter for Semi-Detached House
            sdh_geo_condition = (area >= 200) and (width >= 8)
            if sdh_geo_condition and ((zone == 'Residential') or ('Semi-DetachedHouse' in allowed_programmes)) and (not in_gcba):
                cur_allowed_types.append('Semi-DetachedHouse')
            # filter for Terrace Type 1
            t1_geo_condition_inner = (area >= 150) and (width >= 6) and (not is_corner)
            t1_geo_condition_corner = (area >= 200) and (width >= 8) and is_corner
            if (t1_geo_condition_inner or t1_geo_condition_corner) and ((zone == 'Residential') or (('TerraceHouse' or 'TerraceType1') in allowed_programmes)) and (not in_gcba):
                cur_allowed_types.append('TerraceType1')
            # filter for Terrace Type 2
            t2_geo_condition_inner = area >= 80 and width >= 6 and not is_corner
            t2_geo_condition_corner = area >= 80 and width >= 8 and is_corner
            if (t2_geo_condition_inner or t2_geo_condition_corner) and ((zone == 'Residential') or (('TerraceHouse' or 'TerraceType2') in allowed_programmes)) and (not in_gcba):
                cur_allowed_types.append('TerraceType2')
            # filter for Good Class Bungalow type
            gcb_geo_condition = (area >= 1400) and (width >= 18.5) and (depth >= 30)
            if gcb_geo_condition and (in_gcba or ('GoodClassBungalow' in sbp_programmes)):
                cur_allowed_types.append('GoodClassBungalow')
            # filter for Flats, Condominiums and Serviced Apartments type
            if (area >= 1000) and ((zone in zone_list) or ('Flat' in sbp_programmes)) and (not in_gcba) and (not in_lha):
                cur_allowed_types.append('Flat')
            if (area >= 4000) and (zone in ['Residential', 'ResidentialOrInstitution']) and (not in_gcba) and (not in_lha):
                cur_allowed_types.append('Condominium')
            if (zone == 'Residential') and (not in_gcba) and (not in_lha) and at_fringe and road_condition:
                cur_allowed_types.append('ServicedApartmentResidentialZone')
            if (zone in mixed_zone_list) and (not in_gcba) and (not in_lha) and at_fringe:
                cur_allowed_types.append('ServicedApartmentMixedUseZone')
            allowed_types.append(cur_allowed_types)
        else:
            allowed_types.append(cur_allowed_types)
    plots['allowed_residential_types'] = allowed_types
    return plots

'''gets all relevant plot information to link it to type based regulations.'''

def get_plot_information(plots, endpoint, road_list):
    plots = plots.drop(columns=['zone'])
    plots = get_plot_properties(plots, endpoint)
    print('plot properties retrieved.')
    plots = get_plot_neighbour_types(plots, endpoint)
    print('plot neighbour properties retrieved.')
    plots = get_plot_allowed_programmes(plots, endpoint)
    print('plot allowed programmes by area regulations retrieved.')
    plots = find_allowed_residential_types(plots, road_list)
    print('plot allowed residential types found.')
    return plots

'''Retrieves type based planning regulations.'''


def get_type_regulations(endpoint):
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX oz: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>
    PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
    SELECT ?reg (GROUP_CONCAT(DISTINCT ?zone; separator=",") AS ?for_zones) (SAMPLE(?gpr_value) AS ?requires_gpr) (SAMPLE(?function) AS ?gpr_function) (SAMPLE(?fringe) AS ?for_fringe_plot) (SAMPLE(?corner) AS ?for_corner_plot)  (SAMPLE(?gcba) AS ?abuts_gcba) (SAMPLE(?gcba_in) AS ?in_gcba)  (SAMPLE(?road) AS ?abuts_road) (GROUP_CONCAT(DISTINCT ?programme; separator=",") AS ?for_programme)  (GROUP_CONCAT(?neighbour_zone; separator=",") AS ?neighbour_zones) (GROUP_CONCAT(?areas; separator=",") AS ?in_area_regs)
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/planningregulations/>  {
             ?reg rdf:type opr:DevelopmentControlPlan . }
      OPTIONAL { ?reg opr:forZoningType ?zone_type_uri .
               BIND(STRAFTER(STR(?zone_type_uri), '#') AS ?zone)}
      OPTIONAL { ?reg opr:forNeighbourZoneType ?neighbour_zone_uri . 
               BIND(STRAFTER(STR(?neighbour_zone_uri), '#') AS ?neighbour_zone)}
      OPTIONAL { ?reg opr:plotAbuts1-3RoadCategory ?road . }
      OPTIONAL { ?reg opr:plotAbutsGoodClassBungalowArea ?gcba .}
      OPTIONAL { ?reg opr:plotInGoodClassBungalowArea ?gcba_in .}
      OPTIONAL { ?reg opr:forPlotContainedIn ?areas .}   
      OPTIONAL { ?reg opr:forFringePlot ?fringe . }
      OPTIONAL { ?reg opr:forCornerPlot ?corner . }
      OPTIONAL { ?reg opr:forProgramme ?programme_uri .
               BIND(STRAFTER(STR(?programme_uri), '#') AS ?programme)}
      OPTIONAL {?reg opr:allowsGrossPlotRatio ?gpr.
                ?gpr opr:hasValue ?gpr_value. 
                OPTIONAL {?gpr om:hasAggregateFunction ?function . } }}
    GROUP BY ?reg""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    type_regs = pd.DataFrame(results['results']['bindings'])
    type_regs = type_regs.applymap(lambda cell: cell['value'], na_action='ignore')
    type_regs = type_regs.replace('', np.nan, regex=True)
    type_regs['for_fringe_plot'] = type_regs['for_fringe_plot'].fillna(False).astype(bool)
    type_regs['for_corner_plot'] = type_regs['for_corner_plot'].fillna(False).astype(bool)
    type_regs['abuts_road'] = type_regs['abuts_road'].fillna(False).astype(bool)
    type_regs['for_zones'] = [i.split(',') for i in type_regs['for_zones']]
    type_regs['requires_gpr'] = pd.to_numeric(type_regs['requires_gpr'])
    type_regs['neighbour_zones'] = [type_regs.loc[i, 'neighbour_zones'].split(',') if not pd.isnull(type_regs.loc[i, 'neighbour_zones']) else [] for i in type_regs.index]
    type_regs['in_area_regs'] = [type_regs.loc[i, 'in_area_regs'].split(',') if not pd.isnull(type_regs.loc[i, 'in_area_regs']) else [] for i in type_regs.index]
    type_regs['abuts_gcba'] = pd.to_numeric(type_regs['abuts_gcba'], errors='ignore')
    type_regs['in_gcba'] = pd.to_numeric(type_regs['in_gcba'], errors='ignore')
    return type_regs


'''Links type regulations to plots based on set of conditions.'''


def link_type_regulations_to_plots(regs, plots, road_list):
    reg_plots = []
    for i in regs.index:
        applies_to_plots = plots['zone'].isin(regs.loc[i, 'for_zones'])
        if regs.loc[i, 'for_programme'] in ['Semi-DetahcedHouse', 'Bungalow', 'TerraceType1', 'TerraceType2', 'GoodClassBungalow']:
            applies_to_plots = applies_to_plots & [regs.loc[i, 'for_programme'] in plots.loc[j, 'allowed_residential_types'] for j in plots.index]
        if regs.loc[i, 'for_programme'] in ['Flat', 'Condominium']:
            if not pd.isnull(regs.loc[i, 'gpr_function']):
                applies_to_plots = applies_to_plots & (plots['gpr'] > regs.loc[i, 'requires_gpr'])
            else:
                applies_to_plots = applies_to_plots & (plots['gpr'] == regs.loc[i, 'requires_gpr'])
        if regs.loc[i, 'in_area_regs']:
            pb_condition = plots['in_pb'].apply(lambda in_pb: len(set(in_pb).intersection(regs.loc[i, 'in_area_regs'])) > 0)
            lha_condition = plots['in_lha'].apply( lambda in_lha: len(set(in_lha).intersection(regs.loc[i, 'in_area_regs'])) > 0)
            applies_to_plots = applies_to_plots & (pb_condition | lha_condition)
        if regs.loc[i, 'for_fringe_plot']:
            applies_to_plots = applies_to_plots & (plots['fringe_plot']=='true')
        if regs.loc[i, 'for_corner_plot']:
            applies_to_plots = applies_to_plots & (plots['corner_plot']=='true')
        if regs.loc[i, 'abuts_road']:
            road_condition = plots['neighbour_road_type'].apply(lambda neighbor_road_types: len(set(neighbor_road_types).intersection(road_list)) > 0)
            applies_to_plots = applies_to_plots & road_condition
        if regs.loc[i, 'abuts_gcba'] == 'true':
            abuts_gcba = plots['abuts_gcba'].apply(lambda abuts_gcba: abuts_gcba > 0 if (not pd.isnull(abuts_gcba)) else False)
            applies_to_plots = applies_to_plots & abuts_gcba
        if regs.loc[i, 'in_gcba'] == 'true':
            in_gcba = plots['in_gcba'].apply(lambda gcba: int(gcba) > 0 if (not pd.isnull(gcba)) else False)
            applies_to_plots = applies_to_plots & in_gcba
        if len(regs.loc[i, 'neighbour_zones']) > 1:
            neighbour_zone_condition = plots['neighbour_zones'].apply(lambda neighbor_zones: len(set(neighbor_zones).intersection(regs.loc[i, 'neighbour_zones'])) > 0)
            applies_to_plots = applies_to_plots & neighbour_zone_condition
        reg_plots.append(list(plots.loc[applies_to_plots, 'plots']))
    regs['applies_to'] = reg_plots
    return regs

class TripleDataset:

    def __init__(self):
        self.dataset = Dataset()

    '''A method to generate necessary triples to represent plot area.'''
    def create_site_area_triples(self, plots):
        for i in plots.index:
            city_obj = URIRef(plots.loc[i, 'plots'])
            if not pd.isna(plots.loc[i, 'plot_area']):
                area_value = Literal(str(plots.loc[i, 'plot_area']), datatype=XSD.decimal)
                site_area = URIRef(URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1())))
                measure = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
                self.dataset.add((site_area, RDF.type, GFAOntoManager.SITE_AREA, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
                self.dataset.add((city_obj, GFAOntoManager.HAS_SITE_AREA, site_area, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
                self.dataset.add((site_area, GFAOntoManager.HAS_VALUE, measure, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
                self.dataset.add((site_area, GFAOntoManager.HAS_UNIT, GFAOntoManager.SQUARE_PREFIXED_METRE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
                self.dataset.add((measure, GFAOntoManager.HAS_NUMERIC_VALUE, area_value, GFAOntoManager.BUILDABLE_SPACE_GRAPH))

    '''A method to generate necessary triples to represent plot's average width.'''
    def create_average_width_triples(self, city_obj, width_value):
        avg_width = URIRef(URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1())))
        measure = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        self.dataset.add((city_obj, GFAOntoManager.HAS_WIDTH, avg_width, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((avg_width, RDF.type, GFAOntoManager.AVERAGE_WIDTH, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((avg_width, GFAOntoManager.HAS_VALUE, measure, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((measure, GFAOntoManager.HAS_UNIT, GFAOntoManager.METRE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((measure, GFAOntoManager.HAS_NUMERIC_VALUE, width_value, GFAOntoManager.BUILDABLE_SPACE_GRAPH))

    '''A method to generate necessary triples to represent plot's average depth.'''
    def create_average_depth_triples(self, city_obj, depth_value):
        avg_depth = URIRef(URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1())))
        measure = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        self.dataset.add((city_obj, GFAOntoManager.HAS_DEPTH, avg_depth, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((avg_depth, RDF.type, GFAOntoManager.AVERAGE_DEPTH, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((avg_depth, GFAOntoManager.HAS_VALUE, measure, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((measure, GFAOntoManager.HAS_UNIT, GFAOntoManager.METRE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((measure, GFAOntoManager.HAS_NUMERIC_VALUE, depth_value, GFAOntoManager.BUILDABLE_SPACE_GRAPH))

    '''A method to generate necessary triples to represent residential plot properties.'''
    def create_residential_plot_property_triples(self, res_plots, plots):
        res_plots_df = set_residential_plot_properties(res_plots, plots)
        for i in res_plots_df.index:
            city_obj = URIRef(res_plots_df.loc[i, 'plots'])
            if not pd.isna(res_plots_df.loc[i, 'fringe']):
                at_fringe = Literal(str(res_plots_df.loc[i, 'fringe']), datatype=XSD.boolean)
                self.dataset.add((city_obj, GFAOntoManager.IS_AT_RESIDENTIAL_FRINGE, at_fringe, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            if not pd.isna(res_plots_df.loc[i, 'average_width']):
                width_value = Literal(str(res_plots_df.loc[i, 'average_width']), datatype=XSD.decimal)
                self.create_average_width_triples(city_obj, width_value)
            if not pd.isna(res_plots_df.loc[i, 'average_depth']):
                depth_value = Literal(str(res_plots_df.loc[i, 'average_depth']), datatype=XSD.decimal)
                self.create_average_depth_triples(city_obj, depth_value)
            if not pd.isna(res_plots_df.loc[i, 'is_corner_plot']):
                corner_plot = Literal(str(res_plots_df.loc[i, 'is_corner_plot']), datatype=XSD.boolean)
                self.dataset.add((city_obj, GFAOntoManager.IS_CORNER_PLOT, corner_plot, GFAOntoManager.BUILDABLE_SPACE_GRAPH))

    '''A method to generate necessary triples to represent road plot properties.'''
    def create_road_plot_property_triples(self, road_network, road_plots):
        road_plots = set_road_plot_properties(road_network, road_plots)
        for i in road_plots.index:
            city_obj_uri = URIRef(road_plots.loc[i, 'plots'])
            if not pd.isna(road_plots.loc[i, 'RD_TYP_CD']):
                road_type = Literal(str(road_plots.loc[i, 'RD_TYP_CD']), datatype=XSD.string)
                self.dataset.add((city_obj_uri, GFAOntoManager.HAS_ROAD_TYPE, road_type, GFAOntoManager.BUILDABLE_SPACE_GRAPH))

    '''Generates necessary triples to represent links between type regulations and plots.'''
    def create_type_regulation_overlap_triples(self, type_regs):
        for i in type_regs.index:
            for j in type_regs.loc[i, 'applies_to']:
                self.dataset.add((URIRef(type_regs.loc[i, 'reg']), GFAOntoManager.APPLIES_TO, URIRef(j), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''A method to write the aggregated triple dataset into a nquad(text) file.'''
    def write_triples(self, triple_type):
        with open(cur_dir + "/output_" + triple_type + ".nq", mode="wb") as file:
            file.write(self.dataset.serialize(format='nquads'))


'''Writes generated residential plot property triples into a nquad file.'''


def instantiate_plot_property_triples(res_plots, plots, road_network, road_plots):
    plot_properties = TripleDataset()
    plot_properties.create_residential_plot_property_triples(res_plots, plots)
    print('residential plot property triples created.')
    plot_properties.create_site_area_triples(plots)
    print('plot site area triples created.')
    plot_properties.create_road_plot_property_triples(road_network, road_plots)
    print("road plot properties triples creates.")
    plot_properties.write_triples("plot_properties_triples")
    print("plot properties nquads written.")


'''Write generated triples of plot and type-based regulation overlap into a nquad file.'''

def instantiate_type_regulation_and_plot_triples(type_regs):
    type_regulation_links = TripleDataset()
    type_regulation_links.create_type_regulation_overlap_triples(type_regs)
    print('type regulation and plot overlap triples created.')
    type_regulation_links.write_triples("type_regulation_overlap_triples")
    print("plot properties nquads written.")

if __name__ == "__main__":

    local_endpoint = "http://192.168.0.143:9999/blazegraph/namespace/regulationcontent/sparql"
    plots = get_plots("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql")
    print('plots retrieved')

    road_plots = plots[plots['zone'] == 'ROAD']
    residential_plots = plots[plots['zone'].isin(['RESIDENTIAL', 'RESIDENTIAL / INSTITUTION', 'COMMERCIAL & RESIDENTIAL',
                                                 'RESIDENTIAL WITH COMMERCIAL AT 1ST STOREY', 'WHITE', 'BUSINESS PARK - WHITE',
                                                  'BUSINESS 1 - WHITE', 'BUSINESS 2 - WHITE'])]
    road_list = ['Expressway', 'Semi Expressway', 'Major Arterials/Minor Arterials']
    #road_network = gpd.read_file("C:/Users/AydaGrisiute/Desktop/demonstrator/roads/roads_whole_SG/roads.shp").to_crs(3857)
    #road_network = road_network[['RD_NAME', 'RD_TYP_CD', 'LVL_OF_RD', 'UNIQUE_ID', 'geometry']].copy()
    #print('road network retrieved')
    #instantiate_plot_property_triples(residential_plots, plots, road_network, road_plots)
    #print('plot property triples written to file.')

    plots = get_plot_information(plots, local_endpoint, road_list)
    type_regulations = get_type_regulations(local_endpoint)
    print('type regulations retrieved.')
    type_regulations = link_type_regulations_to_plots(type_regulations, plots, road_list)
    print('type regulations linked to plots.')
    instantiate_type_regulation_and_plot_triples(type_regulations)
    print('type regulation links to plots instantiated.')
