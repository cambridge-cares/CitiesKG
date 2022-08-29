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

def is_regular(plot_geometry):
    #print('plot_geometry: ', type(plot_geometry))
    min_rect = plot_geometry.minimum_rotated_rectangle
    if plot_geometry.area / min_rect.area < 0.6:
        return False
    else:
        return True

def find_neighbours(target_plots, potential_non_road_neighbours, potential_road_neighbours):
    neighbor_id_list = []
    target_plots['buffered_geo'] = [x.buffer(6, cap_style=3) for x in target_plots['geometry']]
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

'''def find_front_edge(resi_df, road_plots):
    resid_plots = resi_df.copy()
    resid_plots['simplified_geo'] = resid_plots['geometry'].simplify(1.5)
    resid_plots['orig_edges'] = resid_plots['geometry'].apply(lambda x: get_edges(x)) #['geometry']), axis=1)
    resid_plots['simplified_edges'] = resid_plots['simplified_geo'].apply(lambda x: get_edges(x))  # ['geometry']), axis=1)
    resid_plots['buffered_edges']  = [[y.buffer(6, single_sided=True) for y in x] for x in resid_plots['simplified_edges']] #list of buggered polygons
    checked_edges_per_plot = defaultdict(list) # to be filled in with plot ids : edge indices intersecting with rows
    road_polys_per_plot = defaultdict(list)  # to be filled in with plot ids : all edge indices intersecting with rows
    road_edges_per_plot_dict = defaultdict(list)  # to be filled in with plot ids : front edges

    front_edge_dict = defaultdict(list) # to be filled in with plot ids : front edges (buffered into polygons)
    for rd_index, rd_row in road_plots.iterrows():
        road_geo = rd_row['geometry']
        for plot_i, plot_row in resid_plots.iterrows():
            cur_plot = plot_row['PlotId']

            cur_buf_edges = plot_row['buffered_edges']
            cur_simplified_edges = plot_row['simplified_edges']
            for i in range (0, len(cur_buf_edges)):
                buf_edge = cur_buf_edges[i]
                simplified_edge = cur_simplified_edges[i]
                if i not in checked_edges_per_plot[cur_plot]: #skip if edge int with road already found (same plot could be next to several roads)
                    if road_geo.intersects(buf_edge): #check if edge intersects with road if not polygon.intersects(line):
                        int = buf_edge.intersection(road_geo)
                        int_area = int.area
                        if int_area / buf_edge.area > 0.05:
                            checked_edges_per_plot[cur_plot].append(i) #add edge to list of edges whose int with road already found
                            road_polys_per_plot[cur_plot].append((i, buf_edge.wkt, int_area)) #add edge index, buffered edge_geo to road edges per plot
                            road_edges_per_plot_dict[cur_plot].append((i, simplified_edge.wkt, int_area)) #add edge index, (non-buffered!) edge_geo to road edges per plot
    for plot_id, edge_info in road_edges_per_plot_dict.items():
        biggest_int_area = 0
        longest_edge = None
        if len(edge_info) > 0:
            for edge_tuple in edge_info:
                int_area2 = edge_tuple[2]
                if int_area2 > biggest_int_area:
                    longest_edge = wkt.loads(edge_tuple[1]) #transform string into shapely geometry

        front_edge_dict[plot_id] = longest_edge

    exceptions_ids = []
    exceptions_geo = []
    resid_plots['front_edge_geo'] = None
    for index, row in resid_plots.iterrows():
        cur_plot_id = row['PlotId']
        cur_plot_edges = row.simplified_edges
        try:
            resid_plots.loc[index, 'front_edge_geo']= front_edge_dict[cur_plot_id]
        except:
            resid_plots.loc[index, 'front_edge_geo'] = None
            exceptions_ids.append(cur_plot_id)
            exceptions_geo.append(cur_plot_edges)
    exceptions_df = pd.DataFrame({'plot_id':exceptions_ids, 'geometry':exceptions_geo})
    exceptions_df.to_excel(root + 'exceptions.xlsx')
    return(resid_plots)'''

