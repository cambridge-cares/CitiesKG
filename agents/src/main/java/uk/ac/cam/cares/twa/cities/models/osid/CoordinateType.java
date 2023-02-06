package uk.ac.cam.cares.twa.cities.models.osid;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import uk.ac.cam.cares.ogm.models.DatatypeModel;
import org.locationtech.jts.geom.Coordinate;

import java.util.HashMap;
import java.util.Map;

public class CoordinateType implements DatatypeModel {

    private static final String VALUE_DELIMITER = "#";
    public static final String LATLON_DATATYPE = "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon";
    private static final Map<String, BaseDatatype> datatypes = new HashMap<>();

    public Coordinate coordinate;
    public String datatype;

    public CoordinateType(double x, double y, String datatype) {
        this.coordinate = new Coordinate(x, y);
        this.datatype = datatype;
    }

    public CoordinateType(String data, String datatype) {
        String[] valueStrings = data.split(VALUE_DELIMITER);
        coordinate = new Coordinate(Double.parseDouble(valueStrings[0]), Double.parseDouble(valueStrings[1]));
        this.datatype = datatype;
    }

    @Override
    public Node getNode() {
        return NodeFactory.createLiteral(coordinate.x + VALUE_DELIMITER + coordinate.y, getDatatype(datatype));
    }
    /**
     * Fetches a datatype with the specified IRI. If one such datatype has been requested in the past, the same is
     * returned; otherwise, a new one is created. This enables correct <code>equals</code> behaviour, since
     * <code>new BaseDatatype(iri) != new BaseDatatype(iri)</code>.
     * @param datatypeIri the iri for which to get a datatype.
     * @return an RDFDatatype which has <code>getURI()==datatypeIri</code>.
     */
    private static RDFDatatype getDatatype(String datatypeIri) {
        if (!datatypes.containsKey(datatypeIri))
            datatypes.put(datatypeIri, new BaseDatatype(datatypeIri));
        return datatypes.get(datatypeIri);
    }

}