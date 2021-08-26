package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;
import uk.ac.cam.cares.twa.cities.tasks.NquadsUploaderTask;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class NquadsUploaderTaskTest  extends TestCase {

    public void testNewNquadsUploaderTask() {
        NquadsUploaderTask task;

        try {
            task = new NquadsUploaderTask(new LinkedBlockingDeque<>(), new URI("http://127.0.0.0.1/sparql"));
            assertNotNull(task);
        }  catch (Exception e) {
            fail();
        }
    }

    public void testNewNquadsUploaderTaskFields() {
        BlockingQueue<File> queue = new LinkedBlockingDeque<>();
        URI uri = null;
        try {
            uri = new URI("http://www.test.com/");
        } catch (URISyntaxException e) {
            fail();
        }
        NquadsUploaderTask task = new NquadsUploaderTask(queue, uri);
        assertEquals(4, task.getClass().getDeclaredFields().length);

        Field CTYPE_NQ ;
        Field stop;
        Field nqQueue;
        Field endpointUri;

        try {
            CTYPE_NQ = task.getClass().getDeclaredField("CTYPE_NQ");
            assertEquals(CTYPE_NQ.get(task), "text/x-nquads");
            stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((Boolean) stop.get(task));
            nqQueue = task.getClass().getDeclaredField("nqQueue");
            nqQueue.setAccessible(true);
            assertEquals(nqQueue.get(task), queue);
            endpointUri = task.getClass().getDeclaredField("endpointUri");
            endpointUri.setAccessible(true);
            assertEquals(endpointUri.get(task), uri);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }

    }

    public void testNewNquadsUploaderTaskMethods() {
        try {
            NquadsUploaderTask task = new NquadsUploaderTask(new LinkedBlockingDeque<>(), new URI("http://127.0.0.0.1/sparql"));
            assertEquals(2, task.getClass().getDeclaredMethods().length);
        } catch (URISyntaxException e) {
            fail();
        }
    }

    public void testNewNquadsUploaderStopMethod() {
        try {
            NquadsUploaderTask task = new NquadsUploaderTask(new LinkedBlockingDeque<>(), new URI("http://127.0.0.0.1/sparql"));
            Field stopF = task.getClass().getDeclaredField("stop");
            stopF.setAccessible(true);
            assertFalse((Boolean) stopF.get(task));
            Method stopM = task.getClass().getDeclaredMethod("stop");
            stopM.setAccessible(true);
            stopM.invoke(task);
            assertTrue((Boolean) stopF.get(task));
        } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            fail();
        }

    }

    public void testNewNquadsUploaderRunMethod() {
        BlockingQueue<File> queue = new LinkedBlockingDeque<>();
        File testNqFile = new File(Objects.requireNonNull(NquadsExporterTaskTest.NquadsExporterTaskTestHelper.class.getClassLoader().getResource("test.nq")).getFile());
        File nqFile = new File(System.getProperty("java.io.tmpdir") + "test.nq");
        String jnlPath = System.getProperty("java.io.tmpdir") + "test.jnl";
        File jnlFile = new File(jnlPath);
        File propFile = new File(jnlPath.replace(ImporterTask.EXT_FILE_JNL, BlazegraphServerTask.PROPERTY_FILE));
        URI uri;

        try {
            queue.put(nqFile);
            uri = new URI("http://localhost/sparql");
            NquadsUploaderTask task = new NquadsUploaderTask(queue, uri);

            try {
                task.run();
            } catch (Exception e) {
                assertEquals(e.getClass(), JPSRuntimeException.class);
            }

            Files.copy(testNqFile.toPath(), nqFile.toPath());
            queue.put(nqFile);
            task = new NquadsUploaderTask(queue, uri);

            try {
                task.run();
            } catch (Exception e) {
                assertEquals(e.getClass(), JPSRuntimeException.class);
            }

            ExecutorService serverExecutor = Executors.newFixedThreadPool(1);
            BlockingQueue<Server> importQueue = new LinkedBlockingDeque<>();
            BlazegraphServerTask serverTask = new BlazegraphServerTask(importQueue, jnlPath);
            serverExecutor.execute(serverTask);

            try {
                Field serverF = serverTask.getClass().getDeclaredField("server");
                serverF.setAccessible(true);
                Server server = null;
                while (server == null) {
                    server = (Server) serverF.get(serverTask);
                }

                URI serverUri = null;
                while (serverUri == null) {
                    if (server.isStarted()) {
                        serverUri = server.getURI();
                    }
                }

                URI endpointUri = new URI(serverUri + "blazegraph/namespace/tmpkb/sparql");

                task = new NquadsUploaderTask(queue, endpointUri);

                new Thread(task).start();
                Field stop = task.getClass().getDeclaredField("stop");
                stop.setAccessible(true);

                while (!(boolean) stop.get(task)) {
                    if (queue.size() == 0) {
                        if (!server.isStopped()) {
                            server.setStopAtShutdown(true);
                            server.setStopTimeout(7_000);
                            server.stop();
                        }
                        stop.set(task, true);
                    }
                }

            } catch (Exception e) {
                fail();
            }
        } catch (InterruptedException | URISyntaxException | IOException e) {
            fail();
        } finally {
            if (Objects.requireNonNull(nqFile).isFile()) {
                if (!nqFile.delete()) {
                    fail();
                }
            }
            if (Objects.requireNonNull(jnlFile).isFile()) {
                if (!jnlFile.delete()) {
                    fail();
                }
            }
            if (Objects.requireNonNull(propFile).isFile()) {
                if (!propFile.delete()) {
                    fail();
                }
            }
        }

    }

}
