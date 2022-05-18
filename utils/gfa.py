from typing import List
import pandas as pd
import numpy as np
from shapely.geometry import LineString
import sys
from shapely.ops import unary_union
import math
import geopandas as gpd
import json

"""
Helper functions to process dataframes, e.g. filter.
"""
def to_list(dataframe, column):
    final_list = []
    for i in dataframe.index:
        list_values = str(dataframe.loc[i, column]).split(';')
        final_list.append([float(x) for x in list_values])
    dataframe[column] = final_list
    return dataframe


def to_bool(dataframe, column):
    for i in dataframe.index:
        if dataframe.loc[i, column] == 1.0:
            dataframe.loc[i, column] = True
        elif dataframe.loc[i, column] == 0.0:
            dataframe.loc[i, column] = False
    return dataframe


def read_description_table(table):
    df = pd.read_html(table, skiprows=1, flavor="html5lib")[0].T
    attributes = df.reset_index().T.rename(columns={0:"name",1:"value"}).reset_index(drop=True).set_index("name").to_dict()["value"]
    return pd.Series(attributes)


def get_edges(polygon):
    curve = polygon.exterior.simplify(0.1)
    return list(map(LineString, zip(curve.coords[:-1], curve.coords[1:])))


def is_narrow(geometry, width, ratio):
    edges = get_edges(geometry.minimum_rotated_rectangle)
    lengths = [e.length for e in edges]
    return (np.min(lengths) < width) or ((np.min(lengths) / np.max(lengths)) < ratio)


"""
Functions that sets necessary params, like GPR for particular zoning types, or road categories.
"""


def intersect_with_regulation(all_plots, regulation, valid_overlap):

    intersection = gpd.overlay(all_plots, regulation, how='intersection', keep_geom_type=True)
    intersection['intersection_area'] = intersection.area
    overlap_ratio = intersection['intersection_area'] / intersection['site_area']
    intersection = intersection.loc[overlap_ratio >= valid_overlap, :]
    return intersection


def assign_road_category(road_plots, rn_lta):
    rn_lta['buffered_network'] = rn_lta.buffer(3)
    rn_lta_buffered = rn_lta.set_geometry(col='buffered_network', drop=True)

    rn_int = gpd.overlay(road_plots, rn_lta_buffered, how='intersection', keep_geom_type=True)
    rn_int['intersection_area'] = rn_int.area
    grouped_intersection = rn_int.groupby(['PlotId']).apply(
        lambda x: x.sort_values(['intersection_area'], ascending=False).iloc[0, :]).drop(columns=['PlotId'])
    grouped_intersection = grouped_intersection.reset_index()
    grouped_intersection = grouped_intersection[['PlotId', 'RD_TYP_CD']].copy()

    road_plots = road_plots.merge(grouped_intersection, on='PlotId', how='left')
    road_plots.loc[road_plots["RD_TYP_CD"].isna(), "RD_TYP_CD"] = "no category"
    return road_plots


def _reset_road_setbacks(plot_row, setbacks, road_setbacks):
    for category, setback in road_setbacks.items():
        for i in plot_row.loc[category]:
            for edge_setback in setbacks.keys():
                setbacks[edge_setback][i] = setback
    return setbacks


def set_road_buffer_edges(plots_for_GFA, road_plots, road_categories):
    for road_category in road_categories.keys():
        plots_for_GFA[road_category] = [[]] * plots_for_GFA.shape[0]

    count = 0
    for count, i in enumerate(plots_for_GFA.index):
        edge_assigned = []
        for edge_index, edge in enumerate(plots_for_GFA.loc[i, "edges"]):
            if edge_index not in edge_assigned:
                buffered_edge = edge.buffer(3, single_sided=True)
                cat_int = (road_plots.geometry.intersection(buffered_edge)
                           .apply(lambda g: g.area)) / buffered_edge.area
                road_intersections = cat_int.index[cat_int > 0.3]
                if not road_intersections.empty:
                    road_plot_index = cat_int.loc[road_intersections].sort_values(ascending=False).index[0]
                    road_type = road_plots.loc[road_plot_index, "RD_TYP_CD"]
                    edge_road_category = None
                    for road_category in road_categories.keys():
                        if road_type in road_categories[road_category]:
                            edge_road_category = road_category
                            break
                    if edge_road_category is not None:
                        edge_assigned.append(edge_index)
                        plots_for_GFA.at[i, edge_road_category] = plots_for_GFA.at[i, edge_road_category] + [edge_index]
        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, plots_for_GFA.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, plots_for_GFA.shape[0]))

    return plots_for_GFA


