import itertools
import numbers
from typing import List
import pandas as pd
import numpy as np
from shapely.geometry import LineString
import sys
from shapely.ops import unary_union
import math
import geopandas as gpd
import json
from collections import defaultdict
from shapely import wkt
from shapely.geometry import box, LineString, Point, Polygon
from shapely.validation import make_valid
import shapely
from itertools import groupby, chain
from operator import itemgetter
from collections.abc import Iterable
from SPARQLWrapper import SPARQLWrapper, JSON

"""
Helper functions to process dataframes, e.g. filter.
"""

def flatten(xs):
    result = []
    print('xs: ', xs)
    if isinstance(xs, (list, tuple)):
        for x in xs:
            print('x in xs: ', x)
            result.extend(flatten(x))
    else:
        result.append(xs)
        print('result: ', result)
    return result


def to_list(dataframe, column):
    final_list = []
    for i in dataframe.index:
        list_values = str(dataframe.loc[i, column]).split(';')
        final_list.append([float(x) for x in list_values])
    dataframe[column] = final_list
    return dataframe

def to_list_str(dataframe, column):
    final_list = []
    for i in dataframe.index:
        list_values = str(dataframe.loc[i, column]).split(';')
        final_list.append([x for x in list_values])
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

def get_edges_no_simplification(polygon):
    curve = polygon.exterior
    return list(map(LineString, zip(curve.coords[:-1], curve.coords[1:])))

def is_narrow(geometry, width, ratio):
    #print('is narrow geometry: ', geometry, type(geometry), geometry.minimum_rotated_rectangle)
    edges = get_edges(geometry.minimum_rotated_rectangle)
    lengths = [e.length for e in edges]
    return (np.min(lengths) < width) or ((np.min(lengths) / np.max(lengths)) < ratio)

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

def intersect_with_regulation(all_plots, regulation, valid_overlap):
    intersection = gpd.overlay(all_plots, regulation, how='intersection', keep_geom_type=True)
    intersection['intersection_area'] = intersection.area
    overlap_ratio = intersection['intersection_area'] / intersection['site_area']
    intersection = intersection.loc[overlap_ratio >= valid_overlap, :]
    return intersection

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

'''Finds neighbours (both road plots and non-road plots) for target plots.'''
def find_neighbours(target_plots, potential_non_road_neighbours, potential_road_neighbours):
    neighbor_id_list = []
    target_plots['buffered_geo'] = [x.buffer(2, cap_style=3) for x in target_plots['geometry']]
    for target_index, target_row in target_plots.iterrows():
        cur_neighbour_list = []
        cur_plot = target_row['PlotId']
        for road_index, road_row in potential_road_neighbours.iterrows():
            if target_row['buffered_geo'].intersects(road_row['geometry']):
                cur_neighbour_list.append(road_row['PlotId'])
        for neighbour_index, neighbour_row in potential_non_road_neighbours.iterrows():
            if cur_plot != neighbour_row['PlotId']:
                if target_row['buffered_geo'].intersects(neighbour_row['geometry']):
                    cur_neighbour_list.append(neighbour_row['PlotId'])

        neighbor_id_list.append(cur_neighbour_list)
    target_plots['neighbour_list'] = neighbor_id_list
    return(target_plots)

'''Classifies the edges of the min rect around each plot: side / rear / front edge'''
def find_min_rect_edge_types(resi_df, road_plots):
    resid_plots = resi_df.copy()
    resid_plots['min_rect'] = [x.minimum_rotated_rectangle for x in resi_df.geometry] # find min rect for each plot
    resid_plots['min_rect_edges'] = [get_edges(x) for x in resid_plots.min_rect]
    resid_plots['min_rect_edges_buffered'] = [[y.buffer(3, single_sided=False, cap_style=3) for y in x] for x in resid_plots['min_rect_edges']] #list of buffered polygons
    road_polys_per_plot = defaultdict(list)  # to be filled in with {plot_id1 : [poly1, poly2,...], ...}
    road_edges_per_plot_dict = defaultdict(list)  # to be filled in with {plot_id1 : [edge1, edge2...], ...}
    front_edge_dict = defaultdict(list) # to be filled in with {plot_id1 : front_edge1, ...}
    rear_edge_dict = defaultdict(list)  # to be filled in with {plot_id1 : rear_edge1, ...}
    side1_edge_dict = defaultdict(list) # to be filled in with {plot_id1 : side1_edge1, ...}
    side2_edge_dict = defaultdict(list) # to be filled in with {plot_id1 : side2_edge1, ...}

    for plot_i, plot_row in resid_plots.iterrows(): #iterate over residential plots
        cur_plot = plot_row['PlotId']
        cur_neighbours = plot_row['neighbour_list']
        cur_buf_edges = plot_row['min_rect_edges_buffered'] # current plot's list of polygons of buffered min rect edges (polygons)
        cur_orig_edges = plot_row['min_rect_edges'] # current plot's original min rect edges (lines)

        for j in range(0, len(cur_neighbours)): # go through all the current plot's neighbours
            if cur_neighbours[j] in road_plots['PlotId'].values: # if current neighbour is a road
                road_geo = road_plots.loc[road_plots['PlotId'] == cur_neighbours[j], 'geometry'] #road geometry
                for i in range (0, len(cur_buf_edges)): # go through plot's buffered min rect edges
                    buf_edge = cur_buf_edges[i]
                    orig_edge = cur_orig_edges[i] # original min rect edge (line)
                    if road_geo.values.intersects(buf_edge): #check if buffered edge intersects with road:
                        int_area = road_geo.intersection(buf_edge).area
                        int_ratio = (int_area / buf_edge.area).values[0]
                        if int_ratio > 0.2:
                            road_polys_per_plot[cur_plot].append((buf_edge.wkt, int_ratio))
                            road_edges_per_plot_dict[cur_plot].append((i, orig_edge.wkt, orig_edge.length, cur_orig_edges, int_ratio)) #add edge index, (non-buffered!) edge_geo, length, and all original min rect edges to road edges per plot

    #find front edge, defined as the shortest edge with biggest intersection with a road
    for plot_id, edge_info in road_edges_per_plot_dict.items():
        front_edge = None
        best_front_metric = 0 # metric used to find best front edge candidate
        front_edge_i = None
        rear_edge = None
        if len(edge_info) > 0: # If plot has edges that intersects with a road
            for edge_tuple in edge_info: # go through info of all edges that intersect with a road
                edge_len = edge_tuple[2] # length of current edge
                road_int_ratio = edge_tuple[4] # ratio of part intersecting with road, to total edge length
                print('edge_len, road_int_ratio: ', edge_len, road_int_ratio)
                front_metric = 1/edge_len * road_int_ratio #front edge is short and has biggest possible road int
                if front_edge is None or front_metric > best_front_metric: # check if current edge is the best front edge candidate so far
                    front_edge = wkt.loads(edge_tuple[1]) #transform string into shapely geometry
                    front_edge_i = edge_tuple[0]
        front_edge_dict[plot_id] = front_edge #add side edge to the dictionary  {plot : edge geo}

        cur_min_rect_edges = edge_tuple[3]
        side_1_found = False

        #find rear edge (which has same length as front edge) and side edges (different lengths)
        for j in range(0, len(cur_min_rect_edges)): # go through min rect edges again to find the rear edge and side edges
            current_edge = cur_min_rect_edges[j]
            if round(current_edge.length, 3) == round(front_edge.length, 3) and j != front_edge_i: #rear edge has same length as front edge, but different index
                rear_edge = current_edge
                rear_edge_dict[plot_id] = rear_edge #add rear edge to the dictionary {plot : edge geo}
            elif round(current_edge.length, 3) != round(front_edge.length, 3) and side_1_found == False:
                side1_edge_dict[plot_id] = current_edge  # add side edge to the dictionary {plot : edge geo}
                side_1_found = True
            elif round(current_edge.length, 3) != round(front_edge.length, 3) and side_1_found == True:
                side2_edge_dict[plot_id] = current_edge  # add side edge to the dictionary {plot : edge geo}

    resid_plots['min_rect_front_edge'] = None
    resid_plots['min_rect_side_edge1'] = None
    resid_plots['min_rect_side_edge2'] = None
    resid_plots['min_rect_rear_edge'] = None
    road_polys_list = [] # to be used in 'is_corner_plot'

    #Fill new columns for edge geometry in plot df
    for index, row in resid_plots.iterrows():
        cur_plot_id = row['PlotId']
        if cur_plot_id in front_edge_dict.keys():
            resid_plots.loc[index, 'min_rect_front_edge'] = front_edge_dict[cur_plot_id]  # front edge
            resid_plots.loc[index, 'min_rect_side_edge1'] = side1_edge_dict[cur_plot_id] #side edge #1
            resid_plots.loc[index, 'min_rect_side_edge2'] = side2_edge_dict[cur_plot_id] #side edge #2
            resid_plots.loc[index, 'min_rect_rear_edge'] = rear_edge_dict[cur_plot_id] # rear edge
        road_polys_list.append(road_polys_per_plot[cur_plot_id] if cur_plot_id in road_polys_per_plot.keys() else None)

    resid_plots['road_polys_per_plot'] = road_polys_list #to be used in 'is_corner_plot'
    return (resid_plots)


