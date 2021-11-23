"""
create_shapefile takes in the data retrieved from knowledge graph queries and creates a shapefile in the
input format required by the CEA
"""

import shapely
import pandas as pd
import geopandas as gpd
import argparse
import json
import math


def create_shapefile(geometry, height, shapefile):
    """
    :param geometry: Contains string of coordinates representing building envelope

    :param height: Height above ground of building

    :param shapefile: Name and path of shapefile to be created

    """
    # Convert geometry data to arrays of points
    split_data = geometry.split("#")
    points = [[] for i in range(4)]

    # Extract x-y data
    for a in range(0, 4):
        for b in range(0, 3):
            if b == 0 or b == 1:
                points[a].append(float(split_data[a*3+b]))
            else:
                points[a].append(0.0)

    floors_bg = 0
    height_bg = 0
    geometry_values = str(points)
    floor_height = 3.6
    name = "B" + str("001")

    # Set floors_ag to building height divided by HDB floor-to-floor height 3.6m (rounded down)
    floors_ag = math.floor(height/floor_height)
    data = {'Name':  name,
            'floors_bg': floors_bg,
            'floors_ag': floors_ag,
            'geometry': geometry_values,
            'height_bg': height_bg,
            'height_ag': height
            }

    df = pd.DataFrame([data])

    geometry = [shapely.geometry.polygon.Polygon(json.loads(g)) for g in df.geometry]
    df.drop('geometry', axis=1)

    crs = {"lon_0": 7.439583333333333, "k_0": 1, "ellps": "bessel", "y_0": 200000, "no_defs": True, "proj": "somerc",
           "x_0": 600000, "units": "m", "lat_0": 46.95240555555556}
    gdf = gpd.GeoDataFrame(df, crs=crs, geometry=geometry)
    gdf.to_file(shapefile, driver='ESRI Shapefile', encoding='ISO-8859-1')

def main(argv):

    shapefile_loc = "C:\\Users\\ELLO01\\Documents\\testProject\\"
    shapefile_file = "zone.shp"
    shapefile = shapefile_loc+shapefile_file
    data_dictionary = json.loads(argv.data)

    try:
        create_shapefile(data_dictionary['geometry'], float(data_dictionary['height']), shapefile)
    except IOError:
        print('Error while processing file: ' + shapefile)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()

    # add arguments to the parser
    parser.add_argument("data")

    # parse the arguments
    args = parser.parse_args()
    main(args)
