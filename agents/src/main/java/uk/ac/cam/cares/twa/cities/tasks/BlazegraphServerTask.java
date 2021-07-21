package uk.ac.cam.cares.twa.cities.tasks;

import com.bigdata.rdf.sail.webapp.ConfigParams;
import com.bigdata.rdf.sail.webapp.NanoSparqlServer;
import com.bigdata.rdf.sail.webapp.StandaloneNanoSparqlServer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.concurrent.BlockingQueue;

public class BlazegraphServerTask implements Runnable {
    public static final String PROPERTY_FILE = "RWStore.properties";
    private final String PROPERTY_FILE_PATH = "../../../../../../../";
    private final String JETTY_CFG_PATH = "../../../../../jetty.xml";
    private final String WAR_PATH = "../../../../../war";
    public static final String NAMESPACE = "tmpkb";
    private final String DEF_JOURNAL_NAME = "citiesKG.jnl";
    private final String FS = System.getProperty("file.separator");
    private final String journalPath;
    private final BlockingQueue<Server> queue;
    private Boolean stop = false;
    private Server server = null;


    public BlazegraphServerTask(BlockingQueue<Server> queue, String journalName) {
        this.queue = queue;
        this.journalPath = journalName;
    }


    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        while (!stop ) {
            if (server == null) {
                try {
                    String propFileAbsPath = setupPaths();
                    setupFiles(propFileAbsPath);
                    String jettyXml = setupSystem(propFileAbsPath);
                    server = setupServer(propFileAbsPath, jettyXml);
                    StandaloneNanoSparqlServer.awaitServerStart(server);
                    queue.put(server);
                } catch (Exception e) {
                    throw new JPSRuntimeException(e);
                }
            } else if (server.isStopped()) {
                server.destroy();
                stop();
            }
        }
    }


    private String setupPaths() {
        File journalFile =  new File(journalPath);
        String propFileName = journalFile.getName().split("\\.")[0] + PROPERTY_FILE;
        String propFilePath = journalFile.getParent();
        String propFileAbsPath = propFilePath + FS + propFileName;

        return propFileAbsPath;
    }

    private File setupFiles(String propFileAbsPath) throws URISyntaxException, IOException {
        Files.copy(Paths.get(getClass().getResource(PROPERTY_FILE_PATH + PROPERTY_FILE).toURI()),
                Paths.get(propFileAbsPath));
        File  propFile= new File(propFileAbsPath);
        String data = FileUtils.readFileToString(propFile, String.valueOf(Charset.defaultCharset()));
        data = data.replace(DEF_JOURNAL_NAME, journalPath);
        FileUtils.writeStringToFile(propFile, data);

        return propFile;
    }

    private String setupSystem(String propFileAbsPath) {
        String jettyXml = NanoSparqlServer.class.getResource(JETTY_CFG_PATH).toExternalForm();
        String war = NanoSparqlServer.class.getResource(WAR_PATH).toExternalForm();
        System.setProperty("jetty.home", war);
        System.setProperty(NanoSparqlServer.SystemProperties.JETTY_XML, jettyXml);
        System.setProperty(NanoSparqlServer.SystemProperties.BIGDATA_PROPERTY_FILE, propFileAbsPath);

        return jettyXml;
    }

    private Server setupServer(String propFileAbsPath, String jettyXml) throws Exception {
        LinkedHashMap<String, String> initParams = new LinkedHashMap<>();
        initParams.put(ConfigParams.PROPERTY_FILE, propFileAbsPath);
        initParams.put(ConfigParams.NAMESPACE, NAMESPACE);
        Server server = StandaloneNanoSparqlServer.newInstance(0, jettyXml, null, initParams);

        return server;
    }

}