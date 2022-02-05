package uk.ac.cam.cares.twa.cities.models.test;

import lombok.Getter;
import lombok.Setter;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

@ModelAnnotation(nativeGraphName = "testmodels")
public class TestModel extends Model {

  @Getter @Setter @FieldAnnotation("JPSLAND:stringprop") private String stringProp;
  @Getter @Setter @FieldAnnotation("dbpediao:intprop") private Integer intProp;
  @Getter @Setter @FieldAnnotation("dbpediao:doubleprop") private Double doubleProp;
  @Getter @Setter @FieldAnnotation("dbpediao:uriprop") private URI uriProp;
  @Getter @Setter @FieldAnnotation("dbpediao:modelprop") private TestModel modelProp;
  @Getter @Setter @FieldAnnotation("JPSLAND:geometryprop") private GeometryType geometryProp;

  @Getter @Setter @FieldAnnotation("JPSLAND:stringpropnull") private String stringNullProp;
  @Getter @Setter @FieldAnnotation("dbpediao:intpropnull") private Integer intNullProp;
  @Getter @Setter @FieldAnnotation("dbpediao:doublepropnull") private Double doubleNullProp;
  @Getter @Setter @FieldAnnotation("dbpediao:uripropnull") private URI uriNullProp;
  @Getter @Setter @FieldAnnotation("dbpediao:modelpropnull") private TestModel modelNullProp;

  @Getter @Setter @FieldAnnotation(value = "JPSLAND:backuriprop", backward = true) private URI backUriProp;
  @Getter @Setter @FieldAnnotation(value = "JPSLAND:backuripropnull", backward = true) private URI backUriPropNull;

  @Getter @Setter @FieldAnnotation(value = "dbpediao:graphtest1a", graphName = "graph1") private Double graphTest1a;
  @Getter @Setter @FieldAnnotation(value = "dbpediao:graphtest2a", graphName = "graph2") private Double graphTest2a;
  @Getter @Setter @FieldAnnotation(value = "dbpediao:graphtest2b", graphName = "graph2") private Double graphTest2b;
  @Getter @Setter @FieldAnnotation(value = "dbpediao:graphtest3a", graphName = "graph3") private Double graphTest3a;
  @Getter @Setter @FieldAnnotation(value = "dbpediao:graphtest3b", graphName = "graph3") private Double graphTest3b;
  @Getter @Setter @FieldAnnotation(value = "dbpediao:graphtest3c", graphName = "graph3") private Double graphTest3c;

  @Getter @Setter @FieldAnnotation(value = "dbpediao:forwardvector", innerType = Double.class) private ArrayList<Double> forwardVector;
  @Getter @Setter @FieldAnnotation(value = "dbpediao:emptyforwardvector", innerType = Double.class) private ArrayList<Double> emptyForwardVector;
  @Getter @Setter @FieldAnnotation(value = "dbpediao:backwardVector", backward = true, innerType = URI.class) private ArrayList<URI> backwardVector;

  public static TestModel createRandom(ModelContext context, long seed, int vectorSize, int recursiveDepth) {
    Random random = new Random(seed);
    // Create random UUID deterministically
    byte[] randomBytes = new byte[5];
    random.nextBytes(randomBytes);
    TestModel model = context.createNewModel(TestModel.class, "https://eg/examplenamespace/" + UUID.nameUUIDFromBytes(randomBytes));
    // Randomise in scalar properties
    model.stringProp = "randomString" + random.nextInt();
    model.intProp = random.nextInt();
    model.doubleProp = random.nextDouble();
    model.uriProp = randomUri(random);
    model.modelProp = recursiveDepth > 0 ? TestModel.createRandom(context, random.nextLong(), vectorSize, recursiveDepth - 1) : null;
    model.backUriProp = randomUri(random);
    // Randomise vector properties
    for (int i = 0; i < vectorSize; i++) {
      model.forwardVector.add(random.nextDouble());
      model.backwardVector.add(randomUri(random));
    }
    return model;
  }

  private static URI randomUri(Random random) {
    return URI.create("https://eg/examplenamespace/randomuris/" + random.nextInt());
  }

}
