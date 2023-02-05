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

    # OntoPlanningReg classes
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
    DEVELOPMENT_CONTROL_PLAN = URIRef(ONTO_PLANNING_REG_PREFIX + "DevelopmentControlPlan")
    ROAD_CATEGORY = URIRef(ONTO_PLANNING_REG_PREFIX + "RoadCategory")
    ROAD_CATEGORY_1 = URIRef(ONTO_PLANNING_REG_PREFIX + "RoadCategory1")
    ROAD_CATEGORY_2 = URIRef(ONTO_PLANNING_REG_PREFIX + "RoadCategory2")
    ROAD_CATEGORY_3 = URIRef(ONTO_PLANNING_REG_PREFIX + "RoadCategory3")
    ROAD_CATEGORY_4 = URIRef(ONTO_PLANNING_REG_PREFIX + "RoadCategory4")
    ROAD_CATEGORY_5 = URIRef(ONTO_PLANNING_REG_PREFIX + "RoadCategory5")

    # OntoPlanningReg predicate
    REQUIRES_SITE_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresSiteArea")
    REQUIRES_SETBACK = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresSetback")
    REQUIRES_WIDTH = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresWidth")
    REQUIRES_DEPTH = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresDepth")
    REQUIRES_PARTYWALL = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresPartyWall")
    REQUIRES_ROAD_BUFFER = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresRoadBuffer")
    REQUIRES_BUILDING_EDGE = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresBuildingEdge")
    ALLOWS_SITE_COVERAGE = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsSiteCoverage")
    ALLOWS_STOREY_AGGREGATE = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsStoreyAggregate")
    ALLOWS_ABSOLUTE_HEIGHT = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsAbsoluteHeight")
    REQUIRES_FLOOR_TO_FLOOR_HEIGHT = URIRef(ONTO_PLANNING_REG_PREFIX + "requiresFloorToFloorHeight")
    ALLOWS_GROSS_FLOOR_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsGrossFloorArea")
    ALLOWS_GROSS_PLOT_RATIO = URIRef(ONTO_PLANNING_REG_PREFIX + "allowsGrossPlotRatio")
    HAS_ADDITIONAL_TYPE = URIRef(ONTO_PLANNING_REG_PREFIX + "hasAdditionalType")
    HAS_EXTERNAL_REF = URIRef(ONTO_PLANNING_REG_PREFIX + "hasExternalRef")
    HAS_NAME = URIRef(ONTO_PLANNING_REG_PREFIX + "hasName")
    APPLIES_TO = URIRef(ONTO_PLANNING_REG_PREFIX + "appliesTo")
    IN_URBAN_DESIGN_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "inUrbanDesignArea")
    IS_PART_OF_OPR = URIRef(ONTO_PLANNING_REG_PREFIX + "isPartOf")
    HAS_VALUE_OPR = URIRef(ONTO_PLANNING_REG_PREFIX + "hasValue")
    FOR_ZONING_TYPE = URIRef(ONTO_PLANNING_REG_PREFIX + "forZoningType")
    FOR_PROGRAMME = URIRef(ONTO_PLANNING_REG_PREFIX + "forProgramme")
    FOR_NEIGHBOUR_ZONE_TYPE = URIRef(ONTO_PLANNING_REG_PREFIX + "forNeighbourZoneType")
    IS_CONSTRAINED_BY = URIRef(ONTO_PLANNING_REG_PREFIX + "isConstrainedBy")
    FOR_CORNER_PLOT = URIRef(ONTO_PLANNING_REG_PREFIX + "forCornerPlot")
    FOR_FRINGE_PLOT = URIRef(ONTO_PLANNING_REG_PREFIX + "forFringePlot")
    FOR_PLOT_CONTAINED_IN = URIRef(ONTO_PLANNING_REG_PREFIX + "forPlotContainedIn")
    PLOT_ABUTS_GOOD_CLASS_BUNGALOW_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "plotAbutsGoodClassBungalowArea")
    PLOT_IN_GOOD_CLASS_BUNGALOW_AREA = URIRef(ONTO_PLANNING_REG_PREFIX + "plotInGoodClassBungalowArea")
    PLOT_ABUTS_1_3_ROAD_CATEGORY = URIRef(ONTO_PLANNING_REG_PREFIX + "plotAbuts1-3RoadCategory")

    # OntoBuildableSpace classes
    BUILDABLE_SPACE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "BuildableSpace")
    GROSS_FLOOR_AREA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "GrossFloorArea")
    GROSS_PLOT_RATIO = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "GrossPlotRatio")
    SITE_COVERAGE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "SiteCoverage")
    SITE_AREA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "SiteArea")
    ABSOLUTE_HEIGHT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "AbsoluteHeight")
    STOREY_AGGREGATE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "StoreyAggregate")
    BUILDING_EDGE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "BuildingEdge")
    STOREY = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "Storey")
    FLOOR_TO_FLOOR_HEIGHT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "FloorToFloorHeight")
    SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "Setback")
    SIDE_SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "SideSetback")
    REAR_SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "RearSetback")
    FRONT_SETBACK = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "FrontSetback")
    ROAD_BUFFER = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "RoadBuffer")
    PARTYWALL = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "PartyWall")
    FOOTPRINT = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "Footprint")
    FOOTPRINT_AREA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "FootprintArea")
    AVERAGE_WIDTH = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "AverageWidth")
    AVERAGE_DEPTH = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "AverageDepth")

    # OntoBuildableSpace predicates
    HAS_BUILDABLE_SPACE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasBuildableSpace")
    HAS_ROAD_TYPE = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + 'hasRoadType')
    HAS_ROAD_CATEGORY = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + 'hasRoadCategory')
    HAS_AREA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasArea")
    HAS_SITE_AREA = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "hasSiteArea")
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
    AT_LEVEL_OF_DETAIL = URIRef(ONTO_BUILDABLE_SPACE_PREFIX + "atLevelOfDetail")

    # OntoZoning ontology classes and properties
    PLOT = URIRef(ONTO_ZONING_URI_PREFIX + "Plot")
    ZONE_TYPE = URIRef(ONTO_ZONING_URI_PREFIX + "ZoneType")
    LAND_USE = URIRef(ONTO_ZONING_URI_PREFIX + "LandUse")
    PROGRAMME = URIRef(ONTO_ZONING_URI_PREFIX + "Programme")
    HAS_ZONE_TYPE = URIRef(ONTO_ZONING_URI_PREFIX + "hasZoneType")
    ALLOWS_PROGRAMME = URIRef(ONTO_ZONING_URI_PREFIX + "allowsProgramme")

    # Units of Measure ontology classes
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

    # Units of Measure ontology properties
    HAS_VALUE = URIRef(OM_URI_PREFIX + "hasValue")
    HAS_NUMERIC_VALUE = URIRef(OM_URI_PREFIX + "hasNumericValue")
    HAS_UNIT = URIRef(OM_URI_PREFIX + "hasUnit")
    HAS_AGGREGATE_FUNCTION = URIRef(OM_URI_PREFIX + "hasAggregateFunction")

    # GeoSPARQL ontology classes and properties
    FEATURE = GEOSPARQL_URI_PREFIX + "Feature"
    GEOMETRY = GEOSPARQL_URI_PREFIX + "Geometry"
    HAS_GEOMETRY = GEOSPARQL_URI_PREFIX + "hasGeometry"
    AS_WKT = GEOSPARQL_URI_PREFIX + "asWKT"

    PREFIX_URI = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/"
    BUILDABLE_SPACE_GRAPH = PREFIX_URI + "buildablespace" + "/"
    ONTO_ZONING_GRAPH = PREFIX_URI + "ontozone" + "/"
    ONTO_PLANNING_REGULATIONS_GRAPH = PREFIX_URI + "planningregulations" + "/"

    # Road categories
    ROAD_BUFFER_15 = [{'category': ROAD_CATEGORY_1, 'buffer': 15},
                      {'category': ROAD_CATEGORY_2, 'buffer': 7.5},
                      {'category': ROAD_CATEGORY_3, 'buffer': 5},
                      {'category': ROAD_CATEGORY_4, 'buffer': 5},
                      {'category': ROAD_CATEGORY_5, 'buffer': 5}]

    ROAD_BUFFER_30 = [{'category': ROAD_CATEGORY_1, 'buffer': 30},
                      {'category': ROAD_CATEGORY_2, 'buffer': 15},
                      {'category': ROAD_CATEGORY_3, 'buffer': 10},
                      {'category': ROAD_CATEGORY_4, 'buffer': 7.5},
                      {'category': ROAD_CATEGORY_5, 'buffer': 7.5}]

    ROAD_BUFFER_24 = [{'category': ROAD_CATEGORY_1, 'buffer': 24},
                      {'category': ROAD_CATEGORY_2, 'buffer': 12},
                      {'category': ROAD_CATEGORY_3, 'buffer': 7.5},
                      {'category': ROAD_CATEGORY_4, 'buffer': 7.5},
                      {'category': ROAD_CATEGORY_5, 'buffer': 7.5}]

    ROAD_BUFFER_7 = [{'category': ROAD_CATEGORY_1, 'buffer': 7.5},
                      {'category': ROAD_CATEGORY_2, 'buffer': 7.5},
                      {'category': ROAD_CATEGORY_3, 'buffer': 7.5},
                      {'category': ROAD_CATEGORY_4, 'buffer': 7.5},
                      {'category': ROAD_CATEGORY_5, 'buffer': 7.5}]

    ROAD_BUFFER_2 = [{'category': ROAD_CATEGORY_1, 'buffer': 2},
                      {'category': ROAD_CATEGORY_2, 'buffer': 2},
                      {'category': ROAD_CATEGORY_3, 'buffer': 2},
                      {'category': ROAD_CATEGORY_4, 'buffer': 2},
                      {'category': ROAD_CATEGORY_5, 'buffer': 2}]





