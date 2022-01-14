package uk.ac.cam.cares.twa.cities;

import org.apache.jena.graph.Node;

public interface DatatypeModel {
  // There should be a constructor accepting (String value, String structure),
  // but we can't specify that in an interface (it is acquired via reflection).
  public Node getNode();
}