def neighbor_zone_type(plot_geometry, all_plots):
    return list(all_plots.loc[all_plots.geometry.intersects(plot_geometry.buffer(1)), 'PlotType'])


def assign_gpr(plots, plot_type, lh, buffer, gpr1, gpr2, gpr3, storeys2, storeys3):
    for plot in plots.loc[plots['PlotType'].isin([plot_type])].index:

        lh_cont = lh.geometry.contains(plots.loc[plot, 'geometry'])
        lh_int = lh.geometry.intersects(plots.loc[plot, 'geometry'].buffer(buffer))
        context_int = plots.geometry.intersects(plots.loc[plot, 'geometry'].buffer(buffer))

        # contained within landed housing boundary case
        if lh_cont.any():
            plots.loc[plot, 'GPR'] = gpr1
            plots.loc[plot, 'context_storeys'] = lh.loc[lh_cont, 'STY_HT'].min()

        # instersection with the landed housing fringe case
        elif lh_int.any():
            plots.loc[plot, 'GPR'] = gpr2
            plots.loc[plot, 'context_storeys'] = storeys2

        # intersection with surrouding area case
        elif (plots.loc[context_int, 'GPR'].mean() > 1.4) or (plots.loc[context_int, 'PlotType'].isin(['BUSINESS 1', 'BUSINESS 2', 'BUSINESS PARK']).any()):
            plots.loc[plot, 'GPR'] = gpr3
            plots.loc[plot, 'context_storeys'] = storeys3


"""
Functions that sets or retrieves main parameters for GFA calculation.
"""


def set_partywall(plots, sb_int, udr_int):
    plots['Party_Wall'] = False
    count = 0

    for count, i in enumerate(plots.index):
        party_wall = False

        plot_id = plots.loc[i, 'PlotId']
        street_block = sb_int.loc[sb_int['PlotId'] == plot_id, :]
        urban_guidelines = udr_int.loc[udr_int['PlotId'] == plot_id, :]
        if not urban_guidelines.empty:
            party_wall = party_wall or urban_guidelines['Party_Wall'].any()
        if not street_block.empty:
            party_wall = party_wall or street_block['Party_Wall'].any()
        plots.loc[i, 'Party_Wall'] = party_wall
        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, plots.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, plots.shape[0]))
    return plots


def set_partywall_edges(plots_for_GFA, plots):
    plots_for_GFA['edges'] = plots_for_GFA.geometry.apply(get_edges)
    plots_for_GFA['partywall_edges'] = [[]] * plots_for_GFA.shape[0]

    for i in plots_for_GFA.index[plots_for_GFA['Party_Wall']]:
        partywall_edges = []
        for edge_index, edge in enumerate(plots_for_GFA.loc[i, "edges"]):
            buffered_edge = edge.buffer(1, single_sided=True)
            partywall_plot_int = (plots.loc[plots['Party_Wall']]
                                  .geometry
                                  .intersection(buffered_edge).apply(lambda g: g.area))
            partywall_plot_int = partywall_plot_int / buffered_edge.area
            partywall_plot_int = partywall_plot_int.drop(index=[i])
            partywall_plot_int = partywall_plot_int > 0.5
            if partywall_plot_int.any():
                partywall_edges.append(edge_index)
        plots_for_GFA.at[i, "partywall_edges"] = partywall_edges
    return plots_for_GFA