#new version of method which uses neighboring plots column instead of going through all plots in planning area
def find_min_rect_edge_types(resi_df, road_plots):
    resid_plots = resi_df.copy()
    resid_plots.reset_index(drop=True, inplace=True)
    resid_plots['min_rect'] = [x.minimum_rotated_rectangle for x in resi_df.geometry]
    resid_plots['min_rect_edges'] = [get_edges(x) for x in resid_plots.min_rect]
    resid_plots['min_rect_edges_buffered'] = [[y.buffer(3, single_sided=False, cap_style=3) for y in x] for x in resid_plots['min_rect_edges']] #list of buggered polygons
    checked_edges_per_plot = defaultdict(list) # to be filled in
    road_polys_per_plot = defaultdict(list)  # to be filled in with plot ids : all edge indices intersecting with roads
    road_edges_per_plot_dict = defaultdict(list)  # to be filled in with plot ids : [edges intersecting with roads]
    front_edge_dict = defaultdict(list) # to be filled in with plot ids : front edges
    rear_edge_dict = defaultdict(list)  # to be filled in with plot ids : rear edges
    #find all edges that intersect with a road:

    road_neigh_found = 0
    all_searches = 0
    plots_with_valid_ints = 0
    for plot_i, plot_row in resid_plots.iterrows():
        cur_plot = plot_row['PlotId']
        cur_neighbours = plot_row['neighbour_list']
        #print('cur_neighbours1: ', cur_neighbours)
        cur_buf_edges = plot_row['min_rect_edges_buffered']
        #print('cur_buf_edges:', cur_buf_edges)
        cur_orig_edges = plot_row['min_rect_edges']
        #print('cur_orig edges:', cur_orig_edges)

        if any(x in road_plots['PlotId'].values.tolist() for x in cur_neighbours):
            road_neigh_found += 1
            all_searches += 1
        else:
            all_searches += 1

        cur_plot_valid_ints = 0
        for j in range(0, len(cur_neighbours)):
            if cur_neighbours[j] in road_plots['PlotId'].values:
                #print('cur_neighbours[j] in road plots')
                road_geo = road_plots.loc[road_plots['PlotId'] == cur_neighbours[j], 'geometry']
                '''if cur_plot in ['869B8DB1499626BC', 'D84F7C29EE59940D', '13369CF3B42E91B6', '15A79E90BDDFDB9A','214E0BF086EF5939']:'''

                for i in range (0, len(cur_buf_edges)):
                    buf_edge = cur_buf_edges[i]
                    orig_edge = cur_orig_edges[i]
                    #print('road_geo', road_geo)
                    #print('buf_edge:', buf_edge)
                    if road_geo.values.intersects(buf_edge): #check if edge intersects with road:
                        int_area = road_geo.intersection(buf_edge).area
                        int_ratio = (int_area / buf_edge.area).values[0]
                        print('int ratio: ', int_ratio)
                        #print('int ratio: ', int_ratio)
                        #print("intersection btw buffered edge and road found, intersection:", int_ratio, cur_plot)
                        if int_ratio > 0.2:
                            road_polys_per_plot[cur_plot].append((buf_edge.wkt, int_ratio))
                            print('road_polys_per_plot[cur_plot]: ', road_polys_per_plot[cur_plot])
                            #road_polys_per_plot[cur_plot].append((buf_edge.wkt, int_ratio, cur_orig_edges)) #add edge index, buffered edge_geo to road edges per plot
                            road_edges_per_plot_dict[cur_plot].append((i, orig_edge.wkt, orig_edge.length, cur_orig_edges, int_ratio)) #add edge index, (non-buffered!) edge_geo, length, and all original min rect edges to road edges per plot
                            cur_plot_valid_ints += 1
        if cur_plot_valid_ints > 0:
            plots_with_valid_ints += 1

    print('road neighbour found:', road_neigh_found, '/', all_searches)
    print('plots with at least 1 valid road int:', plots_with_valid_ints, '/', all_searches)

    #print('road_edges_per_plot_dict', road_edges_per_plot_dict)
    #find front edge, defined as the shortest edge that intersects with a road
    counter_success = 0
    counter_total = 0
    for plot_id, edge_info in road_edges_per_plot_dict.items():
        front_edge = None
        best_front_metric = 0
        front_edge_i = None
        rear_edge = None
        if len(edge_info) > 0:
            for edge_tuple in edge_info:
                edge_len = edge_tuple[2]
                road_int_ratio = edge_tuple[4]
                print('edge_len, road_int_ratio: ', edge_len, road_int_ratio)
                front_metric = 1/edge_len * road_int_ratio #front edge is short and has biggest possible road int
                if front_edge is None or front_metric > best_front_metric:
                    front_edge = wkt.loads(edge_tuple[1]) #transform string into shapely geometry
                    front_edge_i = edge_tuple[0]
        front_edge_dict[plot_id] = front_edge
        #cur_min_rect_edges = resid_plots.loc[resid_plots['PlotId'] == plot_id, 'min_rect_edges']
        #print('cur_min_rect_edges:', cur_min_rect_edges)

        cur_min_rect_edges = edge_tuple[3]
        for j in range(0, len(cur_min_rect_edges)):
            potential_rear = cur_min_rect_edges[j]
            if round(potential_rear.length, 3) == round(front_edge.length, 3) and j != front_edge_i:
                rear_edge = potential_rear
                rear_edge_dict[plot_id] = rear_edge
                counter_success +=1
                counter_total += 1
            else:
                counter_total+=1
        #print('rear edge added to rear edge dict, total len: ', counter_success, '/', counter_total)

    #print('front edge dict:', front_edge_dict)
    side_edge_dict = defaultdict(list)

    #find side edges, i.e. those min bounding box edges that have a different length than the front edge
    for index, row in resid_plots.iterrows():
        cur_id = row['PlotId']
        cur_edges = row['min_rect_edges']
        #print('cur_edges: ', cur_edges)
        cur_front_edge_len = 0
        if cur_id in front_edge_dict.keys():
            cur_front_edge_len = round(front_edge_dict[cur_id].length, 5)
        side_edges = []
        #print('side edges, cur id: ', side_edges, cur_id)
        for i in range (0, len(cur_edges)):
            if cur_front_edge_len != 0 and round(cur_edges[i].length, 5) != cur_front_edge_len:
                side_edges.append(cur_edges[i])
            elif cur_front_edge_len == 0:
                pass
                #print('front edge missing, plot: ', cur_id)

        side_edge_dict[cur_id] = side_edges
        #print('after creation side_edge_dict[plot_id] :', side_edge_dict[cur_id] )

    resid_plots['min_rect_front_edge'] = None
    resid_plots['min_rect_side_edge1'] = None
    resid_plots['min_rect_side_edge2'] = None
    resid_plots['min_rect_rear_edge'] = None

    road_polys_list = []
    #Add edges as columns to plot df
    for index, row in resid_plots.iterrows():
        cur_plot_id = row['PlotId']
        if cur_plot_id in side_edge_dict.keys() and type(side_edge_dict[cur_plot_id]) == list and len(side_edge_dict[cur_plot_id]) == 2:
            resid_plots.loc[index, 'min_rect_side_edge1'] = side_edge_dict[cur_plot_id][0]
            resid_plots.loc[index, 'min_rect_side_edge2'] = side_edge_dict[cur_plot_id][1]
        if cur_plot_id in front_edge_dict.keys():
            resid_plots.loc[index, 'min_rect_front_edge'] = front_edge_dict[cur_plot_id]
        if cur_plot_id in rear_edge_dict.keys():
            resid_plots.loc[index, 'min_rect_rear_edge'] = rear_edge_dict[cur_plot_id]
        road_polys_list.append(road_polys_per_plot[cur_plot_id] if cur_plot_id in road_polys_per_plot.keys() else None)

    resid_plots['road_polys_per_plot'] = road_polys_list
    return (resid_plots)

def classify_neighbours(plots_df, non_road_plots):
    neighbour_types = []
    return_val = None
    for plot_index, plot_row in plots_df.iterrows():
        neighbour_plots = plot_row['neighbour_list']
        cur_plot = plot_row['PlotId']
        print('----------------')
        print('cur plot: ', cur_plot)
        neighbour_dict = {}
        if plot_row['min_rect_side_edge1'] is not None:
            cur_side_edge1 = plot_row['min_rect_side_edge1'].buffer(3, single_sided=False, cap_style=3)
            cur_side_edge2 = plot_row['min_rect_side_edge2'].buffer(3, single_sided=False, cap_style=3)
            cur_rear_edge = plot_row['min_rect_rear_edge'].buffer(3, single_sided=False, cap_style=3)
            for neigh_id in neighbour_plots:
                neigh_geo = non_road_plots.loc[non_road_plots['PlotId'] == neigh_id, 'geometry'].values if neigh_id in non_road_plots.PlotId.values else None
                if neigh_geo is not None and len(neigh_geo) >1:  #plot 492F3A9D3EC7C5FB has 3 copies for some reason
                    neigh_geo = neigh_geo[0]
                print('neigh id: ', neigh_id)
                neigh_type = None
                biggest_int = 0
                #if neigh_geo == None:
                #neigh_geo = road_plots.loc[road_plots['PlotId'] == neigh_id, 'geometry'] if neigh_id in road_plots.PlotId.values else None
                print('neigh_geo: ', neigh_geo)
                if neigh_geo is not None and neigh_geo.is_valid:
                    print('neigh geo not none and is valid ')
                    if neigh_geo.intersects(cur_side_edge1):
                        int_area = neigh_geo.intersection(cur_side_edge1).area
                        if int_area > biggest_int:
                            biggest_int = int_area
                            neigh_type = 'side'
                    if neigh_geo.intersects(cur_side_edge2):
                        int_area = neigh_geo.intersection(cur_side_edge2).area
                        if int_area > biggest_int:
                            biggest_int = int_area
                            neigh_type = 'side'
                    if neigh_geo.intersects(cur_rear_edge):
                        int_area = neigh_geo.intersection(cur_rear_edge).area
                        if int_area > biggest_int:
                            biggest_int = int_area
                            neigh_type = 'rear'
                            print('rear found')


                    neighbour_dict[neigh_id] = neigh_type
        neighbour_types.append(neighbour_dict)
    plots_df['non_road_neighbour_types'] = neighbour_types
    return plots_df

