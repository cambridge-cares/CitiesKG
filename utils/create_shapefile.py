"""
Adapted from CEA script
``excel-to-shapefile``
"""

import shapely
import pandas as pd
import geopandas as gpd
import argparse
import json


def export(geometry, floors_ag, shapefile):
    """
    :param geometry: Contains string of coordinates representing building envelope

    :param floors_ag: Number of floors above ground in building

    :param shapefile: Name and path of shapefile to be created

    """
    # Convert geometry data to arrays of points
    split_data = geometry.split("#")
    points = [[] for i in range(4)]

    for x in range(0, 4):
        for y in range(0, 3):
            points[x].append(float(split_data[x*3+y]))

    floors_bg = 0
    height_bg = 0
    geometry_values = str(points)
    floor_height = 3.5
    name = "B" + str("001")
    height_ag = floors_ag * floor_height

    data = {'Name':  name,
            'floors_bg': floors_bg,
            'floors_ag': floors_ag,
            'geometry': geometry_values,
            'height_bg': height_bg,
            'height_ag': height_ag
            }

    df = pd.DataFrame([data])

    geometry = [shapely.geometry.polygon.Polygon(json.loads(g)) for g in df.geometry]
    df.drop('geometry', axis=1)

    crs = {"lon_0": 7.439583333333333, "k_0": 1, "ellps": "bessel", "y_0": 200000, "no_defs": True, "proj": "somerc",
           "x_0": 600000, "units": "m", "lat_0": 46.95240555555556}

    gdf = gpd.GeoDataFrame(df, crs=crs, geometry=geometry)
    gdf.to_file(shapefile, driver='ESRI Shapefile', encoding='ISO-8859-1')


def main(argv):

    shapefile_loc = "C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario\\inputs\\building-geometry\\"
    shapefile_file = "zone.shp"
    shapefile = shapefile_loc+shapefile_file

    data_dictionary = json.loads(argv.data)

    try:
        export(data_dictionary['geometry'], int(data_dictionary['floors_ag']), shapefile)
    except IOError:
        print('Error while processing file: ' + shapefile)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()

    # add arguments to the parser
    parser.add_argument("data")

    # parse the arguments
    args = parser.parse_args()
    main(args)
