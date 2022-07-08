package org.citydb.database.adapter.blazegraph;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.xml.transform.Result;
import oracle.jdbc.proxy.annotation.Pre;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.postgresql.jdbc2.ArrayAssistantRegistry;

public class OptimizedSparqlQuery {
  private static final String QST_MARK = "?";
  private static String IRI_GRAPH_BASE;
  private static String PREFIX_ONTOCITYGML;
  private static String IRI_GRAPH_OBJECT_REL = "cityobject/";
  private static String IRI_GRAPH_OBJECT;
  private static String FIXED_LOD2MSID = "?fixedlod2MSid";
  private static String LODXMSID = "?lodXMSid";

  public String sqlStatement;
  public String sparqlStatement;
  private final AbstractDatabaseAdapter databaseAdapter;

  public OptimizedSparqlQuery(AbstractDatabaseAdapter databaseAdapter) {
    this.databaseAdapter = databaseAdapter;

    // Note: Read the database connection information from the database GUI setting
    PREFIX_ONTOCITYGML  = databaseAdapter.getConnectionDetails().getSchema();
    IRI_GRAPH_BASE = "http://" + databaseAdapter.getConnectionDetails().getServer() +
        ":" + databaseAdapter.getConnectionDetails().getPort() +
        databaseAdapter.getConnectionDetails().getSid();
    IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;
  }


  public static String getBuildingPartQuery_part1(int lodExport){
    StringBuilder sparqlbuilder = new StringBuilder();
    String lodXMultiSurfaceId = "lod" + Integer.valueOf(lodExport) + "MultiSurfaceId";

    sparqlbuilder.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + ">\n" +
            "SELECT " + "?lodXMSid\n" +
            " WHERE {\n" +
            "GRAPH <" + IRI_GRAPH_BASE + "thematicsurface/> {\n" +
            "?ts_id ocgml:objectClassId 35 ; ocgml:buildingId " + QST_MARK +" ;\n" +
            "ocgml:" +  lodXMultiSurfaceId + " " + "?lodXMSid" + " . ");
    sparqlbuilder.append("} }");

