package uk.ac.cam.cares.twa.cities.agents;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GraphInferenceAgentTest {

    @Test
    public void testNewGraphInferenceAgent() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
        assertNotNull(agent);
    }

    @Test
    public void testNewGraphInferenceAgentFields() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
        assertEquals(1, agent.getClass().getDeclaredFields().length);

        try {
            assertEquals("/inference/graph", agent.getClass().getDeclaredField("URI_ACTION").get(agent));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewGraphInferenceAgentMethods() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
        assertEquals(1, agent.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testGetAllTargetData() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
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