import uuid
import requests
import pandas as pd
from rdflib import Dataset, Literal
from rdflib.namespace import RDF
from rdflib.namespace import XSD
from requests.exceptions import HTTPError
from GFAManager import *

json_dir = "C:/Users/AydaGrisiute/Dropbox/Cities Knowledge Graph - [all team members]/Research/WP6 - Use Cases/PlanningConceptOntology/estimate_GFA[AG][HS]_2022_08_25.json"
cur_dir = 'C://Users/AydaGrisiute/Desktop'
endpoint = "http://192.168.0.144:9999/blazegraph/namespace/singaporeEPSG4326/sparql"


class TripleDataset:

    def __init__(self):
        self.dataset = Dataset()

    # writes necessary triples to represent a trip.
    def create_gfa_triples(self, city_object_uri, gfa_value, zoning_case="default"):
        # uris
        city_object_uri = URIRef(city_object_uri)
        buildable_space = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        gfa = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        measure = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))

        # literals
        gfa_value = Literal(str(gfa_value), datatype=XSD.decimal)
        lod_1 = Literal(str("LOD1"), datatype=XSD.string)

        self.dataset.add((city_object_uri, GFAOntoManager.HAS_BUILDABLE_SPACE, buildable_space, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((buildable_space, RDF.type, GFAOntoManager.BUILDABLE_SPACE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((buildable_space, GFAOntoManager.AT_LEVEL_OF_DETAIL, lod_1, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((buildable_space, GFAOntoManager.HAS_ALLOWED_GFA, gfa, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((gfa, RDF.type, GFAOntoManager.GROSS_FLOOR_AREA, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((gfa, GFAOntoManager.HAS_VALUE, measure, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((measure, RDF.type, GFAOntoManager.MEASURE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((measure, GFAOntoManager.HAS_UNIT, GFAOntoManager.SQUARE_PREFIXED_METRE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((GFAOntoManager.SQUARE_PREFIXED_METRE, RDF.type, GFAOntoManager.AREA_UNIT, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
        self.dataset.add((measure, GFAOntoManager.HAS_NUMERIC_VALUE, gfa_value, GFAOntoManager.BUILDABLE_SPACE_GRAPH))

        if zoning_case != "default":
            self.dataset.add((buildable_space, GFAOntoManager.FOR_ZONING_CASE, URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + zoning_case), GFAOntoManager.BUILDABLE_SPACE_GRAPH))

    def create_height_control_triples(self, city_object_uri, ext_ref, height, unit):

        height_control_plan = URIRef(city_object_uri)
        external_reference_uri = URIRef(ext_ref)
        absolute_height = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        storey_aggregate = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        measure = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        height_value = Literal(str(height), datatype=XSD.integer)

        self.dataset.add((height_control_plan, RDF.type, GFAManager.HEIGHT_CONTROL_PLAN, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((height_control_plan, GFAOntoManager.hasExternalRef, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if 'NUMBER OF STOREYS' in unit:
            self.dataset.add((height_control_plan, GFAOntoManager.ALLOWS_STOREY_AGGREGATE, storey_aggregate, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((storey_aggregate, RDF.type, GFAOntoManager.STOREY_AGGREGATE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((storey_aggregate, GFAOntoManager.HAS_AGGREGATE_FUNCTION, GFAOntoManager.MAXIMUM, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((storey_aggregate, GFAOntoManager.NUMBER_OF_STOREYS, height_value, FAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            #could add more triples representing every storey.
        elif 'METRES BASED ON SHD' in unit:
            self.dataset.add((height_control_plan, GFAOntoManager.ALLOWS_ABSOLUTE_HEIGHT, absolute_height, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((absolute_height, RDF.type, GFAOntoManager.ABSOLUTE_HEIGHT, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((absolute_height, GFAOntoManager.HAS_AGGREGATE_FUNCTION, GFAOntoManager.MAXIMUM, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((absolute_height, GFAOntoManager.HAS_VALUE, measure, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((measure, RDF.type, GFAOntoManager.MEASURE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((measure, GFAOntoManager.HAS_UNIT, GFAOntoManager.METRE, GGFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((GFAOntoManager.METRE, RDF.type, GFAOntoManager.AREA_UNIT, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((measure, GFAOntoManager.HAS_NUMERIC_VALUE, height_value, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        else:
            self.dataset.add((height_control_plan, GFAOntoManager.HAS_ADDITIONAL_TYPE, GFAOntoManager.DETAILED_CONTROL, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    def create_conservation_triples(self, city_object_uri, ext_ref):

        conservation_area = URIRef(city_object_uri)
        external_reference_uri = URIRef(ext_ref)

        self.dataset.add((conservation_area, RDF.type, GFAOntoManager.CONSERVATION_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((conservation_area, GFAOntoManager.hasExternalRef, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    def create_central_area_triples(self, city_object_uri, ext_ref):
        central_area = URIRef(city_object_uri)
        external_reference_uri = URIRef(ext_ref)

        self.dataset.add((central_area, RDF.type, GFAOntoManager.CENTRAL_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((central_area, GFAOntoManager.hasExternalRef, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    def create_planning_boundaries_triples(self, city_object_uri, ext_ref, planning_boundary_name):

        planning_boundary = URIRef(city_object_uri)
        external_reference_uri = URIRef(ext_ref)
        planning_boundary_name = Literal(str(planning_boundary_name), datatype=XSD.string)

        self.dataset.add((planning_boundary, RDF.type, GFAOntoManager.PLANNING_BOUNDARY, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((planning_boundary, GFAOntoManager.hasExternalRef, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((planning_boundary, GFAOntoManager.HAS_NAME, planning_boundary_name, FAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    def create_monument_triples(self, city_object_uri, ext_ref, name):
        monument = URIRef(city_object_uri)
        external_reference_uri = URIRef(ext_ref)
        monument_name = Literal(str(name), datatype=XSD.string)

        self.dataset.add((monument, RDF.type, GFAOntoManager.MONUMENT, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((monument, GFAOntoManager.hasExternalRef, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((monument, GFAOntoManager.HAS_NAME, monument_name, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    def create_landed_housing_areas_triples(self, city_object_uri, ext_ref, height, type, area):
        landed_housing_area = URIRef(city_object_uri)
        external_reference_uri = URIRef(ext_ref)
        height_value = Literal(str(height), datatype=XSD.integer)
        storey_aggregate = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        good_class_bungalow = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "GoodClassBungalow")
        semi_detached_house = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "SemiDetachedHouse")
        bungalow= URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "Bungalow")
        terrace_type_1= URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "TerraceType1")
        terrace_type_2 = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "TerraceType2")

        if 'GOOD_CLASS_BUNGALOW_AREA' in area:
            self.dataset.add((landed_housing_area, RDF.type, GFAOntoManager.GOOD_CLASS_BUNGALOW_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, good_class_bungalow, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        elif 'LANDED HOUSING AREA' in area:
            if 'SEMI-DETACHED' in type:
                self.dataset.add((landed_housing_area, RDF.type, GFAOntoManager.GOOD_CLASS_BUNGALOW_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, semi_detached_house, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            elif 'BUNGALOWS' in type:
                self.dataset.add((landed_housing_area, RDF.type, GFAOntoManager.GOOD_CLASS_BUNGALOW_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, bungalow, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            elif 'MIXED LANDED' in type:
                self.dataset.add((landed_housing_area, RDF.type, GFAOntoManager.MIXED_LANDED_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, bungalow, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, semi_detached_house, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, terrace_type_1, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, terrace_type_2, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

        self.dataset.add((landed_housing_area, GFAOntoManager.hasExternalRef, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_STOREY_AGGREGATE, storey_aggregate, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, RDF.type, GFAOntoManager.STOREY_AGGREGATE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, GFAOntoManager.HAS_AGGREGATE_FUNCTION, GFAOntoManager.MAXIMUM, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, GFAOntoManager.NUMBER_OF_STOREYS, height_value, FAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    def create_street_block_plan_triples(self, city_object_uri, ext_ref, landuse, gpr, programme, height, front_setback, side_setback, rear_setback, partywall):
        street_block_plan = URIRef(city_object_uri)
        ext_ref_uri = URIRef(ext_ref)
        height_value = Literal(str(height), datatype=XSD.integer)
        storey_aggregate = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        partywall = Literal(bool(int(partywall)), datatype=XSD.boolean)
        landuse =

        self.dataset.add((street_block_plan, RDF.type, GFAOntoManager.STREET_BLOCK_PLAN, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((street_block_plan, GFAOntoManager.hasExternalRef, ext_ref_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if not pd.isna(landuse):
            elf.dataset.add((street_block_plan, GFAOntoManager.hasZoneType, URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + ),
                             GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))




        self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_STOREY_AGGREGATE, storey_aggregate, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, RDF.type, GFAOntoManager.STOREY_AGGREGATE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, GFAOntoManager.HAS_AGGREGATE_FUNCTION, GFAOntoManager.MAXIMUM, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, GFAOntoManager.NUMBER_OF_STOREYS, height_value, FAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))





    # writes the aggregated triples into a text file.
    def write_triples(self, triple_type):
        with open(cur_dir + "/output_" + triple_type + ".nq", mode="wb") as file:
            file.write(self.dataset.serialize(format='nquads'))


def add_nquads(nquads, endpoint):
    nquads = "".join(nquads)
    headers = {"Content-Type": "text/x-nquads"}
    try:
        response = requests.post(endpoint, headers=headers, data=nquads)
        response.raise_for_status()
        return response
    except HTTPError as err:
        print(err)


def instantiate_gfa():

    # File paths
    df = pd.read_json(json_dir, orient="index")
    df = df.reset_index()
    df = df.melt(id_vars=['index'], value_vars=list(df.columns[1:])).dropna()
    print("JSON loaded")

    gfa_dataset = TripleDataset()

    for i in df.index:
        gfa_dataset.create_gfa_triples(df.loc[i, 'index'], df.loc[i, 'value'], df.loc[i, 'variable'])
    print('Triples created')
    gfa_dataset.write_triples("gfa")
    print("Nquads written")
    #add_nquads(gfa_dataset, endpoint)


if __name__ == "__main__":
    instantiate_gfa()
