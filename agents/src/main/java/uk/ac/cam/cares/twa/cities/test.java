package uk.ac.cam.cares.twa.cities;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.lucene.util.QueryBuilder;

import java.net.URI;

public class test {
  public static void main(String[] args) throws ParseException {
    Node me = NodeFactory.createURI("http://example.org/test");
    Node node = NodeFactory.createLiteral("test", ArbitraryJenaDatatype.get("http://localhost:9999/bleh"));
    UpdateBuilder builder = new UpdateBuilder();
    builder.addPrefix("ocgml", "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#")
        .addInsert(me, "ocgml:cityObjectId", node);
    System.out.println(builder.buildRequest().toString());
    builder = new UpdateBuilder();
    builder.addPrefix("ocgml", "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#")
        .addWhere(me, "ocgml:id", "?anything");
    System.out.println(builder.buildDeleteWhere().toString());
    SelectBuilder select = new SelectBuilder();
    select.addPrefix("ocgml", "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#").addVar("DATATYPE(?val)", "?dtype").addWhere("?val", "ocgml:bleh", node);
    System.out.println(select.buildString());
  }
}