def line_poly_ints(line_list, polygon):
    intersecting_edges = []
    orig_edge_lens = []
    for i in range(0, len(line_list)):
        cur_line = line_list[i]
        extended_line = shapely.affinity.scale(cur_line, xfact=2, yfact=2, zfact=2, origin='center')
        #print("line poly ints, 202: cur_line:", cur_line, polygon)

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
Buffer plot's front edge on both sides many times. Then take edges of the buffer polygon.
Find all buffered edges that intersect with the plot. Check which one of these is parallel to the original front edge.
Stretch the parallel one so that it intersects with the edges of the plot.
'''
def find_width_or_depth(resi_df, edge_col, output_geo_col_name, output_len_col_name):
    offset_edges_per_plot = defaultdict(list)
    for plot_index, plot_row in resi_df.iterrows():
        cur_edge = plot_row[edge_col]
        if type(cur_edge) == list and len(cur_edge) ==2:
            cur_edge = plot_row[edge_col][0]
        cur_plot_id = plot_row['PlotId']

        validity = gpd.GeoSeries(plot_row['geometry']).is_valid.values[0]
        if cur_edge is not None and validity:
            cur_plot_geo = plot_row['geometry']
            min_rect_edges = get_edges_no_simplification(cur_plot_geo.minimum_rotated_rectangle)
            min_rect_edges_lens = [x.length for x in min_rect_edges]
            shortest_edge_len = min(min_rect_edges_lens)
            longest_edge_len = max(min_rect_edges_lens)
            if shortest_edge_len < 16:
                buffer_dist = 3
            else:
                buffer_dist = 10
            number_of_offsets = longest_edge_len / buffer_dist + 3
            line_offsets = []
            for i in range(1, int(number_of_offsets)):
                cur_buffered_poly = cur_edge.buffer(i*buffer_dist, cap_style=2) #buffer front edge
                cur_buffered_edges = get_edges_no_simplification(cur_buffered_poly)
                intersecting_edge_info = line_poly_ints(cur_buffered_edges, cur_plot_geo) #i, int.wkt, int_length
                if len(intersecting_edge_info) > 0:
                    for index, row in intersecting_edge_info.iterrows():
                        cur_buffered_edge = row['original_length']
                        if round(row['original_length'], 1) != 2*i*buffer_dist:
                            line_offsets.append(row.geometry)
            offset_edges_per_plot[cur_plot_id] = line_offsets
        else:
            offset_edges_per_plot[cur_plot_id] = None

    resi_df[output_geo_col_name] = resi_df['PlotId'].apply(lambda x: offset_edges_per_plot[x])
    resi_df[output_len_col_name] = resi_df[output_geo_col_name].apply(lambda x: find_average_length(x))
    return resi_df


    '''    int_length = buf_edge.intersection(road_geo).length
    if int_length > longest_int:
        longest_int = int_length
        longest_edge_index = i
        print(longest_int, i)'''
    '''if longest_edge_index is not None:
        longest_edge_geo = orig_edges[longest_edge_index]
        return longest_edge_index, longest_edge_geo.wkt'''

def draw_sectors(plots_df, n_sectors, radius, subtracted_radius):
    sectors = []
    for index, row in plots_df.iterrows():
        cur_geo = row.geometry
        cur_centroid = cur_geo.centroid
        circle = cur_centroid.buffer(radius).simplify(0.5)
        xs, ys = circle.exterior.coords.xy
        point_on_circle = Point(xs[0], ys[0])
        line1 = shapely.affinity.scale(LineString([point_on_circle, cur_centroid]), xfact=2, yfact=2, origin=cur_centroid)
        line2 = shapely.affinity.rotate(line1, 360/n_sectors, cur_centroid)
        splitter = LineString([*line2.coords, *line1.coords[::-1]])
        split_results = shapely.ops.split(circle, splitter)
        if len(list(split_results.geoms)) >1:
            sector = split_results[1]
            splitting_circle = cur_centroid.buffer(subtracted_radius).simplify(0.5)
            sector_subtracted = sector.difference(splitting_circle)
            cur_sector_list = [sector_subtracted.wkt]
            for i in range(1, n_sectors+1):
                rot = i * 360/n_sectors
                rotated_sector_sub = shapely.affinity.rotate(sector_subtracted, rot, cur_centroid)
                cur_sector_list.append(rotated_sector_sub.wkt)
            sectors.append(cur_sector_list)
    plots_df['sectors'] = sectors
    return plots_df

def fringe_check(plots_df, target_plots, adjoining_empty_requirement, n_sectors):
    empty_sectors_per_plot = defaultdict(list)
    for index, row in plots_df.iterrows():
        cur_plot = row['PlotId']
        cur_sectors = row['sectors']
        empty_sec_indices = []
        target_plots_minus_current = target_plots.loc[target_plots['PlotId'] != cur_plot, :]
        for i in range(0, len(cur_sectors)):
            int_found = False
            for index2, target_row in target_plots_minus_current.iterrows():
                if wkt.loads(cur_sectors[i]).intersects(target_row.geometry):
                    int_found = True
                    break
            if not int_found:
                empty_sec_indices.append(i)

        gb = groupby(enumerate(empty_sec_indices), key=lambda x: x[0] - x[1])
        all_groups = ([i[1] for i in g] for _, g in gb) # Repack elements from each group into list
        a = list(all_groups)
        print('cur plot: ', cur_plot)
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

def check_if_corner_plot(plots_df, threshold_side_int_ratio):
    corner_plot_list = []
    for index, row in plots_df.iterrows():
        print('row[road_polys_per_plot]:', row['road_polys_per_plot'])
        if row['road_polys_per_plot'] == None:
            corner_plot_list.append(False)
        else:
            geo_list, cur_int_ratios = zip(*row['road_polys_per_plot'])
            cur_road_ints = gpd.GeoSeries(geo_list).apply(wkt.loads) #buffered polys that int with road
            cur_sides = [row['min_rect_side_edge1'], row['min_rect_side_edge2'] ]
            corner_plot_found = False
            for i in range (0, len(cur_road_ints)): #iterate through a plot's buffered edges that int with road
                if cur_int_ratios[i] > threshold_side_int_ratio and not corner_plot_found:
                    cur_poly = cur_road_ints[i] #current buffered edge
                    for edge in cur_sides:
                        if edge.within(cur_poly): #.length > (0.2 * edge.length):
                            corner_plot_list.append(True)
                            corner_plot_found = True
                            break
            if corner_plot_found == False:
                corner_plot_list.append(False)
    plots_df['is_corner_plot'] = corner_plot_list
    return(plots_df)

'''First check if landed housing-related regulations apply and if current residential type fulfills them'''
def lh_regulation_check(residential_type_reg_row, lh_info):
    cur_type_allowed_in_lh = residential_type_reg_row['allowed_in_landed_housing_area']
    lh_ok = False
    if cur_type_allowed_in_lh:
        plots_lh_type = lh_info.TYPE
        suitable_lh_types = residential_type_reg_row['landed_housing_type']
        if plots_lh_type.values in suitable_lh_types:
            lh_ok = True
    return lh_ok

def plot_geo_regulation_check(plot_row, regulation_row):
    index_for_min_values = None
    plot_geo_ok = False
    is_corner_plot = plot_row['is_corner_plot']
    cur_resi_type = regulation_row['residential_type']

    if cur_resi_type in ['Terrace_1', 'Terrace_2'] and is_corner_plot:
        index_for_min_values = 1
    elif cur_resi_type in ['Terrace_1', 'Terrace_2'] and not is_corner_plot:
        index_for_min_values = 2
    else:
        index_for_min_values = 0
    #print('index_for_min_values: ', index_for_min_values, regulation_row['min_area'][index_for_min_values], type(regulation_row['min_area'][index_for_min_values]))
    cur_type_min_area = 0 if np.isnan(regulation_row['min_area'][index_for_min_values]) else regulation_row['min_area'][index_for_min_values]
    #print('cur_type_min_area: ', cur_type_min_area)
    cur_type_min_width =  0 if np.isnan(regulation_row['min_average_width'][index_for_min_values]) else regulation_row['min_average_width'][index_for_min_values]

    cur_type_min_depth = 0 if np.isnan(regulation_row['min_average_depth']) else regulation_row['min_average_depth']

    #print('cur type min area width depth:', cur_type_min_area, cur_type_min_width, cur_type_min_depth)
    #print('cur type min area width depth orig:', regulation_row['min_area'][index_for_min_values], regulation_row['min_average_width'][index_for_min_values], regulation_row['min_average_depth'])
    #print('cur plot area width depth: ', plot_row['geometry'].area, plot_row['average_width'], plot_row['average_depth'])
    if (plot_row['geometry'].area > cur_type_min_area and plot_row['average_width'] > cur_type_min_width and plot_row['average_depth'] > cur_type_min_depth): #or plot_row['average_depth'] < cur_type_min_depth):
        plot_geo_ok = True
        #print('plot_geo_ok', plot_geo_ok)

    return plot_geo_ok

def find_allowed_residential_types(plots_df, roads_df, residential_regs_df, lh_df, sbp_df):
    all_plots_allowed_types = []
    for index, row_plot in plots_df.iterrows():
        allowed_resi_types = []
        cur_plot_id = row_plot['PlotId']
        lh_info = lh_df.loc[lh_df['PlotId'] == cur_plot_id, :] if cur_plot_id in lh_df['PlotId'].values else None
        sbp_info = sbp_df.loc[sbp_df['PlotId'] == cur_plot_id, :] if cur_plot_id in sbp_df['PlotId'].values else None
        in_gcba = False
        neighbour_ids = row_plot.neighbour_list
        neighbour_roads = roads_df.loc[roads_df['PlotId'].isin(neighbour_ids), :]
        print('neighbour_roads: ', neighbour_roads)

        for index_reg, row_reg in residential_regs_df.iterrows():
            cur_resi_type = row_reg['residential_type']
            #print('cur_resi_type: ', cur_resi_type)
            is_corner_plot = row_plot['is_corner_plot']
            is_fringe_plot = row_plot['fringe_plot']

            '''Criteria that must be true for cur_resi_type to be added to plot's list of allowable types'''
            zoning_type_ok = False
            lh_ok = False
            gcba_ok = False
            sb_ok = False
            geo_ok = False
            neighborhood_ok = False
            road_neighbours_ok = False
            #print('lh_info', lh_info)
            print('cur type:', cur_resi_type)
            suitable_zoning_types = [x.strip() for x in row_reg['zoning_type'].split(';')]
            print('row_reg[zoning_type]:', row_reg['zoning_type'], 'row_plot.PlotType:', row_plot.PlotType, row_plot.PlotType in suitable_zoning_types)
            if row_plot.PlotType in suitable_zoning_types: #set(row_plot.PlotType).intersection(set(row_reg['zoning_type'])): #in row_reg['zoning_type']: set(checklist).intersection(set(words))
                zoning_type_ok = True
                print('zoning_type ok')

            if lh_info is None:
                lh_ok = True
            else:
                lh_ok = lh_regulation_check(row_reg, lh_info)
                if lh_info.CLASSIFCTN.values == 'GOOD CLASS BUNGALOW AREA':
                    in_gcba = True

            if in_gcba == False and row_reg['allowed_only_in_GCBA'] == False:
                gcba_ok = True
            elif in_gcba == True and row_reg['allowed_only_in_GCBA'] == True:
                gcba_ok = True

            if sbp_info is not None: #not implemented properly because does not apply to demonstrator area
                suitable_sb_types = row_reg['sbp_housing_type']

                if len(sbp_info.ResidentialType.values) == 1:
                    if sbp_info.ResidentialType.values in suitable_sb_types or sbp_info.ResidentialType.isna:
                        sb_ok = True
                        print('sb ok')
                else:
                    print('sbp_info.ResidentialType.values > 1:', sbp_info.ResidentialType.values, cur_plot_id)
            else:
                sb_ok = True
                print('no sbp --> sb ok')

            if row_reg['neighborhood_requirement'] == 'residential_fringe' and is_fringe_plot:
                neighborhood_ok = True
            elif row_reg['neighborhood_requirement'] != 'residential_fringe':
                neighborhood_ok = True

            geo_ok = plot_geo_regulation_check(row_plot, row_reg)
            print('row_reg[required_road_cat]:', row_reg['required_road_cat'])
            print('neighbour_roads.category_number.values:', neighbour_roads.category_number.values)
            if len(row_reg['required_road_cat']) ==1 and row_reg['required_road_cat'][0] == 'nan':
                road_neighbours_ok = True

            elif len(neighbour_roads.category_number.values) > 0 and neighbour_roads.category_number.values[0] in (row_reg['required_road_cat']):
                road_neighbours_ok = True

            rules = [zoning_type_ok == True,
                    lh_ok == True,
                    gcba_ok == True,
                    sb_ok == True,
                    geo_ok == True,
                    neighborhood_ok == True,
                    road_neighbours_ok == True]
            if all(rules):
                allowed_resi_types.append(cur_resi_type)
                print('all_rules_passed')
            else:
                print(cur_plot_id, cur_resi_type, 'false:')
                print( 'zone ok:', zoning_type_ok, 'lh_ok:', lh_ok, 'gcba_ok:', gcba_ok, 'sb_ok:', sb_ok, 'geo_ok:', geo_ok, 'neighborhood_ok:', neighborhood_ok)

        all_plots_allowed_types.append(allowed_resi_types)

    plots_df['allowed_residential_types'] = all_plots_allowed_types
    return(plots_df)

