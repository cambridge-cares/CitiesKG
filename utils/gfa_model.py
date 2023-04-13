from rdflib.namespace import XSD
from rdflib.namespace import RDF
import uuid
import numpy as np
import pandas as pd
import geopandas as gpd
from GFAOntoManager import *
from SPARQLWrapper import SPARQLWrapper, JSON
import json
from shapely.geometry import Polygon
from rdflib import Dataset, Literal, URIRef
from shapely.geometry import LineString
import sys
from shapely.ops import unary_union


'<--------------------Query Functions------------------->'


def get_plots(endpoint):
    """
    The function queries the KG and retrieves Singapore's Masterplan 2019 plot data.

    :param endpoint: KG endpoint url to which method query is sent.
    :return: plots GeoDataFrame with main attributes 'zone', 'gpr' and 'geom'.
    """
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX ocgml:<http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    SELECT ?plots ?geom ?zone ?gpr
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/>
    { ?plots ocgml:id ?obj_id .
    BIND(IRI(REPLACE(STR(?plots), "cityobject", "genericcityobject")) AS ?gen_obj) }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/> {
    ?s ocgml:cityObjectId ?gen_obj ;
    ocgml:GeometryType ?geom . 
    hint:Prior hint:runLast "true" . }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> {
    ?attr ocgml:cityObjectId ?plots ;
    ocgml:attrName 'LU_DESC' ;
    ocgml:strVal ?zone . 
    ?attr1 ocgml:cityObjectId ?plots ;
    ocgml:attrName 'GPR' ;
    ocgml:strVal ?gpr . } } """)
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    qr = pd.DataFrame(results['results']['bindings'])
    qr = qr.applymap(lambda cell: cell['value'])
    geoms = gpd.GeoSeries(qr['geom'].map(lambda geo: envelope_string_to_polygon(geo, geodetic=True)), crs='EPSG:4326')
    geoms = geoms.to_crs(epsg=3857)
    plots = gpd.GeoDataFrame(qr, geometry=geoms).drop(columns=['geom'])

    return plots


def get_development_control_plans(endpoint):
    """
    The function queries the KG and returns development control plan (dcp) regulations.
    dcp can have 'programme' value - it means that dcp would be used when calculating gfa for that specific programme.

    :param endpoint: KG endpoint url to which method query is sent.
    :return: a DataFrame containing DevelopmentControlPlan regulation content: 'gpr', 'storeys', 'setback', 'site_coverage', etc.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX oz: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>
    PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
    SELECT ?reg (SAMPLE(?gpr) AS ?gpr)
        (SAMPLE(?gpr_function) AS ?gpr_f)
        (SAMPLE(?setback_value) AS ?setback) 
        (SAMPLE(?storey) AS ?storeys)
        (SAMPLE(?storey_function) AS ?storey_f)
        (SAMPLE(?ftf_height) AS ?floor_height)
        (SAMPLE(?site_cov) AS ?site_coverage)
        (SAMPLE(?prog) AS ?programme) 
        (GROUP_CONCAT(DISTINCT(?road_category); separator=',') AS ?road_categories)
    WHERE { ?reg rdf:type opr:DevelopmentControlPlan ;
                opr:isConstrainedBy ?road_category . 
    OPTIONAL { ?reg opr:allowsGrossPlotRatio ?gpr_uri . 
               ?gpr_uri opr:hasValue ?gpr . 
           OPTIONAL { ?gpr_uri om:hasAggregateFunction ?gpr_func .
                    BIND(STRAFTER(STR(?gpr_func), '2/') AS ?gpr_function)} } 
    OPTIONAL {?reg opr:requiresSetback/om:hasValue/om:hasNumericValue ?setback_value } 
    OPTIONAL {?reg opr:forProgramme ?programme_uri .
           BIND(STRAFTER(STR(?programme_uri), '#') AS ?prog)}    
    OPTIONAL { ?reg opr:allowsStoreyAggregate ?storey_aggr . 
               ?storey_aggr obs:numberOfStoreys ?storey . 
           OPTIONAL { ?storey_aggr om:hasAggregateFunction ?storey_func .
                     BIND(STRAFTER(STR(?storey_func), '2/') AS ?storey_function)} } 
    OPTIONAL {?reg opr:requiresFloorToFloorHeight/om:hasValue/om:hasNumericValue ?ftf_height }  
    OPTIONAL { ?reg opr:allowsSiteCoverage ?site_cov_uri . 
           ?site_cov_uri opr:hasValue ?site_cov . } }
    GROUP BY ?reg""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()

    qr = pd.DataFrame(results['results']['bindings'])
    qr = qr.applymap(lambda cell: cell['value'], na_action='ignore')
    dcp = pd.DataFrame(qr)
    dcp['storeys'] = pd.to_numeric(dcp['storeys'], errors='coerce')
    dcp['setback'] = pd.to_numeric(dcp['setback'], errors='coerce')
    dcp['gpr'] = pd.to_numeric(dcp['gpr'], errors='coerce')
    dcp['site_coverage'] = pd.to_numeric(dcp['site_coverage'], errors='coerce')
    dcp['road_categories'] = dcp['road_categories'].str.split(',')

    return dcp


def get_street_block_plans(endpoint):
    """
    The function queries the KG and retrieves street block plan (sbp) regulations.

    :param endpoint: KG endpoint url to which method query is sent.
    :return: a GeoDataFrame containing StreetBlockPlan regulation content: 'gpr', 'storeys', 'setback', 'seback_type', etc.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX ocgml:<http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs:<http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX om:<http://www.ontology-of-units-of-measure.org/resource/om-2/>
    SELECT  ?reg ?geom ?setback ?setback_type ?level ?storeys ?storeys_f ?gpr
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/>
    { ?reg ocgml:id ?obj_id .
    BIND(IRI(REPLACE(STR(?reg), "cityobject", "genericcityobject")) AS ?gen_obj) }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/> 
    {?s ocgml:cityObjectId ?gen_obj ;
    ocgml:GeometryType ?geom . }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/planningregulations/> { 
    ?reg rdf:type opr:StreetBlockPlan .
    OPTIONAL { ?reg opr:requiresSetback | opr:requiresPartyWall ?setback_uri .
    ?setback_uri om:hasValue/om:hasNumericValue ?setback ;
                rdf:type ?setback_type_uri ;
                obs:atStorey/obs:atLevel ?level .         
    BIND(STRAFTER(STR(?setback_type_uri), '#') AS ?setback_type)}                        
    OPTIONAL { ?reg opr:allowsStoreyAggregate/obs:numberOfStoreys ?storeys . }  
    OPTIONAL { ?reg opr:allowsGrossPlotRatio/opr:hasValue ?gpr . } } }""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    qr = pd.DataFrame(results['results']['bindings'])
    qr = qr.applymap(lambda cell: cell['value'], na_action='ignore')

    geoms = gpd.GeoSeries(qr['geom'].map(lambda geo: envelope_string_to_polygon(geo, geodetic=True)),
                          crs='EPSG:4326')
    geoms = geoms.to_crs(epsg=3857)
    sbp = gpd.GeoDataFrame(qr, geometry=geoms).drop(columns=['geom'])
    sbp['storeys'] = pd.to_numeric(sbp['storeys'], errors='coerce')
    sbp['setback'] = pd.to_numeric(sbp['setback'], errors='coerce')
    sbp['gpr'] = pd.to_numeric(sbp['gpr'], errors='coerce')
    sbp['level'] = pd.to_numeric(sbp['level'], errors='coerce')

    return sbp


def get_height_control_plans(endpoint):
    """
    The function queries the KG and retrieves height control plan (hcp) regulations.
    'additional_type' column refers whether hcp has additional types such as 'DetailControl' or 'Conservation'.

    :param endpoint: KG endpoint url to which method query is sent.
    :return: a GeoDataFrame contaning HeightControlPlan regulation content: 'abs_height', 'storeys', 'additional_type'.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX ocgml:<http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs:<http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX om:<http://www.ontology-of-units-of-measure.org/resource/om-2/>
    SELECT  ?reg ?geom ?abs_height ?height_f ?storeys ?storeys_f ?additional_type
    WHERE { 
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/>
    { ?reg ocgml:id ?obj_id .
    BIND(IRI(REPLACE(STR(?reg), "cityobject", "genericcityobject")) AS ?gen_obj) }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/> 
    {?s ocgml:cityObjectId ?gen_obj ;
    ocgml:GeometryType ?geom . }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/planningregulations/> { 
    ?reg rdf:type opr:HeightControlPlan . 
    OPTIONAL {?reg opr:allowsAbsoluteHeight ?height .
    ?height om:hasValue/om:hasNumericValue ?abs_height ;
            om:hasAggregateFunction ?abs_height_f . 
           BIND(STRAFTER(STR(?abs_height_f), "2/") AS ?height_f)}
    OPTIONAL { ?reg opr:allowsStoreyAggregate ?storey_aggr .
    ?storey_aggr om:hasAggregateFunction ?storey_aggr_f ; 
                obs:numberOfStoreys ?storeys . 
           BIND(STRAFTER(STR(?storey_aggr_f), "2/") AS ?storeys_f)}
    OPTIONAL {?reg opr:hasAdditionalType ?detail_control .
           BIND(STRAFTER(STR(?detail_control), "#") AS ?additional_type)} } }""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    qr = pd.DataFrame(results['results']['bindings'])
    qr = qr.applymap(lambda cell: cell['value'], na_action='ignore')

    geoms = gpd.GeoSeries(qr['geom'].map(lambda geo: envelope_string_to_polygon(geo, geodetic=True)),
                          crs='EPSG:4326')
    geoms = geoms.to_crs(epsg=3857)
    hcp = gpd.GeoDataFrame(qr, geometry=geoms).drop(columns=['geom'])
    hcp['abs_height'] = pd.to_numeric(hcp['abs_height'], errors='coerce')
    hcp['storeys'] = pd.to_numeric(hcp['storeys'], errors='coerce')

    return hcp


