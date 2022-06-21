package uk.ac.cam.cares.twa.cities.utils.owlconverter;

import com.github.owlcs.ontapi.OntManagers;
import com.sun.javaws.exceptions.InvalidArgumentException;
import java.io.File;
import java.net.HttpURLConnection;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.formats.NQuadsDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Command line tool to convert ontologies to n-quads format and upload them to a SPARQL endpoint.
 * Accepts CLI arguments:
 * 1. OWL file
 * 2. Ontology IRI
 * 3. SPARQL endpoint
 * The tool converts ontology loaded from file (1) to n-triples first and performs string replacements
 * to convert blank nodes to IRIs, with a prefix relative to the ontology IRI (2). It adds a graph IRI
 * relative to the endpoint (3) to the end of each triple, turning it to n-quad.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
public class OwlConverter {

  private static final String CTYPE_NQ = "text/x-nquads";
  private static final String ERR_INVALID_INPUT = "Mandatory CLI arguments: "
      + "<OWL file path> <ontology IRI> <SPARQL endpoint IRI>";
  private static final String ERR_ONT_IO = "Loading ontology failed.";

  /**
   * Main entry point, calling methods to load ontology, convert it to n-quads and upload it to the
   * specified SPARQL endpoint. Validates CLI arguments and prints argument format information to
   * tge system output in case of invalid input.
   *
   * @param args CLI arguments
   * @throws Exception Thrown in case of invalid input, IO errors, HTTP errors
   */
  public static void main(String[] args) throws Exception {

    if (validateInput(args)) {
      String ontStr = loadOntologyToString(args[0]);
      ontStr = processOntologyToNquads(ontStr, args[1], args[2]);

      uploadToEndpoint(ontStr, args[2]);
    }

  }

  /**
   * Validates string array, checking if:
   * - first element is a path to a file
   * - second element is a valid IRI
   * - third element is a valid IRI
   *
   * @param args Array of CLI arguments.
   * @return Information if the input is valid.
   * @throws InvalidArgumentException In case of errors with instantiating a file or IRIs based on input.
   */
  private static boolean validateInput(String[] args) throws  InvalidArgumentException {
    boolean valid = false;

    try {
      File ontFile = new File(args[0]);
      IRI iriOnt = IRI.create(args[1]);
      IRI iriSparql = IRI.create(args[2]);

      if (IRI.create(ontFile.toURI()).isIRI() && iriOnt.isIRI() && iriSparql.isIRI()) {
        valid = true;
      }
    } catch (Exception e ) {
      throw new InvalidArgumentException(new String[]{ERR_INVALID_INPUT});
    }

    return valid;
  }

  /**
   * Loads ontology from specified file and converts it to n-triples format.
   *
   * @param fileName Path to the ontology file.
   * @return Ontology in n-triples.
   * @throws Exception In case of ontology conversion errors.
   */
  private static String loadOntologyToString(String fileName) throws Exception {

    OWLOntologyManager manager = OntManagers.createManager();
    StringDocumentTarget sdt = new StringDocumentTarget();
    NQuadsDocumentFormat format = new NQuadsDocumentFormat();

    try {
      File file = new File(fileName);
      OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
      ontology.saveOntology(format, sdt);
    } catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
      throw new Exception(ERR_ONT_IO);
    }

    return sdt.toString();
  }

  /**
   * Performs string replacements converting ontology from n-triples to n-quads with blank nodes
   * replaced by node IRIs.
   *
   * @apiNote So far this has been checked with OntoZoning. Most likely, the list of replacements
   * should be updated to work with other ontologies.
   *
   * @param ont Ontology in n-triples.
   * @param ontIRI Ontology IRI.
   * @param endpointIRI Target SPARQL endpoint IRI.
   * @return Ontology in n-quads.
   */
  private static String processOntologyToNquads(String ont, String ontIRI, String endpointIRI) {

    String[] ontTokens = ontIRI.split("/");
    String ontName = ontTokens[ontTokens.length - 1];
    String[] ontNameTokens = ontName.split("\\.");
    ontName = ontNameTokens[0];

    ont = ont.replace("_:", "<" + ontIRI + "#");
    ont = ont.replace(" <http", "> <http");
    ont = ont.replace(" .", "> .");
    ont = ont.replace(">>", ">");
    ont = ont.replace("\">", "\"");
    ont = ont.replace(" .", " <" + endpointIRI + ontName + "/> .");

    return ont;
  }

  /**
   * Uploads ontology in n-quads format to a specified target SPARQL endpoint IRI.
   *
   * @param ontStr Ontology in n-quads format.
   * @param endpointIRI Target SPARQL endpoint IRI.
   * @throws HttpException In case of uploading ontology fails.
   */
  private static void uploadToEndpoint(String ontStr, String endpointIRI) throws HttpException {
    if (!ontStr.isEmpty()) {
      HttpResponse<?> response = Unirest.post(endpointIRI)
          .header(HTTP.CONTENT_TYPE, CTYPE_NQ)
          .body(ontStr)
          .socketTimeout(300000)
          .asEmpty();
      int respStatus = response.getStatus();
      if (respStatus != HttpURLConnection.HTTP_OK) {
        throw new HttpException(endpointIRI + " responded with code: " + respStatus);
      }
    }
  }

}