''' Classify each plot's neighbours as a side/rear neighbour'''
def classify_neighbours(plots_df, non_road_plots):
    neighbour_types = []
    for plot_index, plot_row in plots_df.iterrows():
        neighbour_plots = plot_row['neighbour_list']
        neighbour_dict = {}
        if plot_row['min_rect_side_edge1'] is not None:
            cur_side_edge1 = plot_row['min_rect_side_edge1'].buffer(3, single_sided=False, cap_style=3)
            cur_side_edge2 = plot_row['min_rect_side_edge2'].buffer(3, single_sided=False, cap_style=3)
            cur_rear_edge = plot_row['min_rect_rear_edge'].buffer(3, single_sided=False, cap_style=3)
            for neigh_id in neighbour_plots: # iterate over plot neighbours' ids
                neigh_geo = non_road_plots.loc[non_road_plots['PlotId'] == neigh_id, 'geometry'].values if neigh_id in non_road_plots.PlotId.values else None
                if neigh_geo is not None and len(neigh_geo) >1:  #plot 492F3A9D3EC7C5FB has 3 copies for some reason
                    neigh_geo = neigh_geo[0] # in case plot id has many copies, choose only one
                neigh_type = None # to be updated
                biggest_int = 0 # to be updated
                if neigh_geo is not None and neigh_geo.is_valid:
                    if neigh_geo.intersects(cur_side_edge1): #check int with plot's side edge
                        int_area = neigh_geo.intersection(cur_side_edge1).area
                        if int_area > biggest_int: # if int area biggest so far, assume neighbor is a side neigh.
                            biggest_int = int_area
                            neigh_type = 'side'
                    if neigh_geo.intersects(cur_side_edge2): #check int with plot's side edge
                        int_area = neigh_geo.intersection(cur_side_edge2).area
                        if int_area > biggest_int: # if int area biggest so far, assume neighbor is a side neigh.
                            biggest_int = int_area
                            neigh_type = 'side'
                    if neigh_geo.intersects(cur_rear_edge): #check int with plot's rear edge
                        int_area = neigh_geo.intersection(cur_rear_edge).area
                        if int_area > biggest_int:
                            biggest_int = int_area
                            neigh_type = 'rear' # if int area biggest so far, assume neighbor is a rear neigh.

                    neighbour_dict[neigh_id] = neigh_type
        neighbour_types.append(neighbour_dict)
    plots_df['non_road_neighbour_types'] = neighbour_types
    return plots_df

'''Extend each line in the list of lines.
 If line intersects with polygon, store the index of that edge, the geometry of the intersecting part, and the length of the intersection.
 Return a gdf with the above info in one column, and the length of the original line in another column.'''
def line_poly_ints(line_list, polygon):
    intersecting_edges = []
    orig_edge_lens = []
    for i in range(0, len(line_list)):
        cur_line = line_list[i]
        extended_line = shapely.affinity.scale(cur_line, xfact=2, yfact=2, zfact=2, origin='center')

        if extended_line.intersects(polygon):  # check if edge intersects with road if not polygon.intersects(line):
            int = extended_line.intersection(polygon)
            intersecting_edges.append(int.wkt)  # make tuple with edge index, intersecting part of line, int_length
            orig_edge_lens.append(cur_line.length)
    d = {'geometry': intersecting_edges, 'original_length':orig_edge_lens}
    gdf = gpd.GeoDataFrame(d, columns = d.keys(), crs="EPSG:4326")

    return gdf

def find_average_length(linestr_list):
    if type(linestr_list) == list and len(linestr_list) > 0:
        count = 0
        total_len = 0
        for linestr in linestr_list:
            cur_len = wkt.loads(linestr).length
            total_len += cur_len
            count += 1
        return total_len/count
    else:
        return 0

'''
Buffer plot's front edge / side edge (depends on input params) on both sides many times.
Find all buffer polygon edges that intersect with the plot.
Check which one of these is parallel to the original front/side edge.
Stretch the parallel one so that it intersects beyond the edges of the plot, then intersect with plot.
Calculate the average length of the offset edges' parts that intersect with the plot.
'''
def find_width_or_depth(resi_df, edge_col, output_geo_col_name, output_len_col_name):
    offset_edges_per_plot = defaultdict(list)
    for plot_index, plot_row in resi_df.iterrows(): # go through plots
        cur_edge = plot_row[edge_col] #this is the edge that will be offset
        if type(cur_edge) == list and len(cur_edge) ==2: # if there are duplicate edges, pick 1 edge only
            cur_edge = plot_row[edge_col][0]
        cur_plot_id = plot_row['PlotId']

        validity = gpd.GeoSeries(plot_row['geometry']).is_valid.values[0]

        # find suitable number of offsets, and offset distance, for edge given size of current plot
        if cur_edge is not None and validity: # if current side/front edge exists and is valid
            cur_plot_geo = plot_row['geometry']
            min_rect_edges = get_edges_no_simplification(cur_plot_geo.minimum_rotated_rectangle)
            min_rect_edges_lens = [x.length for x in min_rect_edges]
            shortest_edge_len = min(min_rect_edges_lens) #shortest minimum rect edge
            longest_edge_len = max(min_rect_edges_lens) #longest minimum rect edge
            if shortest_edge_len < 16:
                buffer_dist = 3 # this affects density of depth/width lines used to calculate average depth/width. For small plots, need greater density.
            else:
                buffer_dist = 10 # for large plots, lines for calculating average width/length can be less dense.
            number_of_offsets = longest_edge_len / buffer_dist + 3
            line_offsets = []

            for i in range(1, int(number_of_offsets)): #
                cur_buffered_poly = cur_edge.buffer(i*buffer_dist, cap_style=2) #buffer front/side edge
                cur_buffered_edges = get_edges_no_simplification(cur_buffered_poly) #edges of buffered polygon
                intersecting_edge_info = line_poly_ints(cur_buffered_edges, cur_plot_geo) #returns, for each edge that intersects with a plot, i (index of edge in list of edges), int.wkt (intersecting part's geometry), int_length (length of intersecting part)
                if len(intersecting_edge_info) > 0:
                    for index, row in intersecting_edge_info.iterrows():
                        if round(row['original_length'], 1) != 2*i*buffer_dist: #we know that the edge that is perpendicular to the front/side edge has length 2* buffer distance --> we don't want to use this edge
                            line_offsets.append(row.geometry) #append the other edge (parallel to front/side edge) to the list of offset edges
            offset_edges_per_plot[cur_plot_id] = line_offsets
        else:
            offset_edges_per_plot[cur_plot_id] = None

    resi_df[output_geo_col_name] = resi_df['PlotId'].apply(lambda x: offset_edges_per_plot[x]) #add a column to plots_df with all the offset edges
    resi_df[output_len_col_name] = resi_df[output_geo_col_name].apply(lambda x: find_average_length(x)) #add a column to plots_df with the average length of the offse edges
    return resi_df


'''Draws a donut divided into sectors around each plot.'''
def draw_sectors(plots_df, n_sectors, radius, subtracted_radius):
    sectors = []
    for index, row in plots_df.iterrows():
        cur_geo = row.geometry
        cur_centroid = cur_geo.centroid
        circle = cur_centroid.buffer(radius).simplify(0.5) #draw an almost-circle around the plot's centroid
        xs, ys = circle.exterior.coords.xy
        point_on_circle = Point(xs[0], ys[0]) #find one point along circe edge
        line1 = shapely.affinity.scale(LineString([point_on_circle, cur_centroid]), xfact=2, yfact=2, origin=cur_centroid) #draw line from centroid to point on circle edge
        line2 = shapely.affinity.rotate(line1, 360/n_sectors, cur_centroid) #create a new line by rotating line #1.
        splitter = LineString([*line2.coords, *line1.coords[::-1]]) #split the circle polygon with the two lines
        split_results = shapely.ops.split(circle, splitter) # split results consists two polygons: the sector (the part between the lines) and the rest of the original circle
        if len(list(split_results.geoms)) >1:
            sector = split_results[1]
            splitting_circle = cur_centroid.buffer(subtracted_radius).simplify(0.5) #now remove the inner part of the sector polygon
            sector_subtracted = sector.difference(splitting_circle) #sector_subtracted is shaped like a donut slice
            cur_sector_list = [sector_subtracted.wkt]
            for i in range(1, n_sectors+1): #rotate the donut slice (sector_subtracted) so that a full (sliced) donut is formed
                rot = i * 360/n_sectors
                rotated_sector_sub = shapely.affinity.rotate(sector_subtracted, rot, cur_centroid)
                cur_sector_list.append(rotated_sector_sub.wkt)
            sectors.append(cur_sector_list)
    plots_df['sectors'] = sectors #add, for each plot, a list of donut slice geometry (for each plot you have a full sliced donut)
    return plots_df

'''Check if each plot is a fringe plot; add a new column in plots_for_GFA with that info.
Target plots are all the plots of the type whose fringe is being examined.
E.g. if we are checking if plot_A is at a residnetial fringe, target plots would be all residential plots except plot_A.'''
def fringe_check(plots_df, target_plots, adjoining_empty_requirement, n_sectors):
    empty_sectors_per_plot = defaultdict(list) # dict for storing, for each plot, indices of sectors that are empty. Format is plot_id : [index1, index2...]

    for index, row in plots_df.iterrows():
        cur_plot = row['PlotId']
        cur_sectors = row['sectors'] #donut sector geometry for current plot
        empty_sec_indices = []
        target_plots_minus_current = target_plots.loc[target_plots['PlotId'] != cur_plot, :] #target plots should not contain the current plot
        for i in range(0, len(cur_sectors)): #go through all sectors of current plot
            int_found = False
            for index2, target_row in target_plots_minus_current.iterrows():  # check if the current sector intersects with any of the target plots
                if wkt.loads(cur_sectors[i]).intersects(target_row.geometry): # if one intersection is found, we know that sector is not empty --> move on to next sector.
                    int_found = True
                    break
            if not int_found: # if no intersection is found, the sector is empty, i.e. does not contain intersect with any target plots
                empty_sec_indices.append(i)

        # A fringe is defined as n _consecutive_ empty sectors.
        # Must therefore check if there are n consecutive integers in list of empty sectors.
        # Note that the first and last index also need to be counted as consecutive (first and last sectors of a circle are next to each other).
        # See https://stackoverflow.com/questions/55211695/find-consecutive-integers-in-a-list .
        gb = groupby(enumerate(empty_sec_indices), key=lambda x: x[0] - x[1]) #consecutive integers have a difference of 1
        print('gb: ', gb)
        all_groups = ([i[1] for i in g] for _, g in gb) # Repack elements from each group into list
        print('all groups: ', all_groups)
        a = list(all_groups)
        print('a: ', a)
        if any(0 in i for i in a): #if grouped empty sector indices contain 0, check if empty sectors also contain last sector index (which is next to sector index 0)
            item = [x for x in a if 0 in x][0] #returns list with 1+ elems, e.g. [0,1,2]
            b = [(x + item) for x in a if n_sectors in x]
            [x.remove(0) for x in b if 0 in x and n_sectors in x]
            a = b
        consecutive_lists = list(filter(lambda x: len(x) >= adjoining_empty_requirement, a)) # Filter out one element lists, e.g. [[8, 9], [1, 2, 3]]

        filtered_sector_geo = []
        if len(consecutive_lists) > 0:
            for list1 in consecutive_lists:
                for item in list1:
                    if type(item) == int:
                        filtered_sector_geo.append(cur_sectors[item])
                    elif isinstance(item, Iterable):
                        item2 = list(item)
                        if len(item2) == 1:
                            filtered_sector_geo.append(item2[0])

        empty_sectors_per_plot[cur_plot] = filtered_sector_geo
    sector_list2 = []
    for index, row in plots_df.iterrows():
        if row['PlotId'] in empty_sectors_per_plot.keys():
            sector_list2.append(empty_sectors_per_plot[row['PlotId']])
        else:
            sector_list2.append([])
    plots_df2 = plots_df.copy()
    plots_df2['empty_sectors'] = sector_list2
    plots_df2['fringe_plot'] = [True if len(x) > 0 else False for x in plots_df2.empty_sectors]
    plots_df2.to_csv(root+'plots_for_GFA_fringes_qgis.csv', index=False, sep =';')
    plots_df2.to_csv(root + 'plots_for_GFA_fringes_excel.csv', index=False, sep=',')
    return plots_df2