def get_urban_design_guidelines(endpoint):
    """
    The function queries the KG and retrieves urban design guidelines (udg) regulations.

    :param endpoint: KG endpoint url to which method query is sent.
    :return: a GeoDataFrame containing UrbanDesignGuideline regulation content: 'setback', 'storeys', 'partywall'.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX ocgml:<http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
    PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs:<http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX om:<http://www.ontology-of-units-of-measure.org/resource/om-2/>
    SELECT  ?reg (SAMPLE(?st) as ?storeys) (SAMPLE(?st_f) as ?storeys_f) (SAMPLE(?sb) as ?setback) (SAMPLE(?sb_f) as ?setback_f) (SAMPLE(?pw) as ?partywall) (SAMPLE(?at) as ?additional_type) (SAMPLE(?g) as ?geom) 
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/>
    { ?reg ocgml:id ?obj_id .
    BIND(IRI(REPLACE(STR(?reg), "cityobject", "genericcityobject")) AS ?gen_obj) } 
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/> 
    {?s ocgml:cityObjectId ?gen_obj ;
    ocgml:GeometryType ?g . }
    GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/planningregulations/> { 
    ?reg rdf:type opr:UrbanDesignGuideline ;
    OPTIONAL { ?reg opr:allowsStoreyAggregate ?storey_aggr .
    ?storey_aggr om:hasAggregateFunction ?storey_aggr_f ; 
                obs:numberOfStoreys ?st . 
    BIND(STRAFTER(STR(?storey_aggr_f), "2/") AS ?st_f) }
    OPTIONAL { ?reg opr:requiresSetback ?setback_uri .
    ?setback_uri om:hasValue/om:hasNumericValue ?sb ;
                om:hasAggregateFunction ?setback_function .
    BIND(STRAFTER(STR(?setback_function), "2/") AS ?sb_f) }
    OPTIONAL {?reg opr:requiresPartyWall ?partywall_uri . 
           BIND(BOUND(?partywall_uri) AS ?pw ) }
    OPTIONAL { ?reg opr:hasAdditionalType ?detail_control .
           BIND(STRAFTER(STR(?detail_control), "#") AS ?at) } } }
    GROUP BY ?reg""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    qr = pd.DataFrame(results['results']['bindings'])
    qr = qr.applymap(lambda cell: cell['value'], na_action='ignore')

    geoms = gpd.GeoSeries(qr['geom'].map(lambda geo: envelope_string_to_polygon(geo, geodetic=True)), crs='EPSG:4326')
    geoms = geoms.to_crs(epsg=3857)
    udg = gpd.GeoDataFrame(qr, geometry=geoms).drop(columns=['geom'])
    udg['storeys'] = pd.to_numeric(udg['storeys'], errors='coerce')
    udg['setback'] = pd.to_numeric(udg['setback'], errors='coerce')

    return udg


def get_landed_housing_areas(endpoint):
    """
    The function queries the KG and retrieves landed housing area (lha) regulations.

    :param endpoint: KG endpoint url to which method query is sent.
    :return: a DataFrame containing LandedHousingArea regulation content: 'storeys'.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    SELECT ?reg ?storeys
    WHERE { ?reg opr:allowsStoreyAggregate/obs:numberOfStoreys ?storeys .
    { ?reg rdf:type opr:LandedHousingArea } UNION  
    { ?reg rdf:type opr:GoodClassBungalowArea } } """)
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    qr = pd.DataFrame(results['results']['bindings'])
    lha = qr.applymap(lambda cell: cell['value'], na_action='ignore')
    lha['storeys'] = pd.to_numeric(lha['storeys'], errors='coerce')

    return lha


def get_regulation_links(endpoint):
    """
    The function queries the KG and retrieves planning regulation and plot links.

    :param endpoint: KG endpoint url to which method query is sent.
    :return: a DataFrame containing plot ids, regulation ids, and regulation type.
    """
    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    SELECT ?plots ?reg ?reg_type
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/planningregulations/>  {
    ?reg opr:appliesTo ?plots ;
        rdf:type ?type . }
    BIND(STRAFTER(STR(?type), '#') AS ?reg_type) } """)
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()

    qr = pd.DataFrame(results['results']['bindings'])
    qr = qr.applymap(lambda cell: cell['value'])

    return pd.DataFrame(qr)


def get_road_categories(endpoint):
    """
    The function queries the KG and returns road category regulations.
    Look up: https://miro.com/app/board/uXjVPNZe6Qk=/
    There are more instances than 5 or road category regulations because certain zoning types will have specific rules for road buffers.

    :param endpoint: KG endpoint url to which method query is sent.
    :return: a DataFrame containing road category regulation content: 'category' and 'buffer'.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
    SELECT ?road_reg (SAMPLE(?road_type) AS ?category) (SAMPLE(?road_buffer) AS ?buffer)
    WHERE { ?reg opr:isConstrainedBy ?road_reg .
    ?road_reg rdf:type ?type ;
            opr:requiresRoadBuffer/om:hasValue/om:hasNumericValue ?road_buffer . 
    BIND(STRAFTER(STR(?type), '#RoadCategory') AS ?road_type)}
    GROUP BY ?road_reg""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()

    qr = pd.DataFrame(results['results']['bindings'])
    qr = qr.applymap(lambda cell: cell['value'], na_action='ignore')
    road_cat = pd.DataFrame(qr)
    road_cat['buffer'] = pd.to_numeric(road_cat['buffer'], errors='coerce')
    road_cat['category'] = pd.to_numeric(road_cat['category'], errors='coerce')

    return road_cat


def get_plot_properties(plots, endpoint):
    """
    The function queries the KG and retrieves various plot properties and appends it to plot GeoDataFrame.
    'road_type' property only exist for plots with zoning type 'road'.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param endpoint: KG endpoint url to which method query is sent.
    :return: a modified plots GeoDataFrame contaning columns with plot attributes: 'avg_width', 'avg_depth', 'corner_plot', 'fringe_plot', 'road_type'
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
        PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
        SELECT ?plots ?avg_width
        WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace/> {
        OPTIONAL { ?plots obs:hasWidth/om:hasValue/om:hasNumericValue ?avg_width . } } }""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    plot_properties = pd.DataFrame(results['results']['bindings'])
    plot_properties = plot_properties.applymap(lambda cell: cell['value'], na_action='ignore')
    plots = plots.merge(plot_properties, how='left', on='plots')

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
        PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
        SELECT ?plots ?avg_depth
        WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace/> {
        OPTIONAL { ?plots obs:hasDepth/om:hasValue/om:hasNumericValue ?avg_depth . } } }""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    plot_properties = pd.DataFrame(results['results']['bindings'])
    plot_properties = plot_properties.applymap(lambda cell: cell['value'], na_action='ignore')
    plots = plots.merge(plot_properties, how='left', on='plots')

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>
    PREFIX oz: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>
    SELECT ?plots ?zone ?corner_plot ?fringe_plot ?road_type
    WHERE {?plots oz:hasZone ?zone_uri . 
        BIND(STRAFTER(STR(?zone_uri), '#') AS ?zone)
        OPTIONAL { ?plots obs:isCornerPlot ?corner_plot . }
        OPTIONAL { ?plots obs:atResidentialFringe ?fringe_plot . }
        OPTIONAL { ?plots obs:hasRoadType ?road_type . }}""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    plot_properties = pd.DataFrame(results['results']['bindings'])
    plot_properties = plot_properties.applymap(lambda cell: cell['value'], na_action='ignore')
    plots = plots.merge(plot_properties, how='left', on='plots')
    plots['avg_width'] = pd.to_numeric(plots['avg_width'], errors='coerce')
    plots['avg_depth'] = pd.to_numeric(plots['avg_depth'], errors='coerce')

    return plots


def get_neighbours(plots, endpoint):
    """
    The method queries the KG and retrieves plot neighbour ids and appends to plot GeoDataFrame.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param endpoint: KG endpoint url to which method query is sent.
    :return: a modified plots GeoDataFrame with 'neighbour' column containing neighbour id list.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""
    PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    SELECT ?plots ?neighbour
    WHERE { ?plots obs:hasNeighbour ?neighbour . } """)
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    plot_neighbours = pd.DataFrame(results['results']['bindings'])
    plot_neighbours = plot_neighbours.applymap(lambda cell: cell['value'], na_action='ignore')
    plot_neighbours = plot_neighbours.groupby(by='plots')['neighbour'].apply(list)
    plots = plots.merge(plot_neighbours, left_on='plots', right_index=True, how='left')

    return plots


