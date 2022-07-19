from rdflib import URIRef

'''
This class is meant to work as schema manager for ontologies needed to instantiate Gross Floor Area instance.
'''


class GFAOntoManager:

    # Ontologies
    PLANNING_CONCEPT_ONTO_PREFIX = "http://www.theworldavatar.com/ontology/planningconceptonto/PlanningConceptOntology.owl#"
    OM_URI_PREFIX = "http://ontology.eil.utoronto.ca/icity/OM.owl#"
    GEOSPARQL_URI_PREFIX = "http://www.opengis.net/ont/geosparql#"
    ONTO_ZONING_URI_PREFIX = "http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#"

    # Planning Concept Ontology classes and properties
    GROSS_PLOT_RATIO = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "GrossPlotRatio")
    BUILDABLE_SPACE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "BuildableSpace")
    SITE_COVERAGE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "SiteCoverage")
    ABSOLUTE_HEIGHT = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "AbsoluteHeight")
    STOREY_AGGREGATE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "StoreyAggregate")
    SETBACK = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "Setback")
    SIDE_SETBACK = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "SideSetback")
    REAR_SETBACK = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "RearSetback")
    FRONT_SETBACK = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "FrontSetback")
    COMMON_BOUNDARY_SETBACK = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "CommonBoundarySetback")
    ROAD_BUFFER = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "RoadBuffer")
    STOREY = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "Storey")
    FLOOR_TO_FLOOR_HEIGHT = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "FloorToFloorHeight")
    FOOTPRINT = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "Footprint")
    FOOTPRINT_AREA = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "FootprintArea")
    GROSS_FLOOR_AREA = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "GrossFloorArea")
    AVERAGE_WIDTH = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "AverageWidth")
    AVERAGE_DEPTH = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "AverageDepth")

    HAS_AREA = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasArea")
    HAS_BUILDABLE_SPACE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasBuildableSpace")
    FOR_ZONING_CASE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "forZoningCase")
    HAS_ALLOWED_GPR = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAllowedGPR")
    HAS_ALLOWED_GFA = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAllowedGPR")
    APPLIES_TO = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "appliesTo")
    AT_ALLOWED_STOREY = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "atAllowedStorey")
    CONTAINS_ALLOWED_STOREY = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "containsAllowedStorey")
    HAS_ALLOWED_ABSOLUTE_HEIGHT = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAllowedAbsoluteHeight")
    HAS_ALLOWED_FLOOR_TO_FLOOR_HEIGHT = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAllowedFloorToFloorHeight")
    HAS_ALLOWED_FOOTPRINT = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAllowedFootprint")
    HAS_ALLOWED_SITE_COVERAGE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAllowedSiteCoverage")
    HAS_ALLOWED_STOREY_AGGREGATE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAllowedStoreyAggregate")
    HAS_AVERAGE_WIDTH = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAverageWidth")
    HAS_AVERAGE_DEPTH = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAverageDepth")
    HAS_FOOTPRINT_AREA = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasFootprintArea")
    HAS_NEIGHBOUR = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasNeighbour")
    HAS_PARTYWALL_REGULATION = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasPartywallRegulation")
    HAS_REQUIRED_SETBACK = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasRequiredSetback")
    HAS_SOURCE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasSource")
    IS_PART_OF = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "isPartOf")
    AT_LEVEL_OF_DETAIL = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "atLevelOfDetail")
    HAS_ALLOWED_NUM_OF_STOREYS = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasAllowedNumOfStoreys")
    HAS_STOREY_NAME = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "hasStoreyName")
    IS_AT_RESIDENTIAL_FRINGE = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "isAtResidentialFringe")
    IS_CORNER_PLOT = URIRef(PLANNING_CONCEPT_ONTO_PREFIX + "isCornerPlot")

    # OntoZoning ontology classes and properties
    PLOT = URIRef(ONTO_ZONING_URI_PREFIX + "Plot")
    ZONE_TYPE = URIRef(ONTO_ZONING_URI_PREFIX + "ZoneType")
    LAND_USE = URIRef(ONTO_ZONING_URI_PREFIX + "LandUse")
    PROGRAMME = URIRef(ONTO_ZONING_URI_PREFIX + "Programme")
    HAS_ZONE_TYPE = URIRef(ONTO_ZONING_URI_PREFIX + "hasZoneType")

    # Units of Measure ontology classes and properties
    AREA = URIRef(OM_URI_PREFIX + "Area")
    HEIGHT = URIRef(OM_URI_PREFIX + "Height")
    DISTANCE = URIRef(OM_URI_PREFIX + "Distance")
    WIDTH = URIRef(OM_URI_PREFIX + "Width")
    DEPTH = URIRef(OM_URI_PREFIX + "Depth")
    MEASURE = URIRef(OM_URI_PREFIX + "Measure")
    LENGTH_UNIT = URIRef(OM_URI_PREFIX + "Length_unit")
    AREA_UNIT = URIRef(OM_URI_PREFIX + "Area_unit")
    METRE = URIRef(OM_URI_PREFIX + "metre")
    SQUARE_PREFIXED_METRE = URIRef(OM_URI_PREFIX + "SquarePrefixedMetre")
    HAS_VALUE = URIRef(OM_URI_PREFIX + "hasValue")
    HAS_NUMERIC_VALUE = URIRef(OM_URI_PREFIX + "hasNumericValue")
    HAS_UNIT = URIRef(OM_URI_PREFIX + "hasUnit")

    # GeoSPARQL ontology classes and properties
    FEATURE = GEOSPARQL_URI_PREFIX + "Feature"
    GEOMETRY = GEOSPARQL_URI_PREFIX + "Geometry"
    HAS_GEOMETRY = GEOSPARQL_URI_PREFIX + "hasGeometry"
    AS_WKT = GEOSPARQL_URI_PREFIX + "asWKT"

    PREFIX_URI = "http://theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/"
    BUILDABLE_SPACE_GRAPH = PREFIX_URI + "buildablespace" + "/"
    ONTO_ZONING_GRAPH = PREFIX_URI + "ontozone" + "/"




