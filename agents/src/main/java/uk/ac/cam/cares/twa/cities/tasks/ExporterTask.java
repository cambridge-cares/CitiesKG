package uk.ac.cam.cares.twa.cities.tasks;

import org.apache.commons.io.FileUtils;
import org.citydb.ImpExp;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class ExporterTask implements Runnable {

    public static final String PROJECT_CONFIG = "project.xml";  // project config template
    public static final String PLACEHOLDER_GMLID = "{{gmlid}}";
    public static final String ARG_SHELL = "-shell";
    public static final String ARG_KMLEXPORT = "-kmlExport=";
    public static final String ARG_CFG = "-config=";

    private final String inputs;
    private final String outputpath;
    private Boolean stop = false;


    public ExporterTask(String gmlIds, String outputpath) {
        this.inputs = gmlIds;
        this.outputpath = outputpath;
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
     * Set the GMLOD into Config file,
     * Assume LODx and displayForm are fixed : LOD2 and extruded
     * For bbox option, the parameters are more complex
     * This method should overwrite the gmlId within the project setting to define what to extract. * for all
     *
     * */
    private File setupConfig() throws IOException, URISyntaxException {

        // Copy the template to the location of output
        File outputFile = new File(outputpath);

        String configPath = outputFile.getAbsolutePath().replace(".kml", "project.xml");
        Files.copy(Paths.get(getClass().getClassLoader().getResource(PROJECT_CONFIG).toURI()),
                    Paths.get(configPath), StandardCopyOption.REPLACE_EXISTING);

        File cfgFile = new File(configPath);

        String cfgData = FileUtils.readFileToString(cfgFile, String.valueOf(Charset.defaultCharset()));
        cfgData = cfgData.replace(PLACEHOLDER_GMLID, this.inputs);
        FileUtils.writeStringToFile(cfgFile, cfgData);
        return cfgFile;
    }
}
