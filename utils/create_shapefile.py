"""
create_shapefile takes in the data retrieved from knowledge graph queries and creates a shapefile in the
input format required by the CEA
"""

import shapely
import pandas as pd
import geopandas as gpd
import argparse
import json
from pyproj import CRS
import os


def create_shapefile(geometries, heights, shapefile):
    """
    :param geometries: Contains string of coordinates representing building envelope

    :param heights: Height above ground of building

    :param shapefile: Name and path of shapefile to be created

    """

    geometry_values = []
    floors_bg = []
    height_bg = []
    floor_height = []
    names = []
    i = 0
    floors_ag = []
    # Convert geometry data to arrays of points
    for geom in geometries:

        split_data = geom.split("#")
        points = [[] for i in range(4)]

        # Extract x-y footprint data
        for a in range(0, 4):
            for b in range(0, 3):
                if b == 0 or b == 1:
                    points[a].append(float(split_data[a * 3 + b]))
                else:
                    points[a].append(0.0)

        geometry_values.append(str(points))
        floors_bg.append(0)
        height_bg.append(0)
        floor_height.append(3.2)
        names.append("B00" + str(i))
        i = i+1

    # Set floors_ag to building height divided by HDB floor-to-floor height 3.6m (rounded down)
    for x in range(len(heights)):
        floors_ag.append(round(heights[x] / floor_height[x]))

    zone_data = {'Name': names,
            'floors_bg': floors_bg,
            'floors_ag': floors_ag,
            'geometry': geometry_values,
            'height_bg': height_bg,
            'height_ag': heights}

    df = pd.DataFrame(data=zone_data)

    geometry = [shapely.geometry.polygon.Polygon(json.loads(g)) for g in df.geometry]
    df.drop('geometry', axis=1)

    # crs = CRS.from_user_input(24500)
    crs = CRS.from_user_input(27700)
    gdf = gpd.GeoDataFrame(df, crs=crs, geometry=geometry)
    gdf.to_file(shapefile, driver='ESRI Shapefile', encoding='ISO-8859-1')


def main(argv):
    shapefile_file = "zone.shp"
    shapefile = argv.zone_file_location + os.sep + shapefile_file
    data_dictionary = json.loads(argv.data)
    geometries = []
    heights = []

    for data in data_dictionary:
        geometries.append(data['geometry'])
        heights.append(float(data['height']))

    try:
        create_shapefile(geometries, heights, shapefile)
    except IOError:
        print('Error while processing file: ' + shapefile)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()

    # add arguments to the parser
    parser.add_argument("data")
    parser.add_argument("zone_file_location")

    # parse the arguments
    args = parser.parse_args()
    main(args)