'''Check if a plot is a corner plot based on whether at least 1 of plot's min rectangle side edges intersects with a road'''
def check_if_corner_plot(plots_df, threshold_side_int_ratio):
    corner_plot_boolean_list = []
    for index, row in plots_df.iterrows():
        if row['road_polys_per_plot'] == None or len[row['road_polys_per_plot'] <= 1]: #if max 1 road poly found, not a corner plot.
            corner_plot_boolean_list.append(False)
        else: # plot might be a corner plot if at least 2 road polys (i.e. min rect edges that int. with roads)
            geo_list, cur_int_ratios = zip(*row['road_polys_per_plot'])
            road_polys = gpd.GeoSeries(geo_list).apply(wkt.loads) #road_polys buffered polys that int with road
            cur_sides = [row['min_rect_side_edge1'], row['min_rect_side_edge2'] ]
            corner_plot_found = False
            for i in range (0, len(road_polys)): #iterate through a plot's road_polys
                if cur_int_ratios[i] > threshold_side_int_ratio and not corner_plot_found:
                    cur_road_poly = road_polys[i]
                    for edge in cur_sides: #check if either *side edge* is within the road polys
                        if edge.within(cur_road_poly):
                            corner_plot_boolean_list.append(True) #if so, plot is a corner plot
                            corner_plot_found = True
                            break
            if corner_plot_found == False:
                corner_plot_boolean_list.append(False)
    plots_df['is_corner_plot'] = corner_plot_boolean_list
    return(plots_df)

'''Check if landed housing type regulations apply to the current development type, and if current development type fulfills them.
I.e. compare requirements of the current development type (e.g. Flat, Bungalow) to the current plot's landed housing information (is the plot in a lha, and what type of lha).'''
def lh_regulation_check(residential_type_reg_row, lh_info):
    cur_type_allowed_in_lh = residential_type_reg_row['allowed_in_landed_housing_area'] #Is the current development type allowed in a landed housing area (True / False)
    lh_ok = False
    print('lh_info:', lh_info)
    if cur_type_allowed_in_lh:
        plots_lh_type = lh_info.TYPE # current plot's landed housing area's type (e.g. 'MIXED LANDED', 'BUNGALOWS', 'N.A.' (for good class bungalow)...)
        plots_lh_classif = lh_info.CLASSIFCTN
        suitable_lh_types = residential_type_reg_row['landed_housing_type'] #list of landed housing types that the current development type (e.g. Bungalow, TerraceType1...) is allowed in
        suitable_lh_classification = residential_type_reg_row['landed_housing_classification']
        print('lh_info.TYPE: ', lh_info.TYPE, ' vs ', 'residential_type_reg_row[landed_housing_type]', residential_type_reg_row['landed_housing_type'])
        print('lh_info.CLASSIFCTN: ', lh_info.CLASSIFCTN, ' vs ', 'residential_type_reg_row[landed_housing_classification]',  residential_type_reg_row['landed_housing_classification'])
        if plots_lh_type.values in suitable_lh_types and plots_lh_classif.values == suitable_lh_classification:
            lh_ok = True
            print('lh ok true in lh regulation check, both type and classification match', lh_ok)

    return lh_ok

def plot_geo_regulation_check(plot_row, regulation_row):
    index_for_min_values = None
    plot_geo_ok = False
    is_corner_plot = plot_row['is_corner_plot']
    cur_resi_type = regulation_row['development_type']

    if cur_resi_type in ['TerraceType1', 'TerraceType2'] and is_corner_plot:
        index_for_min_values = 1
    elif cur_resi_type in ['TerraceType1', 'TerraceType2'] and not is_corner_plot:
        index_for_min_values = 2
    else:
        index_for_min_values = 0
    #print('index_for_min_values: ', index_for_min_values, regulation_row['min_area'][index_for_min_values], type(regulation_row['min_area'][index_for_min_values]))
    cur_type_min_area = 0 if np.isnan(regulation_row['min_area'][index_for_min_values]) else regulation_row['min_area'][index_for_min_values]
    #print('cur_type_min_area: ', cur_type_min_area)
    cur_type_min_width =  0 if np.isnan(regulation_row['min_width_regular_plot'][index_for_min_values]) else regulation_row['min_width_regular_plot'][index_for_min_values]

    cur_type_min_depth = 0 if np.isnan(regulation_row['min_depth']) else regulation_row['min_depth']

    #print('cur type min area width depth:', cur_type_min_area, cur_type_min_width, cur_type_min_depth)
    #print('cur type min area width depth orig:', regulation_row['min_area'][index_for_min_values], regulation_row['min_average_width'][index_for_min_values], regulation_row['min_average_depth'])
    #print('cur plot area width depth: ', plot_row['geometry'].area, plot_row['average_width'], plot_row['average_depth'])
    if (plot_row['geometry'].area > cur_type_min_area and plot_row['average_width'] > cur_type_min_width and plot_row['average_depth'] > cur_type_min_depth): #or plot_row['average_depth'] < cur_type_min_depth):
        plot_geo_ok = True
        #print('plot_geo_ok', plot_geo_ok)

    return plot_geo_ok

'''Check which residential types are allowed on each plot, based on the plot's properties and the requirements for each residential type.'''
def find_allowed_residential_types(plots_df, roads_df, residential_regs_df, lh_df, sbp_df):
    all_plots_allowed_types = []
    for index, row_plot in plots_df.iterrows(): # iterate over plots
        allowed_resi_types = [] # to be filled in
        cur_plot_id = row_plot['PlotId']
        lh_info = lh_df.loc[lh_df['PlotId'] == cur_plot_id, :] if cur_plot_id in lh_df['PlotId'].values else None #all landed housing df columns' values for the lh area the plot is in (if exists)
        sbp_info = sbp_df.loc[sbp_df['PlotId'] == cur_plot_id, :] if cur_plot_id in sbp_df['PlotId'].values else None #all sbp df columns' values for the sbp that applies to the plot (if exists)
        neighbour_ids = row_plot.neighbour_list # all plot's non-road neighbour ids
        neighbour_roads = roads_df.loc[roads_df['PlotId'].isin(neighbour_ids), :] # plot's road neighbours' ids

        for index_reg, row_reg in residential_regs_df.iterrows():
            cur_resi_type = row_reg['development_type']
            is_fringe_plot = row_plot['fringe_plot']

            '''Criteria that must be true for cur_resi_type to be added to plot's list of allowable types'''
            zoning_type_ok = False #does plot's zoning type match the development type's required zoning type
            lh_ok = False # does plot's landed housing area (if it exists) and its type and classification values match the development type's lha requirement / type / classification.
            sb_ok = False # does the plot's street block plan development type value match the current development type
            geo_ok = False # do the plot's geometrical features match the current development type's requirements
            fringe_ok = False # does the plot's status at or not at a residential fringe match the development type's requirements
            road_neighbours_ok = False # do the categories of the roads next to the plot match the development type's requirements

            suitable_zoning_types = [x.strip() for x in row_reg['zoning_type'].split(';')] # will result in list, e.g. ['RESIDENTIAL'] or ['RESIDENTIAL', 'RESIDENTIAL AND COMMERCIAL']
            print('suitable zoning types: ', suitable_zoning_types)
            if row_plot.PlotType in suitable_zoning_types: #set(row_plot.PlotType).intersection(set(row_reg['zoning_type'])): #in row_reg['zoning_type']: set(checklist).intersection(set(words))
                zoning_type_ok = True

            if lh_info is None and cur_resi_type != 'GoodClassBungalow': #plot is not in a landed housing area.
                lh_ok = True #all residential types are allowed in a non-landed housing area (except good class bungalows).
            elif lh_info is not None:
                lh_ok = lh_regulation_check(row_reg, lh_info) #checks if the plot is in a lha and if that location and the lha's type matches the requirements for the current development type

            if sbp_info is not None: #if a street block plan applies to a plot, check that the current development type is allowed by the street block plan.
                suitable_sb_types = row_reg['sbp_development_type'] #suitable street block plan 'ResidentialType' column values, for the current development type
                if len(sbp_info.ResidentialType.values) == 1:
                    if sbp_info.ResidentialType.values in suitable_sb_types or sbp_info.ResidentialType.isna:
                        sb_ok = True #sb ok because current development type is allowed in sbp
            else: #sbp does not apply to the plot
                sb_ok = True # if no street block plan applies to the plot, then the sbp requirement is met

            if row_reg['fringe_requirement'] == 'residential_fringe' and is_fringe_plot:
                fringe_ok = True
            elif row_reg['fringe_requirement'] != 'residential_fringe':
                fringe_ok = True # current development type has no fringe requirement --> any plot allows this type.

            geo_ok = plot_geo_regulation_check(row_plot, row_reg)

            if len(row_reg['required_road_cat']) ==1 and row_reg['required_road_cat'][0] == 'nan': #if the current development type has no road neighbour requirements, any plot allows the development type
                road_neighbours_ok = True
            elif len(neighbour_roads.category_number.values) > 0 and any(x in row_reg['required_road_cat'] for x in neighbour_roads.category_number.values): #if the current development type has a road neighbour requirement, and one of the plot's road neighbours has the required road category.
                road_neighbours_ok = True

            rules = [zoning_type_ok == True, lh_ok == True, sb_ok == True, geo_ok == True,
                    fringe_ok == True, road_neighbours_ok == True]

            if all(rules): # if all requirements are fulfilled:
                allowed_resi_types.append(cur_resi_type) #add the current type to the list of residential types allowed on the plot

        all_plots_allowed_types.append(allowed_resi_types)
    plots_df['allowed_residential_types'] = all_plots_allowed_types
    return(plots_df)

