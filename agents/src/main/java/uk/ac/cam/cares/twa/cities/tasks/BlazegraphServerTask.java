package uk.ac.cam.cares.twa.cities.tasks;

import com.bigdata.rdf.sail.webapp.ConfigParams;
import com.bigdata.rdf.sail.webapp.NanoSparqlServer;
import com.bigdata.rdf.sail.webapp.StandaloneNanoSparqlServer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class BlazegraphServerTask implements Runnable {
    private final String PROPERTY_FILE = "RWStore.properties";
    private final String JETTY_CFG_PATH = "../../../../../jetty.xml";
    private final String WAR_PATH = "../../../../../war";
    private final String NAMESPACE = "tmpkb";
    private final String DEF_JOURNAL_NAME = "citiesKG.jnl";
    private String journalPath;
    private URI serviceUri;
    private Boolean stop = false;

    public BlazegraphServerTask(String journalName) {
        this.journalPath = journalName;
    }

    public String getJournalName() {
        return journalPath;
    }

    public URI getServiceUri() {
        return serviceUri;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        Server server = null;
        File  propFile = null;
        while (!stop) {
            try {
                int port = 0;
                String propFileName = new File(journalPath).getName().split("\\.")[0] + PROPERTY_FILE;
                Files.copy(Paths.get(PROPERTY_FILE), Paths.get(propFileName));
                propFile = new File(propFileName);
                String data = FileUtils.readFileToString(propFile, Charset.defaultCharset());
                data = data.replace(DEF_JOURNAL_NAME, journalPath);
                FileUtils.writeStringToFile(propFile, data, Charset.defaultCharset());

                String jettyXml = NanoSparqlServer.class.getResource(JETTY_CFG_PATH).toExternalForm();
                String war = NanoSparqlServer.class.getResource(WAR_PATH).toExternalForm();

                System.setProperty("jetty.home", war);
                System.setProperty(NanoSparqlServer.SystemProperties.JETTY_XML, jettyXml);
                System.setProperty(NanoSparqlServer.SystemProperties.BIGDATA_PROPERTY_FILE, propFileName);
                LinkedHashMap<String, String> initParams = new LinkedHashMap<String, String>();
                initParams.put(ConfigParams.PROPERTY_FILE, propFileName);
                initParams.put(ConfigParams.NAMESPACE, NAMESPACE);
                initParams.put(ConfigParams.QUERY_THREAD_POOL_SIZE, String.valueOf(ConfigParams.DEFAULT_QUERY_THREAD_POOL_SIZE));
                initParams.put(ConfigParams.FORCE_OVERFLOW, "false");
                initParams.put(ConfigParams.READ_LOCK, "0");

                server = StandaloneNanoSparqlServer.newInstance(port, jettyXml, null, initParams);

                StandaloneNanoSparqlServer.awaitServerStart(server);

                serviceUri = server.getURI();

                server.join();
               

                server.stop();

            } catch (Exception e) {
                throw new JPSRuntimeException(e);
            }
        }
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                throw new JPSRuntimeException(e);
            }
        }
        if (propFile != null && propFile.isFile()) {
            propFile.delete();
        }
    }
}