def find_residential_gfa_row(plots_df, all_non_road_plots, residential_gfa_regs_df, hc_int_df, pab_int_df):
    relevant_regulatory_indices = []

    for plot_index, plot_row in plots_df.iterrows():
        cur_relevant_indices = []

        cur_plot_id = plot_row['PlotId']
        print('cur_plot_id:', cur_plot_id)
        cur_hc_area = hc_int_df.loc[hc_int_df['PlotId'] == cur_plot_id, 'INC_CRC'] if cur_plot_id in hc_int_df.PlotId.values else None
        cur_pab = pab_int_df.loc[pab_int_df['PlotId'] == cur_plot_id, 'PLN_AREA_N'] if cur_plot_id in pab_int_df.PlotId.values else None
        cur_gpr = plot_row['GPR']
        cur_allowed_types = plot_row['allowed_residential_types']
        cur_neighbour_ids = plot_row['neighbour_list']
        cur_non_road_neighbour_types = all_non_road_plots.loc[all_non_road_plots['PlotId'].isin(cur_neighbour_ids), 'PlotType']

        if len(cur_allowed_types) > 0:
            for reg_index, reg_row in residential_gfa_regs_df.iterrows():
                greater_than_in_gpr = False
                if type(reg_row['gpr_for_height']) != float and '>' in reg_row['gpr_for_height']:
                    reg_row['gpr_for_height'] = float(reg_row['gpr_for_height'].replace('>', ""))
                    greater_than_in_gpr == True
                #print('reg_row[gpr_for_height] after replace', reg_row['gpr_for_height'])
                #print('cur_gpr (plot)', cur_gpr)
                #print('greater_than_in_gpr ', greater_than_in_gpr)
                #print('residential_gfa_regs_df: ',residential_gfa_regs_df)
                prev_type_property = residential_gfa_regs_df.loc[reg_index - 1 if reg_index < 1 else reg_index, 'type_property']

                print('prev_type_property:', prev_type_property)
                print('cur_type_property:', reg_row['type_property'])
                prev_gpr_val = None
                if(prev_type_property) == reg_row['type_property'] and reg_index > 1:
                    prev_gpr_val = float(str(residential_gfa_regs_df.loc[reg_index - 1, 'gpr_for_height']).replace('>', ""))

                if pd.isnull(reg_row['gpr_for_height']):
                    rule1_gpr = True
                    #print('if: no gpr regulation')
                elif prev_gpr_val is None and cur_gpr <= float(reg_row['gpr_for_height']):
                    rule1_gpr = True
                    #print('if: no previous gpr val, plot gpr less than cur reg row')
                elif prev_gpr_val != None and (cur_gpr <= float(reg_row['gpr_for_height']) and cur_gpr > prev_gpr_val):
                    rule1_gpr = True
                    #print('if: prev gpr val exists, plots gpr between previous and current reg row val')
                elif greater_than_in_gpr and (cur_gpr > float(reg_row['gpr_for_height'])):
                    rule1_gpr = True
                    #print('if: reg row contains >, and plot gpr greater than reg row val')
                else:
                    rule1_gpr = False
                    #print('if: else')

                #if rule1_gpr:
                #print('rule1_gpr true')
                rule2_zt = True if plot_row['PlotType'] == reg_row['zoning_type'] else False
                #print('after rule2')
                #print('cur_hc_area', cur_hc_area)
                #print('reg_row[within_hc_boundary].values', reg_row['within_hc_boundary'], type(reg_row['within_hc_boundary']), str(reg_row['within_hc_boundary']))
                if cur_hc_area is not None:
                    rule3_hc = True if (cur_hc_area.values.any() == reg_row['within_hc_boundary']) else False
                    #print('rule3_hc true because cur_hc_area.values.any() =', cur_hc_area.values.any(), reg_row['within_hc_boundary'])
                else:
                    rule3_hc = True if str(reg_row['within_hc_boundary']) == 'nan' else False
                    #print("rule3_hc true because cur hcarea:", cur_hc_area, 'reg row hc:', reg_row['within_hc_boundary'])

                rule4_type_prop = True if reg_row['type_property'] == 'nan' or reg_row['type_property'] in cur_allowed_types else False
                rule5_pab = True if (reg_row['within_pab'][0] == 'nan' or cur_pab.values.any() in reg_row['within_pab']) else False
                type_context_neighbour_found = False
                #print('rule5_type_prop', rule5_type_prop)
                for x in reg_row['type_context']:
                    if x in cur_non_road_neighbour_types:
                        type_context_neighbour_found = True
                #print('reg_row[type_context]', reg_row['type_context'])
                #print('type_context_neighbour_found:', type_context_neighbour_found)
                rule6_neighbour = True if reg_row['type_context'][0] == 'nan' or type_context_neighbour_found == True else False
                all_rules = [rule1_gpr == True,
                             rule2_zt == True,
                             rule3_hc == True,
                             rule4_type_prop == True,
                             rule5_pab == True,
                             rule6_neighbour == True]
                #print('all rules:', all_rules)
                #print('cur_relevant_indices: ', cur_relevant_indices)
                if all(all_rules):
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
def find_edge_road_type(edge_geo, plot_neighbours, roads_df):
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
    return cur_edge_road_cat

