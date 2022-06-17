package uk.ac.cam.cares.twa.cities.utils.owlconverter;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.jena.model.OntModel;
import java.io.File;
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
//import org.semanticweb.owlapi.model.IRI;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.semanticweb.owlapi.formats.NTriplesDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;


public class OwlConverter {

  public static final String CTYPE_NQ = "text/x-nquads";

  public static void main(String[] args) throws Exception {

    String ontStr = loadOntologyToString(args[0]);
    ontStr = processOntologyToNquads(ontStr, args[1], args[2]);

    uploadToEndpoint(ontStr, args[2]);

  }

  private static String loadOntologyToString(String fileName) throws Exception {

    OWLOntologyManager manager = OntManagers.createManager();
    StringDocumentTarget sdt = new StringDocumentTarget();
    NTriplesDocumentFormat nTriplesFormat = new NTriplesDocumentFormat();

    try {
      File file = new File(fileName);
      OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
      ontology.saveOntology(nTriplesFormat, sdt);
    } catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
      throw new Exception("Loading ontology failed.");
    }

    return sdt.toString();
  }

  private static String processOntologyToNquads(String ont, String ontIRI, String endpointIRI) {

    ont = ont.replace("_:", "<" + ontIRI + "#");
    ont = ont.replace(" <http", "> <http:");
    ont = ont.replace(" .", "> .");
    ont = ont.replace(">>", ">");
    ont = ont.replace("\">", "\"");
    ont = ont.replace(" .", " <" + endpointIRI + "ontology/> .");

    return ont;
  }

  private static void uploadToEndpoint(String ontStr, String endpointIRI) throws HttpException {
    if (!ontStr.isEmpty()) {
      HttpResponse<?> response = Unirest.post(endpointIRI)
          .header(HTTP.CONTENT_TYPE, CTYPE_NQ)
          .body(ontStr)
          .socketTimeout(300000)
          .asEmpty();
      int respStatus = response.getStatus();
      if (respStatus != HttpURLConnection.HTTP_OK) {
        throw new HttpException(endpointIRI + " " + respStatus);
      }
    }
  }

}
