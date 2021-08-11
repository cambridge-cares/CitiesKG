package uk.ac.cam.cares.twa.cities.tasks;

import org.apache.commons.io.FileUtils;
import org.citydb.ImpExp;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import org.eclipse.jetty.server.Server;

/**
 * Runnable task importing a CityGML model into an instance of a SPARQL sever picked up from the {@link BlockingQueue}.
 * It uses an instance of {@link ImpExp} configured before starting the import of a specific model CityGML file. The
 * server instance and the task is stopped after the import process is finished and a temporary .nq file is created to
 * indicate that the .jnl file is ready for processing by other tasks.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 *
 */
public class ImporterTask implements Runnable {
    public static final String PROJECT_CONFIG = "project.xml";
    public static final String PLACEHOLDER_HOST = "{{host}}";
    public static final String PLACEHOLDER_PORT = "{{port}}";
    public static final String PLACEHOLDER_NS = "{{namespace}}";
    public static final String URL_BLZG_NS = "/blazegraph/namespace/";
    public static final String URL_SPARQL = "/sparql/";
    public static final String EXT_FILE_JNL = ".jnl";
    public static final String EXT_FILE_GML = ".gml";
    public static final String EXT_FILE_NQUADS = ".nq";
    public static final String ARG_SHELL = "-shell";
    public static final String ARG_IMPORT = "-import=";
    public static final String ARG_CFG = "-config=";
    private final BlockingQueue<Server> serverInstances;
    private final File importFile;
    private boolean stop = false;

    public ImporterTask(BlockingQueue<Server> serverInstances, File importFile) {
        this.serverInstances = serverInstances;
        this.importFile = importFile;
    }


    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        while (!stop) {
            while (!serverInstances.isEmpty()) {
                try {
                    Server server = serverInstances.take();
                    File cfgfile = setupFiles(server.getURI());
                    String[] args = {ARG_SHELL, ARG_IMPORT + importFile.getAbsolutePath(),
                            ARG_CFG + cfgfile.getAbsolutePath()};
                    ImpExp.main(args);
                    //Create temporary nq file indicating that import to the particular instance is finished
                    File nqFile = new File(importFile.getAbsolutePath().replace(EXT_FILE_GML, EXT_FILE_NQUADS));
                    if (!nqFile.createNewFile()) {
                        throw new IOException();
                    }
                    server.stop();
               } catch (Exception e) {
                    throw new JPSRuntimeException(e);
                } finally {
                    stop();
                }
            }
        }
    }

    /**
     * Replaces placeholder values in Importer/Exporter project config file with the ones of the running local instance
     * of the Blazegraph server.
     *
     * @param endpointUri - local server instance URL
     * @return - config file with changed values
     * @throws URISyntaxException - when the project path could not be converted to URI
     * @throws IOException - when copy/read/write operations on files fail
     */
    private File setupFiles(URI endpointUri) throws URISyntaxException, IOException {
        String projectCfg = importFile.getAbsolutePath().replace(EXT_FILE_GML, PROJECT_CONFIG);
        Files.copy(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(PROJECT_CONFIG)).toURI()),
                Paths.get(projectCfg));
        File cfgFile = new File(projectCfg);
        String cfgData = FileUtils.readFileToString(cfgFile, String.valueOf(Charset.defaultCharset()));
        cfgData = cfgData.replace(PLACEHOLDER_HOST, endpointUri.getHost());
        cfgData = cfgData.replace(PLACEHOLDER_PORT, String.valueOf(endpointUri.getPort()));
        cfgData = cfgData.replace(PLACEHOLDER_NS, URL_BLZG_NS + BlazegraphServerTask.NAMESPACE + URL_SPARQL);

        FileUtils.writeStringToFile(cfgFile, cfgData);

        return cfgFile;
    }

}