def check_neighbour_gcba(edge_geo, plot_neighbours, lh_plots):
    offset_edge = edge_geo.buffer(6, single_sided=False, cap_style=3)
    neighbour_gcba = False
    for neighbour_id in plot_neighbours:
        if neighbour_id in lh_plots.PlotId:
            neighbour_classifctn = lh_plots.loc[lh_plots['PlotId'] == neighbour_id, 'CLASSIFCTN']
            if neighbour_classifctn == 'GOOD CLASS BUNGALOW AREA':
                neighbour_geo = lh_plots.loc[lh_plots['PlotId'] == neighbour_id, 'geometry']
                neighbour_gcba = True if neighbour_geo.intersects(offset_edge) else False
    return neighbour_gcba

def find_edge_setbacks2(edge_geo,non_rd_neigh_dict, all_neighs_list, non_road_plots_df, cur_rd_neighs_df, lh_plots, sbp_info, gfa_regs_for_cur_plot, rd_cat_offset_index_dict):
    print('----------find_edge_setbacks2 start-------------------')
    edge_buffered = edge_geo.buffer(6, single_sided=False, cap_style=3)
    edge_type = None
    road_cat = find_edge_road_type(edge_geo, all_neighs_list, cur_rd_neighs_df)
    biggest_int = 0
    for neighbor_id, neighbor_type in non_rd_neigh_dict.items(): #find type of first neighbour that edge intersects with
        biggest_non_rd_int = 0
        if neighbor_id in non_road_plots_df.PlotId.values:
            neighbor_geo = non_road_plots_df.loc[non_road_plots_df['PlotId'] == neighbor_id, 'geometry'].values
            if neighbor_geo.intersects(edge_buffered) and neighbor_geo.intersection(edge_buffered).area > biggest_int:
                print('neighbor type: ', neighbor_type)
                edge_type = neighbor_type if neighbor_type != 'front' else 'side'
                biggest_non_rd_int = neighbor_geo.intersection(edge_buffered).area
                biggest_int = biggest_non_rd_int
    for index, row in cur_rd_neighs_df.iterrows():
        cur_rd_geo = row['geometry']
        if cur_rd_geo.intersects(edge_buffered) and cur_rd_geo.intersection(edge_buffered).area > biggest_int:
            edge_type = 'front'

    if edge_type is None:
        edge_type = 'side'
    print('gfa_regs_for_cur_plot:', gfa_regs_for_cur_plot)
    edges_sbp_col_names_dict = {'front': 'Setback_Front', 'side':'Setback_Side', 'rear':'Setback_Rear'}
    edges_res_reg_col_names_dict = {'front': None, 'side': 'setback_side', 'rear': 'setback_rear'}
    setback_dict = {}

    neighbour_gcba = check_neighbour_gcba(edge_geo, all_neighs_list, lh_plots)
    neighbour_waterbody = False #placeholder
    print('gfa_regs_for_cur_plot:', gfa_regs_for_cur_plot)
    if not gfa_regs_for_cur_plot.empty:
        print('gfa regulations found')
        for index, row_reg in gfa_regs_for_cur_plot.iterrows():
            cur_type = row_reg['type_property']
            cur_setback = None

            if sbp_info is not None:
                sbp_col_name = edges_sbp_col_names_dict[edge_type]
                sbp_setback = sbp_info[sbp_col_name].values[0][0]
                cur_setback = float(sbp_setback) if not pd.isnull(sbp_setback) else None

            if cur_setback == None: # i.e. setback not found in sbp, look for it in residential regs
                #print('cur edge type: ', edge_type)
                res_reg_col_name = edges_res_reg_col_names_dict[edge_type]
                if road_cat != None:
                    print('road cat found: ', road_cat, road_cat[0])
                    if edge_type == 'front': #change to beginning of loop
                        #print('road cat not none: ', road_cat)
                        road_offset_index = rd_cat_offset_index_dict[road_cat[0]]
                        print('road offset index: ', road_offset_index)
                        cur_setback = float(row_reg['setback_road_general'][road_offset_index])
                        print('road edge type found')

                elif res_reg_col_name != None and edge_type != 'front':
                    print('res_reg_col_name: ', res_reg_col_name)
                    print('row_reg[setback_side][0]: ', row_reg[res_reg_col_name][0])
                    if pd.notnull(row_reg[res_reg_col_name][0]): # edge col name can be side or rear setback
                        offset_index = 1 if neighbour_gcba else 0
                        cur_setback = float(row_reg[res_reg_col_name][offset_index])
                    elif pd.notnull(row_reg['setback_common'][0]):
                        print('common setback: ', row_reg['setback_common'][0])
                        common_setback_offset_index = 1 if neighbour_gcba else 0
                        cur_setback = float(row_reg['setback_common'][common_setback_offset_index])
            setback_dict[cur_type] = cur_setback
    print('setback_dict ', edge_type, ': ', setback_dict)
    print('----------find_edge_setbacks2 end-------------------')
    return setback_dict, edge_type

