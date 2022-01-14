package uk.ac.cam.cares.twa.cities.models.geo.test;

import org.apache.jena.sparql.lang.sparql_11.ParseException;
import uk.ac.cam.cares.twa.cities.Model;

public class test {
  public static void main(String[] args) throws ParseException {
//    Node me = NodeFactory.createURI("http://example.org/test");
//    Node node = NodeFactory.createLiteral("test", ArbitraryJenaDatatype.get("http://localhost:9999/bleh"));
//    UpdateBuilder builder = new UpdateBuilder();
//    builder.addInsert(me, new WhereBuilder().addWhere(me, NodeFactory.createURI("http://example.org/cityObjectId"), node));
//    PrefixUtils.autoPrefix(builder);
//    System.out.println(builder.buildRequest().toString());
//    builder = new UpdateBuilder();
//    builder.addPrefix("ocgml", "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#").addPrefix("ocgml", "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#")
//        .addWhere(me, "ocgml:id", "?anything");
//    System.out.println(builder.buildDeleteWhere().toString());
//    SelectBuilder select = new SelectBuilder();
//    select.addPrefix("ocgml", "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#").addVar("DATATYPE(?val)", "?dtype").addWhere("?val", "ocgml:bleh", node);
//    System.out.println(select.buildString());
//    Building building = new Building();
//    building.setIri(URI.create("http://example.org/test/building"));
//    ThematicSurface ts1 = new ThematicSurface();
//    ThematicSurface ts2 = new ThematicSurface();
//    ts1.setIri(URI.create("http://example.org/test/ts1"));
//    ts2.setIri(URI.create("http://example.org/test/ts2"));
//    building.setThematicSurfaces(new ArrayList<>());
//    building.getThematicSurfaces().add(ts1);
//    building.getThematicSurfaces().add(ts2);
//    System.out.println(building.buildPushForwardQuery());
    TestModel model = new TestModel(12345);
    model.queuePushForwardUpdate();
    model = new TestModel(2345);
    model.queuePushForwardUpdate();
    System.out.println(Model.updateQueue.toString());
    System.out.println(TestModel.buildPopulateQuery(model.getIri().toString(), false));
  }
}