def retrieve_number_of_storeys(plots_for_GFA, hc_int, sb_int, udr_int):
    count = 0
    for count, i in enumerate(plots_for_GFA.index):
        plot_id = plots_for_GFA.loc[i, 'PlotId']
        height_control = hc_int.loc[hc_int['PlotId'] == plot_id, :]
        street_block = sb_int.loc[sb_int['PlotId'] == plot_id, :]
        urban_guidelines = udr_int.loc[udr_int['PlotId'] == plot_id, :].copy()
        urban_guidelines = urban_guidelines.loc[~urban_guidelines["Storeys"].isna()]

        if not pd.isna(plots_for_GFA.loc[i, 'context_storeys']):
            min_storey = plots_for_GFA.loc[i, 'context_storeys']
        else:
            min_storey = float("inf")
        parts = []
        storeys = []
        if not street_block.empty:
            min_storey = min(min_storey, street_block["Storeys"].min())

        # covers scenarios where there are more than one plot part with different number of storeys.
        if (height_control.shape[0] > 1) or (urban_guidelines.shape[0] > 1):
            if height_control.shape[0] > urban_guidelines.shape[0]:
                parts = list(height_control.geometry)
                storeys = [min(min_storey, storey) for storey in height_control["BLD_HT_STY"]]
                if not urban_guidelines.empty:
                    for part, part_geometry in enumerate(parts):
                        other_min = urban_guidelines["Storeys"].iloc[(urban_guidelines.geometry.intersection(part_geometry).area.argsort().iloc[-1])]
                        storeys[part] = min(storeys[part], other_min)
            else:
                parts = list(urban_guidelines.geometry)
                storeys = [min(min_storey, storey) for storey in urban_guidelines["Storeys"]]
                if not height_control.empty:
                    for part, part_geometry in enumerate(parts):
                        other_min = height_control["BLD_HT_STY"].iloc[(height_control.geometry.intersection(part_geometry).area.argsort().iloc[-1])]
                        storeys[part] = min(storeys[part], other_min)

        # covers scenarios where height regulation applies to whole plot area.
        else:
            if not height_control.empty:
                min_storey = min(min_storey, height_control["BLD_HT_STY"].min())
            if not urban_guidelines.empty:
                min_storey = min(min_storey, urban_guidelines["Storeys"].min())
            if not math.isinf(min_storey):
                parts = [plots_for_GFA.loc[i, "geometry"]]
                storeys = [min_storey]
        if not pd.isna(storeys).any():
            plots_for_GFA.at[i, "storeys"] = storeys
            plots_for_GFA.at[i, "parts"] = parts

        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, plots_for_GFA.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, plots_for_GFA.shape[0]))

    return plots_for_GFA