def find_residential_gfa_row(plots_df, all_non_road_plots, residential_gfa_regs_df, hc_int_df, pab_int_df):
    relevant_regulatory_indices = []
    hc_ids_per_dev = resid_gfa_params.loc[:, ['within_hc_boundary', 'type_property']].groupby('type_property').aggregate(lambda x: ','.join(map(str, x))) # df with columns type_property and within_hc_boundary, which contains list of possible hc_ids for that development type.
    for plot_index, plot_row in plots_df.iterrows():
        cur_relevant_indices = [] #list of indices to be filled in
        cur_plot_id = plot_row['PlotId']
        plot_hc_area = hc_int_df.loc[hc_int_df['PlotId'] == cur_plot_id, 'INC_CRC'].values[0] if cur_plot_id in hc_int_df.PlotId.values else None #plot's hc area id (str)
        cur_pab = pab_int_df.loc[pab_int_df['PlotId'] == cur_plot_id, 'PLN_AREA_N'] if cur_plot_id in pab_int_df.PlotId.values else None #plot's planning area name
        plot_gpr = plot_row['GPR'] #plot's GPR (according to master plan)
        cur_allowed_types = plot_row['allowed_residential_types'] #
        cur_neighbour_ids = plot_row['neighbour_list']
        cur_non_road_neighbour_types = all_non_road_plots.loc[all_non_road_plots['PlotId'].isin(cur_neighbour_ids), 'PlotType']

        if len(cur_allowed_types) > 0:
            for reg_index, reg_row in residential_gfa_regs_df.iterrows():
                rule1_gpr = False
                rule2_zt = False
                rule3_hc = False
                rule4_type_prop = False
                rule5_pab = False
                greater_than_in_gpr = False
                cur_type_property =reg_row['type_property']
                print('--------------------------------------')
                print('current development type: ', cur_type_property)
                cur_type_hc_vals = set(hc_ids_per_dev.loc[cur_type_property, 'within_hc_boundary'].split(sep=','))
                cur_type_hc_vals.remove('nan')
                print('all possible hc vals for current dev type', cur_type_hc_vals)
                cur_gpr_bounds = reg_row['gpr_for_height']
                cur_min_gpr = None
                cur_max_gpr = None
                if type(cur_gpr_bounds) == list and len(cur_gpr_bounds) == 2:
                    cur_min_gpr = cur_gpr_bounds[0]
                    cur_max_gpr = cur_gpr_bounds[1]

                if cur_min_gpr is None and cur_max_gpr is None:
                    rule1_gpr = True # no gpr requirement --> any plot will pass
                    print('rule 1 gpr passed: no gpr regulation')
                elif plot_gpr >= cur_min_gpr and plot_gpr < cur_max_gpr:
                    rule1_gpr = True
                    print('rule 1 gpr passed: plot gpr in between min and max')
                else:
                    print('rule 1 gpr failed. Required gpr bounds: ', cur_gpr_bounds ,'Plot GPR = ', plot_gpr)

                print('rule2 zt plot_row[PlotType] == reg_row[zoning_type]:', plot_row['PlotType'], reg_row['zoning_type'] )
                rule2_zt = True if plot_row['PlotType'] == reg_row['zoning_type'] else False
                print('plot hc_area', plot_hc_area)
                print('reg_row requires hc area: ', reg_row['within_hc_boundary'])
                if plot_hc_area is not None and plot_hc_area in cur_type_hc_vals: #if current plot has a hc val and that value exists in the residential gfa params
                    rule3_hc = True if (plot_hc_area.values.any() == reg_row['within_hc_boundary']) else False
                    print('rule3_hc true if plot_hc_area matches', plot_hc_area, reg_row['within_hc_boundary'])
                else:
                    rule3_hc = True if str(reg_row['within_hc_boundary']) == 'nan' else False
                    print("rule3_hc true because plot_hc_area:", plot_hc_area, 'reg row hc:', reg_row['within_hc_boundary'])

                rule4_type_prop = True if reg_row['type_property'] == 'nan' or reg_row['type_property'] in cur_allowed_types else False
                print('rule4 passed:', rule4_type_prop)
                print('current reg type property:', reg_row['type_property'])
                print('plot allowed types:', cur_allowed_types)
                rule5_pab = True if (reg_row['within_pab'][0] == 'nan' or cur_pab.values.any() in reg_row['within_pab']) else False
                type_context_neighbour_found = False
                print('rule5_pab', rule5_pab)

                all_rules = [rule1_gpr == True,
                             rule2_zt == True,
                             rule3_hc == True,
                             rule4_type_prop == True,
                             rule5_pab == True]
                print('all rules:', all_rules)
                #print('cur_relevant_indices: ', cur_relevant_indices)
                if all(all_rules):
                    print('all(all_rules):', all(all_rules))
                    cur_relevant_indices.append(reg_index)

        #Add part that deals with situation where plot is in both hc_boundary and pab ?
        '''df1 = residential_gfa_regs_df.loc[residential_gfa_regs_df.index.intersection(cur_relevant_indices)]
        print('df1: ', df1)
        index_with_hc_val = None
        index_with_pab_val = None
        index_with_hc_pab_NA = None
        for index,row in df1:
            cur_type_prop = row['type_property']
            rows_with_same_type = residential_gfa_regs_df.loc[residential_gfa_regs_df['type_property'] == cur_type_prop, :]
            if len(rows_with_same_type) > 1:
                print('row with same type:', cur_type_prop, row)
                cur_hc_val = row['within_hc_boundary']
                cur_pab_val = row['within_pab']
                if cur_pab_val == 'nan':
                    pass'''
        relevant_regulatory_indices.append(cur_relevant_indices)
    new_df = plots_df.copy()
    new_df['GFA_reg_indices'] = relevant_regulatory_indices

    return new_df

#find biggest intersection between edge and a road plot
# if none found, or road's category is unknown, return none
'''def find_edge_road_type(edge_geo, plot_neighbours, roads_df):
    print('in find_edge_road_type')
    offset_edge = edge_geo.buffer(6, single_sided=False, cap_style=3)
    cur_edge_road_cat = None
    biggest_int = 0
    for neighbour_id in plot_neighbours:
        if neighbour_id in roads_df.PlotId.values:
            road_geo = roads_df.loc[roads_df['PlotId'] == neighbour_id, 'geometry'].values[0]
            if offset_edge.intersects(road_geo):
                intersection_area = offset_edge.intersection(road_geo).area / offset_edge.area
                cur_road_cat = roads_df.loc[roads_df['PlotId'] == neighbour_id, 'category_number'].values
                if intersection_area > biggest_int and cur_road_cat != 'unknown':
                    cur_edge_road_cat = cur_road_cat
    print('cur_edge_road_cat: ', cur_edge_road_cat)
    return cur_edge_road_cat'''
def check_neighbour_road_cat(main_neighbour_id, roads_df):
    neighbour_gcba = False
    cur_road_cat = None
    if main_neighbour_id in roads_df.PlotId.values:
        cur_road_cat = roads_df.loc[roads_df['PlotId'] == main_neighbour_id, 'category_number'].values
    return cur_road_cat

'''Check if the edge's main neighbour is in a gcba area.'''
def check_neighbour_gcba(main_neighbour_id, lh_plots):
    neighbour_gcba = False
    if main_neighbour_id in lh_plots.PlotId.values:
        neighbour_classifctn = lh_plots.loc[lh_plots['PlotId'] == main_neighbour_id, 'CLASSIFCTN'].values
        if neighbour_classifctn == 'GOOD CLASS BUNGALOW AREA':
            neighbour_gcba = True
    return neighbour_gcba

'''Check if the edge's main neighbour is a waterbody.'''
def check_neighbour_waterbody(main_neighbour_id, all_non_road_plots):
    neighbour_waterbody = False
    print('in check_neighbour_waterbody')
    if main_neighbour_id in all_non_road_plots.PlotId.values:
        print('check_neighbour_waterbody found plot')
        neighbour_zoning_type = all_non_road_plots.loc[all_non_road_plots['PlotId'] == main_neighbour_id, 'PlotType'].values
        if neighbour_zoning_type == 'WATERBODY':
            print('check_neighbour_waterbody found zoning type WATERBODY')
            neighbour_waterbody = True
    print('neighbour_waterbody not found')
    return neighbour_waterbody

'''Find the id of the main neighbour of the edge (= the neighbour that the offset edge has the biggest intersection with.)
Based on the main neighbour, find the type of the edge ('side' / 'rear' / 'front'). '''
def find_edge_type_and_main_neighbour(edge_buffered, non_rd_neigh_dict, non_road_plots_df, rd_neighs_df):
    biggest_int = 0
    main_neighbour_id = ''
    edge_type = None
    for neighbor_id, neighbor_type in non_rd_neigh_dict.items(): # go through non-road plots, and classify edge as either a side/rear edge
        biggest_non_rd_int = 0
        #if neighbor_id in non_road_plots_df.PlotId.values:
        neighbor_geo = non_road_plots_df.loc[non_road_plots_df['PlotId'] == neighbor_id, 'geometry'].values
        if neighbor_geo.intersects(edge_buffered) and neighbor_geo.intersection(edge_buffered).area > biggest_int:
            edge_type = neighbor_type if neighbor_type != 'front' else 'side' # edge can only be a rear or side edge if its main neighbour is a non-road plot.
            biggest_non_rd_int = neighbor_geo.intersection(edge_buffered).area
            biggest_int = biggest_non_rd_int
            main_neighbour_id = neighbor_id

    for index, row in rd_neighs_df.iterrows(): # go through road neighbours
        cur_rd_geo = row['geometry']
        cur_rd_id = row['PlotId']
        if cur_rd_geo.intersects(edge_buffered) and cur_rd_geo.intersection(edge_buffered).area > biggest_int: #edge is a front edge if its main neighbour is a road plot.
            edge_type = 'front'
            main_neighbour_id = cur_rd_id
    return edge_type, main_neighbour_id

