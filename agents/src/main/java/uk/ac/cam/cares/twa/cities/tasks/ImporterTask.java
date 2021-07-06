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

public class ImporterTask implements Runnable {
    private final String PROJECT_CONFIG = "project.xml";
    private final String PLACEHOLDER_HOST = "{{host}}";
    private final String PLACEHOLDER_PORT = "{{port}}";
    private final String PLACEHOLDER_NS = "{{namespace}}";
    public static final String EXT_FILE_JNL = ".jnl";
    public static final String EXT_FILE_GML = ".gml";
    public static final String EXT_FILE_NQUADS = ".nq";
    private BlazegraphServerTask serverTask;
    private File importFile;

    private Boolean stop = false;

    public ImporterTask(BlazegraphServerTask serverTask, File importFile) {
        this.serverTask = serverTask;
        this.importFile = importFile;
        this.serverTask.stop(false);
    }

    public boolean getStop() {
        return stop;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        while (!stop) {
            while (!serverTask.getStop()) {
                URI endpointUri = serverTask.getServiceUri();
                if (endpointUri != null) {
                    try {
                        File cfgfile = setupFiles(endpointUri);
                        String[] args = {"-shell", "-import=" + importFile.getAbsolutePath(),
                        "-config=" + cfgfile.getAbsolutePath()};
                        ImpExp.main(args);
                        new File(importFile.getAbsolutePath().replace(EXT_FILE_GML, EXT_FILE_NQUADS)).createNewFile();
                    } catch (Exception e) {
                        throw new JPSRuntimeException(e);
                    } finally {
                        serverTask.stop(true);
                        stop();
                    }
                }
            }
        }
    }

    private File setupFiles(URI endpointUri) throws URISyntaxException, IOException {
        String projectCfg = importFile.getAbsolutePath().replace(EXT_FILE_GML, PROJECT_CONFIG);
        Files.copy(Paths.get(getClass().getClassLoader().getResource(PROJECT_CONFIG).toURI()),
                Paths.get(projectCfg));
        File cfgFile = new File(projectCfg);
        String cfgData = FileUtils.readFileToString(cfgFile, String.valueOf(Charset.defaultCharset()));
        cfgData = cfgData.replace(PLACEHOLDER_HOST, endpointUri.getHost());
        cfgData = cfgData.replace(PLACEHOLDER_PORT, String.valueOf(endpointUri.getPort()));
        cfgData = cfgData.replace(PLACEHOLDER_NS, "/blazegraph/namespace/" +
                BlazegraphServerTask.NAMESPACE +
                "/sparql/");
        FileUtils.writeStringToFile(cfgFile, cfgData);

        return cfgFile;
    }

}