def retrieve_edge_setback(plots_for_GFA: pd.DataFrame, plots: pd.DataFrame, dcp: pd.DataFrame, sb_int: pd.DataFrame,
                          udr: pd.DataFrame, plots_in_control: List[str], plots_in_streetblocks: List[str],
                          plots_in_urban_design_guidelines: List[str]) -> dict:
    plot_setbacks = {plot_id: {} for plot_id in plots_for_GFA['PlotId'].unique()}
    count = 0

    for count, plot in enumerate(plots_for_GFA.index):
        plot_row = plots_for_GFA.loc[plot, :]
        plot_id = plot_row.loc["PlotId"]
        number_of_edges = len(plot_row["edges"])
        setbacks = {'default': [0] * number_of_edges}

        # set setbacks if plot is not in street block plans.
        if plot_id in plots_in_control:
            plot_control = dcp.loc[dcp["Zone"] == plot_row.loc["PlotType"]]
            for setback_type in plot_control["type_property"].unique():
                plot_control_type = plot_control[plot_control["type_property"] == setback_type]
                if plot_control_type.shape[0] > 1:
                    neighbor_types = set(neighbor_zone_type(plot_row.loc["geometry"], plots))
                    plot_control_with_context = plot_control_type.loc[~(plot_control_type["type_context"] == "default"), :].iloc[0]
                    zoning_types = plot_control_with_context.loc["type_context"].split(";")
                    plot_control_without_context = plot_control_type.loc[plot_control_type["type_context"] == "default", :].iloc[0]
                    if np.any([neighbor_type in zoning_types for neighbor_type in neighbor_types]):
                        setbacks['default'] = [plot_control_with_context.loc["setback_common"]] * number_of_edges
                    else:
                        setbacks['default'] = [plot_control_without_context.loc["setback_common"]] * number_of_edges
                else:
                    setbacks[setback_type] = [plot_control_type["setback_common"].iloc[0]] * number_of_edges

            # reset edge setback to road category buffer for edges in road category columns.
            if plot_row.loc['PlotType'] == 'EDUCATIONAL INSTITUTION':
                if np.any((np.array(plot_row.loc['storeys']) >= 6)):
                    setbacks = _reset_road_setbacks(plot_row, setbacks, {'cat_1_edges': 30, 'cat_2_edges': 15,
                                                                         'cat_3_5_edges': 7.5, 'backlane_edges': 0})
                else:
                    setbacks = _reset_road_setbacks(plot_row, setbacks, {'cat_1_edges': 24, 'cat_2_edges': 12,
                                                                         'cat_3_5_edges': 7.5, 'backlane_edges': 0})
            else:
                setbacks = _reset_road_setbacks(plot_row, setbacks, {'cat_1_edges': 15, 'cat_2_edges': 7.5,
                                                                     'cat_3_5_edges': 5, 'backlane_edges': 0})

        #  set setbacks if plot is in street block plans
        if plot_id in plots_in_streetblocks:
            street_block = sb_int.loc[sb_int['PlotId'] == plot_id, :].iloc[0]
            for i, edge in enumerate(plot_row.loc['edges']):
                edge_type = find_edge_type(edge, get_edges(plot_row.loc["geometry"].minimum_rotated_rectangle))
                if edge_type in street_block.index:
                    cur_setbacks = street_block.loc[edge_type]
                    if not pd.isna(cur_setbacks).all():
                        if len(cur_setbacks) == 1:
                            cur_setbacks = cur_setbacks[0]
                        for setback in setbacks.keys():
                            setbacks[setback][i] = cur_setbacks

        #  set setbacks if urban guideline setbacks apply
        if plot_id in plots_in_urban_design_guidelines:
            for i, edge in enumerate(plot_row.loc['edges']):
                edge_setback = find_udr_edge_setback(edge, udr)
                if edge_setback is not None:
                    for setback in setbacks.keys():
                        setbacks[setback][i] = edge_setback

        # reset edge setback to 0 if they are partywall edges.
        if plot_row["Party_Wall"]:
            for i in plot_row.loc["partywall_edges"]:
                for setback in setbacks.keys():
                    setbacks[setback][i] = 0
        plot_setbacks[plot_id] = setbacks

        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, plots_for_GFA.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, plots_for_GFA.shape[0]))
    return plot_setbacks


def find_udr_edge_setback(edge, udr):
    buffered_edge = edge.buffer(3, single_sided=True)
    udr_int = (udr.loc[~udr["Setbacks"].isna(), :].geometry.intersection(buffered_edge)
               .apply(lambda g: g.area)) / buffered_edge.area
    udr_valid_int = udr_int.index[udr_int > 0.3]
    setback = None
    if not udr_valid_int.empty:
        udr_index = udr_int.loc[udr_valid_int].sort_values(ascending=False).index[0]
        setback = udr.loc[udr_index, "Setbacks"]
    return setback


def find_edge_type(edge, rectangle_edges) -> str:
    types = ["nan"] * 4
    edge_index_sorted_by_length = np.argsort([edge.length for edge in rectangle_edges])
    types[edge_index_sorted_by_length[-1]] = "Setback_Side"
    types[edge_index_sorted_by_length[-2]] = "Setback_Side"
    types[edge_index_sorted_by_length[0]] = "Setback_Front"
    types[edge_index_sorted_by_length[1]] = "Setback_Rear"
    max_intersection = 0
    edge_type = 'nan'
    for i, rectangle_edge in enumerate(rectangle_edges):
        buffered_edge = edge.buffer(1, single_sided=True)
        buffered_rectangle_edge = rectangle_edge.buffer(-1, single_sided=True)
        edge_intersection = buffered_rectangle_edge.intersection(buffered_edge)
        if edge_intersection.area > max_intersection:
            max_intersection = edge_intersection.area
            edge_type = types[i]
    return edge_type


