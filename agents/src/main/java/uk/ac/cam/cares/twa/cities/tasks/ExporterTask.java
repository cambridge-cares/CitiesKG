package uk.ac.cam.cares.twa.cities.tasks;

import org.apache.commons.io.FileUtils;
import org.citydb.ImpExp;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;


public class ExporterTask implements Runnable {

    public static final String PROJECT_CONFIG = "project.xml";  // project config template
    public static final String EXT_FILE_KML = ".kml";
    public static final String PLACEHOLDER_GMLID = "{{gmlid}}";
    public static final String PLACEHOLDER_HOST = "{{host}}";
    public static final String PLACEHOLDER_PORT = "{{port}}";
    public static final String PLACEHOLDER_NS = "{{namespace}}";
    public static final String ARG_SHELL = "-shell";
    public static final String ARG_KMLEXPORT = "-kmlExport=";
    public static final String ARG_CFG = "-config=";

    private final String[] inputs;
    private final String outputpath;
    private final JSONObject serverinfo;
    private Boolean stop = false;


    public ExporterTask(String[] gmlIds, String outputpath, JSONObject serverInfo) {
        this.inputs = gmlIds;
        this.outputpath = outputpath;
        this.serverinfo = serverInfo;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        File cfgfile;
        while (!stop) {

            try {
                cfgfile = setupConfig();  // modify the gmlIds within the config file
                String[] args = {ARG_SHELL, ARG_KMLEXPORT + outputpath,
                        ARG_CFG + cfgfile.getAbsolutePath()};
                ImpExp.main(args);

            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
    }

    /**
     * Replaces placeholder values in Importer/Exporter project config file with the gmlids
     *
     * Assume LODx and displayForm are fixed : LOD2 and extruded
     * For bbox option, the parameters are more complex
     * This method should overwrite the gmlId within the project setting to define what to extract. * for all
     * @return - config file with changed values
     * @throws URISyntaxException - when the project path could not be converted to URI
     * @throws IOException        - when copy/read/write operations on files fail
     * */
    private File setupConfig() throws IOException, URISyntaxException {

        // Copy the template to the location of output
        File exportFile = new File(outputpath);

        String projectCfg = exportFile.getAbsolutePath().replace(EXT_FILE_KML, PROJECT_CONFIG);
        Files.copy(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(PROJECT_CONFIG)).toURI()),
                    Paths.get(projectCfg), StandardCopyOption.REPLACE_EXISTING);

        File cfgFile = new File(projectCfg);

        String cfgData = FileUtils.readFileToString(cfgFile, String.valueOf(Charset.defaultCharset()));
        cfgData = cfgData.replace(PLACEHOLDER_GMLID, createGmlids(this.inputs));
        cfgData = cfgData.replace(PLACEHOLDER_HOST, String.valueOf(serverinfo.get("host")));
        cfgData = cfgData.replace(PLACEHOLDER_PORT, String.valueOf(serverinfo.get("port")));
        cfgData = cfgData.replace(PLACEHOLDER_NS, String.valueOf(serverinfo.get("namespace")));

        FileUtils.writeStringToFile(cfgFile, cfgData);
        return cfgFile;
    }

    private String createGmlids (String[] gmlids) {

        StringBuilder sb = new StringBuilder();
        for (String gmlid : gmlids){
            sb.append("<id>");
            sb.append(gmlid);
            sb.append("</id>");
        }
        return sb.toString();
    }

}
