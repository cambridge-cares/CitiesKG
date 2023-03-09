package uk.ac.cam.cares.twa.cities.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;

class OntologyInferenceAgentTest {

    @Test
    public void testNewOntologyInferenceAgent() {
        OntologyInferenceAgent agent = new OntologyInferenceAgent();
        assertNotNull(agent);
    }

    @Test
    public void testNewOntologyInferenceAgentFields() {
        OntologyInferenceAgent agent = new OntologyInferenceAgent();
        assertEquals(1, agent.getClass().getDeclaredFields().length);

        try {
            assertEquals("/inference/ontology", agent.getClass().getDeclaredField("URI_ACTION").get(agent));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewOntologyInferenceAgentMethods() {
        OntologyInferenceAgent agent = new OntologyInferenceAgent();
        assertEquals(1, agent.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testGetAllTargetData() {
        OntologyInferenceAgent agent = new OntologyInferenceAgent();
        JSONArray result = new JSONArray();
        try (MockedStatic<AccessAgentCaller> aacMock = Mockito.mockStatic(AccessAgentCaller.class)) {
            aacMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(result);
            agent.getClass().getSuperclass().getDeclaredField("route").set(agent, "http://localhost:48080/test");
            Method getAllTargetData = agent.getClass().getDeclaredMethod("getAllTargetData", IRI.class, String.class);
            getAllTargetData.setAccessible(true);
            assertEquals(result, getAllTargetData.invoke(agent, IRI.create("http://127.0.0.1:9999/blazegraph/namespace/test/sparql/"),
                "/testgraph"));
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            fail();
        }
    }

}