    return sparqlbuilder.toString();
  }

  /*
   *
  */
  public static String getBuildingPartQuery_part2() {
    StringBuilder sparqlbuilder = new StringBuilder();
    sparqlbuilder.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + ">\n" +
        "SELECT ?geomtype (datatype(?geomtype) AS ?datatype)\n" +
        "WHERE {\n" +
        "GRAPH <" + IRI_GRAPH_BASE + "surfacegeometry/> {\n" +
        "?sg_id ocgml:rootId " + QST_MARK + " ; \nocgml:GeometryType ?geomtype . \nFILTER(!isBlank(?geomtype))} }");
    return sparqlbuilder.toString();
  }

  /*
   * Retrieve the existing GroundSurface from the database
   * Note: The data in the TWA contains some missing "/" in the graph, it requires a temporary solution before the fix in the TWA
   * For the TWA, query across different graphs need to be divided.
   *
   * "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
          "SELECT ?geomtype (datatype(?geomtype) AS ?type)" +
          "WHERE {" +
          "GRAPH <" + IRI_GRAPH_BASE + "thematicsurface/> {" +
          "?ts_id ocgml:objectClassId 35 ; ocgml:buildingId ? ;ocgml:lod2MultiSurfaceId ?lod2MSid .}" +
          "GRAPH <" + IRI_GRAPH_BASE + "surfacegeometry/> {" +
          "?sg_id ocgml:rootId ?lod2MSid; ocgml:GeometryType ?geomtype . FILTER(!isBlank(?geomtype))} }"
   * */
  public static ArrayList<ResultSet> getSPARQLBuildingPart(Connection connection, String sqlQuery, int lod2Export, String buildingPartId)
      throws SQLException {
    ResultSet rs1 = null;
    ResultSet rs2 = null;
    String sparqlStr1 = getBuildingPartQuery_part1(lod2Export);

    // Value Assignment and Query Execution. Assume the intermediate results has more than one
    PreparedStatement psQuery = connection.prepareStatement(sparqlStr1, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

    URL url = null;
    try {
      url = new URL(buildingPartId);
      psQuery.setURL(1, url);
      rs1 = psQuery.executeQuery();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    ArrayList<ResultSet> results = new ArrayList<>();  // GroundSurface
    int num = 0;
    while (rs1.next()){
      String lodXMSid = rs1.getString(1); // lodXMSid

      // @TODO: check if the link has missing slash at the end, if yes, fix it
      if (!lodXMSid.endsWith("/")) {
        // append a backslash to it
        lodXMSid = lodXMSid + "/";
      }
      String sparqlStr2 = getBuildingPartQuery_part2();
      psQuery = connection.prepareStatement(sparqlStr2, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

      if (lodXMSid != null){
        try {
          url = new URL(lodXMSid);
          psQuery.setURL(1, url);
          rs2 = psQuery.executeQuery();

          if (rs2.next()){ // true for not-empty ResultSet
            rs2.beforeFirst();  // reset the cursor in order not to lose the first row
            results.add(rs2);
          }
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
      }
      //System.out.println(++num);
    }
    return results;
  }

  /* Execution of the complex query and return a list of ResultSet
  * */
  public static ArrayList<ResultSet> getSPARQLAggregateGeometriesForLOD2OrHigher(Connection connection, int lodToExportFrom, String buildingPartId)
      throws SQLException {

    StringBuilder sparqlStr = new StringBuilder();
    ArrayList<ResultSet> rootIds = new ArrayList<>();
    String lodLevel = String.valueOf(lodToExportFrom);
    ResultSet intermRs = null;
    String lodXMultiSurfaceId = "lod" + lodLevel + "MultiSurfaceId";
    String lodId = "?lodId";

    // subquery 1.1
    sparqlStr.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> \n" +
        "SELECT (" + lodId + " AS ?rootId) " +
        "WHERE { " +
        "GRAPH <" + IRI_GRAPH_BASE + "building/> {" +
        " ?id ocgml:id " +  QST_MARK + " ;  ocgml:" + lodXMultiSurfaceId + " " + lodId + ".\n" +
        "FILTER (!isBlank(" + lodId + ")) }}");
    intermRs = executeQuery(connection, sparqlStr.toString(), buildingPartId, "rootId");
    if (intermRs.next()){
      intermRs.beforeFirst();
      rootIds.add(intermRs);
    }

    String lodXSolidId = "lod" + lodLevel + "SolidId";
    // subquery 1.2
    sparqlStr.setLength(0);
    sparqlStr.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> \n" +
        "SELECT (" + lodId + " AS ?rootId) " +
        "\nWHERE\n { " +
        "GRAPH <" + IRI_GRAPH_BASE + "building/> { \n" +
        " ?id ocgml:buildingId " +  QST_MARK + " ;  \n ocgml:" + lodXSolidId + " " + lodId + "\n" +
        "FILTER (!isBlank(" + lodId + ")) }}");
    intermRs = executeQuery(connection, sparqlStr.toString(), buildingPartId, "rootId");
    if (intermRs.next()){
      intermRs.beforeFirst();
      rootIds.add(intermRs);
    }

    // subquery 1.3
    sparqlStr.setLength(0);
    sparqlStr.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> \n" +
        "SELECT (" + lodId + " AS ?rootId) " +
        "\nWHERE\n { " +
        "GRAPH <" + IRI_GRAPH_BASE + "thematicsurface/> { \n" +
        " ?id ocgml:buildingId " +  QST_MARK + " ;  \n ocgml:" + lodXMultiSurfaceId + " " + lodId + "; \n" +
        "FILTER (!isBlank(" + lodId + ")) }}");
    intermRs = executeQuery(connection, sparqlStr.toString(), buildingPartId, "rootId");
    if (intermRs.next()){
      intermRs.beforeFirst();
      rootIds.add(intermRs);
    }

    // query stage 2 for extractig the aggregated geometries
    //System.out.println("OptimizedSparqlQuery, size of the rootId: " + rootIds.size());
    // return a list of geometry with # separator
    ArrayList<ResultSet> geometries = new ArrayList<>();
    if (!rootIds.isEmpty()) {
      sparqlStr.setLength(0);

      sparqlStr.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
          "SELECT ?geometry (datatype(?geometry) AS ?datatype)" +
          "\nWHERE\n { " +
          "GRAPH <" + IRI_GRAPH_BASE + "surfacegeometry/> { \n" +
          " ?id ocgml:rootId " + QST_MARK + " ;  \n ocgml:GeometryType    ?geometry\n" +
          "FILTER (!isBlank(?geometry)) }}");

      for (ResultSet rs : rootIds) {
        try {
          while (rs.next()) {
            String rootid = rs.getString(1);
            ResultSet finalRs = executeQuery(connection, sparqlStr.toString(), rootid, "geometry");

            if (finalRs.next()) {
              finalRs.beforeFirst();
              geometries.add(finalRs);
            }
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
    return geometries;
  }

  public static ResultSet executeQuery(Connection connection, String querystr, String buildingPartId, String selectToken){

    URL url = null;
    ResultSet rs = null;

    try {
      url = new URL(buildingPartId);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    try {
      PreparedStatement psQuery = connection.prepareStatement(querystr, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
      psQuery.setURL(1, url);
      rs = psQuery.executeQuery();
    } catch (SQLException e) {
      e.printStackTrace();  //@TODO: to define how to handle
    }
    return rs;
  }
}