#def classify_original_edges(plot_geo, roads_df )

def find_buildable_footprints2(plots_df, residential_gfa_regs_df, lh_plots, non_road_plots_df, roads_df, sbp_df):
    # This dict determines which road offset value is read from the Regulation Grouping 4 file, column 'setback_road_general'
    # Note that cat 3 gets the cat 2 offset (more conservative), since cats 2-3 not differentiated in ura data
    road_cat_offset_index_dict = {'cat_1': 0,
                            'cat_2_to_3' : 1,
                            'cat_4' : 3,
                            'cat_5' : 4}

    buildable_geos = []
    buildable_footprint_areas = []
    edges_types_per_plot_dict = defaultdict(list)  # to be filled in so that it is {PlotId : [(edge1_geo, edge1_type), (edge2_geo, edge2_type)...]
    for index, row_plot in plots_df.iterrows():
        front_edge = row_plot['min_rect_front_edge']
        cur_gfa_reg_indices = row_plot['GFA_reg_indices']
        cur_gfa_regs = residential_gfa_regs_df.loc[cur_gfa_reg_indices]
        #print('cur gfa regs type: ', cur_gfa_regs.type_property)
        buildable_geo_per_type_dict = {}
        buildable_area_per_type_dict = {}

        if front_edge == None or cur_gfa_regs.empty:
            pass
        else:
            edge_setbacks_per_type = []  # list of dicts for each edge
            plot_edges = get_edges(row_plot['geometry'])
            cur_plot_id = row_plot['PlotId']
            #print('cur plot id: ', cur_plot_id)
            sbp_info = sbp_df.loc[sbp_df['PlotId'] == cur_plot_id, :] if cur_plot_id in sbp_df['PlotId'].values else None
            cur_non_rd_neigh_dict = row_plot['non_road_neighbour_types']
            cur_all_neighbours = row_plot['neighbour_list']
            cur_rd_neighbours_df = roads_df.loc[roads_df['PlotId'].isin(cur_all_neighbours), :]
            #print('cur_rd_neighbours_df: ',cur_rd_neighbours_df, cur_rd_neighbours_df.columns )
            #print('cur_neighbours: ', cur_all_neighbours)
            cur_site_coverage = None
            buildable_geo = row_plot['geometry']
            for i in range (0, len(plot_edges)):
                cur_edge = plot_edges[i]
                cur_setbacks, edge_type = find_edge_setbacks2(cur_edge, cur_non_rd_neigh_dict, cur_all_neighbours, non_road_plots_df, cur_rd_neighbours_df, lh_plots, sbp_info, cur_gfa_regs, road_cat_offset_index_dict)
                #print('cur setbacks: ', cur_setbacks) # cur setbacks is e.g. {'Good_class_bungalow': 3.0}
                #print('edge_type: ', edge_type)

                edges_types_per_plot_dict[cur_plot_id].append((cur_edge.wkt, edge_type))
                edge_setbacks_per_type.append(cur_setbacks)

            #print('cur gfa regs: ', cur_gfa_regs)
            for index, row_reg in cur_gfa_regs.iterrows():
                cur_type = row_reg['type_property']
                print('cur type: ', cur_type)
                buildable_geo = row_plot['geometry']
                buildable_footprint_area = 0
                for i in range(0, len(edge_setbacks_per_type)):
                    edge_dict = edge_setbacks_per_type[i]
                    offset_for_cur_type = edge_dict[cur_type]
                    #print('edge_dict:', edge_dict)
                    #print('cur type: ', cur_type)
                    #print('plot_edges[i]:', plot_edges[i])
                    #print('offset_for_cur_type: ', offset_for_cur_type)
                    if offset_for_cur_type is not None: #this might happen if the edge does not have any neighbor due to e.g. removal of narrow plots at beginning of script. hence the plot's type (inferred based on neighbour) can't be found
                        setback_geo = plot_edges[i].buffer(offset_for_cur_type, single_sided=False, cap_style=3)
                    else:
                        setback_geo = plot_edges[i].buffer(0, single_sided=False, cap_style=3)
                        #print('buildable geo: ', buildable_geo)
                    #print('setback geo: ', setback_geo)
                    buildable_geo = buildable_geo - setback_geo


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

