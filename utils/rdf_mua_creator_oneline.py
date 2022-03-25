"""
This script creates rdf files for mixed-use plot's programme profiles formulated in the mua paper.
Each plot contains only one line about the ratios of each programme type.

Credit: Zhongming Shi, Heidi Silvennoinen (Singapore-ETH Centre)
"""


import pandas as pd
from rdflib import URIRef, Dataset
from rdflib.namespace import RDF



'''Ontology URIs'''
tbox_uri = 'http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#'
tbox_uri_local = 'http://localhost/berlin/cityobject/'
#abox_uri =  'http://www.theworldavatar.com/kb/ontozoning'


# Link to your local path to the CKG folder
cur_dir = 'C:\\Users\\ShiZhongming\\Dropbox\\Cities Knowledge Graph - [all team members]\\'

hasProgramme_uri = tbox_uri + 'hasProgramme'
hasProgrammeKey_uri = tbox_uri + 'hasProgrammekey'
plot_class_uri = tbox_uri + 'Plot'
hasProgrammeRatio_uri = tbox_uri + 'hasProgrammeRatio'

path_to_instantiate = cur_dir + 'Research\WP1 - OntoCityGML\Data Transformation\MUA\programme_profiles_5435_uuid.csv'
path_output_oneline = cur_dir + 'Research\WP1 - OntoCityGML\Data Transformation\MUA\programme_profiles_output_oneline.txt'
path_output_mlines = cur_dir + 'Research\WP1 - OntoCityGML\Data Transformation\MUA\programme_profiles_output_mlines.txt'

'''Read csv file with plot_id (not yet matched to citygml plot URIs) and programme profiles'''
plots_df = pd.read_csv(path_to_instantiate)
plots_df['ratio'] = plots_df.loc[:, 'gym':'unknown'].astype(str).add(',').sum(axis=1).str.rstrip(',')

'''Create an empty dataset'''
ds = Dataset()
g = ds.graph(URIRef('http://localhost/berlin/plot'))


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
for key, val in Programme_type_dict.items():
    programme_type_list.append(val)
programme_type = ','.join(programme_type_list)

'''Fill dataset with 1) subject, 2) predicate, 3) object and 4) graph URIs for each item in your data'''

# Creating Plot hasProgramme ProgrammeKey
cur_programme = programme_type.strip("<>")
cur_programme_uri = tbox_uri + cur_programme
ds.add((URIRef(plot_class_uri), URIRef(hasProgrammeKey_uri), URIRef(cur_programme_uri),
        g))  # add row with subject (any plot), predicate (hasProgrammeKey), object, graph

for i in range(0, len(plots_df)):
# for i in range(0, 2):

    cur_cityobject = plots_df.at[i, 'UUID'].__str__().strip("<>") + '/'
    cur_uri = tbox_uri_local + cur_cityobject
    cur_ratio = plots_df.at[i, 'ratio'].strip("<>")
    cur_ratio_uri = tbox_uri + cur_ratio

    ds.add((URIRef(cur_uri), RDF.type ,  URIRef(plot_class_uri), g )) # add row with subject, predicate (is a plot), object, graph
    ds.add((URIRef(cur_uri), URIRef(hasProgrammeRatio_uri), URIRef(cur_ratio_uri), g))  # add row with subject, predicate (has programme_ratio), object, graph
    # print(ds)

'''Save the dataset in nquads format (this format accepts the 4th graph argument too)'''
file = open(path_output_oneline, mode="w")
file.write(ds.serialize(format='nquads'))