def find_edge_setbacks2(edge_geo,non_rd_neigh_dict, all_neighs_list, non_road_plots_df, cur_rd_neighs_df, lh_plots, sbp_info, gfa_regs_for_cur_plot, rd_cat_offset_index_dict):
    print('----------find_edge_setbacks2 start-------------------')
    edge_buffered = edge_geo.buffer(2, single_sided=False, cap_style=2)
    edge_type, main_neighbour_id = find_edge_type_and_main_neighbour(edge_buffered, non_rd_neigh_dict, non_road_plots_df, cur_rd_neighs_df)

    if edge_type is None: #this happens if a plot's neighbour is missing or not classified
        edge_type = 'side'

    edges_sbp_col_names_dict = {'front': 'Setback_Front', 'side':'Setback_Side', 'rear':'Setback_Rear'} #name of street block plan column used to find setback value for different kinds of edges
    edges_res_reg_col_names_dict = {'front': None, 'side': 'setback_side', 'rear': 'setback_rear'} #name of residential gfa param column used to find setback value for different kinds of edges
    setback_dict = {} # to be filled in with {development_type1 : float1, development_type2 : float2, ... }

    neighbour_gcba = check_neighbour_gcba(main_neighbour_id, lh_plots) #True / False, depending on if the current edge's main neighbour is a GCBA plot
    neighbour_waterbody = check_neighbour_waterbody(main_neighbour_id, non_road_plots_df) # True / False, depending on if the current edge's main neighbour is a waterbody
    neighbour_road_cat = check_neighbour_road_cat(main_neighbour_id, cur_rd_neighs_df) # string, e.g. 'cat_4' if the main neighbour is a road. None if not a road.
    neighbour_backlane = False # Placeholder for the future. No backlanes in our road data so this cannot be checked.

    print('neighbour gcba: ',neighbour_gcba , 'beighbour_backlane:', neighbour_backlane, 'neighbour_waterbody: ',neighbour_waterbody , 'neighbour_road_cat: ',neighbour_road_cat )
    if not gfa_regs_for_cur_plot.empty: # if the plot allows no residential development types (e.g. plot is too small), then it does not have any GFA regulations / parameters
        print('in if #1')
        for index, row_reg in gfa_regs_for_cur_plot.iterrows(): #go through all sets of gfa parameters that apply to the plot
            cur_type = row_reg['type_property'] # development type of the current gfa parameter (e.g. 'Flat', 'Condominium'...)
            cur_setback = None
            print('cur_setback initial value: ', cur_setback)
            if sbp_info is not None: #if street block info applies to plot, check setback for current edge from there
                sbp_col_name = edges_sbp_col_names_dict[edge_type]
                sbp_setback = sbp_info[sbp_col_name].values[0][0]
                cur_setback = float(sbp_setback) if not pd.isnull(sbp_setback) else None
            print('cur_setback after sbp check: ', cur_setback)

            if cur_setback == None: # if setback not found in sbp, look for it in residential regulations (gfa parameters)
                res_reg_col_name = edges_res_reg_col_names_dict[edge_type] #name of residential gfa param column used to find setback for current edge type

                print('neighbour_road_cat: ', neighbour_road_cat)
                if neighbour_road_cat != None and neighbour_road_cat != ['unknown'] and edge_type == 'front': #road offsets only apply to 'front' edges, and choosing the right offset requires knowing the road category.
                    road_offset_index = rd_cat_offset_index_dict[neighbour_road_cat[0]]
                    cur_setback = float(row_reg['setback_road_general'][road_offset_index])
                print('cur_setback after road check: ', cur_setback)
                if res_reg_col_name == 'setback_side' and edge_type != 'front':
                    if pd.notnull(row_reg['setback_side'][0]): # only proceed if the setback_side value is not null
                        if neighbour_gcba:
                            side_offset_index = 1
                        else:
                            side_offset_index = 0
                        cur_setback = float(row_reg[res_reg_col_name][side_offset_index])
                        print('row_reg[setback_side]: ', row_reg[res_reg_col_name])
                        print('cur_setback after side check: ', cur_setback)
                elif res_reg_col_name == 'setback_rear' and edge_type != 'front':
                    if pd.notnull(row_reg['setback_rear'][0]):  # only proceed if the setback_side value is not null
                        if neighbour_backlane:
                            rear_offset_index = 1
                        else:
                            rear_offset_index = 0
                        cur_setback = float(row_reg[res_reg_col_name][rear_offset_index])
                        print('row_reg[setback_rear]: ', row_reg[res_reg_col_name])
                        print('cur_setback after rear check: ', cur_setback)
                elif res_reg_col_name == 'setback_common' and edge_type != 'front':
                    if pd.notnull(row_reg['setback_common'][0]):
                        print('row_reg[setback_common]: ', row_reg['setback_common'])
                        if neighbour_gcba:
                            common_setback_offset_index = 1
                        elif neighbour_waterbody:
                            common_setback_offset_index = 2
                        else:
                            common_setback_offset_index = 0
                        print('common_setback_offset_index: ', common_setback_offset_index)
                        cur_setback = float(row_reg['setback_common'][common_setback_offset_index])
                        print('cur_setback : ', cur_setback)
            print('cur_setback after side/rear/common offset check: ', cur_setback)
            setback_dict[cur_type] = cur_setback
    print('setback_dict ', edge_type, ': ', setback_dict)
    print('----------find_edge_setbacks2 end-------------------')
    return setback_dict, edge_type

def find_buildable_footprints2(plots_df, residential_gfa_regs_df, lh_plots, non_road_plots_df, roads_df, sbp_df):
    # This dict determines which road offset value is read from the Regulation Grouping 4 file, column 'setback_road_general'
    # Note that cat 3 gets the cat 2 offset (more conservative), since cats 2-3 not differentiated in ura data
    road_cat_offset_index_dict = {'cat_1': 0,
                            'cat_2_to_3' : 1,
                            'cat_4' : 3,
                            'cat_5' : 4}

    buildable_geos = []
    buildable_footprint_areas = []
    edges_types_per_plot_dict = defaultdict(list)  # to be filled in with {PlotId : [(edge1_geo, edge1_type), (edge2_geo, edge2_type)...]
    for index, row_plot in plots_df.iterrows(): # iterate over plots
        front_edge = row_plot['min_rect_front_edge']
        cur_gfa_reg_indices = row_plot['GFA_reg_indices']
        cur_gfa_regs = residential_gfa_regs_df.loc[cur_gfa_reg_indices]
        #print('cur gfa regs type: ', cur_gfa_regs.type_property)
        buildable_geo_per_type_dict = {} # to be filled in with {development_type1: geometry1, development_type2: geometry2...}
        buildable_area_per_type_dict = {} # to be filled in with {development_type1: area1, development_type2: area2...}

        if front_edge == None or cur_gfa_regs.empty: # if front edge of min rect is missing for some reason, not enough info to calculate gfa
            pass
        else:
            edge_setbacks_per_edge = []  # list of dicts for each edge
            plot_edges = get_edges(row_plot['geometry'])
            cur_plot_id = row_plot['PlotId']
            #print('cur plot id: ', cur_plot_id)
            sbp_info = sbp_df.loc[sbp_df['PlotId'] == cur_plot_id, :] if cur_plot_id in sbp_df['PlotId'].values else None #street block plan info that applies to plot, if it exists
            cur_non_rd_neigh_dict = row_plot['non_road_neighbour_types'] #example: {neighbor1_id : 'side', neighbor2_id : 'side', neighbor3_id : 'rear',...}
            cur_all_neighbours = row_plot['neighbour_list']
            cur_rd_neighbours_df = roads_df.loc[roads_df['PlotId'].isin(cur_all_neighbours), :]
            #print('cur_rd_neighbours_df: ',cur_rd_neighbours_df, cur_rd_neighbours_df.columns )
            #print('cur_neighbours: ', cur_all_neighbours)
            cur_site_coverage = None
            buildable_geo = row_plot['geometry']
            for i in range (0, len(plot_edges)):
                cur_edge = plot_edges[i]
                print('edge i:', i)
                cur_setbacks, edge_type = find_edge_setbacks2(cur_edge, cur_non_rd_neigh_dict, cur_all_neighbours, non_road_plots_df, cur_rd_neighbours_df, lh_plots, sbp_info, cur_gfa_regs, road_cat_offset_index_dict)
                print('cur setbacks: ', cur_setbacks) # cur setbacks is e.g. {'Good_class_bungalow': 3.0}
                #print('edge_type: ', edge_type)

                edges_types_per_plot_dict[cur_plot_id].append((cur_edge.wkt, edge_type))
                edge_setbacks_per_edge.append(cur_setbacks)

            #print('cur gfa regs: ', cur_gfa_regs)
            for index, row_reg in cur_gfa_regs.iterrows(): #iterate over GFA param rows relevant to current plot
                cur_type = row_reg['type_property'] # e.g. Flat, Condominium...
                print('cur type: ', cur_type)
                buildable_geo = row_plot['geometry'] #initial buildable geo is the plot polygon. Edge offsets will be subtracted.
                buildable_footprint_area = 0
                print('edge_setbacks_per_edge: ', edge_setbacks_per_edge)
                for i in range(0, len(edge_setbacks_per_edge)): #iterate over edges
                    edge_dict = edge_setbacks_per_edge[i] # Offset values (float) per development type applicable to the edge, e.g. {'Flat':7.5, 'Condominium':12.4, ...})
                    offset_for_cur_type = edge_dict[cur_type]
                    print('edge i:', i)
                    print('edge_dict:', edge_dict)
                    print('cur type: ', cur_type)
                    print('plot_edges[i]:', plot_edges[i])
                    print('offset_for_cur_type: ', offset_for_cur_type)
                    if offset_for_cur_type is not None: #this might happen if the edge does not have any neighbor due to e.g. removal of narrow plots at beginning of script. hence the plot's type (inferred based on neighbour) can't be found
                        setback_geo = plot_edges[i].buffer(offset_for_cur_type, single_sided=False, cap_style=2) # geometry of the offset part
                    else:
                        setback_geo = plot_edges[i].buffer(0, single_sided=False, cap_style=2)
                        #print('buildable geo: ', buildable_geo)
                    #print('setback geo: ', setback_geo)
                    buildable_geo = buildable_geo - setback_geo #subtract offset geometry from the current buildable geometry

                '''remove any artefact pieces that are separate from the plot after offsetting edges'''
                print('buildable geo: ', buildable_geo)
                if buildable_geo is not None:
                    biggest_part_area = 0
                    biggest_geo = None
                    if buildable_geo.geom_type == 'MultiPolygon':
                        for polygon in buildable_geo:
                            if polygon.area > biggest_part_area:
                                biggest_part_area = polygon.area
                                biggest_geo = polygon
                        buildable_geo = biggest_geo

                    buildable_geo = buildable_geo.buffer(-0.15, single_sided=True, cap_style=2).simplify(0.1)
                buildable_geo_per_type_dict[cur_type] = buildable_geo.wkt
                #print('buildable_geo_per_type_dict[cur_type]: ', buildable_geo_per_type_dict[cur_type])
                cur_site_coverage = float(row_reg['site_coverage'])
                #print('cur_site_coverage:', cur_site_coverage, type(cur_site_coverage))
                buildable_vs_original_ratio = buildable_geo.area / row_plot['geometry'].area
                #print('buildable_vs_original_ratio: ', buildable_vs_original_ratio)
                if math.isnan(float(cur_site_coverage)):
                    buildable_footprint_area = buildable_geo.area
                else:
                    #print('cur_site_coverage * row_plot[geometry].area', cur_site_coverage * row_plot['geometry'].area)
                    buildable_footprint_area = min(cur_site_coverage * row_plot['geometry'].area,
                                              buildable_geo.area)
                #print('buildable_footprint_area: ', buildable_footprint_area)

                buildable_area_per_type_dict[cur_type] = buildable_footprint_area

        buildable_geos.append(buildable_geo_per_type_dict)
        buildable_footprint_areas.append(buildable_area_per_type_dict)

    #print('buildable_geos:', buildable_geos)
    plots_df['buildable_geo'] = buildable_geos
    plots_df['buildable_footprint_areas'] = buildable_footprint_areas
    return plots_df, edges_types_per_plot_dict