def process_plots_TWA(queried_plots):
    plots = queried_plots
    plots.geometry = plots.geometry.simplify(0.1)
    plots = plots[~(plots.geometry.type == "MultiPolygon")]
    plots['site_area'] = plots.area
    plots = plots.loc[plots['site_area' ] >= 50]
    plots = plots.rename(columns = {'cityObjectId':'PlotId', 'ZoningType':'PlotType'})
    plots['GPR'] = pd.to_numeric(plots['GPR'], errors = 'coerce')
    plots = plots[~plots['PlotType'].isin(['ROAD','WATERBODY', 'PARK', 'OPEN SPACE',
                                           'CEMETERY', 'BEACH AREA', 'TRANSPORT FACILITIES',
                                           'MASS RAPID TRANSIT'])]
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

    lh = gpd.read_file(fn_landed_housing).to_crs(3857)  # landed housing
    lh['STY_HT'] = lh['STY_HT'].map({'3-STOREY': 3, '2-STOREY': 2})

    pab = gpd.read_file(fn_planning_area_boundaries).to_crs(3857)  # planning area boundaries
    pab = pab.loc[:, ['PLN_AREA_N', 'INC_CRC', 'geometry']]
    # dcp = pd.read_excel(fn_control_plans)  # development control  plans
    # dcp['type_context'].fillna(value='default', inplace=True)
    # dcp['type_property'].fillna(value='default', inplace=True)
    con_int1 = intersect_with_regulation(plots_df, con, 0.1)  # 1414
    hc_int1 = intersect_with_regulation(plots_df, hc, 0.1)  # 853
    sb_int1 = intersect_with_regulation(plots_df, sb, 0.1)  # 402
    udr_int1 = intersect_with_regulation(plots_df, udr, 0.1)  # 478
    lh_int1 = intersect_with_regulation(plots_df, lh, 0.1)
    pab_int1 = intersect_with_regulation(plots_df, pab, 0.1)  # 1662

    #con, hc, sb, udr, lh, pab,
    return con_int1, hc_int1, sb_int1, udr_int1, lh_int1, pab_int1

def process_residential_regs(fn_residential_requirements, fn_residential_gfa_parameters):
    res_gfa_reg = pd.read_excel(fn_residential_gfa_parameters, usecols='A:T').drop(0)  # Drop row 0 because this is an explanation row meant for humans
    res_gfa_reg = to_list_str(res_gfa_reg, 'within_pab')
    res_gfa_reg = to_list_str(res_gfa_reg, 'type_context')
    res_gfa_reg = to_list(res_gfa_reg, 'setback_road_general')
    res_gfa_reg = to_list(res_gfa_reg, 'setback_road_first_floor_residential_with_commercial_at_1st_floor')
    #res_gfa_reg = to_list(res_gfa_reg, 'setback_front_irregular')
    res_gfa_reg = to_list(res_gfa_reg, 'setback_side')

    res_gfa_reg = to_list(res_gfa_reg, 'setback_rear')
    res_gfa_reg = to_list_str(res_gfa_reg, 'setback_common')
    res_gfa_reg = to_list(res_gfa_reg, 'setback_common_first_floor_residential_with_commercial_at_1st_floor')
    res_gfa_reg = to_list(res_gfa_reg, 'height_m')
    res_gfa_reg = to_list(res_gfa_reg, 'height_floor_to_floor')

    res_req = pd.read_excel(fn_residential_requirements, usecols='A:K').drop(
        0)  # Drop row 0 because this is an explanation row meant for humans
    res_req = to_bool(res_req, 'allowed_in_landed_housing_area')
    res_req = to_bool(res_req, 'allowed_only_in_GCBA')
    res_req = to_list_str(res_req, 'landed_housing_type')
    res_req = to_list_str(res_req, 'sbp_housing_type')
    res_req = to_list(res_req, 'min_area')
    res_req = to_list(res_req, 'min_average_width')
    res_req = to_list_str(res_req, 'required_road_cat')

    return res_gfa_reg, res_req

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
                    'no category': 'unknown'} #see chrome-extension://efaidnbmnnnibpcajpcglclefindmkaj/https://www.lta.gov.sg/content/dam/ltagov/industry_innovations/industry_matters/development_construction_resources/public_streets/pdf/RT-COP%20V2.0%20(3%20April%202019).pdf
    rd_plots = gpd.read_file(fn_rd_plots).to_crs(epsg=3857)
    rd_plots = rd_plots.rename(columns={'INC_CRC': 'PlotId', 'LU_DESC': 'PlotType'})
    rd_plots = assign_road_category(rd_plots, rn_lta)
    rd_plots['category_number'] = [road_cat_dict[x] for x in road_plots["RD_TYP_CD"]]
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
    plots_udr_unclear = (
        udr_int_df.loc[:, ["PlotId", "Type"]].groupby("PlotId").apply(lambda plot: plot.isin(type_dropped).values.any()))
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
fn_hc = root + "height_control/G_MP08_BUILDHTCTRL_STY_PL.shp"
fn_sb_boundary = root + "street_blocks/G_MP08_STREET_BLK_PLAN_PL.shp"
fn_sb_reg = root + "street_blocks/street_block_plans.xlsx"
fn_udr = root + "urban_design_guidelines/UrbanDesignGuidelines.shp"
fn_control_plans = root + "Regulation_grouping2.xlsx"
fn_landed_housing = root + "landed_housing/landed_housing.shp"
fn_roads = root + "roads/ROADNETWORKLINE.shp"
fn_road_plots = root + "roads/road_plots/roads.shp"
fn_residential_requirements = root + "residential_type_requirements.xlsx"
fn_planning_area_boundaries = root + "planning_area_boundaries/planning_boundary_area.shp"
fn_residential_gfa_parameters = root + "residential_GFA\Regulation_grouping4.xlsx"


#invalid_zones = ['RESERVE SITE', 'SPECIAL USE ZONE', 'UTILITY', 'RESIDENTIAL', 'WHITE',   'RESIDENTIAL WITH COMMERCIAL AT 1ST STOREY' 'COMMERCIAL & RESIDENTIAL']  # last 3 types are only temporary

all_zt_local_plots = process_all_local_plots(fn_plots)

twa_query_res = get_plots(endpoint)
all_zt_twa_plots = process_plots_TWA(twa_query_res)


all_zt_plots = all_zt_twa_plots #all_zt_local_plots #Change this to TWA if it works
plots_all_residential = all_zt_plots[all_zt_plots['PlotType'].isin(['RESIDENTIAL', 'COMMERCIAL & RESIDENTIAL', 'RESIDENTIAL WITH COMMERCIAL AT 1ST STOREY'])]

#plots_for_GFA = filter_residential_plots(plots_all_residential) #i.e. remove narrow, small plots
plots_for_GFA = plots_all_residential


road_plots = process_roads(fn_roads, fn_road_plots) #Add road plots
non_road_plots = gpd.overlay(all_zt_plots, road_plots, how='difference', keep_geom_type=True) #all non-road plots (all zoning types)

