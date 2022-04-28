package uk.ac.cam.cares.twa.cities.models.geo.test;

import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.OntoCityGMLModel;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class OntoCityGMLModelTest {

    @Test
    public void testNewOntoCityGMLModelAnnotation() throws NoSuchFieldException {

        OntoCityGMLModel ontoCityGMLModel = new OntoCityGMLModel();

        Field id = ontoCityGMLModel.getClass().getDeclaredField("id");
        assertEquals(SchemaManagerAdapter.ONTO_ID, id.getAnnotation(FieldAnnotation.class).value());
        assertFalse(id.getAnnotation(FieldAnnotation.class).backward());
        assertEquals("", id.getAnnotation(FieldAnnotation.class).graphName());
        assertEquals(Model.class, id.getAnnotation(FieldAnnotation.class).innerType());

    }

    @Test
    public void testgetId(){

        String prefix = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";

        ModelContext context = new ModelContext("", "");
        OntoCityGMLModel ontoCityGMLModel = context.createNewModel(OntoCityGMLModel.class, prefix + "temp_uuid");

        assertEquals(URI.create(prefix+"temp_uuid"), ontoCityGMLModel.getId());

    }

    @Test
    public void testsetId() throws NoSuchFieldException, IllegalAccessException {

        String prefix = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";

        ModelContext context = new ModelContext("", "");
        OntoCityGMLModel ontoCityGMLModel = context.createNewModel(OntoCityGMLModel.class, prefix + "temp1_uuid");
        Field ID_MISMATCH_ERROR_TEXT = ontoCityGMLModel.getClass().getDeclaredField("ID_MISMATCH_ERROR_TEXT");
        ID_MISMATCH_ERROR_TEXT.setAccessible(true);

        URI value = URI.create(prefix+"temp2_uuid");

        JPSRuntimeException exception = assertThrows(JPSRuntimeException.class, () -> ontoCityGMLModel.setId(value));
        assertEquals(ID_MISMATCH_ERROR_TEXT.get(ontoCityGMLModel), exception.getMessage());

    }
}
