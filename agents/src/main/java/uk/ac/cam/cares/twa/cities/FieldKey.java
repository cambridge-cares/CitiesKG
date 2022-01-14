package uk.ac.cam.cares.twa.cities;

import java.net.URI;
import java.util.Objects;

public class FieldKey implements Comparable {
  // The predicate is expanded to a full IRI but the graph is kept as a fragment because the full graph iri is namespace
  // -dependent and will not be the same across different applications, while the full predicate iri does not change.
  public final URI predicate;
  public final String graph;
  public final boolean backward;

  /**
   * Constructs a FieldKey from a short graph name and a full predicate URI
   * @param graph short graph name, e.g. "surfacegeometry".
   * @param predicate full predicate IRI.
   * @param backward whether the declaring class is the object of the quad.
   */
  public FieldKey(String graph, URI predicate, boolean backward) {
    this.predicate = predicate;
    this.graph = graph;
    this.backward = backward;
  }

  /**
   * Constructs a FieldKey from a full graph IRI and a full predicate URI
   * @param graph full graph IRI.
   * @param predicate full predicate IRI.
   * @param backward whether the declaring class is the object of the quad.
   */
  public FieldKey(URI graph, URI predicate, boolean backward) {
    this.predicate = predicate;
    String[] splitGraph = graph.toString().split("/");
    this.graph = splitGraph[splitGraph.length-1];
    this.backward = backward;
  }

  public FieldKey(FieldAnnotation annotation, String defaultGraph) {
    predicate = URI.create(PrefixUtils.expandQualifiedName(annotation.value()));
    graph = annotation.graphName().equals("") ? defaultGraph : annotation.graphName();
    backward = annotation.backward();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FieldKey fieldKey = (FieldKey) o;
    return backward == fieldKey.backward && predicate.equals(fieldKey.predicate) && graph.equals(fieldKey.graph);
  }



  @Override
  public int hashCode() {
    return Objects.hash(predicate, graph, backward);
  }

  @Override
  public int compareTo(Object o) {
    if (!(o instanceof FieldKey)) return 0;
    FieldKey other = (FieldKey)o;
    return (graph + predicate + backward).compareTo(other.graph + other.predicate + other.backward);
  }
}
