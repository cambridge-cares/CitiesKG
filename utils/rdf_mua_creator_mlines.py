"""
This script creates rdf files for mixed-use plot's programme profiles formulated in the mua paper.
Each plot contains multiple lines of ProgrammeRatio information with one line for each programme type ratio.

Credit: Zhongming Shi, Heidi Silvennoinen (Singapore-ETH Centre)
"""

import pandas as pd
from rdflib import URIRef, Dataset, Literal
from rdflib.namespace import RDF
from rdflib.namespace import FOAF


# Link to your local path to the CKG folder
cur_dir = 'C:\\Users\\ShiZhongming\\Dropbox\\Cities Knowledge Graph - [all team members]\\'

'''Ontology URIs'''
tbox_uri = 'http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#'
tbox_uri_local = 'http://localhost/berlin/cityobject/'
tbox_uri_twa = 'http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/cityobject/'
#abox_uri =  'http://www.theworldavatar.com/kb/ontozoning'

hasProgramme_uri = tbox_uri + 'hasProgramme'
plot_class_uri = tbox_uri + 'Plot'
hasProgrammeRatio_uri = tbox_uri + 'hasProgrammeRatio'

path_to_instantiate = cur_dir + 'Research\WP1 - OntoCityGML\Data Transformation\MUA\programme_profiles_5435_uuid.csv'
path_output_oneline = cur_dir + 'Research\WP1 - OntoCityGML\Data Transformation\MUA\programme_profiles_output_oneline.txt'
path_output_mlines = cur_dir + 'Research\WP1 - OntoCityGML\Data Transformation\MUA\programme_profiles_output_mlines_berlin.txt'

'''Read csv file with plot_id (not yet matched to citygml plot URIs) and programme profiles'''
plots_df = pd.read_csv(path_to_instantiate)

'''Create an empty dataset'''
ds = Dataset()
g = ds.graph(URIRef('http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/MixedUsePlotProgrammeRatios/'))


'''Match mua programme type names with the programme type names in the zoning ontology'''
Programme_type_dict = {'gym': 'Gym',
                       'bank': 'Bank/PostOffice',
                       'apparel_store': 'ApparelStore',
                       'car_repair': 'CarRepair/CarWash',
                       'electronics_store': 'ElectronicsStore',
                       'car_rental': 'CarDealer/CarRental',
                       'veterinary_care': 'PetStore/VeterinaryCare',
                       'restaurant': 'Restaurant',
                       'doctor': 'HealthService',
                       'beauty_service': 'BeautyService',
                       'supermarket': 'Supermarket',
                       'school': 'School',
                       'bar/cafe': 'Bar/Cafe',
                       'department_store': 'DepartmentStore',
                       'laundry': 'Laundry',
                       'convenience_store': 'ConvenienceStore',
                       'movie_rental': 'MovieRental',
                       'furniture_store': 'FurnitureStore',
                       'hardware_store': 'HardwareStore',
                       'home_goods_store': 'HomeGoodsStore',
                       'locksmith': 'Locksmith',
                       'book_store': 'BookStore',
                       'pharmacy': 'Pharmacy',
                       'bicycle_store': 'BicycleStore',
                       'florist': 'Florist',
                       'jewelry_store': 'JewelryStore',
                       'lodging': 'Lodging',
                       'art_gallery': 'ArtGallery',
                       'liquor_store': 'LiquorStore',
                       'night_club': 'NightClub',
                       'museum': 'Museum',
                       'embassy': 'Embassy',
                       'library': 'Library',
                       'movie_theater': 'MoiveTheater',
                       'casino': 'Casino',
                       'bowling_alley': 'BowlingAlley',
                       'unknown': 'OtherProgramme'
                       }
programme_type_list = []

'''Fill dataset with 1) subject, 2) predicate, 3) object and 4) graph URIs for each item in your data'''
# Creating Plot hasProgramme Office, Plot hasProgramme Restaurant, ...
for key, val in Programme_type_dict.items():
    cur_uri = tbox_uri + val
    ds.add((URIRef(plot_class_uri), URIRef(hasProgramme_uri), URIRef(cur_uri), g)) # add row with subject (Plot), predicate (hasProgramme), object, graph

#for i in range(0, len(plots_df)):
for i in range(0, 2):
    cur_cityobject = plots_df.at[i, 'UUID'].__str__().strip("<>") + '/'
    cur_uri = tbox_uri_local + cur_cityobject
    ds.add((URIRef(cur_uri), RDF.type ,  URIRef(plot_class_uri), g )) # add row with subject, predicate (is a plot), object, graph

    for key, val in Programme_type_dict.items():
        hasProgrammeRatio_uri = tbox_uri + 'hasRatio' + val
        cur_ratio = plots_df.at[i, key].astype(str)
        # cur_ratio_uri = tbox_uri + cur_ratio

        ds.add((URIRef(cur_uri), URIRef(hasProgrammeRatio_uri), Literal(cur_ratio), g))  # add row with subject, predicate (has programme_ratio), object, graph
    # print(ds)

'''Save the dataset in nquads format (this format accepts the 4th graph argument too)'''
file = open(path_output_mlines, mode="w")
file.write(ds.serialize(format='nquads'))




