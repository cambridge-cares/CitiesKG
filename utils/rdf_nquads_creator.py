import pandas as pd
from rdflib import URIRef, Dataset
from rdflib.namespace import RDF


cur_dir = 'C:\\Users\\HeidiSilvennoinen\\Dropbox (ontoZoning)\\Zoning ontology paper\\plot_data'

'''Ontology URIs'''
tbox_uri = 'http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#'
hasZone_uri = tbox_uri + 'hasZone'
plot_class_uri = tbox_uri + 'Plot'
zone_class_uri = tbox_uri + 'Zone'

g = ds.graph(URIRef('http://localhost/berlin/plot'))


'''Read csv file with citygml plot URIs and LU_DESC values'''
plots_df = pd.read_csv(cur_dir + '\\plots_mixed_use.csv')

'''Create an empty dataset'''
ds = Dataset()

'''Match citygml LU_DESC values with the zone names in the zoning ontology'''
LUdesc_zones_dict = {'COMMERCIAL' : 'CommercialZone', 'WHITE': 'White',
                     'RESIDENTIAL WITH COMMERCIAL AT 1ST STOREY': 'ResidentialWithCommercialAt1stStorey',
                    'HOTEL': 'HotelZone',
                     'COMMERCIAL & RESIDENTIAL' : 'CommercialAndResidential'}

'''Add quads to the dataset saying each zoning type belongs to the class Zone'''
for key, val in LUdesc_zones_dict.items():
    cur_uri = tbox_uri + val
    ds.add((URIRef(cur_uri), RDF.type, URIRef(zone_class_uri), g))

'''Add quads stating zoning type of each plot in your data'''
for i in range(0,len(plots_df)):
    cur_uri = plots_df.at[i, 'CityObject'].strip("<>")
    cur_LU_DESC = plots_df.at[i, 'Zone'].strip("<>")
    cur_zone_uri = tbox_uri + LUdesc_zones_dict[cur_LU_DESC]


    ds.add( (URIRef(cur_uri), RDF.type ,  URIRef(plot_class_uri), g )) # add row with subject, predicate (is a plot), object, graph
    ds.add( (URIRef(cur_uri), URIRef(hasZone_uri) , URIRef(cur_zone_uri), g)) # add row with subject, predicate (has zone), object, graph



'''Save the dataset in nquads format (this format accepts the 4th graph argument too)'''
file = open(cur_dir + "\\output9.txt", mode="w")
file.write(ds.serialize(format= 'nquads'))


