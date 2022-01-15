package uk.ac.cam.cares.twa.cities;

import org.apache.jena.graph.Node;

public abstract class DatatypeModel {
  // There should be a constructor accepting (String value, String datatype),
  // but we can't specify that in an interface (it is acquired via reflection).
  public abstract Node getNode();

  @Override
  public boolean equals(Object obj) {
    return obj instanceof DatatypeModel &&
        ((DatatypeModel)obj).getNode().equals(getNode());
  }
}
