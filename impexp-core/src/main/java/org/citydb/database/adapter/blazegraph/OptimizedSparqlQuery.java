package org.citydb.database.adapter.blazegraph;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import oracle.jdbc.proxy.annotation.Pre;
import org.citydb.database.adapter.AbstractDatabaseAdapter;

public class OptimizedSparqlQuery {
  private static final String QST_MARK = "?";
  private static String IRI_GRAPH_BASE;
  private static String PREFIX_ONTOCITYGML;
  private static String IRI_GRAPH_OBJECT_REL = "cityobject/";
  private static String IRI_GRAPH_OBJECT;

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

  /* Execution of the complex query and return a list of ResultSet
  * */
  public static ArrayList<ResultSet> getSPARQLAggregateGeometriesForLOD2OrHigher(
      PreparedStatement psQuery, Connection connection, int lodToExportFrom, String buildingPartId) {

    StringBuilder sparqlStr = new StringBuilder();
    ResultSet rs = null;
    ArrayList<String> rootIds = new ArrayList<String>();
    String lodLevel = String.valueOf(lodToExportFrom);

    // subquery 1.1
    sparqlStr.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
        "SELECT (?lod2MultiSurfaceId AS ?rootId) " +
        "WHERE { " +
        "GRAPH <" + IRI_GRAPH_BASE + "building/> {" +
        " ?id ocgml:buildingId " +  QST_MARK + " ;  ocgml:lod2MultiSurfaceId ?lod2MultiSurfaceId " +
        "FILTER (!isBlank(?lod2MultiSurfaceId)) }}");
    rootIds.addAll(executeQuery(connection, sparqlStr.toString(), buildingPartId));

    // subquery 1.2
    sparqlStr.setLength(0);
    sparqlStr.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
        "SELECT (?lod2SolidId AS ?rootId) " +
        "\nWHERE\n { " +
        "GRAPH <" + IRI_GRAPH_BASE + "building/> { \n" +
        " ?id ocgml:buildingId " +  QST_MARK + " ;  \n ocgml:lod2SolidId  ?lod2SolidId\n" +
        "FILTER (!isBlank(?lod2SolidId)) }}");
    rootIds.addAll(executeQuery(connection, sparqlStr.toString(), buildingPartId));

    // subquery 1.3
    sparqlStr.setLength(0);
    sparqlStr.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
        "SELECT (?lod2MultiSurfaceId AS ?rootId) " +
        "\nWHERE\n { " +
        "GRAPH <" + IRI_GRAPH_BASE + "thematicsurface/> { \n" +
        " ?id ocgml:buildingId " +  QST_MARK + " ;  \n ocgml:lod2MultiSurfaceId  ?lod2MultiSurfaceId\n" +
        "FILTER (!isBlank(?lod2MultiSurfaceId)) }}");
    rootIds.addAll(executeQuery(connection, sparqlStr.toString(), buildingPartId));

    // query stage 2 for extractig the aggregated geometries
    System.out.println("OptimizedSparqlQuery, size of the rootId: " + rootIds.size());
    // @TODO


    return null;
  }

  public static ArrayList<String> executeQuery(Connection connection, String querystr, String buildingPartId){

    URL url = null;
    ResultSet rs = null;
    ArrayList<String> results = new ArrayList<String>();
    try {
      url = new URL(buildingPartId);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    try {
      PreparedStatement psQuery = connection.prepareStatement(querystr, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
      psQuery.setURL(1, url);
      rs = psQuery.executeQuery();

      while (rs.next()) {
        results.add(rs.getString("rootId"));
      }
    } catch (SQLException e) {
      e.printStackTrace();  //@TODO: to define how to handle
    }
    return results;
  }
}
