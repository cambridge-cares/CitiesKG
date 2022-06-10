package uk.ac.cam.cares.twa.cities.utils.owlconverter;

import java.io.File;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class OwlConverter {

  public static void main(String[] args) throws Exception {

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    File file = new File(args[0]);

    OWLOntology ontology = processOntology(manager.loadOntologyFromOntologyDocument(file));

    RDFXMLOntologyFormat rdfxmlFormat = new RDFXMLOntologyFormat();

    File tfile = new File(file.getParentFile().getPath() + System.getProperty("file.separator")
        + "Converted-" + file.getName());

    manager.saveOntology(ontology, rdfxmlFormat, IRI.create(tfile));
  }

  private static OWLOntology processOntology(OWLOntology ont) {
    //ontology rearrangement logic goes here

    return ont;
  }

}
