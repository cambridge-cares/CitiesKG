package uk.ac.cam.cares.twa.cities;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.impl.LiteralLabel;

import java.util.HashMap;
import java.util.Map;

public class ArbitraryJenaDatatype implements RDFDatatype {

  private final String datatypeIri;
  private static Map<String, ArbitraryJenaDatatype> registeredTypes = new HashMap<String, ArbitraryJenaDatatype>();

  private ArbitraryJenaDatatype(String datatypeIri) {
    this.datatypeIri = datatypeIri;
  }

  /**
   * Checks if an existing ArbitraryJenaDatatype of this IRI already exists. If it does, return the existing instance;
   * else, create one and register it with the Jena TypeMapper.
   * @param datatypeIri the iri of the datatype to be retrieved or created.
   * @return an ArbitraryJenaDatatype object which returns datatypeIri on getURI().
   */
  public static ArbitraryJenaDatatype get(String datatypeIri) {
    if(registeredTypes.containsKey(datatypeIri)) {
      return registeredTypes.get(datatypeIri);
    } else {
      ArbitraryJenaDatatype datatype = new ArbitraryJenaDatatype(datatypeIri);
      registeredTypes.put(datatypeIri, datatype);
      TypeMapper.getInstance().registerDatatype(datatype);
      return datatype;
    }
  }

  @Override
  public String getURI() {
    return datatypeIri;
  }

  @Override
  public String unparse(Object o) {
    return (String) o;
  }

  @Override
  public Object parse(String s) throws DatatypeFormatException {
    return s;
  }

  @Override
  public boolean isValid(String s) {
    return true;
  }

  @Override
  public boolean isValidValue(Object o) {
    return o instanceof String;
  }

  @Override
  public boolean isValidLiteral(LiteralLabel literalLabel) {
    return true;
  }

  @Override
  public boolean isEqual(LiteralLabel literalLabel, LiteralLabel literalLabel1) {
    return literalLabel.getDatatype().equals(literalLabel1.getDatatype()) && literalLabel.getValue().equals(literalLabel1.getValue());
  }

  @Override
  public int getHashCode(LiteralLabel literalLabel) {
    return literalLabel.getDefaultHashcode();
  }

  @Override
  public Class<?> getJavaClass() {
    return String.class;
  }

  @Override
  public Object cannonicalise(Object o) {
    return o;
  }

  @Override
  public Object extendedTypeDefinition() {
    return null;
  }

  @Override
  public RDFDatatype normalizeSubType(Object o, RDFDatatype rdfDatatype) {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ArbitraryJenaDatatype && ((ArbitraryJenaDatatype)o).getURI() == getURI();
  }

}
