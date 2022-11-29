from rdflib import URIRef

'''
This class is meant to work as schema manager for ontologies needed to instantiate Gross Floor Areas.
'''


class GFAOntoManager:

    # Ontologies
    ONTO_BUILDABLE_SPACE_PREFIX = "http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#"
    OM_URI_PREFIX = "http://www.ontology-of-units-of-measure.org/resource/om-2/"
    GEOSPARQL_URI_PREFIX = "http://www.opengis.net/ont/geosparql#"
    ONTO_ZONING_URI_PREFIX = "http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#"
    ONTO_PLANNING_REG_PREFIX = "http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#"

    # OntoPlanningReg classes and properties
    URA_REGULATION = URIRef(ONTO_PLANNING_REG_PREFIX + "URAPlanningRegulation")
    AREA_PLANNING_REGULATION = URIRef(ONTO_PLANNING_REG_PREFIX + "AreaPlanningRegulation")
    TYPE_PLANNING_REGULATION = URIRef(ONTO_PLANNING_REG_PREFIX + "TypePlanningRegulation")
    HEIGHT_CONTROL_PLAN = URIRef(ONTO_PLANNING_REG_PREFIX + "HeightControlPlan")
    URBAN_DESIGN_GUIDELINE = URIRef(ONTO_PLANNING_REG_PREFIX + "UrbanDesignGuideline")
    URBAN_DESIGN_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "UrbanDesignArea")
    DETAILED_CONTROL = URIRef(ONTO_PLANNING_REG_PREFIX + "DetailedControl")
    STREET_BLOCK_PLAN = URIRef(ONTO_PLANNING_REG_PREFIX + "StreetBlockPlan")
    MONUMENT = URIRef(ONTO_PLANNING_REG_PREFIX + "Monument")
    CONSERVATION_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "ConservationArea")
    LANDED_HOUSING_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "LandedHousingArea")
    GOOD_CLASS_BUNGALOW_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "GoodClassBungalowArea")
    PLANNING_BOUNDARY = URIRef(ONTO_PLANNING_REG_PREFIX + "PlanningBoundary")
    PLANNING_REGION = URIRef(ONTO_PLANNING_REG_PREFIX + "PlanningRegion")
    CENTRAL_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "CentralArea")

    REQUIRES_SITE_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresSiteArea")
    REQUIRES_SETBACK  = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresSetback")
    REQUIRES_PARTYWALL = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresPartyWall")
    REQUIRES_BUILDING_EDGE = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresBuildingEdge")
    ALLOWS_STOREY_AGGREGATE = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsStoreyAggregate")
    ALLOWS_ABSOLUTE_HEIGHT = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsAbsoluteHeight")
    REQUIRES_FLOOR_TO_FLOOR_HEIGHT = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresFloorToFloorHeight")
    ALLOWS_GROSS_FLOOR_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsGrossFloorArea")
    HAS_ADDITIONAL_TYPE = URIRef(ONTO_PLANNING_REG_PREFIX + "hasAdditionalType")
    HAS_EXTERNAL_REF = URIRef(ONTO_PLANNING_REG_PREFIX + "hasExternalRef")
    HAS_NAME = URIRef(ONTO_PLANNING_REG_PREFIX + "hasName")
    ALLOWS_GROSS_PLOT_RATIO = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsGrossPlotRatio")
    APPLIES_TO = URIRef(ONTO_PLANNING_REG_PREFIX + "appliesTo")
    IN_URBAN_DESIGN_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "inUrbanDesignArea")
    IS_PART_OF_OPR = URIRef(ONTO_PLANNING_REG_PREFIX + "isPartOf")
    HAS_VALUE_OPR = URIRef(ONTO_PLANNING_REG_PREFIX + "hasValue")

    # OntoBuildableSpace classes and properties
    BUILDABLE_SPACE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "BuildableSpace")
    GROSS_FLOOR_AREA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "GrossFloorArea")
    GROSS_PLOT_RATIO = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "GrossPlotRatio")
    SITE_COVERAGE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "SiteCoverage")
    ABSOLUTE_HEIGHT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "AbsoluteHeight")
    STOREY_AGGREGATE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "StoreyAggregate")
    BUILDING_EDGE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "BuildingEdge")
    STOREY = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "Storey")
    FLOOR_TO_FLOOR_HEIGHT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "FloorToFloorHeight")
    SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "Setback")
    SIDE_SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "SideSetback")
    REAR_SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "RearSetback")
    FRONT_SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "FrontSetback")
    PARTYWALL =  URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "PartyWall")
    FOOTPRINT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "Footprint")
    FOOTPRINT_AREA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "FootprintArea")

    HAS_BUILDABLE_SPACE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasBuildableSpace")
    HAS_AREA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasArea")
    HAS_WIDTH = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasWidth")
    HAS_DEPTH = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasDepth")
    HAS_NEIGHBOUR = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasNeighbour")
    IS_AT_RESIDENTIAL_FRINGE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "atResidentialFringe")
    IS_CORNER_PLOT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "isCornerPlot")
    FOR_ZONING_CASE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "forZoningCase")
    HAS_ALLOWED_GPR = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasAllowedGPR")
    HAS_ALLOWED_GFA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasAllowedGFA")
    HAS_ALLOWED_SITE_COVERAGE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasAllowedSiteCoverage")
    HAS_ALLOWED_STOREY_AGGREGATE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasAllowedStoreyAggregate")
    AT_STOREY = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "atStorey")
    CONTAINS_STOREY = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "containsStorey")
    AT_LEVEL = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "atLevel")
    NUMBER_OF_STOREYS = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "numberOfStoreys")
    HAS_ALLOWED_ABSOLUTE_HEIGHT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasAllowedAbsoluteHeight")
    HAS_ALLOWED_FLOOR_TO_FLOOR_HEIGHT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasAllowedFloorToFloorHeight")
    HAS_ALLOWED_FOOTPRINT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasAllowedFootprint")
    HAS_REQUIRED_SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasRequiredSetback")
    HAS_REQUIRED_PARTYWALL = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasRequiredPartyWall")
    IS_PART_OF = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "isPartOf")
    HAS_SOURCE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasSource")
    AT_LEVEL_OF_DETAIL = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "atLOD")

    # OntoZoning ontology classes and properties
    PLOT = URIRef(ONTO_ZONING_URI_PREFIX + "Plot")
    ZONE_TYPE = URIRef(ONTO_ZONING_URI_PREFIX + "ZoneType")
    LAND_USE = URIRef(ONTO_ZONING_URI_PREFIX + "LandUse")
    PROGRAMME = URIRef(ONTO_ZONING_URI_PREFIX + "Programme")
    HAS_ZONE_TYPE = URIRef(ONTO_ZONING_URI_PREFIX + "hasZoneType")
    ALLOWS_PROGRAMME = URIRef(ONTO_ZONING_URI_PREFIX + "allowsProgramme")

    # Units of Measure ontology classes and properties
    AREA = URIRef(OM_URI_PREFIX + "Area")
    HEIGHT = URIRef(OM_URI_PREFIX + "Height")
    DISTANCE = URIRef(OM_URI_PREFIX + "Distance")
    WIDTH = URIRef(OM_URI_PREFIX + "Width")
    DEPTH = URIRef(OM_URI_PREFIX + "Depth")
    MEASURE = URIRef(OM_URI_PREFIX + "Measure")
    LENGTH_UNIT = URIRef(OM_URI_PREFIX + "LengthUnit")
    AREA_UNIT = URIRef(OM_URI_PREFIX + "AreaUnit")
    RATIO_UNIT = URIRef(OM_URI_PREFIX + "RatioUnit")
    METRE = URIRef(OM_URI_PREFIX + "metre")
    SQUARE_PREFIXED_METRE = URIRef(OM_URI_PREFIX + "SquarePrefixedMetre")
    MAXIMUM = URIRef(OM_URI_PREFIX + "maximum")
    MINIMUM = URIRef(OM_URI_PREFIX + "minimum")
    AVERAGE = URIRef(OM_URI_PREFIX + "average")

    HAS_VALUE = URIRef(OM_URI_PREFIX + "hasValue")
    HAS_NUMERIC_VALUE = URIRef(OM_URI_PREFIX + "hasNumericValue")
    HAS_UNIT = URIRef(OM_URI_PREFIX + "hasUnit")
    HAS_AGGREGATE_FUNCTION = URIRef(OM_URI_PREFIX + "hasAggregateFunction")

    #GeoSPARQL ontology classes and properties
    FEATURE = GEOSPARQL_URI_PREFIX + "Feature"
    GEOMETRY = GEOSPARQL_URI_PREFIX + "Geometry"
    HAS_GEOMETRY = GEOSPARQL_URI_PREFIX + "hasGeometry"
    AS_WKT = GEOSPARQL_URI_PREFIX + "asWKT"

    PREFIX_URI = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/"
    BUILDABLE_SPACE_GRAPH = PREFIX_URI + "buildablespace" + "/"
    ONTO_ZONING_GRAPH = PREFIX_URI + "ontozone" + "/"
    ONTO_PLANNING_REGULATIONS_GRAPH = PREFIX_URI + "planningregulations" + "/"