def find_allowed_gfa(plots_df, residential_gfa_regs_df, lh_plots, sbp_df):
    storey_list = []
    gfa_list = []
    for index, row_plot in plots_df.iterrows():
        cur_gfa_reg_indices = row_plot['GFA_reg_indices']
        cur_gfa_regs = residential_gfa_regs_df.loc[cur_gfa_reg_indices]
        cur_plot_id = row_plot['PlotId']
        print('------------------------')
        print('cur_plot_id: ', cur_plot_id)

        sbp_stories = sbp_df.loc[sbp_df['PlotId'] == cur_plot_id, 'Storeys'].values if cur_plot_id in sbp_df['PlotId'].values else None
        max_storeys_per_type_dict = {}
        max_gfa_per_type_dict = {}
        if row_plot['buildable_footprint_areas']: #check that not empty
            for index, row_reg in cur_gfa_regs.iterrows():
                cur_type = row_reg['type_property']
                cur_footprint = row_plot['buildable_footprint_areas'][cur_type]
                cur_storeys = None
                print('sbp_stories: ', sbp_stories)
                if sbp_stories is not None:
                    cur_storeys = float(sbp_stories[0]) if not pd.isnull(sbp_stories) else None
                print('cur_storeys:', cur_storeys)
                if cur_storeys == None:  # i.e. storeys not found in sbp --> check residential regulations
                    if pd.notnull(row_reg['height_storeys']):
                        print('3: row_reg[height_storeys]', row_reg['height_storeys'])
                        if type(row_reg['height_storeys']) not in [float, int] and '>' in row_reg['height_storeys']:
                            row_reg['height_storeys'] = 100
                            print('4')
                        cur_storeys = row_reg['height_storeys']
                    else:
                        cur_storeys = 3
                if cur_plot_id in lh_plots['PlotId'].values:
                    cur_storeys = lh_plots.loc[lh_plots['PlotId'] == cur_plot_id, 'STY_HT'].values[0]
                    print('cur_storeys lh: ', cur_storeys)
                max_storeys_per_type_dict[cur_type] = cur_storeys
                print('cur_footprint:', cur_footprint)
                print('cur_storeys:', cur_storeys)
                if cur_storeys != None and cur_footprint != None:
                    max_gfa_per_type_dict[cur_type] =cur_footprint * cur_storeys
                    print('2')
                else:
                    max_gfa_per_type_dict[cur_type] = None
            storey_list.append(max_storeys_per_type_dict)
            gfa_list.append(max_gfa_per_type_dict)
        else:
            storey_list.append({})
            gfa_list.append({})
        max_gfa_per_type_dict['default'] = 0
    plots_df['max_storeys'] = storey_list
    plots_df['max_gfa'] = gfa_list
    return plots_df

def get_plots(endpoint):
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery(
        """PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
        PREFIX geo: <http://www.bigdata.com/rdf/geospatial#>
        SELECT ?cityObjectId ?Geometry ?ZoningType ?GPR
        WHERE {
        GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/> {
        SERVICE geo:search {
        ?cityObjectId geo:predicate ocgml:EnvelopeType .
        ?cityObjectId geo:searchDatatype <http://localhost/blazegraph/literals/POLYGON-3-15> .
        ?cityObjectId geo:customFields "X0#Y0#Z0#X1#Y1#Z1#X2#Y2#Z2#X3#Y3#Z3#X4#Y4#Z4" .
        ?cityObjectId geo:customFieldsLowerBounds "1.279372#103.815651#0#1.279372#103.815651#0#1.279372#103.815651#0#1.279372#103.815651#0#1.279372#103.815651#0".
        ?cityObjectId geo:customFieldsUpperBounds  "1.306702#103.863544#1000#1.306702#103.863544#1000#1.306702#103.863544#1000#1.306702#103.863544#1000#1.306702#103.863544#1000".
        ?cityObjectId geo:customFieldsValues ?envelopes . }
        BIND(IRI(REPLACE(STR(?cityObjectId), "cityobject", "genericcityobject")) AS ?Id) }
        GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/>
        { ?surfaceId ocgml:cityObjectId ?Id ;
                     ocgml:GeometryType ?Geometry .
        hint:Prior hint:runLast "true" .}
        GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
        ?genAttrId ocgml:cityObjectId ?cityObjectId ;
                   ocgml:attrName 'LU_DESC' ;
                   ocgml:strVal ?ZoningType .
        ?genAttrId2 ocgml:cityObjectId ?cityObjectId ;
                    ocgml:attrName 'GPR';
                    ocgml:strVal ?GPR . } } """)

    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    queryResults = pd.DataFrame(results['results']['bindings'])
    queryResults = queryResults.applymap(lambda cell:cell['value'])
    geometries = gpd.GeoSeries(queryResults['Geometry'].map(lambda geo: envelope_string_to_polygon(geo, geodetic=True, flip=True)), crs='EPSG:4326')
    queried_plots = gpd.GeoDataFrame(queryResults, geometry=geometries).to_crs(epsg=3857).drop(columns = ['Geometry'])

    return queried_plots

def envelope_string_to_polygon(envelopeString, geodetic = False, flip=True):
    pointsAsString = envelopeString.split('#')
    numOfPoints = int(len(pointsAsString)/3)
    points = []
    for i in range(numOfPoints):
        startIndex = i*3
        x,y,z = float(pointsAsString[startIndex]), float(pointsAsString[startIndex+1]), float(pointsAsString[startIndex+2])
        if geodetic:
            if flip:
                points.append((y,x))
            else:
                points.append((x,y))
        else:
            points.append((x,y,z))
    return Polygon(points)

'''Remove plots that are multipolygons, roads or too narrow; rename some column names. Orginally a non-residential method.'''
def process_plots_TWA(queried_plots):
    plots = queried_plots
    plots.geometry = plots.geometry.simplify(0.1)
    plots = plots[~(plots.geometry.type == "MultiPolygon")]
    plots['site_area'] = plots.area
    plots = plots.loc[plots['site_area' ] >= 50]
    plots = plots.rename(columns = {'cityObjectId':'PlotId', 'ZoningType':'PlotType'})
    plots['GPR'] = pd.to_numeric(plots['GPR'], errors = 'coerce')
    plots = plots[~plots['PlotType'].isin(['ROAD'])]
    plots['context_storeys'] = float('NaN')

    narrow_plots = []
    for plot in plots.geometry:
        narrow_plots.append(~is_narrow(plot, 3, 0.1))
    plots = plots.loc[narrow_plots,:]
    return plots

def process_all_local_plots(fn_local_plots):
    # 4398 at this point
    plots = gpd.read_file(fn_local_plots).to_crs(epsg=3857)  # use this whenever TWA doesnt work
    plots.geometry = plots.geometry.simplify(0.1)
    plots = plots[~(plots.geometry.type == "MultiPolygon")]
    plots['site_area'] = plots.area
    # plots = plots.rename(columns = {'cityObjectId':'PlotId', 'ZoningType':'PlotType'}) ##Use when twa works
    plots = plots.rename(columns={'INC_CRC': 'PlotId', 'LU_DESC': 'PlotType'})  ##use this whenever TWA doesnt work
    # plots = plots[['INC_CRC', 'PlotType', 'GPR', 'geometry']].copy() ##old?
    plots['GPR'] = pd.to_numeric(plots['GPR'], errors='coerce')

    narrow_plots = []
    for plot in plots.geometry:
        narrow_plots.append(~is_narrow(plot, 3, 0.1))
    plots = plots.loc[narrow_plots,:]
    return plots #local as in on own computer, not TWA

