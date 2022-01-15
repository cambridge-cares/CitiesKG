package uk.ac.cam.cares.twa.cities;

import java.net.URI;
import java.util.Objects;

public class FieldKey implements Comparable<FieldKey> {
  // The predicate is expanded to a full IRI but the graph is kept as a fragment because the full graph iri is namespace
  // -dependent and will not be the same across different applications, while the full predicate iri does not change.
  public final String predicate;
  public final String graph;
  public final boolean backward;

  /**
   * Constructs a FieldKey from a graph name and a predicate IRI
   * @param graph either a graph IRI or the short name of a graph.
   * @param predicate full predicate IRI.
   * @param backward whether the declaring class is the object of the quad.
   */
  public FieldKey(String graph, String predicate, boolean backward) {
    this.predicate = predicate;
    String[] splitGraph = graph.split("/");
    this.graph = splitGraph[splitGraph.length-1];
    this.backward = backward;
  }

  public FieldKey(FieldAnnotation annotation, String defaultGraph) {
    predicate = PrefixUtils.expandQualifiedName(annotation.value());
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

  /**
   * The comparison order of keys here is deliberate. It optimises querying by first sorting by graph, enabling the
   * <code>GRAPH &lt;graph&gt; { ... }</code> pattern, and then by backward/forward, enabling the <code> &lt;subject&gt;
   * &lt;predicate&gt; &lt;object&gt;; &lt;predicate&gt; &lt;object&gt;; &lt;predicate&gt; &lt;object&gt;.</code> pattern.
   * @param otherKey the key being compared against.
   * @return the comparison outcome.
   */
  @Override
  public int compareTo(FieldKey otherKey) {
    return (graph + backward + predicate).compareTo(otherKey.graph + otherKey.backward + otherKey.predicate);
  }

}