all_reg_ints = find_regulation_ints(plots_for_GFA, fn_con, fn_hc, fn_sb_boundary, fn_sb_reg, fn_udr , fn_control_plans, fn_landed_housing, fn_planning_area_boundaries)
con_int,hc_int, sb_int, udr_int, lh_int, pab_int = all_reg_ints
print('2. Intersections between plots and regulations found')
plots_for_GFA = remove_unclear_plots(plots_for_GFA, con_int, udr_int)
plots_for_GFA = assign_gprs(plots_for_GFA, sb_int) #replace plots' GPR value with smaller value from SBP, if it exists


'''Read residential regulations'''
all_resid_regs = process_residential_regs(fn_residential_requirements, fn_residential_gfa_parameters)
resid_gfa_params, resid_type_requirs = all_resid_regs
print('2. Processed plots & regulations loaded.')


# to identify small amount of plots for which setbacks come from udr.Doing it separately makes it faster. 63
#plots_for_setbacks = plots.copy()
#udr_setback_int = intersect_with_regulation(
    #gpd.GeoDataFrame(plots_for_setbacks, geometry=plots_for_setbacks.buffer(3)),
    #udr.loc[~udr["Setbacks"].isna(), :], 0)
"""
Assign GPR for plots with EDUCATIONAL INSTITUTION, CIVIC & COMMUNITY INSTITUTION and PLACE OF WORSHIP zoning type.
Assignment is based on the relation to landed housing boundaries or surrounding context, like GPR of neighboring plots.
Fringe is implemented as a buffer zone around every plot of respective zoning types.
"""
# Assign GPR for plots with specific zoning type or overwrite if in a street block.
#assign_gpr(plots, 'EDUCATIONAL INSTITUTION', lh, 400, 1, 1, 1.4, 3, 4)  # 37 plots
#assign_gpr(plots, 'CIVIC & COMMUNITY INSTITUTION', lh, 400, 1, 1, 1.4, 3, 4)  # 89 plots
#assign_gpr(plots, 'PLACE OF WORSHIP', lh, 400, 1, 1.4, 1.6, 4, 5)  # 46 plots

"""
Extract Partywall edges.
Check if plot id is in any of the intersection dataframes.
Set Party_Wall boolean True if in any of the intersection dataframes it is True.
Identify party wall edges by checking if edge buffer is in any other plot that has Party_Wall set to True.
"""
#plots = set_partywall(plots, sb_int, udr_int)  # 474 plots with partywalls



#Add column indicating whether each plot is regular (True) or irregular (False). This isn't currently used in the script.
#plots_for_GFA['regular'] = [is_regular(x) for x in plots_for_GFA['geometry']]

plots_for_GFA = draw_sectors(plots_for_GFA, 10, 400, 100) #number params: n_sectors, radius of sector, radius of part subtracted from sector middle (to avoid neighbors intersecting with sector)
print('sectors_drawn')
plots_for_GFA = fringe_check(plots_for_GFA, plots_all_residential, 2, 10) #number params: required consecutive empty sectors, n sectors
print('fringe check done')
plots_for_GFA = find_neighbours(plots_for_GFA, all_zt_plots, road_plots) #add column with list of neighbours for each plot
print('neighbours found')

plots_for_GFA = find_min_rect_edge_types(plots_for_GFA, road_plots)
print('min_rect_edge types found and added to plots df')

plots_for_GFA = classify_neighbours(plots_for_GFA, all_zt_plots)
print('neighbours classified')

#Add columns for average width and depth of each plot
plots_for_GFA = find_width_or_depth(plots_for_GFA, 'min_rect_front_edge', 'offset_front_edges_geo', 'average_width')
plots_for_GFA = find_width_or_depth(plots_for_GFA, 'min_rect_side_edge1', 'offset_side_edges_geo', 'average_depth')
print('min depth and width of plots found')

# Add column indicating if plot is a corner plot
plots_for_GFA = check_if_corner_plot(plots_for_GFA, 0.3)

plots_for_GFA = find_allowed_residential_types(plots_for_GFA, road_plots, resid_type_requirs, lh_int, sb_int)

plots_for_GFA = find_residential_gfa_row(plots_for_GFA, non_road_plots, resid_gfa_params, hc_int, pab_int)

plots_for_GFA, edge_types_per_plot = find_buildable_footprints2(plots_for_GFA, resid_gfa_params, lh_int,non_road_plots, road_plots, sb_int)

L = [(k, *t) for k, v in edge_types_per_plot.items() for t in v]
all_edges_df = pd.DataFrame(L, columns=['PlotId', 'edge_geo', 'edge_type'])
front_edges = all_edges_df.loc[all_edges_df['edge_type'] == 'front', :]
side_edges = all_edges_df.loc[all_edges_df['edge_type'] == 'side', :]
rear_edges = all_edges_df.loc[all_edges_df['edge_type'] == 'rear', :]
front_edges.to_csv(root+'front_edges_qgis.csv', index=False, sep=';')
side_edges.to_csv(root+'side_edges_qgis.csv', index=False, sep=';')
rear_edges.to_csv(root+'rear_edges_qgis.csv', index=False, sep=';')

buildable_geos = plots_for_GFA['buildable_geo'].apply(pd.Series)
buildable_geos.Good_class_bungalow.to_csv(root+'buildable_gcb_qgis.csv', index=False, sep=';')
buildable_geos.Good_class_bungalow.to_csv(root+'buildable_gcb_excel.csv', index=False, sep=',')
buildable_geos.Terrace_2.to_csv(root+'buildable_terrace2_qgis.csv', index=False, sep=';')
buildable_geos.Terrace_2.to_csv(root+'buildable_terrace2_excel.csv', index=False, sep=',')
buildable_geos['Semi-detached'].to_csv(root+'buildable_semi_detached_excel.csv', index=False, sep=',')
buildable_geos['Semi-detached'].to_csv(root+'buildable_semi_detachedqgis.csv', index=False, sep=';')
buildable_geos.Terrace_1.to_csv(root+'buildable_terrace1_qgis.csv', index=False, sep=';')
buildable_geos.Terrace_1.to_csv(root+'buildable_terrace1_excel.csv', index=False, sep=',')
buildable_geos.Bungalow.to_csv(root+'buildable_bungalow_qgis.csv', index=False, sep=';')
buildable_geos.Bungalow.to_csv(root+'buildable_bungalow_excel.csv', index=False, sep=',')
buildable_geos.Flat.to_csv(root+'buildable_flat_qgis.csv', index=False, sep=';')
buildable_geos.Flat.to_csv(root+'buildable_flat_excel.csv', index=False, sep=',')
buildable_geos.Condo.to_csv(root+'buildable_condo_qgis.csv', index=False, sep=';')
buildable_geos.Condo.to_csv(root+'buildable_condo_excel.csv', index=False, sep=',')


plots_for_GFA = find_allowed_gfa(plots_for_GFA, resid_gfa_params, lh_int, sb_int)


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