def filter_residential_plots(all_residential_plots):
    plots = all_residential_plots
    plots['context_storeys'] = float('NaN')
    plots = plots.loc[plots['site_area'] >= 50]
    narrow_plots = []
    for plot in plots.geometry:
        narrow_plots.append(~is_narrow(plot, 3, 0.1))
    plots = plots.loc[narrow_plots, :]
    return plots

def find_regulation_ints(plots_df, fn_con, fn_hc, fn_sb_boundary, fn_sb_reg, fn_udr , fn_control_plans, fn_landed_housing, fn_planning_area_boundaries):
    con = gpd.read_file(fn_con).to_crs(epsg=3857)  # conservation areas
    hc = gpd.read_file(fn_hc).to_crs(epsg=3857)  # height controls
    hc.HT_CTL_TXT.fillna(value='', inplace=True) #replace None values with ''
    hc['HT_CTL_TXT'] = hc['HT_CTL_TXT'].map(lambda x: ''.join([i for i in x if type(x) == str and i.isdigit()])) #filter out all non-digit characters (e.g. "C30m SHD")
    hc_storeys = hc.loc[hc['HT_CTL_TYP'] == 'NUMBER OF STOREYS', :]  # only hc areas with type 'number of storeys' are included. Later must consider what to do with max metres regulations. 'Subject to detailed planning' dropped because other regulations (sbp, landed housing...) are assumed to cover those plots.
    hc_storeys = hc_storeys.loc[hc_storeys['HT_CTL_TXT'] != '', :] # remove regulations with no value for max number of storeys (1 such regulation)
    hc_storeys['HT_CTL_TXT'] = pd.to_numeric(hc_storeys['HT_CTL_TXT']) # convert string digits into int/float

    sb = gpd.read_file(fn_sb_boundary).to_crs(epsg=3857)  # street blocks
    sb_reg = pd.read_excel(fn_sb_reg, engine='openpyxl')
    sb = sb.merge(sb_reg, on="INC_CRC")
    sb = to_list(sb, 'Setback_Front')
    sb = to_list(sb, 'Setback_Side')
    sb = to_list(sb, 'Setback_Rear')

    udr = gpd.read_file(fn_udr).to_crs(epsg=3857)  # urban design goudelines
    udr['Storeys'] = pd.to_numeric(udr['Storeys'], errors='coerce')
    udr = to_bool(udr, 'Party_Wall')

    lh = gpd.read_file(fn_landed_housing).to_crs(3857)  # landed housing
    lh['STY_HT'] = lh['STY_HT'].map({'3-STOREY': 3, '2-STOREY': 2})

    pab = gpd.read_file(fn_planning_area_boundaries).to_crs(3857)  # planning area boundaries
    pab = pab.loc[:, ['PLN_AREA_N', 'INC_CRC', 'geometry']]
    # dcp = pd.read_excel(fn_control_plans)  # development control  plans
    # dcp['type_context'].fillna(value='default', inplace=True)
    # dcp['type_property'].fillna(value='default', inplace=True)
    con_int1 = intersect_with_regulation(plots_df, con, 0.1)  # 1414
    hc_int1 = intersect_with_regulation(plots_df, hc_storeys, 0.1)  # 853
    sb_int1 = intersect_with_regulation(plots_df, sb, 0.1)  # 402
    udr_int1 = intersect_with_regulation(plots_df, udr, 0.1)  # 478
    lh_int1 = intersect_with_regulation(plots_df, lh, 0.1)
    pab_int1 = intersect_with_regulation(plots_df, pab, 0.1)  # 1662

    return con_int1, hc_int1, sb_int1, udr_int1, lh_int1, pab_int1

'''Read residential regulation files into dataframes.'''
def process_residential_regs(fn_residential_requirements, fn_residential_gfa_parameters):
    resid_params = pd.read_excel(fn_residential_gfa_parameters, usecols='A:U', skiprows=1).drop(0)  # Drop/skip original excel sheet's rows 0 and row 2 because these contain explanations meant for humans. Row 1 has col names.
    resid_params = to_list_str(resid_params, 'within_pab')
    res_req = to_list(resid_params, 'gpr_for_height')
    resid_params = to_list_str(resid_params, 'type_context')
    resid_params = to_list(resid_params, 'setback_road_general')
    resid_params = to_list(resid_params, 'setback_road_first_floor_residential_with_commercial_at_1st_floor')
    #res_gfa_reg = to_list(res_gfa_reg, 'setback_front_irregular')
    resid_params = to_list(resid_params, 'setback_side')

    resid_params = to_list(resid_params, 'setback_rear')
    resid_params = to_list_str(resid_params, 'setback_common')
    resid_params = to_list(resid_params, 'setback_common_first_floor_residential_with_commercial_at_1st_floor')
    resid_params = to_list(resid_params, 'height_m')
    resid_params = to_list(resid_params, 'height_floor_to_floor')

    res_req = pd.read_excel(fn_residential_requirements, usecols='A:L').drop(0)  # Drop rows 0 because it contains explanations for humans
    res_req = to_bool(res_req, 'allowed_in_landed_housing_area')
    res_req = to_bool(res_req, 'landed_housing_classification')
    res_req = to_list_str(res_req, 'landed_housing_type')
    res_req = to_list_str(res_req, 'sbp_development_type')
    res_req = to_list(res_req, 'min_area')
    res_req = to_list(res_req, 'min_width_regular_plot')
    res_req = to_list_str(res_req, 'required_road_cat')

    return resid_params, res_req

"""
Road category assignment to the road type plot.
road graph segments and their attributes are mapped to road plots.
"""
def process_roads(fn_rd_net, fn_rd_plots):
    rn_lta = gpd.read_file(fn_rd_net).to_crs(epsg=3857)  # road network
    rn_lta = rn_lta[
        ~rn_lta['RD_TYP_CD'].isin(['Cross Junction', 'T-Junction', 'Expunged', 'Other Junction', 'Pedestrian Mall',
                                   '2 T-Junction opposite each other', 'Unknown', 'Y-Junction', 'Imaginary Line'])]
    road_cat_dict = {'Expressway': 'cat_1',
                    'Major Arterials/Minor Arterials': 'cat_2_to_3',
                    'Local Collector/Primary Access': 'cat_4',
                    'Local Access': 'cat_5',
                    'Slip Road': 'cat_5',
                    'no category': 'unknown'} #see https://www.lta.gov.sg/content/dam/ltagov/industry_innovations/industry_matters/development_construction_resources/public_streets/pdf/RT-COP%20V2.0%20(3%20April%202019).pdf
    rd_plots = gpd.read_file(fn_rd_plots).to_crs(epsg=3857)
    rd_plots = rd_plots.rename(columns={'INC_CRC': 'PlotId', 'LU_DESC': 'PlotType'})
    rd_plots = assign_road_category(rd_plots, rn_lta)
    rd_plots['category_number'] = [road_cat_dict[x] for x in rd_plots["RD_TYP_CD"]] #road plots: new column name 'category_number'
    return rd_plots

def assign_gprs(plots_df, sb_int_df): #if sbp has a gpr, replace plot's gpr with min(original gpr, sbp gpr)
    for plot_id in plots_df["PlotId"].unique():
        street_block = sb_int_df.loc[sb_int['PlotId'] == plot_id, :]
        if not street_block.empty:
            gpr = street_block["GPR_2"].min()
            current_gpr = plots_df.loc[plots_df["PlotId"] == plot_id, "GPR"].min()
            plots_df.loc[plots_df["PlotId"] == plot_id, "GPR"] = min(current_gpr, gpr)

    plots_df['GPR_GFA'] = plots_df['GPR'] * plots_df['site_area']
    return plots_df

'''Remove plots with unclear regulations:
- Conservation area
- UDR type is monument / conservation / subj. to detailed control'''
def remove_unclear_plots(plots_df, con_int_df, udr_int_df):
    plots_in_con = set(con_int_df['PlotId'].unique())
    type_dropped = ['MONUMENT', 'CONSERVATION', 'SUBJECT TO DETAILED CONTROL']
    plots_udr_unclear = (udr_int_df.loc[:, ["PlotId", "Type"]].groupby("PlotId").apply(lambda plot: plot.isin(type_dropped).values.any()))
    plots_udr_unclear = set(plots_udr_unclear.index[plots_udr_unclear])
    # unclear_plots = plots_hc_unclear.union(plots_udr_unclear.union(plots_in_con))
    unclear_plots = plots_udr_unclear
    plots_df = plots_df.loc[~plots_df["PlotId"].isin(unclear_plots), :].copy()  # filtering unclear plots
    return plots_df

#def run_estimate_gfa():
# file paths
endpoint = "http://theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql"
root = "C:/Users/HeidiSilvennoinen/Desktop/demonstrator/"

fn_plots = root + "URA_plots/G_MP19_LAND_USE_PL.shp"
fn_con = root + "conservation_areas/master-plan-2019-sdcp-conservation-area-layer-geojson.geojson"
fn_hc = root + "height_control/master-plan-2019-sdcp-building-height-control-layer.shp" #G_MP08_BUILDHTCTRL_STY_PL.shp
fn_sb_boundary = root + "street_blocks/G_MP08_STREET_BLK_PLAN_PL.shp" #to be replaced by 2019 version
fn_sb_reg = root + "street_blocks/street_block_plans.xlsx"
fn_udr = root + "urban_design_guidelines/UrbanDesignGuidelines.shp"
fn_control_plans = root + "Regulation_grouping2.xlsx"
fn_landed_housing = root + "landed_housing/landed_housing.shp"
fn_roads = root + "roads/ROADNETWORKLINE.shp"
fn_road_plots = root + "roads/road_plots/roads.shp"
fn_residential_requirements = root + "residential_GFA/residential_regulations2_2022_11_22.xlsx" #file name updated
fn_planning_area_boundaries = root + "planning_area_boundaries/planning_boundary_area.shp"
fn_residential_gfa_parameters = root + "residential_GFA/Regulation_grouping4_2022_11_17.xlsx" # file name updated


