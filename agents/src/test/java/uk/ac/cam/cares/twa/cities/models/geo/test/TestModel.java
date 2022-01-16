package uk.ac.cam.cares.twa.cities.models.geo.test;

import lombok.Getter;
import lombok.Setter;
import org.apache.jena.update.UpdateRequest;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.MetaModel;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.ModelAnnotation;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;

import java.net.URI;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

@ModelAnnotation(nativeGraph = "testmodels")
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

  private Random random;

  public TestModel() {
    super();
  }

  public TestModel(long seed, int vectorSize, int recursiveDepth) {
    random = new Random(seed);
    // Create random UUID deterministically
    byte[] randomBytes = new byte[5];
    random.nextBytes(randomBytes);
    setIri(UUID.nameUUIDFromBytes(randomBytes).toString(), "https://eg/examplenamespace");
    // Randomise in scalar properties
    stringProp = "randomString" + random.nextInt();
    intProp = random.nextInt();
    doubleProp = random.nextDouble();
    uriProp = randomUri();
    modelProp = recursiveDepth > 0 ? new TestModel(random.nextLong(), vectorSize, recursiveDepth - 1) : null;
    backUriProp = randomUri();
    // Randomise vector properties
    for (int i = 0; i < vectorSize; i++) {
      forwardVector.add(random.nextDouble());
      backwardVector.add(randomUri());
    }
  }

  private URI randomUri() {
    return URI.create(getNamespace() + "testmodels/model" + random.nextInt());
  }

}
