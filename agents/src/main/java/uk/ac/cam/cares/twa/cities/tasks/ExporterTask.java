package uk.ac.cam.cares.twa.cities.tasks;

import org.apache.commons.io.FileUtils;
import org.citydb.ImpExp;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ResourceBundle;

public class ExporterTask implements Runnable {

    public static final String PROJECT_CONFIG = "project.xml";
    private final String inputs;
    private final String outputpath;
    private Boolean stop = false;
    private final String PLACEHOLDER_GMLID = "{{gmlid}}";

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
        //System.out.println("look here 1");
        while (!stop) {

            try {
                cfgfile = setupConfig();  // modify the gmlIds within the config file
                String[] args = {"-shell", "-kmlExport=" + outputpath,
                        "-config=" + cfgfile.getAbsolutePath()};
                ImpExp.main(args);

            } catch (IOException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
    }

    /**
     * Set the GMLOD into Config file, @TODO: database setup, gmlId setup
     * Assume LODx and displayForm are fixed : LOD2 and extruded
     * For bbox option, the parameters are more complex
     * This method should overwrite the gmlId within the project setting to define what to extract. * for all
     *
     * */
    private File setupConfig() throws IOException {
        // config file is stored in system-default place
        ResourceBundle rd = ResourceBundle.getBundle("config");
        String projectCfg = rd.getString("projectCfg");

        File cfgFile = new File(projectCfg);
        String cfgData = FileUtils.readFileToString(cfgFile, String.valueOf(Charset.defaultCharset()));
        cfgData = cfgData.replace(PLACEHOLDER_GMLID, this.inputs);
        FileUtils.writeStringToFile(cfgFile, cfgData, (Charset) null);
        return cfgFile;
    }
}