#invalid_zones = ['RESERVE SITE', 'SPECIAL USE ZONE', 'UTILITY', 'RESIDENTIAL', 'WHITE',   'RESIDENTIAL WITH COMMERCIAL AT 1ST STOREY' 'COMMERCIAL & RESIDENTIAL']  # last 3 types are only temporary

all_zt_local_plots = process_all_local_plots(fn_plots)

twa_query_res = get_plots(endpoint)
all_zt_twa_plots = process_plots_TWA(twa_query_res)

all_zt_plots = all_zt_twa_plots # in case TWA doesn't work, use: all_zt_local_plots

plots_all_residential = all_zt_plots[all_zt_plots['PlotType'].isin(['RESIDENTIAL', 'COMMERCIAL & RESIDENTIAL', 'RESIDENTIAL WITH COMMERCIAL AT 1ST STOREY'])]

plots_for_GFA = plots_all_residential

road_plots = process_roads(fn_roads, fn_road_plots) #Add road plots
non_road_plots = gpd.overlay(all_zt_plots, road_plots, how='difference', keep_geom_type=True) #all non-road plots (all zoning types)

all_reg_ints = find_regulation_ints(plots_for_GFA, fn_con, fn_hc, fn_sb_boundary, fn_sb_reg, fn_udr , fn_control_plans, fn_landed_housing, fn_planning_area_boundaries)
con_int,hc_int, sb_int, udr_int, lh_int, pab_int = all_reg_ints

plots_for_GFA = remove_unclear_plots(plots_for_GFA, con_int, udr_int) #remove plots with conservation/monument status or unclear urban design guidelines
plots_for_GFA = assign_gprs(plots_for_GFA, sb_int) #replace plots' GPR value with smaller value from SBP, if it exists


'''Read residential regulations'''
all_resid_regs = process_residential_regs(fn_residential_requirements, fn_residential_gfa_parameters)
resid_gfa_params, resid_type_requirs = all_resid_regs

'''Add plot analytics'''
plots_for_GFA = draw_sectors(plots_for_GFA, 10, 400, 100) #number params: n_sectors, radius of sector, radius of part subtracted from sector middle (to avoid neighbors intersecting with sector)
plots_for_GFA = fringe_check(plots_for_GFA, plots_all_residential, 1, 10) #add true/false value to indicate if a plot is at a residntial fringe. Number params: required consecutive empty sectors, n sectors

plots_for_GFA = find_neighbours(plots_for_GFA, all_zt_plots, road_plots) #add column with list of neighbours ids for each plot
print('neighbours found')

plots_for_GFA = find_min_rect_edge_types(plots_for_GFA, road_plots) # find out which of plot's minimum bounding box rectangle's edges are rear / side / front edges.
print('min_rect_edge types found and added to plots df')

plots_for_GFA = classify_neighbours(plots_for_GFA, all_zt_plots)

#Add columns for average width and depth of each plot
plots_for_GFA = find_width_or_depth(plots_for_GFA, 'min_rect_front_edge', 'offset_front_edges_geo', 'average_width')
plots_for_GFA = find_width_or_depth(plots_for_GFA, 'min_rect_side_edge1', 'offset_side_edges_geo', 'average_depth')

# Add column indicating if plot is a corner plot
plots_for_GFA = check_if_corner_plot(plots_for_GFA, 0.3)

'''Find relevant GFA regulations for the plots'''
plots_for_GFA = find_allowed_residential_types(plots_for_GFA, road_plots, resid_type_requirs, lh_int, sb_int) #find residential types that are allowed on the plot

plots_for_GFA = find_residential_gfa_row(plots_for_GFA, non_road_plots, resid_gfa_params, hc_int, pab_int) #adds indices of the residential GFA parameter regulations to each plot

'''Calculate GFA based on regulation parameters that were found'''
plots_for_GFA, edge_types_per_plot = find_buildable_footprints2(plots_for_GFA, resid_gfa_params, lh_int,non_road_plots, road_plots, sb_int)
plots_for_GFA = find_allowed_gfa(plots_for_GFA, resid_gfa_params, lh_int, sb_int)

'''for visualisation of classified plot edges and buildable footprints - create files that can be opened on QGis '''
L = [(k, *t) for k, v in edge_types_per_plot.items() for t in v]
all_edges_df = pd.DataFrame(L, columns=['PlotId', 'edge_geo', 'edge_type'])
front_edges = all_edges_df.loc[all_edges_df['edge_type'] == 'front', :]
side_edges = all_edges_df.loc[all_edges_df['edge_type'] == 'side', :]
rear_edges = all_edges_df.loc[all_edges_df['edge_type'] == 'rear', :]
front_edges.to_csv(root+'front_edges_qgis2.csv', index=False, sep=';')
side_edges.to_csv(root+'side_edges_qgis2.csv', index=False, sep=';')
rear_edges.to_csv(root+'rear_edges_qgis2.csv', index=False, sep=';')

buildable_geos = plots_for_GFA['buildable_geo'].apply(pd.Series)
buildable_geos.GoodClassBungalow.to_csv(root+'buildable_gcb_qgis2.csv', index=False, sep=';')
buildable_geos.GoodClassBungalow.to_csv(root+'buildable_gcb_excel.csv', index=False, sep=',')
buildable_geos.TerraceType2.to_csv(root+'buildable_terrace2_qgis.csv', index=False, sep=';')
buildable_geos.TerraceType2.to_csv(root+'buildable_terrace2_excel.csv', index=False, sep=',')

buildable_geos.TerraceType1.to_csv(root+'buildable_terrace1_qgis.csv', index=False, sep=';')
buildable_geos.TerraceType1.to_csv(root+'buildable_terrace1_excel.csv', index=False, sep=',')
buildable_geos.Bungalow.to_csv(root+'buildable_bungalow_qgis.csv', index=False, sep=';')
buildable_geos.Bungalow.to_csv(root+'buildable_bungalow_excel.csv', index=False, sep=',')
buildable_geos.Flat.to_csv(root+'buildable_flat_qgis.csv', index=False, sep=';')
buildable_geos.Flat.to_csv(root+'buildable_flat_excel.csv', index=False, sep=',')
buildable_geos.Condominium.to_csv(root+'test_plot_buildable_condo_qgis.csv', index=False, sep=';')
buildable_geos.Condominium.to_csv(root+'buildable_condo_excel.csv', index=False, sep=',')
buildable_geos['Semi-detachedHouse'].to_csv(root+'buildable_semi_detached_excel.csv', index=False, sep=',')
buildable_geos['Semi-detachedHouse'].to_csv(root+'buildable_semi_detachedqgis.csv', index=False, sep=';')


'''create JSON file, remove plots that also exist in non-residential GFA... this should be improved'''
fn_overlap = root + "overlapping_plots.csv"
overlap_df = pd.read_csv(fn_overlap)
overlap_list = overlap_df.PlotId.values.tolist()
overlap_list = [x.replace('<', '') for x in overlap_list]
overlap_list = [x.replace('>', '') for x in overlap_list]
no_overlap = plots_for_GFA[~plots_for_GFA['PlotId'].isin(overlap_list)]

residential_only = plots_for_GFA.loc[plots_for_GFA['PlotType'] == 'RESIDENTIAL', :]
gfa_dict = residential_only.set_index('PlotId').to_dict()['max_gfa']
with open('C:/Users/HeidiSilvennoinen/Desktop/demonstrator/estimate_residential_GFA7.json', 'w') as f:
    json.dump(gfa_dict, f, indent=4)
print('GFAs computed')

'''
#print(types_after_lh_filter)
#plots_for_GFA = plots_for_GFA[~plots_for_GFA['PlotType'].isin(invalid_zones)]  # filtering additional zoning types
#plots_for_GFA = set_partywall_edges(plots_for_GFA, plots)  # 480 plots'''
#print('Partywalls set')
"""
Extracting number of storeys.
Covers five regulation scenarios impacting how storeys are computed.
"""

'''    plots_for_GFA["storeys"] = [[]] * plots_for_GFA.shape[0]
plots_for_GFA["parts"] = [[]] * plots_for_GFA.shape[0]
plots_for_GFA = retrieve_number_of_storeys(plots_for_GFA, hc_int, sb_int, udr_int)
print('Number of stories set')'''

"""
Extract road buffer edges.
Checks plot edge intersection with  road plots and assigns edges to one of five road categories.
"""
'''plots_for_GFA['edges'] = plots_for_GFA.geometry.apply(get_edges)
road_categories = {'cat_1_edges': ['Expressway'],
                   'cat_2_edges': ['Major Arterials/Minor Arterials'],
                   'cat_3_5_edges': ['Local Access', 'Local Collector/Primary Access'],
                   'backlane_edges': ['no category']}
plots_for_GFA = set_road_buffer_edges(plots_for_GFA, road_plots, road_categories)
print('Road buffer edges set')
print(plots_for_GFA)'''

"""
Extracting setbacks for every edge.
Split plots into ones that setbacks apply from street block plans, and setbacks from control plan.
There is no need to try to identify  every edge type  if street blocks do not apply.
"""

'''    setback_names = ['Setback_Front', 'Setback_Side', 'Setback_Rear']
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
                                          plots_in_control, plots_in_streetblocks, plots_in_urban_design_guidelines)'''
#print('Setbacks set')

"""
Compute GFA.
Extract setback area and for every storey part (1 or more) construct footprint area.
Check if constructed area per storey does not exceed site coverage.
Add all storey areas and check if sum of it does not exceed allowed GFA.
"""

'''gfas = compute_gfa(plots_for_GFA, plot_setbacks, dcp)

with open('C:/Users/HeidiSilvennoinen/Desktop/demonstrator/estimate_GFA.json', 'w') as f:
    json.dump(gfas, f, indent=4)
print('GFAs computed')'''


'''if __name__ == "__main__":
    run_estimate_gfa()'''

