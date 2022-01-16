package uk.ac.cam.cares.twa.cities;

import org.apache.jena.sparql.lang.sparql_11.ParseException;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import uk.ac.cam.cares.twa.cities.models.geo.test.TestModel;

import java.time.Duration;
import java.time.Instant;

public class SpeedTest {
  public static void main(String[] args) throws ParseException {

    StoreClientInterface kgClient = new RemoteStoreClient("http://localhost:9999/blazegraph/namespace/churchill/sparql", "http://localhost:9999/blazegraph/namespace/churchill/sparql");

    int rounds = 1;
    long[] news = new long[rounds];
    long[] olds = new long[rounds];
    long newSum = 0;
    long oldSum = 0;
    for(int i = 0; i < rounds; i++) {
      newSum += news[i] = newTest(kgClient);
      oldSum += olds[i] = oldTest(kgClient);
    }
    double newAverage = 1.0 * newSum / rounds;
    double oldAverage = 1.0 * oldSum / rounds;
    System.out.println("NEW    OLD");
    for(int i = 0; i < rounds; i++) {
      System.out.println(news[i] + "  " + olds[i]);
    }
    System.out.println(String.format("AVERAGE: %32.1f  %32.1f", newAverage, oldAverage));

  }

  private static long newTest(StoreClientInterface kgClient) {
    TestModel model = new TestModel(Instant.now().getNano(), 25, 0);
    model.queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    Instant start = Instant.now();
    pulledModel.pullScalars(kgClient);
    long millis = Duration.between(start, Instant.now()).toMillis();
    model.queueDeletionUpdate();
    Model.executeUpdates(kgClient, true);
    assert(model.equals(pulledModel));
    return millis;
  }

  private static long oldTest(StoreClientInterface kgClient) {
    TestModel model = new TestModel(Instant.now().getNano(), 25, 0);
    model.queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    Instant start = Instant.now();
    pulledModel.pullAll(kgClient, 0);
    long millis = Duration.between(start, Instant.now()).toMillis();
    model.queueDeletionUpdate();
    Model.executeUpdates(kgClient, true);
    assert(model.equals(pulledModel));
    return millis;
  }
}