def create_setback_area(edges, edge_setbacks):
    assert len(edges) == len(edge_setbacks), "Fewer or more setbacks than edges"
    edges_buffered = []
    for i, edge in enumerate(edges):
        if edge_setbacks[i] > 0:
            edges_buffered.append(edge.buffer(-edge_setbacks[i], single_sided=True))
    return unary_union(edges_buffered)


def compute_gfa(plots_for_GFA, plot_setbacks, dcp):

    gfas = {plot_id: {} for plot_id in plots_for_GFA['PlotId'].unique()}
    count = 0
    for count, plot in enumerate(plots_for_GFA.index):
        plot_row = plots_for_GFA.loc[plot, :]
        plot_id = plot_row.loc["PlotId"]
        plot_type = plot_row.loc['PlotType']
        plot_storeys = plot_row.loc['storeys']
        plot_parts = plot_row.loc['parts']
        plot_area = plot_row.loc["geometry"].area
        setbacks = plot_setbacks[plot_id]
        plot_gfas = {}

        for subtype in setbacks.keys():
            # consider storey regulation from control plans that may apply in special cases
            plot_control = dcp.loc[(dcp['Zone'] == plot_type) & (dcp['type_property'] == subtype), :]
            control_storeys = plot_control['height_storeys'].min()
            if not pd.isna(control_storeys):
                if not plot_storeys:
                    plot_storeys = [control_storeys]
                    plot_parts = [plot_row.loc["geometry"]]
                else:
                    plot_storeys = [min(control_storeys, storeys) for storeys in plot_storeys]
            # No minimum storeys
            if not plot_storeys:
                plot_gfa = plot_row["GPR_GFA"]
                #plot_gfa = float("NaN")
            # Minimum storeys apply
            else:
                subtype_setbacks = setbacks[subtype]
                # Getting the setback geometry for each storey.
                # Note: will be a list with one element if the same setback applies to all storeys.
                setback_area = []
                # check if any edge has more than one setback value.
                if np.any([type(edge_setbacks) == list for edge_setbacks in subtype_setbacks]):
                    # set setback list length.
                    setback_length = 2
                    for edge_setbacks in subtype_setbacks:
                        if type(edge_setbacks) == list:
                            setback_length = len(edge_setbacks)
                            break
                    # get setbacked areas for every storey with different setbacks.
                    for i in range(setback_length):
                        cur_setbacks = [edge_setback[i] if type(edge_setback) == list else edge_setback for edge_setback in subtype_setbacks]
                        setback_area.append(create_setback_area(plot_row.loc['edges'], cur_setbacks))
                else:
                    # setback is the same across all storeys
                    # create same size setback area for all storeys.
                    setback_area.append(create_setback_area(plot_row.loc['edges'], subtype_setbacks))
                # Check site coverage
                site_coverage = plot_control["site_coverage"].min()
                site_coverage_applies = not pd.isna(site_coverage)

                # Compute GFA by going through all the storeys
                plot_gfa = 0
                for storey in range(int(max(plot_storeys))):
                    # Get the setbacked area for the current storey
                    if len(setback_area) > storey:
                        cur_setback_area = setback_area[storey]
                    else:
                        cur_setback_area = setback_area[-1]
                    # Compute the storey area over all parts
                    storey_area = 0
                    for i, part in enumerate(plot_parts):
                        # Only consider part if within allowed storeys
                        if plot_storeys[i] > storey:
                            storey_area += part.difference(cur_setback_area).area
                    # Check whether the storey are is larger than allowed site coverage
                    if site_coverage_applies:
                        if storey_area / plot_area > site_coverage:
                            storey_area = plot_area * site_coverage
                    plot_gfa += storey_area
                # Use the minimum of the computed GFA and GFA based on GPR
                plot_gfa = min(plot_gfa, plot_row.loc["GPR_GFA"])
            plot_gfas[subtype] = plot_gfa
        gfas[plot_id] = plot_gfas
        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, plots_for_GFA.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, plots_for_GFA.shape[0]))
    return gfas