def get_plot_neighbour_types(plots, endpoint):
    """
    The function queries the KG and retrieves plot neighbour information and appends to plot GeoDataFrame.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param endpoint: KG endpoint url to which method query is sent.
    :return: a modified plots GeoDataFrame with added columns 'neighbour_road_type', 'zone',
    'neighbour_zones', 'abuts_gcba, 'in_central_area'.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX oz: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>
    SELECT ?plots (GROUP_CONCAT(?type; separator=",") as ?neighbour_road_type) (GROUP_CONCAT(DISTINCT ?zone; separator=",") AS ?neighbour_zones) (COUNT(?gcba) AS ?abuts_gcba)  (COUNT(?reg) AS ?in_central_area)
    WHERE { ?plots obs:hasNeighbour ?neighbour .
    ?neighbour oz:hasZone ?zone_uri .
    BIND(STRAFTER(STR(?zone_uri), '#') AS ?zone)
    OPTIONAL { ?reg opr:appliesTo ?plots ;
                    rdf:type opr:CentralArea . }
    OPTIONAL { ?neighbour obs:hasRoadType ?type . }
    OPTIONAL { ?gcba opr:appliesTo ?neighbour ;
    rdf:type opr:GoodClassBungalowArea . }} 
    GROUP By ?plots""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    neighbours = pd.DataFrame(results['results']['bindings'])
    neighbours = neighbours.applymap(lambda cell: cell['value'], na_action='ignore')
    plots = plots.merge(neighbours, how='left', on='plots')
    plots['neighbour_road_type'] = [
        plots.loc[i, 'neighbour_road_type'].split(',') if not pd.isnull(plots.loc[i, 'neighbour_road_type']) else [] for
        i in plots.index]
    plots['neighbour_zones'] = [
        plots.loc[i, 'neighbour_zones'].split(',') if not pd.isnull(plots.loc[i, 'neighbour_zones']) else [] for i in
        plots.index]
    plots['abuts_gcba'] = pd.to_numeric(plots['abuts_gcba'], errors='ignore')
    plots['in_central_area'] = pd.to_numeric(plots['in_central_area'], errors='ignore')

    return plots


def get_plot_allowed_programmes(plots, endpoint):
    """
    The function queries the KG and retrieves allowed programmes on a plot and appends it to plots GeoDataFrame.
    Allowed programmes refer to allowed programmes by street block plan and landed housing area regulations and not ontozoning.
    GoodClassBungallowArea allows only GoodClassBungalows.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param endpoint: KG endpoint url to which method query is sent.
    :return: a modified plots GeoDataFrame with a column containing allowed residential development types.
    """

    sparql = SPARQLWrapper(endpoint)
    sparql.setQuery("""PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>
    PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>
    PREFIX oz: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>
    SELECT ?plots (GROUP_CONCAT(DISTINCT ?pb; separator=',') AS ?in_pb) (GROUP_CONCAT(DISTINCT ?sbp_programme_name; separator=",") as ?sbp_programmes) (GROUP_CONCAT(DISTINCT ?lha) AS ?in_lha) (GROUP_CONCAT(DISTINCT ?lha_programme_name; separator=",") as ?lha_programmes) (COUNT(?gcba) as ?in_gcba)
    WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/> 
              { ?plots oz:hasZone ?zone . }
    OPTIONAL  { ?sbp rdf:type opr:StreetBlockPlan ;
                    opr:appliesTo ?plots ;
                    oz:allowsProgramme ?sbp_programme . 
    BIND(STRAFTER(STR(?sbp_programme), '#') as ?sbp_programme_name)}
    OPTIONAL {?lha rdf:type opr:LandedHousingArea ;
                    opr:appliesTo ?plots ;
                    oz:allowsProgramme ?lha_programme .
    BIND(STRAFTER(STR(?lha_programme), '#') as ?lha_programme_name)}
    OPTIONAL  {?gcba rdf:type opr:GoodClassBungalowArea ;
                    opr:appliesTo ?plots . }
    OPTIONAL {?pb opr:appliesTo ?plots;
                  rdf:type opr:PlanningBoundary.  }  }
    GROUP BY ?plots""")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()
    applicable_regs = pd.DataFrame(results['results']['bindings'])
    applicable_regs = applicable_regs.applymap(lambda cell: cell['value'])
    plots = plots.merge(applicable_regs, how='left', on='plots')
    plots['sbp_programmes'] = [
        plots.loc[i, 'sbp_programmes'].split(',') if not pd.isnull(plots.loc[i, 'sbp_programmes']) else [] for i in
        plots.index]
    plots['lha_programmes'] = [
        plots.loc[i, 'lha_programmes'].split(',') if not pd.isnull(plots.loc[i, 'lha_programmes']) else [] for i in
        plots.index]
    plots['in_pb'] = [plots.loc[i, 'in_pb'].split(',') if not pd.isnull(plots.loc[i, 'in_pb']) else [] for i in
                      plots.index]
    plots['in_lha'] = [plots.loc[i, 'in_lha'].split(',') if not pd.isnull(plots.loc[i, 'in_lha']) else [] for i in
                       plots.index]
    plots['in_gcba'] = pd.to_numeric(plots['in_gcba'], errors='ignore')

    return plots


'<--------------------Utility Functions------------------------>'


def process_plots(plots):
    """
    The function process plot dataframe geometry and attributes: removes multipolygons, drawing artefacts.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :return: plots GeoDataFrame
    """

    plots.loc[:, 'geometry'] = plots.loc[:, 'geometry'].buffer(0)
    plots = plots.loc[plots.area >= 50]
    plots = plots[~(plots.geometry.type == "MultiPolygon")]
    plots['plot_area'] = plots.area
    plots['gpr'] = pd.to_numeric(plots['gpr'], errors='coerce')

    return plots


def envelope_string_to_polygon(geom_string, geodetic=False, flip=True):
    """
    The function process queried geometries in the KG datatype and transformes it into WKT.

    :param geom_string: geometry string as stored in the KG.
    :param geodetic: boolean indicating whether the coordinate reference system is in degrees or metres.
    :param flip: boolean indicating whether original x and y coordinates are in wrong order.
    :return: geometry as WKT.
    """

    points_str = geom_string.split('#')
    num_of_points = int(len(points_str) / 3)
    points = []

    for i in range(num_of_points):
        start_index = i * 3
        x, y, z = float(points_str[start_index]), float(points_str[start_index + 1]), float(points_str[start_index + 2])
        if geodetic:
            if flip:
                points.append((y, x))
            else:
                points.append((x, y))
        else:
            points.append((x, y, z))

    return Polygon(points)


def find_allowed_residential_types(plots, road_list):
    """
    The function determines the allowed residential development types on a plot based on a set of criteria.
    It takes into account oz, sbp and lha as well as geometric criteria.
    Look up residential regulations: https://www.ura.gov.sg/Corporate/Guidelines/Development-Control/Residential

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param road_list: a list with road type names that affect the allowed residential developments on a plot.
    :return: a modified plots GeoDataFrame with a new column 'allowed_residential_types'
    containing a final list with allowed residential programmes.
    """

    zone_list = ['Residential', 'ResidentialWithCommercialAtFirstStorey', 'CommercialAndResidential',
                 'ResidentialOrInstitution', 'White', 'BusinesParkWhite', 'Business1White', 'Business2White']
    mixed_zone_list = ['ResidentialWithCommercialAtFirstStorey', 'CommercialAndResidential', 'White',
                       'BusinesParkWhite', 'Business1White', 'Business2White']
    allowed_types = []

    for i in plots.index:
        zone = plots.loc[i, 'zone']
        cur_allowed_types = []
        if zone in zone_list:
            lha_programmes = plots.loc[i, 'lha_programmes']
            in_lha = '' not in lha_programmes
            in_gcba = plots.loc[i, 'in_gcba'] > 0
            sbp_programmes = plots.loc[i, 'sbp_programmes']
            area = plots.loc[i, 'plot_area']
            width = plots.loc[i, 'avg_width'] if not pd.isnull(plots.loc[i, 'avg_width']) else 0
            depth = plots.loc[i, 'avg_depth'] if not pd.isnull(plots.loc[i, 'avg_depth']) else 0
            is_corner = plots.loc[i, 'corner_plot'] == 'true'
            at_fringe = plots.loc[i, 'fringe_plot'] == 'true'
            road_condition = len(set(plots.loc[i, 'neighbour_road_type']).intersection(road_list)) > 0
            allowed_programmes = sbp_programmes + lha_programmes

            # filter for Bungalow
            b_geo_condition = (area >= 400) and (width >= 10)
            if (b_geo_condition and (
                    (zone == 'Residential') or ('Bungalow' in (sbp_programmes or lha_programmes))) and (
                    not in_gcba)):
                cur_allowed_types.append('Bungalow')

            # filter for Semi-Detached House
            sdh_geo_condition = (area >= 200) and (width >= 8)
            if (sdh_geo_condition and ((zone == 'Residential') or ('Semi-DetachedHouse' in allowed_programmes)) and (
                    not in_gcba)):
                cur_allowed_types.append('Semi-DetachedHouse')

            # filter for Terrace Type 1
            t1_geo_condition_inner = (area >= 150) and (width >= 6) and (not is_corner)
            t1_geo_condition_corner = (area >= 200) and (width >= 8) and is_corner
            if ((t1_geo_condition_inner or t1_geo_condition_corner) and (
                    (zone == 'Residential') or (('TerraceHouse' or 'TerraceType1') in allowed_programmes)) and (
                    not in_gcba)):
                cur_allowed_types.append('TerraceType1')

            # filter for Terrace Type 2
            t2_geo_condition_inner = area >= 80 and width >= 6 and not is_corner
            t2_geo_condition_corner = area >= 80 and width >= 8 and is_corner
            if (t2_geo_condition_inner or t2_geo_condition_corner) and (
                    (zone == 'Residential') or (('TerraceHouse' or 'TerraceType2') in allowed_programmes)) and (
                    not in_gcba):
                cur_allowed_types.append('TerraceType2')

            # filter for Good Class Bungalow type
            gcb_geo_condition = (area >= 1400) and (width >= 18.5) and (depth >= 30)
            if gcb_geo_condition and (in_gcba or ('GoodClassBungalow' in sbp_programmes)):
                cur_allowed_types.append('GoodClassBungalow')

            # filter for Flats, Condominiums and Serviced Apartments type
            if (area >= 1000) and ((zone in zone_list) or ('Flat' in sbp_programmes)) and (not in_gcba) and (
                    not in_lha):
                cur_allowed_types.append('Flat')
            if ((area >= 4000) and (zone in ['Residential', 'ResidentialOrInstitution']) and (not in_gcba) and (
                    not in_lha)):
                cur_allowed_types.append('Condominium')
            if (zone == 'Residential') and (not in_gcba) and (not in_lha) and at_fringe and road_condition:
                cur_allowed_types.append('ServicedApartmentResidentialZone')
            if (zone in mixed_zone_list) and (not in_gcba) and (not in_lha) and at_fringe:
                cur_allowed_types.append('ServicedApartmentMixedUseZone')
            allowed_types.append(cur_allowed_types)
        else:
            allowed_types.append(cur_allowed_types)
    plots['allowed_residential_types'] = allowed_types

    return plots


def get_plot_information(plots, endpoint, road_list):
    """
    The function calls other functions the query KG for various plot information.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param endpoint: KG endpoint url to which queries are sent.
    :param road_list: a list with road type names that affect the allowed residential developments on a plot.
    :return: a modified plots GeoDataFrame.
    """

    plots = plots.drop(columns=['zone'])
    plots = get_plot_properties(plots, endpoint)
    plots = get_plot_neighbour_types(plots, endpoint)
    plots = get_plot_allowed_programmes(plots, endpoint)
    plots = get_neighbours(plots, endpoint)
    plots = find_allowed_residential_types(plots, road_list)

    return plots


def assign_gpr(plots, zone_type, lha, reg_links, in_lha_gpr, fringe_gpr, in_context_gpr, fringe_storeys,
               context_storeys):
    """
    The function writes missing GPR values for educational, religious and civic plots.
    Look up: https://www.ura.gov.sg/Corporate/Guidelines/Development-Control/Non-Residential/EI/GPR-Building-Height

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param zone_type: plot zoning type for which GPRs are reset.
    :param lha: a GeoDataFrame with LandedHousingArea planning regulations.
    :param reg_links: a DataFrame containing plot ids, regulation ids, and regulation type.
    :param in_lha_gpr: a GPR value for case when plot is within landed housing area.
    :param fringe_gpr: a GPR value for case when plot is at residential fringe.
    :param in_context_gpr: a GPR value for case when plot is in relevant to the zone type context.
    :param fringe_storeys: number of storeys for the case when plot is at residential fringe.
    :param context_storeys: number of storeys for the case when plot is in relevant to the zone type context.
    :return: a modified plots GeoDataFrame with GPR values added for the plots with relevant zoning type.
    """

    lha_names = ['LandedHousingArea', 'GoodClassBungalowArea']
    industrial_estates = ['Business1', 'Business2', 'BusinessPark']

    for i in plots[plots['zone'] == zone_type].index:
        cur_regs = reg_links[reg_links['plots'] == plots.loc[i, 'plots']]
        cur_neighbours = plots[plots['plots'].isin(plots.loc[i, 'neighbour'])]
        cur_neighbour_regs = reg_links.loc[reg_links['plots'].isin(plots.loc[i, 'neighbour'])]
        context_gpr = round(cur_neighbours['gpr'].mean(), 1)
        neighbour_zones = len(set(plots.loc[i, 'neighbour_zones']).intersection(industrial_estates)) > 0
        in_lha = any(cur_regs['reg_type'].isin(lha_names))
        in_fringe = any(cur_neighbour_regs['reg_type'].isin(lha_names))

        # these are the rules interpreted from the online text.
        if in_lha and (not context_gpr > 1.4):
            lha_id = cur_regs.loc[cur_regs['reg_type'].isin(lha_names), 'reg']
            plots.loc[i, 'gpr'] = in_lha_gpr
            plots.loc[i, 'context_storeys'] = int(lha.loc[lha['reg'].isin(list(lha_id)), 'storeys'])
        elif in_fringe and (not context_gpr > 1.4):
            plots.loc[i, 'gpr'] = fringe_gpr
            plots.loc[i, 'context_storeys'] = fringe_storeys
        elif (context_gpr > 1.4) or neighbour_zones:
            plots.loc[i, 'gpr'] = in_context_gpr
            plots.loc[i, 'context_storeys'] = context_storeys
        else:
            plots.loc[i, 'gpr'] = context_gpr

    return plots


def assign_sbp_gpr(plots, sbp, reg_links):
    """
    The function writes missing or updates existing GPR values for plots contained in street block plans.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param sbp: a GeoDataFrame containing StreetBlockPlans planning regulations.
    :param reg_links: a DataFrame containing plot ids, regulation ids, and regulation type.
    :return: a modified plots GeoDataFrame with GPR values added or modified for relevant plots.
    """

    # this method is relevant only for plots in sbp because only sbp regulates setbacks as front, side and rear.
    for i in plots[plots['plots'].isin(reg_links[reg_links['reg_type'] == 'StreetBlockPlan']['plots'])].index:
        cur_gpr = float(plots.loc[i, 'gpr'])
        cur_regs = reg_links[reg_links['plots'] == plots.loc[i, 'plots']]
        sbp_id = cur_regs.loc[cur_regs['reg_type'] == 'StreetBlockPlan', 'reg']
        sbp_gpr = float(sbp.loc[sbp['reg'].isin(list(sbp_id)), 'gpr'].sample())

        if not pd.isna(sbp_gpr) and pd.isna(cur_gpr):
            plots.loc[i, 'gpr'] = sbp_gpr
        elif (not pd.isna(sbp_gpr)) and (not pd.isna(cur_gpr)):
            plots.loc[i, 'gpr'] = min(sbp_gpr, cur_gpr)

    return plots


def set_partywall_plots(plots, reg_links, sbp, udg):
    """
    The function adds a boolean value for partywall plots based on applicable planning regulations.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param reg_links: DataFrame containing plot ids, regulation ids, and regulation type.
    :param sbp: a GeoDataFrame containing StreetBlockPlans planning regulations.
    :param udg: a GeoDataFrame containing UrbanDesignGuideline planning regulations.
    :return: a modified plots GeoDataFrame with a column 'partywall'.
    """

    def intersect_party_wall_types(allowed_residential_types):
        """
        The helper function checks whether partywall typologies are allowed on the plot.
        :param allowed_residential_types: a list of allowed residential development on the plot.
        :return: a list of plot ids that have partywall residential typology allowed on a plot.
        """
        party_wall_types = ['TerraceType1', 'TerraceType2', 'Semi-DetachedHouse']
        return len(set(allowed_residential_types).intersection(party_wall_types)) > 0

    plots['partywall'] = False

    sbp_plots = reg_links.loc[
        reg_links['reg'].isin(sbp.loc[sbp['setback_type'] == 'PartyWall', 'reg']), 'plots'].unique()
    udg_plots = reg_links.loc[reg_links['reg'].isin(udg.loc[udg['partywall'] == 'true', 'reg']), 'plots'].unique()
    res_plots = plots.loc[plots['allowed_residential_types'].apply(intersect_party_wall_types), 'plots'].unique()

    partywall_plots = list(set(res_plots).union(set(sbp_plots).union(udg_plots)))
    plots.loc[plots["plots"].isin(partywall_plots), 'partywall'] = True

    return plots


def get_edges(polygon):
    """
    The function explodes polygon into edges.

    :param polygon: a geometry stored in KG geometry datatype 3-15 - ((x,y,z)*5)
    :return: a list of polygon's LineStrings
    """

    curve = polygon.exterior.simplify(0.1)
    return list(map(LineString, zip(curve.coords[:-1], curve.coords[1:])))


def get_min_rect_edges(sbp_plots):
    """
    The function modified plots dataset and generates all plots minimum bounding rectangle edge dataframe.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :return: a modified plots GeoDataFrame and a GeoDataFrame with every plot's minimum bounding rectangle edges.
    """

    sbp_plots['min_rect_edges'] = [get_edges(x) for x in [y.minimum_rotated_rectangle for y in sbp_plots.geometry]]

    edges = sbp_plots.loc[:, ['plots', 'min_rect_edges']].explode(column='min_rect_edges')
    edges = edges.set_geometry(edges['min_rect_edges'], crs=3857)
    edges.geometry = edges.buffer(-3, single_sided=True)
    edges['length'] = edges.length.round(decimals=3)
    edges['min_rect_edge_index'] = edges.groupby(level=0).cumcount()

    return sbp_plots, edges


def classify_min_rect_edges(edges, plots, roads):
    """
    The function clasifies minimum bounding rectangle edges into front, side and rear based on its overlap with road plots.
    The function first identifies the front edge - an edge that verlap mpst with the road.
    Other edges can be interpolated from there on.
    Applicable only to min_rect_edges of plots that are in StreetBlockPlans.

    :param edges: a GeoDataFrame containing every plots every edge.
    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param roads: plots GeoDataFrame which has zoning type 'Road'.
    :return: a modified plots GeoDataFrame with columns storing indexes of categorised minimum bounding rectangle edges.
    """

    intersection = gpd.overlay(edges, roads, how='intersection', keep_geom_type=True)
    intersection['intersection_area'] = intersection.area

    edges = edges.sort_values('plots')

    # define a front edge indice based on largest intersection with the longest edge.
    front_edge = \
    intersection.sort_values(by=['plots_1', 'intersection_area', 'length'], ascending=False).groupby(['plots_1'])[
        'min_rect_edge_index'].first()
    front_edge.name = 'min_rect_front_edge'
    plots = plots.merge(front_edge, left_on='plots', right_index=True, how='left')

    not_front_edge = edges["min_rect_edge_index"] != plots.sort_values('plots')['min_rect_front_edge'].repeat(
        4).to_numpy()
    front_edge_length = edges.loc[~not_front_edge, ["plots", 'length']].set_index("plots")['length']
    front_edge_length_missing = list(set(plots["plots"].unique()).difference(front_edge_length.index))
    front_edge_length = pd.concat(
        [front_edge_length, pd.Series([0] * len(front_edge_length_missing), index=front_edge_length_missing)])
    front_edge_length = front_edge_length.sort_index().repeat(4)

    # define a rear edge indice which is not a front edge indice but the same length.
    rear_edge = not_front_edge & (edges["length"] == front_edge_length.to_numpy())
    rear_edge = edges.loc[rear_edge, :].groupby("plots")["min_rect_edge_index"].first()
    rear_edge.name = 'min_rect_rear_edge'
    plots = plots.merge(rear_edge, left_on='plots', right_index=True, how='left')

    # define side edge indices which are the remaining indices that are not front or rear edge indices.
    edge_indices = [
        list({0.0, 1.0, 2.0, 3.0}.difference([plots.loc[x, 'min_rect_front_edge'], plots.loc[x, 'min_rect_rear_edge']]))
        for x in plots.index]
    plots['min_rect_side_edges'] = [edge_indices[i] for i in range(len(edge_indices))]
    plots.loc[plots['min_rect_front_edge'].isna(), 'min_rect_side_edges'] = np.nan

    return plots


def classify_neighbours(sbp_plots, plots, reg_links, min_rect_edge_df):
    """
    The function classifies neighbours based on overlap with corresponding classified minimum bounding rectangle edges.
    Applicable only to plots that are in StreetBlockPlans.

    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param reg_links: a DataFrame containing plot ids, regulation ids, and regulation type.
    :return: a modified plots GeoDataFrame with neighbour ids categorised and stored in new relevant columns.
    """

    sbp_plots['side_neighbours'] = [[] for t in sbp_plots.index]
    sbp_plots['front_neighbours'] = [[] for t in sbp_plots.index]
    sbp_plots['rear_neighbours'] = [[] for t in sbp_plots.index]

    for i in sbp_plots.index:
        if not pd.isnull(sbp_plots.loc[i, 'min_rect_front_edge']):
            cur_neighbours = plots.loc[plots['plots'].isin(sbp_plots.loc[i, 'neighbour']), ['plots', 'geometry']]
            cur_min_rect_edges = min_rect_edge_df.loc[min_rect_edge_df['plots'] == sbp_plots.loc[i, 'plots'],
                                                      ['geometry', 'min_rect_edge_index']].set_index(
                'min_rect_edge_index')
            cur_min_rect_edges['edge_type'] = 'side'
            cur_min_rect_edges.loc[int(sbp_plots.loc[i, 'min_rect_front_edge']), 'edge_type'] = 'front'
            cur_min_rect_edges.loc[int(sbp_plots.loc[i, 'min_rect_rear_edge']), 'edge_type'] = 'rear'

            neighbour_intersection = gpd.overlay(cur_neighbours, cur_min_rect_edges, how='intersection',
                                                 keep_geom_type=True)
            neighbour_intersection['intersection_area'] = neighbour_intersection.area
            neighbour_types = \
            neighbour_intersection.sort_values(by=['plots', 'intersection_area'], ascending=False).groupby(['plots'])[
                'edge_type'].first().reset_index()

            sbp_plots.loc[i, 'side_neighbours'].extend(
                list(neighbour_types[neighbour_types['edge_type'] == 'side']['plots']))
            sbp_plots.loc[i, 'front_neighbours'].extend(
                list(neighbour_types[neighbour_types['edge_type'] == 'front']['plots']))
            sbp_plots.loc[i, 'rear_neighbours'].extend(
                list(neighbour_types[neighbour_types['edge_type'] == 'rear']['plots']))

    return sbp_plots


def get_plot_edges(plots):
    """
    The function modifies plot dataframe with new column and generates dataframe with all plot edges.

    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :return: a modified plots GeoDataFrame and a GeoDataFrame with all plots' edges.
    """

    plots['edges'] = [get_edges(i) for i in plots['geometry']]

    # The column 'geometry' is a buffered 'edge' string because overlay can be checked only across polygons.
    edges = plots.loc[:, ['partywall', 'plots', 'edges', 'geometry']].explode(column='edges').drop(['geometry'], axis=1)
    edges = edges.set_geometry(edges['edges'], crs=3857)
    edges.geometry = edges.buffer(1, single_sided=True)
    edges['buffered_edge_area'] = edges.area
    edges['edge_index'] = edges.groupby(level=0).cumcount()

    return plots, edges


def classify_plot_edges(gfa_plots, sbp_plots, plots, edges):
    """
    The function classifies plot edges into front, side and rear based on overlap with classified neighbours.
    Applicable only to plots that are in StreetBlockPlans.

    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param edges: a GeoDataFrame containing every plots every edge.
    :return: a modified plots GeoDataFrame with plot edges classified and edge indexes stored in relevant columns.
    """

    gfa_plots['side_edges'] = [[] for i in gfa_plots.index]
    gfa_plots['front_edges'] = [[] for i in gfa_plots.index]
    gfa_plots['rear_edges'] = [[] for i in gfa_plots.index]

    for i in sbp_plots.index:
        cur_neighbours = plots.loc[plots['plots'].isin(gfa_plots.loc[i, 'neighbour']), ['plots', 'geometry']].rename(
            columns={'plots': 'neighbours'})
        cur_plot_edges = edges[edges['plots'] == gfa_plots.loc[i, 'plots']]

        edge_int = gpd.overlay(cur_plot_edges, cur_neighbours, how='intersection', keep_geom_type=True)
        edge_int['intersection_area'] = edge_int.area
        edge_int = edge_int.sort_values(by=['plots', 'intersection_area'], ascending=False).groupby(
            ['edge_index']).first()
        edge_int = edge_int.reset_index()

        gfa_plots.loc[i, 'side_edges'].extend(
            edge_int.loc[edge_int['neighbours'].isin(sbp_plots.loc[i, 'side_neighbours']), 'edge_index'])
        gfa_plots.loc[i, 'front_edges'].extend(
            edge_int.loc[edge_int['neighbours'].isin(sbp_plots.loc[i, 'front_neighbours']), 'edge_index'])
        gfa_plots.loc[i, 'rear_edges'].extend(
            edge_int.loc[edge_int['neighbours'].isin(sbp_plots.loc[i, 'rear_neighbours']), 'edge_index'])

    return gfa_plots


def set_road_buffer_edges(edges, plots, gfa_plots):
    """
    The function identifies plot edge indexes that will be subject to relevant road buffers.

    :param edges: a GeoDataFrame containing every plots every edge.
    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :return: a modified plots GeoDataFrame with new columns 'cat_1_edges', 'cat_2_edges and 'cat_3_5_edges',
    containing classified plot edge indexes.
    """

    edges_int = gpd.overlay(edges, plots.loc[plots['zone'] == 'Road', ['geometry', 'road_type']], how='intersection',
                            keep_geom_type=False)
    edges_int['intersection_area'] = edges_int.area
    edges_int = edges_int[(edges_int['intersection_area'] / edges_int['buffered_edge_area']) > 0.2]

    # this mapping is our interpretation of the regulations. There are no explicit mapping by the agencies.
    road_cats = {'cat_1_edges': ['Expressway', 'Semi Expressway'],
                 'cat_2_edges': ['Major Arterials/Minor Arterials'],
                 'cat_3_5_edges': ['Local Access', 'Local Collector/Primary Access', 'Slip Road', 'Service Road'],
                 'no_cat_edges': ['no category']}

    for category, road_type in road_cats.items():
        cat = edges_int[edges_int['road_type'].isin(road_type)]
        cat_edges = cat.groupby(by='plots')['edge_index'].apply(lambda edges: list(set(edges))).rename(category)
        gfa_plots = gfa_plots.merge(cat_edges, left_on='plots', right_index=True, how='left')
        gfa_plots[category] = gfa_plots[category].fillna('').apply(list)

    return gfa_plots


def set_partywall_edges(edges, plots, gfa_plots):
    """
    The function specifies plot edge indexes that are partywall edges (0m buffer)
    based on buffered partywall plot edge and neighbouring partywall plot intersections.

    :param edges: a GeoDataFrame containing every plots every edge.
    :param plots: Singapore's Masterplan 2019 plots GeoDataFrame queried from the KG.
    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :return: a modified plots GeoDataFrame with a new column 'partywall_edges', containing a list of edge indexes.
    """

    # filter edges that belong to partywall plots.
    partywall_edges_df = edges[edges['partywall']]

    # check id buffered edge polygons overlap with other partywall plots.
    edges_int = gpd.overlay(partywall_edges_df, plots.loc[plots['partywall'], ['geometry']], how='intersection',
                            keep_geom_type=False)
    edges_int = edges_int[(edges_int.area / edges_int['buffered_edge_area']) > 0.5]
    partywall_edges = edges_int.groupby(by='plots')['edge_index'].apply(lambda edges: list(set(edges))).rename(
        'partywall_edges')

    gfa_plots = gfa_plots.merge(partywall_edges, left_on='plots', right_index=True, how='left')
    gfa_plots['partywall_edges'] = gfa_plots['partywall_edges'].fillna("").apply(list)
    gfa_plots['partywall_edges'] = gfa_plots.apply(
        lambda row: list(set(row['partywall_edges']) - set(row['rear_edges'])), axis=1)

    return gfa_plots


def get_udg_edge_setbacks(udg, reg_links, edges):
    """
    The function generates a DataFrame with plot ids and edge indexes to which urban design guideline for setbacks apply.

    :param udg: a GeoDataFrame containing UrbanDesignGuideline regulation content
    :param reg_links: a DataFrame containing plot ids, regulation ids, and regulation type.
    :param edges: a GeoDataFrame containing every plots every edge.
    :return: a DataFrame containing plot edge indexes and applicable udg setbacks.
    """

    # udgs may have regulations not only for setbacks, hence need to filter out those.
    udg_with_setback = udg.loc[~udg['setback'].isna(), ['reg', 'setback', 'geometry']]
    udg_with_setback = udg_with_setback.loc[udg_with_setback['reg'].isin(reg_links['reg'].unique())]

    edges_int = gpd.overlay(edges, udg_with_setback, how='intersection', keep_geom_type=False)
    edges_int['intersection_area'] = edges_int.area
    edges_int = edges_int[(edges_int['intersection_area'] / edges_int['buffered_edge_area']) > 0.5]
    edges_int = edges_int.merge(reg_links.loc[reg_links['reg'].isin(udg_with_setback['reg'].unique()), :],
                                left_on='plots', right_on='plots', how='left', suffixes=(None, '_links'))
    edges_int = edges_int.loc[(~edges_int['reg_links'].isna()) & (edges_int['reg_links'] == edges_int['reg']), :].drop(
        columns=['reg_type'])

    # edge indexes get assigned a setback based on largest overlap with udg.
    udg_edges = (edges_int.sort_values(by=['plots', 'edge_index', 'intersection_area'], ascending=False)
                 .groupby(by=['plots', 'edge_index']).first().reset_index()
                 .drop(columns=['partywall', 'buffered_edge_area', 'intersection_area', 'reg_links']))

    return udg_edges


'<-------------------GFA Calculation Functions------------------>'


def get_unclear_plots(reg_links, hcp, udg):
    """
    The function generates a list with plot ids which do not qualify for gfa calculation.
    That includes plots that fall in 'ConservationArea' or 'Monument' regulations or
    are linked to other regulations that has additional_type 'DetailControl' or 'ConservationArea', or
    plots that has invalid zoning types, like 'BeachArea', 'Utility', 'Cemetery', etc.

    :param reg_links: a DataFrame containing plot ids, regulation ids, and regulation type.
    :param hcp: a GeoDataFrame containing HeightControlPlan planning regulations.
    :param udg: a GeoDataFrame containing UrbanDesignGuideline planning regulations.
    :return: a list with plot ids.
    """

    con_plots = list(reg_links[reg_links['reg_type'].isin(['ConservationArea', 'Monument'])]['plots'])
    hcp_plots = list(reg_links[reg_links['reg'].isin(list(hcp[~pd.isna(hcp['additional_type'])]['reg']))]['plots'])
    udg_plots = list(reg_links[reg_links['reg'].isin(list(udg[~pd.isna(udg['additional_type'])]['reg']))]['plots'])
    unclear_plots = list(set(con_plots + hcp_plots + udg_plots))

    return unclear_plots


def set_plot_edge_setbacks(gfa_plots, reg_links, dcp, sbp, road_cats, udg_edges):
    """
    The function updates every plot edge setback value based on applicable regulations and write edge setbacks as a list.

    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :param reg_links: a DataFrame containing plot ids, regulation ids, and regulation type.
    :param dcp: a DataFrame containing DevelopmentControlPlan regulation content.
    :param sbp: a GeoDataFrame containing StreetBlockPlan regulation content.
    :param road_cats: a DataFrame containing road category regulation content.
    :param udg_edges: a DataFrame containing plot edge indexes and applicable udg setbacks.
    :return: a modified plot GeoDataFrame with a list of plot edge setbacks stored in a column 'edge_setbacks'.
    """
    all_setbacks = []
    for count, i in enumerate(gfa_plots.index):
        plot_setbacks = {}
        plot_id = gfa_plots.loc[i, 'plots']

        if plot_id in reg_links['plots'].unique():

            num_of_edges = len(gfa_plots.loc[i, 'edges'])
            cur_regs = reg_links[reg_links['plots'] == plot_id]
            cur_dcp = dcp[dcp['reg'].isin(cur_regs[cur_regs['reg_type'] == 'DevelopmentControlPlan']['reg'])]
            plot_zone = gfa_plots.loc[i, 'zone']

            if plot_zone in ['Residential', 'ResidentialWithCommercialAtFirstStorey', 'CommercialAndResidential']:
                allowed_res_programmes = gfa_plots.loc[i, 'allowed_residential_types'] + ['Clinic']
                cur_dcp = cur_dcp[
                    cur_dcp['programme'].isin(set(cur_dcp['programme']).intersection(set(allowed_res_programmes)))]

            cur_road_cats = cur_dcp['road_categories'].explode().unique()
            cur_road_cats = road_cats.loc[road_cats['road_reg'].isin(cur_road_cats), :]
            udg_setbacks = udg_edges.loc[udg_edges['plots'] == plot_id, ['edge_index', 'setback']]

            front_sbp_setback, side_sbp_setback, rear_sbp_setback = [], [], []

            if 'StreetBlockPlan' in list(cur_regs['reg_type'].unique()):
                cur_sbp = sbp[sbp['reg'].isin(cur_regs[cur_regs['reg_type'] == 'StreetBlockPlan']['reg'])]

                front_sbp_setback = list(
                    cur_sbp[cur_sbp['setback_type'] == 'FrontSetback'].sort_values(by=['level'])['setback'])
                side_sbp_setback = list(
                    cur_sbp[cur_sbp['setback_type'] == 'SideSetback'].sort_values(by=['level'])['setback'])
                rear_sbp_setback = list(
                    cur_sbp[cur_sbp['setback_type'] == 'RearSetback'].sort_values(by=['level'])['setback'])

            for reg in cur_dcp.index:

                partywall_edges = list(gfa_plots.loc[i, 'partywall_edges']) if gfa_plots.loc[i, 'partywall'] else []
                programme = cur_dcp.loc[reg, 'programme']
                programme_road_cats = cur_road_cats[cur_road_cats['road_reg'].isin(
                    set(cur_road_cats['road_reg']).union(cur_dcp.loc[reg, 'road_categories']))]
                setback_list = [float(cur_dcp.loc[reg, 'setback'])] * num_of_edges

                # adjust the initial setback list based on known regulation setbacks.
                if not udg_setbacks.empty:
                    for udg_edge in udg_setbacks['edge_index']:
                        setback_list[udg_edge] = udg_setbacks[udg_setbacks['edge_index'] == udg_edge]['setback']

                if front_sbp_setback:
                    for front_edge in gfa_plots.loc[i, 'front_edges']:
                        setback_list[front_edge] = front_sbp_setback
                if side_sbp_setback:
                    for side_edge in gfa_plots.loc[i, 'side_edges']:
                        setback_list[side_edge] = side_sbp_setback
                if rear_sbp_setback:
                    for rear_edge in gfa_plots.loc[i, 'rear_edges']:
                        setback_list[rear_edge] = rear_sbp_setback

                if gfa_plots.loc[i, 'cat_1_edges']:
                    for cat_1 in gfa_plots.loc[i, 'cat_1_edges']:
                        setback_list[cat_1] = float(
                            programme_road_cats.loc[(programme_road_cats['category'] == 1), 'buffer'].max())
                if gfa_plots.loc[i, 'cat_2_edges']:
                    for cat_2 in gfa_plots.loc[i, 'cat_2_edges']:
                        setback_list[cat_2] = float(
                            programme_road_cats.loc[programme_road_cats['category'] == 2, 'buffer'].max())
                if gfa_plots.loc[i, 'cat_3_5_edges']:
                    for cat_3 in gfa_plots.loc[i, 'cat_3_5_edges']:
                        setback_list[cat_3] = float(
                            programme_road_cats.loc[programme_road_cats['category'] == 3, 'buffer'].max())

                # some residential typologies by definition are partywall developments.
                if programme == 'Semi-DetachedHouse' and gfa_plots.loc[i, 'side_edges']:
                    partywall_edges = list(set(partywall_edges + [gfa_plots.loc[i, 'side_edges'][0]]))
                if (programme == 'TerraceType1') or (programme == 'TerraceType2'):
                    partywall_edges = list(set(partywall_edges + gfa_plots.loc[i, 'side_edges']))
                if (programme == 'Bungalow') or (programme == 'GoodClassBungalow'):
                    partywall_edges = []

                if partywall_edges:
                    for partywall_edge in partywall_edges:
                        setback_list[partywall_edge] = 0.
                plot_setbacks[programme] = setback_list
        all_setbacks.append(plot_setbacks)
        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, gfa_plots.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, gfa_plots.shape[0]))

    gfa_plots['edge_setbacks'] = all_setbacks

    return gfa_plots


def create_setback_area(edges, edge_setbacks, plot_geom):
    """
    The function buffers every edge with associated setback, merges resulting polygons and subtract it from plot geometry.

    :param edges: a GeoDataFrame containing every plots every edge.
    :param edge_setbacks: a list with buffer values for every edge of a plot geometry.
    :param plot_geom: plot geometry polygon.
    :return: a remaining geometry after subtracting buffered edge geometry from plot geometry.
    """

    edges_buffered = []
    for i, edge in enumerate(edges):
        if edge_setbacks[i] > 0:
            edges_buffered.append(edge.buffer(-edge_setbacks[i], single_sided=True))
    buffered_area = unary_union(edges_buffered)
    remaining_area = plot_geom.difference(buffered_area)
    remaining_area = remaining_area.buffer(-1, cap_style=2, join_style=2, single_sided=True)
    remaining_area = remaining_area.buffer(1, cap_style=2, join_style=2, single_sided=True)

    return remaining_area


def get_buildable_footprints(gfa_plots):
    """
    The function generates a list of buildable footprints for every unique/known from the setbacks storey.
    Position in the list indicates at which floor that footprint exist.

    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :return: a modified plots GeoDataFrame with list of footprints.
    """
    all_setbacked_geom = []
    for count, plot in enumerate(gfa_plots.index):

        setbacked_geom = {}
        plot_edges = gfa_plots.loc[plot, 'edges']
        plot_setbacks = gfa_plots.loc[plot, 'edge_setbacks']

        if plot_setbacks:
            for programme, cur_setback in plot_setbacks.items():
                # setback can be simply a value or a list of values which indicates
                # that the edge at different storeys will have different setback.
                setback_type = [type(setback) for setback in cur_setback]
                cur_setback = [[setback] if not setback_type[j] is list else setback for j, setback in
                               enumerate(cur_setback)]

                num_of_levels = max(map(len, cur_setback))
                setbacked_geom[programme] = []

                for level in range(num_of_levels):
                    level_setback = [
                        float(cur_setback[j][level]) if (len(cur_setback[j]) > level) else float(cur_setback[j][-1]) for
                        j in range(len(cur_setback))]
                    remaining_geom = create_setback_area(plot_edges, level_setback, gfa_plots.loc[plot, 'geometry'])
                    remaining_area = remaining_geom.area
                    if remaining_area > 0:
                        setbacked_geom[programme].append(remaining_geom)

        all_setbacked_geom.append(setbacked_geom)

        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, gfa_plots.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, gfa_plots.shape[0]))

    gfa_plots['footprints'] = all_setbacked_geom

    return gfa_plots


def get_plot_parts(plot, cur_reg_type, storey_regs, plots):
    """
    The function generates a list of plot parts that are based on planning regulations that apply only to a part of a plot.
    The function splits plot into the maximum number of parts of udg or hcp.
    Only hcp and udg may govern plot only partially.
    By default, whole plot is one part.

    :param plot: plot index for which plot parts are generated.
    :param cur_reg_type: regulation tpe for which current parts are retrieved.
    :param storey_regs: a list of regulations applicable to specific floor.
    :param plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :return: plots part geometries as a list and corresponding number of storeys for every part as a list.
    """
    cur_reg = gpd.GeoDataFrame(storey_regs.loc[storey_regs['reg_type'] == cur_reg_type, ['reg', 'storeys', 'geometry']],
                               geometry='geometry', crs=3857)

    used_regs = list(cur_reg['reg'])
    reg_parts = gpd.overlay(cur_reg, gpd.GeoDataFrame(
        pd.DataFrame(plots.loc[plot, :].to_numpy().reshape(1, -1), columns=plots.columns, index=[plot]),
        geometry='geometry', crs=3857), how='intersection', keep_geom_type=False)
    storeys = list(reg_parts['storeys'])
    reg_part = reg_parts['geometry'].iloc[0]

    # Define storeys for each part
    for part in range(1, reg_parts.shape[0]):
        reg_part = reg_part.union(reg_parts['geometry'].iloc[part])

        # buffering to clean the resulting part geometry.
    remaining_part = plots.loc[plot, 'geometry'].difference(reg_part).buffer(-1, join_style=2).buffer(1, join_style=2)
    parts = list(reg_parts['geometry'])

    if remaining_part.area / plots.loc[plot, 'geometry'].area > 0.1:
        parts.append(remaining_part)
        storeys.append(float('inf'))

    return used_regs, storeys, parts


def get_buildable_storeys(gfa_plots, udg, hcp, dcp, lha, sbp, reg_links):
    """
    The function estimates allowed number of storeys on a plot or plot part.
    Plot parts are defined only by hcp or udg regulations.

    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :param udg: a GeoDataFrame containing UrbanDesignGuideline regulation content.
    :param hcp: a GeoDataFrame containing HeightControlPlan regulation content.
    :param dcp: a DataFrame containing DevelopmentControlPlan regulation content.
    :param lha: a GeoDataFrame containing LandedHousingArea regulation content.
    :param sbp: a GeoDataFrame containing StreetBlockPlan regulation content.
    :param reg_links: a DataFrame containing plot ids, regulation ids, and regulation type.
    :return: a modified plots GeoDataFrame with new columns containing number of storeys for every plot parts.
    """

    combined_storeys = []
    combined_parts = []
    residential_zones = ['Residential', 'CommercialAndResidential', 'ResidentialOrInstitution',
                         'ResidentialWithCommercialAtFirstStorey']

    # get hcp or udg regulations that has a height value and regulates buildable space height.
    hcp_udg = pd.concat(
        [hcp.loc[:, ['reg', 'storeys', 'abs_height', 'geometry']], udg.loc[:, ['reg', 'storeys', 'geometry']]])
    height_regs = reg_links.merge(hcp_udg, on='reg', how='left')
    height_regs = height_regs.loc[~(height_regs['storeys'].isna()) | ~(height_regs['abs_height'].isna()), :]

    for count, plot in enumerate(gfa_plots.index):

        plot_id = gfa_plots.loc[plot, 'plots']
        plot_zone = gfa_plots.loc[plot, 'zone']
        storey_height = 3.6 if plot_zone in residential_zones else 5.0
        cur_height_regs = height_regs[height_regs['plots'] == plot_id].copy()
        cur_lha, cur_sbp = float('inf'), float('inf')

        # by default is only one part for the plot with regulated height - the whole plot area.
        parts = [gfa_plots.loc[plot, 'geometry']]
        part_storeys = [float('inf')]
        used_regs = []

        # Divide plot into parts based on udg or hcp. Contains storeys from hcp and udg.
        if cur_height_regs.shape[0] > 0:
            if 'UrbanDesignGuideline' in cur_height_regs['reg_type'].unique():
                used_regs, part_storeys, parts = get_plot_parts(plot, 'UrbanDesignGuideline', cur_height_regs,
                                                                gfa_plots)
            else:
                # This is the case when HeightControlPlan regulatons apply to the plot.
                height_regs_metres = pd.isna(cur_height_regs['storeys'])
                height_metres = cur_height_regs.loc[height_regs_metres, 'abs_height'].to_numpy()
                # Overwrite absolute height in metres with number of storeys.
                cur_height_regs.loc[height_regs_metres, 'storeys'] = np.floor(height_metres / storey_height)
                used_regs, part_storeys, parts = get_plot_parts(plot, 'HeightControlPlan', cur_height_regs, gfa_plots)

        cur_regs = reg_links[reg_links['plots'] == plot_id]
        cur_regs = cur_regs[~cur_regs['reg'].isin(used_regs)]

        if 'LandedHousingArea' in list(cur_regs['reg_type']):
            cur_lha = lha[lha['reg'].isin(cur_regs[cur_regs['reg_type'] == 'LandedHousingArea']['reg'])][
                'storeys'].min()
        if 'StreetBlockPlan' in list(cur_regs['reg_type']):
            cur_sbp = sbp[sbp['reg'].isin(cur_regs[cur_regs['reg_type'] == 'StreetBlockPlan']['reg'])]['storeys'].min()

        cur_dcp = dcp.loc[dcp['reg'].isin(cur_regs[cur_regs['reg_type'] == 'DevelopmentControlPlan']['reg']), :]
        # this fixes an issue that there are more dcp regulations linked to a some plots than there should be.
        if plot_zone in ['Residential', 'ResidentialWithCommercialAtFirstStorey', 'CommercialAndResidential']:
            allowed_res_programmes = gfa_plots.loc[plot, 'allowed_residential_types'] + ['Clinic']
            cur_dcp = cur_dcp[
                cur_dcp['programme'].isin(set(cur_dcp['programme']).intersection(set(allowed_res_programmes)))]

        storey = {}

        for programme in cur_dcp['programme'].unique():
            # contains storeys from lha and sbp.
            storey[programme] = np.nanmin(np.concatenate(([float('inf'), cur_lha, cur_sbp], cur_dcp.loc[
                cur_dcp['programme'] == programme, 'storeys'].to_numpy(dtype=object))))

        storeys = {programme: [min(storey[programme], part_storey) for part_storey in part_storeys] for programme in
                   storey.keys()}
        combined_storeys.append(storeys)
        combined_parts.append(parts)

        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, gfa_plots.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, gfa_plots.shape[0]))

    gfa_plots['storeys'] = combined_storeys
    gfa_plots['parts'] = combined_parts

    return gfa_plots


def compute_part_gfa(allowed_storeys, footprint_areas, plot_area, site_coverage):
    """
    The function estimates allowed plot gfa by adding known footprint areas in plot parts for all allowed storeys.

    :param allowed_storeys: number of allowed storeys.
    :param footprint_areas: footprints allowed on a plot.
    :param plot_area: area of the whole plot.
    :param site_coverage: float value for allowed site coverage on a plot.
    :return: estimated plot part gfa. By default, plot has one part - whole plot.
    """
    part_gfa = 0

    # Go through storey footprints' areas as long as the storey is still allowed
    for storey in range(len(footprint_areas)):
        # Check whether storey is allowed (less than number of storeys)
        # and append it to part gfa if area do not exceed allowed site coverage.
        if storey < allowed_storeys:
            storey_area = footprint_areas[storey]
            if storey_area / plot_area > site_coverage:
                storey_area = plot_area * site_coverage
            part_gfa += storey_area

    # Use the last known storey footprint for the remaining storeys.
    if allowed_storeys > len(footprint_areas):
        part_gfa += storey_area * (allowed_storeys - len(footprint_areas))

    return part_gfa


def compute_plot_gfa(gfa_plots, reg_links, sbp, dcp):
    """
    The function estimates gfa value for every allowed programme on a plot
    or a general gfa if there are no regulation sceptions for specific programmes.
    Gfa can only be estimated if plota has generated allowed footprints and derived storeys and plot parts
    or a known gpr value.

    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :param reg_links: a DataFrame containing plot ids, regulation ids, and regulation type.
    :param sbp: a GeoDataFrame containing StreetBlockPlan regulation content.
    :param dcp: a DataFrame containing DevelopmentControlPlan regulation content.
    :return: estimated plot gfa for every allowed programme
    """

    all_gfas = []

    for count, plot in enumerate(gfa_plots.index):

        plot_id = gfa_plots.loc[plot, 'plots']
        plot_zone = gfa_plots.loc[plot, 'zone']
        plot_area = gfa_plots.loc[plot, 'geometry'].area
        plot_mp_gpr = gfa_plots.loc[plot, 'gpr']

        cur_regs = list(reg_links[reg_links['plots'] == plot_id]['reg'])
        cur_sbp = sbp[sbp['reg'].isin(cur_regs)]
        cur_dcp = dcp[dcp['reg'].isin(cur_regs)]

        # this fixes an issue that there are more dcp regulations linked to a some plots than there should be.
        # TODO: script that links regulations to plots should be inspected.
        residential_zones = ['Residential', 'ResidentialWithCommercialAtFirstStorey', 'CommercialAndResidential']
        if plot_zone in residential_zones:
            allowed_res_programmes = gfa_plots_test.loc[plot, 'allowed_residential_types'] + ['Clinic']
            cur_dcp = cur_dcp[
                cur_dcp['programme'].isin(set(cur_dcp['programme']).intersection(set(allowed_res_programmes)))]

        gfa = {}

        cur_parts = gfa_plots.loc[plot, 'parts']
        cur_footprints = gfa_plots.loc[plot, 'footprints']
        cur_storeys = gfa_plots.loc[plot, 'storeys']

        if cur_footprints and cur_storeys:
            for programme, footprints in cur_footprints.items():

                programme_gfa = 0.0
                programme_storeys = cur_storeys[programme]

                # get the list of all linked gprs and set the smallest.
                gpr_list = list(cur_dcp[cur_dcp['programme'] == programme]['gpr'].dropna()) + list(
                    cur_sbp['gpr'].dropna())
                if not pd.isnull(plot_mp_gpr):
                    gpr_list.append(plot_mp_gpr)
                cur_gpr = min(gpr_list) if gpr_list else None

                if not np.any(np.isinf(programme_storeys)) and footprints:
                    site_coverage_list = list(cur_dcp[cur_dcp['programme'] == programme]['site_coverage'])
                    cur_site_coverage = min(site_coverage_list) if site_coverage_list else 1.0

                    # Only one part -> no intersection with footprint(s) required.
                    if len(cur_parts) == 1:
                        # One storey restriction since there is only one part
                        part_programme_storeys = programme_storeys[0]
                        programme_gfa = compute_part_gfa(part_programme_storeys,
                                                         [footprint.area for footprint in footprints],
                                                         plot_area, cur_site_coverage)
                    else:
                        for part_index, part in enumerate(cur_parts):
                            # Different storey restriction for each part
                            part_programme_storeys = programme_storeys[part_index]
                            programme_gfa += compute_part_gfa(part_programme_storeys,
                                                              [footprint.intersection(part).area for footprint in
                                                               footprints],
                                                              plot_area, cur_site_coverage)
                else:
                    programme_gfa = np.nan

                # Check whether resultant gfa is within allowed gpr.
                if cur_gpr is not None:
                    programme_gfa = float('inf') if pd.isnull(programme_gfa) else programme_gfa
                    gfa[programme] = min(programme_gfa, (plot_area * cur_gpr))
                else:
                    gfa[programme] = programme_gfa

        # Only default programme (NaN) and no available storeys or footprints.
        else:
            programme = np.nan
            gpr_list = list(cur_dcp['gpr'].dropna()) + list(cur_sbp['gpr'].dropna())

            if not pd.isnull(plot_mp_gpr):
                gpr_list.append(plot_mp_gpr)
            cur_gpr = min(gpr_list) if gpr_list else None

        gfa[programme] = (plot_area * cur_gpr) if cur_gpr is not None else np.nan

        all_gfas.append(gfa)
        sys.stdout.write("{:d}/{:d} plots processed\r".format(count + 1, gfa_plots.shape[0]))
    sys.stdout.write("{:d}/{:d} plots processed\n".format(count + 1, gfa_plots.shape[0]))

    gfa_plots['gfa'] = all_gfas

    return gfa_plots

def write_gfa_triples(gfa_plots, out_dir):
    """
    The function creates allowed gfa triples for every plot and its programme.
    :param gfa_plots: filtered plots GeoDataFrame for which gfa should be estimated.
    :param out_dir: directory to which generated triples is written as a .txt file.
    """

    gfa_dataset = TripleDataset()

    for plot in gfa_plots.index:
        plot_id = gfa_plots.loc[plot, 'plots']
        gfa_dict = gfa_plots.loc[plot, 'gfa']
        gfa_dataset.create_gfa_triples(plot_id, gfa_dict)

    gfa_dataset.write_triples('gfa', out_dir)


class TripleDataset:

    def __init__(self):
        self.dataset = Dataset()

    # writes necessary triples to represent allowed gfa.
    def create_gfa_triples(self, obj_uri, gfa_dict):

        obj_uri = URIRef(obj_uri)
        for programme, gfa_value in gfa_dict.items():
            print(type(programme))
            buildable_space_uri = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
            gfa_uri = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))
            measure = URIRef(GFAOntoManager.BUILDABLE_SPACE_GRAPH + str(uuid.uuid1()))

            gfa_value = Literal(str(gfa_value), datatype=XSD.decimal)

            self.dataset.add((obj_uri, GFAOntoManager.HAS_BUILDABLE_SPACE, buildable_space_uri, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            self.dataset.add((buildable_space_uri, RDF.type, GFAOntoManager.BUILDABLE_SPACE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            self.dataset.add((buildable_space_uri, GFAOntoManager.HAS_ALLOWED_GFA, gfa_uri, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            self.dataset.add((gfa_uri, RDF.type, GFAOntoManager.GROSS_FLOOR_AREA, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            self.dataset.add((gfa_uri, GFAOntoManager.HAS_VALUE, measure, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            self.dataset.add((measure, RDF.type, GFAOntoManager.MEASURE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            self.dataset.add((measure, GFAOntoManager.HAS_UNIT, GFAOntoManager.SQUARE_PREFIXED_METRE, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            self.dataset.add((GFAOntoManager.SQUARE_PREFIXED_METRE, RDF.type, GFAOntoManager.AREA_UNIT, GFAOntoManager.BUILDABLE_SPACE_GRAPH))
            self.dataset.add((measure, GFAOntoManager.HAS_NUMERIC_VALUE, gfa_value, GFAOntoManager.BUILDABLE_SPACE_GRAPH))

            # we add programme link only if it is not the general gfa for whole zoning type.
            if not pd.isnull(programme):
                programme_uri = URIRef(GFAOntoManager.ONTO_ZONING_URI_PREFIX + programme)
                self.dataset.add((buildable_space_uri, GFAOntoManager.FOR_ZONING_CASE, programme_uri, GFAOntoManager.BUILDABLE_SPACE_GRAPH))

    # writes the aggregated triples into a text file.
    def write_triples(self, triple_type, cur_dir):
        with open(cur_dir + "quads_" + triple_type + ".nq", mode="wb") as file:
            file.write(self.dataset.serialize(format='nquads'))


if __name__ == "__main__":

    out_dir = 'C://Users/AydaGrisiute/Desktop/'
    twa_endpoint = "http://theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql"
    # in case regulation data is stored locally.
    local_endpoint = "http://192.168.0.143:9999/blazegraph/namespace/regulationcontent/sparql"
    road_list = ['Expressway', 'Semi Expressway', 'Major Arterials/Minor Arterials']
    non_gfa_zones = ['Road', 'Waterbody', 'Utility', 'OpenSpace', 'ReserveSite', 'Park', 'Agriculture',
                     'RapidTransit', 'PortOrAirport', 'SpecialUseZone', 'Cemetery', 'BeachArea']

    dcp = get_development_control_plans(local_endpoint)
    lha = get_landed_housing_areas(local_endpoint)
    sbp = get_street_block_plans(local_endpoint)
    hcp = get_height_control_plans(local_endpoint)
    udg = get_urban_design_guidelines(local_endpoint)
    reg_links = get_regulation_links(local_endpoint)
    road_cats = get_road_categories(local_endpoint)

    print('Planning regulations retrieved.')

    plots = get_plots(twa_endpoint)
    plots = process_plots(plots)

    print('Plots retrieved and processed.')

    plots = get_plot_information(plots, local_endpoint, road_list)

    print("Plots information retrieved.")

    plots = assign_gpr(plots, 'PlaceOfWorship', lha, reg_links, float('nan'), float('nan'), float('nan'), 4, 5)
    plots = assign_gpr(plots, 'EducationalInstitution', lha, reg_links, 1, 1.4, 1.4, 3, 4)
    plots = assign_gpr(plots, 'CivicAndCommunityInstitutionZone', lha, reg_links, 1, 1.4, 1.4, 3, 4)
    plots = assign_sbp_gpr(plots, sbp, reg_links)

    print("Relevant plot GPRs set or updated.")

    plots = set_partywall_plots(plots, reg_links, sbp, udg)

    print("Set partywall plots.")

    # GFA should be calculated only for plots that has suitable zoning types and don't fall in conservation regulations.
    unclear_reg_plots = get_unclear_plots(reg_links, hcp, udg)
    non_gfa_plots = list(set(list(plots.loc[plots['zone'].isin(non_gfa_zones), 'plots']) + unclear_reg_plots))
    road_plots = plots.loc[plots['zone'] == 'Road', ['plots', 'geometry']]
    gfa_plots = plots[(~plots['plots'].isin(non_gfa_plots))].copy()
    gfa_plots, edges = get_plot_edges(gfa_plots)

    # TODO: instead of sbp_plots should be whole gfa_plots df. To be fixed.
    sbp_plots = gfa_plots[gfa_plots['plots'].isin(list(reg_links[reg_links['reg_type'] == 'StreetBlockPlan']['plots'].unique()))]
    sbp_plots, min_rect_edge_df = get_min_rect_edges(sbp_plots)

    sbp_plots = classify_min_rect_edges(min_rect_edge_df, sbp_plots.copy(), road_plots)
    sbp_plots = classify_neighbours(sbp_plots.copy(), plots, reg_links, min_rect_edge_df)
    gfa_plots = classify_plot_edges(gfa_plots, sbp_plots, plots, edges)

    print("Plot edges classified")

    gfa_plots = set_road_buffer_edges(edges, plots, gfa_plots.copy())
    gfa_plots = set_partywall_edges(edges, plots, gfa_plots.copy())
    udg_edges = get_udg_edge_setbacks(udg, reg_links, edges)

    print("Plot edge setbacks set.")

    '<----------------------------Test Start------------------------------>'

    # Here you can define the scope of gfa calculation: a copy of a whole gfa_plots df, single plot or a sample.
    gfa_plots_test = gfa_plots.copy()
    gfa_plots_test = set_plot_edge_setbacks(gfa_plots_test, reg_links, dcp, sbp, road_cats, udg_edges)

    print('Plot every edge setbacks retrieved.')

    gfa_plots_test = get_buildable_footprints(gfa_plots_test)
    gfa_plots_test = get_buildable_storeys(gfa_plots_test, udg, hcp, dcp, lha, sbp, reg_links)

    print('Plot allowed footprints and storeys retrieved.')

    gfa_plots_test = compute_plot_gfa(gfa_plots_test, reg_links, sbp, dcp)

    # for quick gfa values inspection one can check this file.
    gfas_json = {gfa_plots_test.loc[i, 'plots']: gfa_plots_test.loc[i, 'gfa'] for i in gfa_plots_test.index}
    with open(out_dir + 'gfa.json', 'w') as f:
        json.dump(gfas_json, f, indent=4)

    '<-----------------------------Test End------------------------------->'

    write_gfa_triples(gfa_plots_test, out_dir)

    print("Plot gfa values estimated, triples generated and written to " + out_dir)
