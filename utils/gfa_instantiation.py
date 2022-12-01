import uuid
import requests
import pandas as pd
import numpy as np
from rdflib import Dataset, Literal
from rdflib.namespace import RDF
from rdflib.namespace import XSD
from requests.exceptions import HTTPError
from GFAOntoManager import *
from SPARQLWrapper import SPARQLWrapper, JSON

json_dir = "C:/Users/AydaGrisiute/Dropbox/Cities Knowledge Graph - [all team members]/Research/WP6 - Use Cases/PlanningConceptOntology/estimate_GFA[AG][HS]_2022_08_25.json"
cur_dir = 'C://Users/AydaGrisiute/Desktop'
endpoint = "http://192.168.0.144:9999/blazegraph/namespace/singaporeEPSG4326/sparql"


class TripleDataset:

    def __init__(self):
        self.dataset = Dataset()

    ''' writes necessary triples to represent a Gross Floor Area triplesto a triple dataset.'''
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

    '''writes necessary triples to represent a Height Control Plans triples to a triple dataset.'''
    def create_height_control_triples(self, city_object_uri, ext_ref, height, unit):
        height_control_plan = URIRef(city_object_uri)
        external_reference_uri = URIRef(ext_ref)
        storey_aggregate = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        height_value = Literal(str(height), datatype=XSD.integer)
        self.dataset.add((height_control_plan, RDF.type, GFAOntoManager.HEIGHT_CONTROL_PLAN, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((height_control_plan, GFAOntoManager.HAS_EXTERNAL_REF, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if 'NUMBER OF STOREYS' in unit:
            self.create_storey_aggregate_triples(height_control_plan, storey_aggregate, height_value)
            i = 1
            while i <= int(height):
                storey = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
                self.dataset.add((storey_aggregate, GFAOntoManager.CONTAINS_STOREY, storey, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((storey, RDF.type, GFAOntoManager.STOREY, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((storey, GFAOntoManager.AT_LEVEL, Literal(str(i), datatype=XSD.integer),GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                i += 1
        elif 'METRES BASED ON SHD' in unit:
            self.create_absolute_height_triples(height_control_plan, height_value)
        else:
            self.dataset.add((height_control_plan, GFAOntoManager.HAS_ADDITIONAL_TYPE, GFAOntoManager.DETAILED_CONTROL, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent a Absolute Height triples to a triple dataset.'''
    def create_absolute_height_triples(self, city_obj, height):
        absolute_height = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        measure = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        self.dataset.add((city_obj, GFAOntoManager.ALLOWS_ABSOLUTE_HEIGHT, absolute_height, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((absolute_height, RDF.type, GFAOntoManager.ABSOLUTE_HEIGHT, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((absolute_height, GFAOntoManager.HAS_AGGREGATE_FUNCTION, GFAOntoManager.MAXIMUM, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((absolute_height, GFAOntoManager.HAS_VALUE, measure, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((measure, RDF.type, GFAOntoManager.MEASURE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((measure, GFAOntoManager.HAS_UNIT, GFAOntoManager.METRE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((GFAOntoManager.METRE, RDF.type, GFAOntoManager.AREA_UNIT, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((measure, GFAOntoManager.HAS_NUMERIC_VALUE, height, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent a Storey Aggregate triples to a triple dataset.'''
    def create_storey_aggregate_triples(self, city_obj, storey_aggregate, height_value):
        self.dataset.add((city_obj, GFAOntoManager.ALLOWS_STOREY_AGGREGATE, storey_aggregate, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, RDF.type, GFAOntoManager.STOREY_AGGREGATE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, GFAOntoManager.HAS_AGGREGATE_FUNCTION, GFAOntoManager.MAXIMUM, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((storey_aggregate, GFAOntoManager.NUMBER_OF_STOREYS, height_value, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent a Conservation Area triples to a triple dataset.'''
    def create_conservation_triples(self, city_obj, ext_ref):

        conservation_area = URIRef(city_obj)
        external_reference_uri = URIRef(ext_ref)

        self.dataset.add((conservation_area, RDF.type, GFAOntoManager.CONSERVATION_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((conservation_area, GFAOntoManager.HAS_EXTERNAL_REF, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent a Central Area triples to a triple dataset.'''
    def create_central_area_triples(self, city_obj, ext_ref):
        central_area = URIRef(city_obj)
        external_reference_uri = URIRef(ext_ref)

        self.dataset.add((central_area, RDF.type, GFAOntoManager.CENTRAL_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((central_area, GFAOntoManager.HAS_EXTERNAL_REF, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent a Planning Boundaries triples to a triple dataset.'''
    def create_planning_boundaries_triples(self, city_obj, ext_ref, planning_boundary_name, region):

        planning_boundary = URIRef(city_obj)
        external_reference_uri = URIRef(ext_ref)
        planning_boundary_name = Literal(str(planning_boundary_name), datatype=XSD.string)
        regions = {'EAST REGION':URIRef(GFAOntoManager.ONTO_PLANNING_REG_PREFIX + 'EastRegion'),
                   'CENTRAL REGION': URIRef(GFAOntoManager.ONTO_PLANNING_REG_PREFIX + 'CentralRegion'),
                   'WEST REGION': URIRef(GFAOntoManager.ONTO_PLANNING_REG_PREFIX + 'WestRegion'),
                   'NORTH-EAST REGION': URIRef(GFAOntoManager.ONTO_PLANNING_REG_PREFIX + 'NorthEastRegion'),
                   'NORTH REGION': URIRef(GFAOntoManager.ONTO_PLANNING_REG_PREFIX + 'NorthRegion')}
        self.dataset.add((planning_boundary, RDF.type, GFAOntoManager.PLANNING_BOUNDARY, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((planning_boundary, GFAOntoManager.HAS_EXTERNAL_REF, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((planning_boundary, GFAOntoManager.HAS_NAME, planning_boundary_name, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((planning_boundary, GFAOntoManager.IS_PART_OF_OPR, regions[region], GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((regions[region], RDF.type, GFAOntoManager.PLANNING_REGION, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent a Monument triples to a triple dataset.'''
    def create_monument_triples(self, city_obj, ext_ref, name):
        monument = URIRef(city_obj)
        external_reference_uri = URIRef(ext_ref)
        monument_name = Literal(str(name), datatype=XSD.string)

        self.dataset.add((monument, RDF.type, GFAOntoManager.MONUMENT, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((monument, GFAOntoManager.HAS_EXTERNAL_REF, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((monument, GFAOntoManager.HAS_NAME, monument_name, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent a Landed Housing Triples triples to a triple dataset.'''
    def create_landed_housing_areas_triples(self, city_obj, ext_ref, height, type, area):
        landed_housing_area = URIRef(city_obj)
        external_reference_uri = URIRef(ext_ref)
        height_value = Literal(str(height), datatype=XSD.integer)
        storey_aggregate = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        good_class_bungalow = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "GoodClassBungalow")
        semi_detached_house = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "Semi-DetachedHouse")
        bungalow = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "Bungalow")
        terrace_type_1 = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "TerraceType1")
        terrace_type_2 = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + "TerraceType2")

        self.dataset.add((landed_housing_area, GFAOntoManager.HAS_EXTERNAL_REF, external_reference_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.create_storey_aggregate_triples(landed_housing_area, storey_aggregate, height_value)
        i = 1
        while i <= int(height):
            storey = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
            self.dataset.add((storey_aggregate, GFAOntoManager.CONTAINS_STOREY, storey, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((storey, RDF.type, GFAOntoManager.STOREY, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((storey, GFAOntoManager.AT_LEVEL, Literal(str(i), datatype=XSD.integer), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            i += 1
        if 'GOOD CLASS BUNGALOW AREA' in area:
            self.dataset.add((landed_housing_area, RDF.type, GFAOntoManager.GOOD_CLASS_BUNGALOW_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, good_class_bungalow, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        elif 'LANDED HOUSING AREA' in area:
            if 'SEMI-DETACHED' in type:
                self.dataset.add((landed_housing_area, RDF.type, GFAOntoManager.LANDED_HOUSING_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, semi_detached_house, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            elif 'BUNGALOWS' in type:
                self.dataset.add((landed_housing_area, RDF.type, GFAOntoManager.LANDED_HOUSING_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, bungalow, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            elif 'MIXED LANDED' in type:
                self.dataset.add((landed_housing_area, RDF.type, GFAOntoManager.LANDED_HOUSING_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, bungalow, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, semi_detached_house, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, terrace_type_1, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((landed_housing_area, GFAOntoManager.ALLOWS_PROGRAMME, terrace_type_2, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent a setback triples to a triple dataset.'''
    def create_setback_triples(self, city_obj, setback, predicate_type, setback_type, setback_value):
        setback_measure = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        self.dataset.add((city_obj, predicate_type, setback, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((setback, RDF.type, setback_type, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((setback, GFAOntoManager.HAS_AGGREGATE_FUNCTION, GFAOntoManager.MINIMUM,  GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((setback, GFAOntoManager.HAS_VALUE, setback_measure, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((setback_measure, GFAOntoManager.HAS_UNIT, GFAOntoManager.METRE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((setback_measure, GFAOntoManager.HAS_NUMERIC_VALUE, Literal(str(setback_value), datatype=XSD.double), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent different types of setback triples and store it to a triple dataset.'''
    def create_setback_collection(self, city_obj, setbacks, storey, storey_level, predicate_type, setback_type, height_provided):
        if height_provided:
            if not pd.isna(setbacks):
                setbacks_list = setbacks.split(',')
                if len(setbacks_list) > 1:
                    current_setback_value = setbacks_list[storey_level - 1]
                else:
                    current_setback_value = setbacks_list[0]
                setback = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
                self.create_setback_triples(city_obj, setback, predicate_type, setback_type, current_setback_value)
                self.dataset.add((setback, GFAOntoManager.AT_STOREY, storey, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        else:
            if not pd.isna(setbacks):
                setbacks_list = setbacks.split(',')
                if len(setbacks_list) > 1:
                    for index, setback_value in enumerate(setbacks_list):
                        setback = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
                        storey = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
                        storey_level = index + 1
                        self.create_setback_triples(city_obj, setback,predicate_type, setback_type, setback_value)
                        self.dataset.add((storey, GFAOntoManager.AT_LEVEL, Literal(str(storey_level)), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                else:
                    current_setback_value = setbacks_list[0]
                    setback = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
                    self.create_setback_triples(city_obj, setback, predicate_type, setback_type, current_setback_value)

    '''writes necessary triples to represent the Street Block Plan to a triple dataset.'''
    def create_street_block_plan_triples(self, city_obj, height, front_setback, side_setback, rear_setback, partywall_setback, ext_ref, name, landuse, gpr, allowed_programmes):
        street_block_plan = URIRef(city_obj)
        self.dataset.add((street_block_plan, RDF.type, GFAOntoManager.STREET_BLOCK_PLAN, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        storey_aggregate = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
        if not str(ext_ref):
            ext_ref_uri = URIRef(ext_ref.strip())
            self.dataset.add((street_block_plan, GFAOntoManager.HAS_EXTERNAL_REF, ext_ref_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if not pd.isna(name):
            self.dataset.add((street_block_plan, GFAOntoManager.HAS_NAME, Literal(str(name), datatype=XSD.string), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if not pd.isna(landuse):
            zone = URIRef(str(GFAOntoManager.ONTO_ZONING_URI_PREFIX + landuse.strip()))
            self.dataset.add((street_block_plan, GFAOntoManager.HAS_ZONE_TYPE, zone, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if not pd.isna(gpr):
            gpr_uri = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
            self.dataset.add((street_block_plan, GFAOntoManager.ALLOWS_GROSS_PLOT_RATIO, gpr_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((gpr_uri, RDF.type, GFAOntoManager.GROSS_PLOT_RATIO, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((gpr_uri, GFAOntoManager.HAS_VALUE_OPR, Literal(str(gpr), datatype=XSD.double),GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if (not pd.isna(allowed_programmes)) and (not 'Existing' in allowed_programmes.split(',')):
            programmes = allowed_programmes.split(',')
            for i in programmes:
                self.dataset.add((street_block_plan, GFAOntoManager.ALLOWS_PROGRAMME, URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + i), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if not pd.isna(height):
            self.create_storey_aggregate_triples(street_block_plan, storey_aggregate, Literal(str(height), datatype=XSD.integer))
            storey_level = 1
            while storey_level <= int(height):
                storey = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
                self.dataset.add((storey_aggregate, GFAOntoManager.CONTAINS_STOREY, storey, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((storey, RDF.type, GFAOntoManager.STOREY, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((storey, GFAOntoManager.AT_LEVEL, Literal(str(storey_level), datatype=XSD.integer), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.create_setback_collection(street_block_plan, front_setback, storey, storey_level, GFAOntoManager.REQUIRES_SETBACK, GFAOntoManager.FRONT_SETBACK, True)
                self.create_setback_collection(street_block_plan, side_setback, storey, storey_level, GFAOntoManager.REQUIRES_SETBACK,GFAOntoManager.SIDE_SETBACK, True)
                self.create_setback_collection(street_block_plan, rear_setback, storey, storey_level, GFAOntoManager.REQUIRES_SETBACK, GFAOntoManager.REAR_SETBACK, True)
                if (not pd.isna(partywall_setback)) and bool(int(partywall_setback)):
                    partywall = '0.0'
                    self.create_setback_collection(street_block_plan, partywall, storey, storey_level, GFAOntoManager.REQUIRES_PARTYWALL, GFAOntoManager.PARTYWALL, True)
                storey_level += 1
        else:
            storey = 'nan'
            storey_level = 'nan'
            self.create_setback_collection(street_block_plan, front_setback, storey, storey_level, GFAOntoManager.REQUIRES_SETBACK, GFAOntoManager.FRONT_SETBACK, False)
            self.create_setback_collection(street_block_plan, side_setback, storey, storey_level, GFAOntoManager.REQUIRES_SETBACK, GFAOntoManager.SIDE_SETBACK, False)
            self.create_setback_collection(street_block_plan, rear_setback, storey, storey_level, GFAOntoManager.REQUIRES_SETBACK, GFAOntoManager.REAR_SETBACK, False)
            if (not pd.isna(partywall_setback)) and bool(int(partywall_setback)):
                partywall = '0.0'
                self.create_setback_collection(street_block_plan, partywall, storey, storey_level, GFAOntoManager.REQUIRES_PARTYWALL, GFAOntoManager.PARTYWALL, False)

    ''' writes necessary triples to represent Urban Design Areas'''
    def create_urban_design_areas_triples(self, city_obj, ext_ref, name):
        urban_design_area = URIRef(city_obj)
        ext_ref_uri = URIRef(ext_ref.strip())
        self.dataset.add((urban_design_area, RDF.type, GFAOntoManager.URBAN_DESIGN_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((urban_design_area, GFAOntoManager.HAS_EXTERNAL_REF, ext_ref_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((urban_design_area, GFAOntoManager.HAS_NAME, Literal(str(name.strip()), datatype=XSD.string), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))

    '''writes necessary triples to represent the Urban Design Guidelines to a triple dataset.'''
    def create_urban_design_guidelines_triples(self, city_obj, ext_ref, partywall_setback, height, setback, additional_type, area, urban_design_areas):
        urban_design_guideline = URIRef(city_obj)
        ext_ref_uri = URIRef(str(ext_ref.strip()))
        self.dataset.add((urban_design_guideline, RDF.type, GFAOntoManager.URBAN_DESIGN_GUIDELINE, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        self.dataset.add((urban_design_guideline, GFAOntoManager.HAS_EXTERNAL_REF, ext_ref_uri, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if not pd.isna(additional_type):
            additional_type = URIRef(GFAOntoManager.ONTO_PLANNING_REG_PREFIX + additional_type.strip())
            self.dataset.add((urban_design_guideline, GFAOntoManager.HAS_ADDITIONAL_TYPE, additional_type, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if not pd.isna(area):
            current_design_area = URIRef(urban_design_areas[area])
            self.dataset.add((urban_design_guideline, GFAOntoManager.IN_URBAN_DESIGN_AREA, current_design_area, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
            self.dataset.add((current_design_area, RDF.type, GFAOntoManager.URBAN_DESIGN_AREA, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
        if not pd.isna(height):
            storey_aggregate = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
            self.create_storey_aggregate_triples(urban_design_guideline, storey_aggregate, Literal(str(height), datatype=XSD.integer))
            storey_level = 1
            while storey_level <= int(height):
                storey = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
                self.dataset.add((storey_aggregate, GFAOntoManager.CONTAINS_STOREY, storey, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((storey, RDF.type, GFAOntoManager.STOREY, GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.dataset.add((storey, GFAOntoManager.AT_LEVEL, Literal(str(storey_level), datatype=XSD.integer), GFAOntoManager.ONTO_PLANNING_REGULATIONS_GRAPH))
                self.create_setback_collection(urban_design_guideline, setback, storey, storey_level, GFAOntoManager.REQUIRES_SETBACK, GFAOntoManager.SETBACK, True)
                if (not pd.isna(partywall_setback)) and bool(int(partywall_setback)):
                    partywall = '0.0'
                    self.create_setback_collection(urban_design_guideline, partywall, storey, storey_level, GFAOntoManager.REQUIRES_PARTYWALL, GFAOntoManager.PARTYWALL, True)
                storey_level += 1
        else:
            storey = 'nan'
            storey_level = 'nan'
            self.create_setback_collection(urban_design_guideline, setback, storey, storey_level, GFAOntoManager.REQUIRES_SETBACK, GFAOntoManager.SETBACK, False)
            if (not pd.isna(partywall_setback)) and bool(int(partywall_setback)):
                partywall = '0.0'
                self.create_setback_collection(urban_design_guideline, partywall, storey, storey_level, GFAOntoManager.REQUIRES_PARTYWALL, GFAOntoManager.PARTYWALL, False)

    ''' writes the aggregated triples into a nquad(text) file.'''
    def write_triples(self, triple_type):
        with open(cur_dir + "/output_" + triple_type + ".nq", mode="wb") as file:
            file.write(self.dataset.serialize(format='nquads'))

def get_urban_design_areas(endpoint):
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
SELECT  ?uda ?name   
WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/planningregulations/>
    {?uda  rdf:type opr:UrbanDesignArea ;
    		opr:hasName ?name . } }""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    uda = query_results.applymap(lambda cell: cell['value'], na_action='ignore')
    uda.reset_index()
    area_dict = {}
    for i in uda.index:
        area_dict[uda.loc[i, 'name']] = uda.loc[i, 'uda']
    return area_dict

def add_nquads(nquads, endpoint):
    nquads = "".join(nquads)
    headers = {"Content-Type": "text/x-nquads"}
    try:
        response = requests.post(endpoint, headers=headers, data=nquads)
        response.raise_for_status()
        return response
    except HTTPError as err:
        print(err)


''' method to query and instantiate height control plans regulation content.'''
def instantiate_height_control(hc_endpoint):
    sparql = SPARQLWrapper(hc_endpoint)
    sparql.setQuery("""PREFIX ocgml:<http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    SELECT ?city_obj ?ext_ref ?unit_type ?height
    WHERE {GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
    ?attr_1 ocgml:cityObjectId ?city_obj ;
        ocgml:attrName 'ExtRef' ;
        ocgml:uriVal ?ext_ref .
    ?attr_2 ocgml:cityObjectId ?city_obj ;
        ocgml:attrName 'HT_CTL_TYP' ;
        ocgml:strVal ?unit_type .
    ?attr_3 ocgml:cityObjectId ?city_obj ;
        ocgml:attrName 'HT_CTL_TXT' ;
        ocgml:strVal ?height . } } """)
    print("Height control data retrieved.")

    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    hc = query_results.applymap(lambda cell: cell['value'])
    hc.reset_index()
    hc_dataset = TripleDataset()
    for i in hc.index:
        hc_dataset.create_height_control_triples(hc.loc[i, 'city_obj'], str(hc.loc[i, 'ext_ref']), hc.loc[i, 'height'], hc.loc[i, 'unit_type'])
    hc_dataset.write_triples("height_control")
    print("Height control nquads written.")


''' method to query and instantiate conservation areas regulation content.'''
def instantiate_conservation_areas(con_endpoint):
    sparql = SPARQLWrapper(con_endpoint)
    sparql.setQuery("""PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    SELECT ?city_obj ?ext_ref
    WHERE {
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
    ?genAttr ocgml:cityObjectId ?city_obj .
    ?genAttr ocgml:attrName 'ExtRef' ;
    ocgml:uriVal ?ext_ref . } }  """)
    print("Conservation areas data retrieved.")

    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    ca = query_results.applymap(lambda cell: cell['value'])
    ca.reset_index()
    ca_dataset = TripleDataset()
    for i in ca.index:
        ca_dataset.create_conservation_triples(ca.loc[i, 'city_obj'], ca.loc[i, 'ext_ref'])
    ca_dataset.write_triples("conservation_areas")
    print("Conservation areas nquads written.")


''' method to query and instantiate Central Area regulation content.'''
def instantiate_central_area(ca_endpoint):
    sparql = SPARQLWrapper(ca_endpoint)
    sparql.setQuery("""PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
        SELECT ?city_obj ?ext_ref
        WHERE {
        GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
        ?genAttr ocgml:cityObjectId ?city_obj .
        ?genAttr ocgml:attrName 'ExtRef' ;
        ocgml:uriVal ?ext_ref . } } """)
    print("Central areas data retrieved.")

    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    ca = query_results.applymap(lambda cell: cell['value'])
    ca.reset_index()
    ca_dataset = TripleDataset()
    for i in ca.index:
        ca_dataset.create_central_area_triples(ca.loc[i, 'city_obj'], ca.loc[i, 'ext_ref'])
    ca_dataset.write_triples("central_area")
    print("Central area nquads written.")


''' method to query and instantiate Planning Areas regulation content.'''
def instantiate_planning_boundaries(pb_endpoint):
    sparql = SPARQLWrapper(pb_endpoint)
    sparql.setQuery("""PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    SELECT ?city_obj ?ext_ref ?planning_area ?region
    WHERE {GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
    ?genAttr ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'ExtRef' ;
    ocgml:uriVal ?ext_ref .
    ?genAttr2 ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'PLN_AREA_N';
    ocgml:strVal ?planning_area .
    ?genAttr3 ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'REGION_N';
    ocgml:strVal ?region . } } """)
    print("Planning Boundaries data retrieved.")

    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    pb = query_results.applymap(lambda cell: cell['value'])
    pb.reset_index()
    pb_dataset = TripleDataset()
    for i in pb.index:
        pb_dataset.create_planning_boundaries_triples(pb.loc[i, 'city_obj'], pb.loc[i, 'ext_ref'], pb.loc[i, 'planning_area'], pb.loc[i, 'region'])
    pb_dataset.write_triples("planning_areas")
    print("Planning Boundaries nquads written.")


''' method to query and instantiate Monuments regulation content.'''
def instantiate_monuments(m_endpoint):
    sparql = SPARQLWrapper(m_endpoint)
    sparql.setQuery("""PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    SELECT ?city_obj ?ext_ref ?name
    WHERE {
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
    ?genAttr ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'ExtRef' ;
    ocgml:uriVal ?ext_ref .
    ?genAttr2 ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'NAME' ; 
    ocgml:strVal ?name .} } """)
    print("Monument data retrieved.")

    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    m = query_results.applymap(lambda cell: cell['value'])
    m.reset_index()
    m_dataset = TripleDataset()
    for i in m.index:
        m_dataset.create_monument_triples(m.loc[i, 'city_obj'], m.loc[i, 'ext_ref'], m.loc[i, 'name'])
    m_dataset.write_triples("monuments")
    print("Monument nquads written.")


''' method to query and instantiate Landed housing Area regulation content.'''
def instantiate_landed_housing_areas(lha_endpoint):
    sparql = SPARQLWrapper(lha_endpoint)
    sparql.setQuery("""PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    SELECT ?city_obj ?ext_ref ?height ?type ?area
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
    ?genAttr1 ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'STY_HT' ;
    ocgml:strVal ?height .
    ?genAttr2 ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'TYPE' ;
    ocgml:strVal ?type. 
    ?genAttr3 ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'CLASSIFCTN' ;
    ocgml:strVal ?area.
    ?genAttr4 ocgml:cityObjectId ?city_obj ;
    ocgml:attrName 'ExtRef' ;
    ocgml:uriVal ?ext_ref . } } """)
    print("Landed housing area data retrieved.")

    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    lha = query_results.applymap(lambda cell: cell['value'])
    lha.reset_index()
    lha_dataset = TripleDataset()
    for i in lha.index:
        lha_dataset.create_landed_housing_areas_triples(lha.loc[i, 'city_obj'], str(lha.loc[i, 'ext_ref']), lha.loc[i, 'height'], lha.loc[i, 'type'], lha.loc[i, 'area'])
    lha_dataset.write_triples("landed_housing_areas")
    print("Landed housing area nquads written.")


''' method to query and instantiate Street Block Plan regulation content.'''
def instantiate_street_block_plan(sb_endpoint):
    sparql = SPARQLWrapper(sb_endpoint)
    sparql.setQuery("""PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
SELECT ?city_obj ?front_setback ?side_setback ?rear_setback ?partywall_setback ?storeys ?gpr ?ext_ref ?allowed_programmes ?landuse ?name
WHERE {  GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> { 
         ?attr_0 ocgml:cityObjectId ?city_obj ; 
         		ocgml:attrName 'UNIQUE_ID';
				ocgml:strVal ?id .  
OPTIONAL { ?attr_1 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'SetbackFront' ;
             	 ocgml:strVal ?front_setback . }
OPTIONAL { ?attr_2 ocgml:cityObjectId ?city_obj ;
            	  ocgml:attrName 'Storeys' ;
            	  ocgml:strVal ?storeys . }
OPTIONAL { ?attr_3 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'SetbackSide' ;
             	 ocgml:strVal ?side_setback .}    
OPTIONAL { ?attr_4 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'SetbackRear' ;
             	 ocgml:strVal ?rear_setback . }    
OPTIONAL { ?attr_5 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'PartyWall' ;
             	 ocgml:strVal ?partywall_setback . }    
OPTIONAL { ?attr_6 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'GPR' ;
             	 ocgml:strVal ?gpr . }
OPTIONAL { ?attr_7 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'ExtRef' ;
             	 ocgml:uriVal ?ext_ref . }
OPTIONAL { ?attr_8 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'LandUse' ;
             	 ocgml:strVal ?landuse . }
OPTIONAL { ?attr_9 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'NAME' ;
             	 ocgml:strVal ?name . }
OPTIONAL { ?attr_10 ocgml:cityObjectId ?city_obj ;
             	 ocgml:attrName 'AllowedProgrammes' ;
             	 ocgml:strVal ?allowed_programmes . }
  } }  """)
    print("Street block plan data retrieved.")

    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    sbp = query_results.applymap(lambda cell: cell['value'], na_action='ignore')
    sbp.reset_index()
    sbp_dataset = TripleDataset()
    for i in sbp.index:
        sbp_dataset.create_street_block_plan_triples(sbp.loc[i, 'city_obj'], sbp.loc[i, 'storeys'], sbp.loc[i, 'front_setback'], sbp.loc[i, 'side_setback'], sbp.loc[i, 'rear_setback'],
                                                     sbp.loc[i, 'partywall_setback'], sbp.loc[i, 'ext_ref'], sbp.loc[i, 'name'], sbp.loc[i, 'landuse'], sbp.loc[i, 'gpr'], sbp.loc[i, 'allowed_programmes'])
    sbp_dataset.write_triples("street_block_plans")
    print("Street block plan nquads written.")


''' method to query and instantiate Urban Design Guidelines regulation content.'''
def instantiate_urban_design_areas(uda_endpoint):
    sparql = SPARQLWrapper(uda_endpoint)
    sparql.setQuery("""PREFIX ocgml:<http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
                PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
                SELECT ?city_obj ?name ?ext_ref
                WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
                ?genAttr ocgml:cityObjectId ?city_obj .
                ?genAttr ocgml:attrName 'Name' ;
                ocgml:strVal ?name .
                ?genAttr1 ocgml:cityObjectId ?city_obj .
                ?genAttr1 ocgml:attrName 'ExtRef' ;
                ocgml:uriVal ?ext_ref . } }""")
    print("Urban design areas data retrieved.")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    uda = query_results.applymap(lambda cell: cell['value'], na_action='ignore')
    uda.reset_index()
    uda_dataset = TripleDataset()
    for i in uda.index:
        uda_dataset.create_urban_design_areas_triples(uda.loc[i, 'city_obj'], str(uda.loc[i, 'ext_ref']), uda.loc[i, 'name'])
    uda_dataset.write_triples("urban_design_areas")
    print("Urban design areas nquads written.")


''' method to query and instantiate Urban Design Guidelines regulation content.'''
def instantiate_urban_design_guidelines(udg_endpoint):
    sparql = SPARQLWrapper(udg_endpoint)
    sparql.setQuery("""PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
SELECT ?city_obj ?additional_type ?ext_ref ?partywall ?setback ?area ?height
WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
  ?attr0 ocgml:cityObjectId ?city_obj ;
              ocgml:attrName 'TYPE';
              ocgml:strVal 'URBAN_DESIGN_GUIDELINES' . 
  OPTIONAL { ?attr1 ocgml:cityObjectId ?city_obj ;
              ocgml:attrName 'Type' ;
              ocgml:strVal ?additional_type . }
  OPTIONAL { ?attr2 ocgml:cityObjectId ?city_obj ;
              ocgml:attrName 'ExtRef' ;
              ocgml:uriVal ?ext_ref . }
  OPTIONAL { ?attr3 ocgml:cityObjectId ?city_obj ;
              ocgml:attrName 'PartyWall' ;
              ocgml:strVal ?partywall . }
  OPTIONAL { ?attr4 ocgml:cityObjectId ?city_obj ;
              ocgml:attrName 'Setback' ;
              ocgml:strVal ?setback . }
  OPTIONAL { ?attr5 ocgml:cityObjectId ?city_obj ;
              ocgml:attrName 'Urban_Design_Area' ;
              ocgml:strVal ?area . }
  OPTIONAL { ?attr6 ocgml:cityObjectId ?city_obj ;
              ocgml:attrName 'Storeys' ;
              ocgml:strVal ?height . } } }
""")
    print("Urban design guidelines data retrieved.")

    urban_desing_areas = get_urban_design_areas('http://192.168.1.98:9999/blazegraph/namespace/regulation_content/sparql')
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    query_results = pd.DataFrame(results['results']['bindings'])
    udg = query_results.applymap(lambda cell: cell['value'], na_action='ignore')
    udg.reset_index()
    udg_dataset = TripleDataset()
    for i in udg.index:
        udg_dataset.create_urban_design_guidelines_triples(udg.loc[i, 'city_obj'], str(udg.loc[i, 'ext_ref']), udg.loc[i, 'partywall'],
                                                           udg.loc[i, 'height'], udg.loc[i, 'setback'], udg.loc[i, 'additional_type'], udg.loc[i, 'area'], urban_desing_areas)
    udg_dataset.write_triples("urban_design_guidelines")
    print("Urban design guidelines nquads written.")


''' method to query and instantiate Gross Floor Area regulation content.'''
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
    #instantiate_height_control('http://192.168.1.98:9999/blazegraph/namespace/heightcontrol/sparql')
    #instantiate_conservation_areas('http://192.168.1.98:9999/blazegraph/namespace/conservation/sparql')
    #instantiate_central_area('http://192.168.1.98:9999/blazegraph/namespace/centralarea/sparql')
    #instantiate_planning_boundaries('http://192.168.1.98:9999/blazegraph/namespace/planningareas/sparql')
    #instantiate_monuments('http://192.168.1.98:9999/blazegraph/namespace/monument/sparql')
    #instantiate_landed_housing_areas('http://192.168.1.98:9999/blazegraph/namespace/landedhousingarea/sparql')
    #instantiate_street_block_plan('http://192.168.1.98:9999/blazegraph/namespace/streetblockplan/sparql')
    #instantiate_urban_design_areas('http://192.168.1.98:9999/blazegraph/namespace/urbandesingareas/sparql')
    instantiate_urban_design_guidelines('http://192.168.1.98:9999/blazegraph/namespace/urbandesignguidelines/sparql')
    #instantiate_gfa()