def run_estimate_gfa():

    # file paths
    root = "C:/Users/AydaGrisiute/Desktop/demonstrator/"
    fn_plots = root + "URA_plots/G_MP19_LAND_USE_PL.shp"
    fn_con = root + "conservation_areas/master-plan-2019-sdcp-conservation-area-layer-geojson.geojson"
    fn_hc = root + "height_control/G_MP08_BUILDHTCTRL_STY_PL.shp"
    fn_sb_boundary = root + "street_blocks/G_MP08_STREET_BLK_PLAN_PL.shp"
    fn_sb_reg = root + "street_blocks/street_block_plans.xlsx"
    fn_udr = root + "urban_design_guidelines/UrbanDesignGuidelines.shp"
    fn_control_plans = root + "Regulation_grouping2.xlsx"
    fn_landed_housing = root + "landed_housing/landed_housing.shp"
    fn_roads = root + "roads/ROADNETWORKLINE.shp"
    fn_road_plots = root + "roads/road_plots/roads.shp"

    """ 
    Removing plots with zoning types for which calculating GFA does not make sense, like PARK, OPEN SPACE, WATERBODY, ROAD.
    Apply initial filtering to eliminate invalid plots and minimise bad input.
    """

    # 4398 at this point
    plots = gpd.read_file(fn_plots).to_crs(epsg=3857)
    plots.geometry = plots.geometry.simplify(0.1)
    plots = plots[['INC_CRC', 'LU_DESC', 'GPR', 'geometry']].copy()
    plots = plots.rename(columns={'INC_CRC': 'PlotId', 'LU_DESC': 'PlotType'})
    plots['GPR'] = pd.to_numeric(plots['GPR'], errors='coerce')

    plots = plots[~plots['PlotType'].isin(['ROAD', 'WATERBODY', 'PARK', 'OPEN SPACE', 'CEMETERY', 'BEACH AREA'])]
    plots = plots[~(plots.geometry.type == "MultiPolygon")]
    plots['context_storeys'] = float('NaN')
    plots['site_area'] = plots.area
    plots = plots.loc[plots['site_area'] >= 50]

    narrow_plots = []
    for plot in plots.geometry:
        narrow_plots.append(~is_narrow(plot, 3, 0.1))
    plots = plots.loc[narrow_plots, :]
    invalid_zones = ['RESERVE SITE', 'SPECIAL USE ZONE', 'UTILITY']
    print('Plots loaded')

    """
    Reads in geometry and regulation data for: 
        Conservation Areas - 248 boundaries; 
        Height Control Plan - 362 boundaries; 
        Street Block Plan - 90 boundaries; 
        Urban Design Guidelines - 171 boundaries; 
        Development Control Plans
    """
    lh = gpd.read_file(fn_landed_housing).to_crs(3857)  # landed housing
    lh['STY_HT'] = lh['STY_HT'].map({'3-STOREY': 3, '2-STOREY': 2})

    con = gpd.read_file(fn_con).to_crs(epsg=3857)  # conservation areas

    hc = gpd.read_file(fn_hc).to_crs(epsg=3857)  # height controls
    hc.loc[hc['BLD_HT_STY'] == '> 50', 'BLD_HT_STY'] = '100'
    hc['BLD_HT_STY'] = pd.to_numeric(hc['BLD_HT_STY'])

    sb = gpd.read_file(fn_sb_boundary).to_crs(epsg=3857)  # street blocks
    sb_reg = pd.read_excel(fn_sb_reg, engine='openpyxl')
    sb = sb.merge(sb_reg, on="INC_CRC")
    sb = to_list(sb, 'Setback_Front')
    sb = to_list(sb, 'Setback_Side')
    sb = to_list(sb, 'Setback_Rear')

    udr = gpd.read_file(fn_udr).to_crs(epsg=3857)  # urban design goudelines
    udr['Storeys'] = pd.to_numeric(udr['Storeys'], errors='coerce')
    udr = to_bool(udr, 'Party_Wall')

    dcp = pd.read_excel(fn_control_plans)  # development control  plans
    dcp['type_context'].fillna(value='default', inplace=True)
    dcp['type_property'].fillna(value='default', inplace=True)

    rn_lta = gpd.read_file(fn_roads).to_crs(epsg=3857)  # road network
    rn_lta = rn_lta[
        ~rn_lta['RD_TYP_CD'].isin(['Cross Junction', 'T-Junction', 'Expunged', 'Other Junction', 'Pedestrian Mall',
                                   '2 T-Junction opposite each other', 'Unknown', 'Y-Junction', 'Imaginary Line',
                                   'Slip Road'])]
    print('Regulations loaded')

    """
    Road category assignment to the road type plot.
    road graph segments and their attributes are mapped to road plots.
    """

    road_plots = gpd.read_file(fn_road_plots).to_crs(epsg=3857)
    road_plots = road_plots.rename(columns={'INC_CRC': 'PlotId', 'LU_DESC': 'PlotType'})
    road_plots = assign_road_category(road_plots, rn_lta)

    """ 
    Intersect regulation areas dataset with plot dataset. Valid intersections if overlap > than 0.1 of site area. 
    Store these intersections into dataframe to be used whenever intersection related information is needed.
    """

    con_int = intersect_with_regulation(plots, con, 0.1)  # 1414
    hc_int = intersect_with_regulation(plots, hc, 0.1)  # 853
    sb_int = intersect_with_regulation(plots, sb, 0.1)  # 402
    udr_int = intersect_with_regulation(plots, udr, 0.1)  # 478

    # to identify small amount of plots for which setbacks come from udr.Doing it separately makes it faster. 63
    plots_for_setbacks = plots.copy()
    udr_setback_int = intersect_with_regulation(
        gpd.GeoDataFrame(plots_for_setbacks, geometry=plots_for_setbacks.buffer(3)),
        udr.loc[~udr["Setbacks"].isna(), :], 0)

    """
    Assign GPR for plots with EDUCATIONAL INSTITUTION, CIVIC & COMMUNITY INSTITUTION and PLACE OF WORSHIP zoning type.
    Assignment is based on the relation to landed housing boundaries or surrounding context, like GPR of neighboring plots.
    Fringe is implemented as a buffer zone around every plot of respective zoning types.
    """

    # Assign GPR for plots with specific zoning type or overwrite if in a street block.
    assign_gpr(plots, 'EDUCATIONAL INSTITUTION', lh, 400, 1, 1, 1.4, 3, 4)  # 37 plots
    assign_gpr(plots, 'CIVIC & COMMUNITY INSTITUTION', lh, 400, 1, 1, 1.4, 3, 4)  # 89 plots
    assign_gpr(plots, 'PLACE OF WORSHIP', lh, 400, 1, 1.4, 1.6, 4, 5)  # 46 plots

    for plot_id in plots["PlotId"].unique():
        street_block = sb_int.loc[sb_int['PlotId'] == plot_id, :]
        if not street_block.empty:
            gpr = street_block["GPR_2"].min()
            current_gpr = plots.loc[plots["PlotId"] == plot_id, "GPR"].min()
            plots.loc[plots["PlotId"] == plot_id, "GPR"] = min(current_gpr, gpr)

    plots['GPR_GFA'] = plots['GPR'] * plots['site_area']

    # extract unclear plot ids.
    plots_in_con = set(con_int['PlotId'].unique())

    plots_hc_unclear = (hc_int.loc[:, ["PlotId", "BLD_HT_STY"]].groupby("PlotId").apply(lambda plot: plot.isnull().values.any()))
    plots_hc_unclear = set(plots_hc_unclear.index[plots_hc_unclear])

    type_dropped = ['MONUMENT', 'CONSERVATION', 'SUBJECT TO DETAILED CONTROL']
    plots_udr_unclear = (udr_int.loc[:, ["PlotId", "Type"]].groupby("PlotId").apply(lambda plot: plot.isin(type_dropped).values.any()))
    plots_udr_unclear = set(plots_udr_unclear.index[plots_udr_unclear])

    unclear_plots = plots_hc_unclear.union(plots_udr_unclear.union(plots_in_con))
    print('Regulations with plots intersected')

    """
    Extract Partywall edges.
    Check if plot id is in any of the intersection dataframes. 
    Set Party_Wall boolean True if in any of the intersection dataframes it is True. 
    Identify party wall edges by checking if edge buffer is in any other plot that has Party_Wall set to True.
    """

    plots = set_partywall(plots, sb_int, udr_int)  # 474 plots with partywalls
    plots_for_GFA = plots.loc[~plots["PlotId"].isin(unclear_plots), :].copy()  # filtering unclear plots
    plots_for_GFA = plots_for_GFA[~plots_for_GFA['PlotType'].isin(invalid_zones)]  # filtering additional zoning types
    plots_for_GFA = set_partywall_edges(plots_for_GFA, plots)  # 480 plots
    print('Partywalls set')

    """
    Extracting number of storeys.
    Covers five regulation scenarios impacting how storeys are computed.
    """

    plots_for_GFA["storeys"] = [[]] * plots_for_GFA.shape[0]
    plots_for_GFA["parts"] = [[]] * plots_for_GFA.shape[0]
    plots_for_GFA = retrieve_number_of_storeys(plots_for_GFA, hc_int, sb_int, udr_int)
    print('Number of stories set')

    """
    Extract road buffer edges.
    Checks plot edge intersection with  road plots and assigns edges to one of five road categories.
    """

    road_categories = {'cat_1_edges': ['Expressway'],
                       'cat_2_edges': ['Major Arterials/Minor Arterials'],
                       'cat_3_5_edges': ['Local Access', 'Local Collector/Primary Access'],
                       'backlane_edges': ['no category']}
    plots_for_GFA = set_road_buffer_edges(plots_for_GFA, road_plots, road_categories)
    print('Road buffer edges set')

    """
    Extracting setbacks for every edge.
    Split plots into ones that setbacks apply from street block plans, and setbacks from control plan. 
    There is no need to try to identify  every edge type  if street blocks do not apply. 
    """

    setback_names = ['Setback_Front', 'Setback_Side', 'Setback_Rear']
    plots_in_control = []
    plots_in_streetblocks = []
    plots_in_urban_design_guidelines = []
    count = 0
    for count, plot in enumerate(plots_for_GFA.index):
        plot_id = plots_for_GFA.loc[plot, 'PlotId']
        street_block = sb_int.loc[sb_int['PlotId'] == plot_id, :]
        urban_design_guideline = udr_setback_int.loc[udr_setback_int['PlotId'] == plot_id, :]

        if not street_block.empty:
            street_block = street_block.iloc[0]
            if not np.all([pd.isna(street_block.loc[col][0]) for col in setback_names]):
                plots_in_streetblocks.append(plot_id)

        if not urban_design_guideline.empty:
            plots_in_urban_design_guidelines.append(plot_id)

        plot_type = plots_for_GFA.loc[plot, 'PlotType']
        dcp_zone_type = dcp.loc[dcp['Zone'] == plot_type, :]
        if not dcp_zone_type["setback_common"].isna().all():
            plots_in_control.append(plot_id)

        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, plots_for_GFA.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, plots_for_GFA.shape[0]))

    plot_setbacks = retrieve_edge_setback(plots_for_GFA, plots, dcp, sb_int, udr,
                                              plots_in_control, plots_in_streetblocks, plots_in_urban_design_guidelines)
    print('Setbacks set')

    """
    Compute GFA.
    Extract setback area and for every storey part (1 or more) construct footprint area.
    Check if constructed area per storey does not exceed site coverage.
    Add all storey areas and check if sum of it does not exceed allowed GFA.
    """

    gfas = compute_gfa(plots_for_GFA, plot_setbacks, dcp)

    with open('C:/Users/AydaGrisiute/Desktop/estimate_GFA.json', 'w') as f:
        json.dump(gfas, f, indent=4)
    print('GFAs computed')


if __name__ == "__main__":
    run_estimate_gfa()

