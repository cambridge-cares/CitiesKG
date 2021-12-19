package uk.ac.cam.cares.twa.cities;

public interface DatatypeModel {
  // There should be a constructor accepting (String value, String structure),
  // but we can't specify that in an interface (it is acquired via reflection).
  public String getLiteralString();
}
