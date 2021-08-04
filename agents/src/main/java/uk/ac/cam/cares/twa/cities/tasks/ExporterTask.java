package uk.ac.cam.cares.twa.cities.tasks;

import org.apache.commons.io.FileUtils;
import org.citydb.ImpExp;
import org.json.JSONObject;
import org.json.XML;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class ExporterTask implements Runnable {

    public static final String PROJECT_CONFIG = "project.xml";
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
        File cfgfile = null;
        System.out.println("look here 1");
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
     * Set the GMLOD into Config file, @TODO: database setup
     * Assume LODx and displayForm are fixed : LOD2 and extruded
     * For bbox option, the parameters are more complex
     *
     *
     * */
    private File setupConfig() throws IOException {
        // config file is stored in system-default place
        String projectCfg = "C:\\Users\\Shiying\\Documents\\CKG\\CitiesKG-git\\agents\\src\\main\\resources\\project.xml";

        File cfgFile = new File(projectCfg);
        /*
        byte[] b = Files.readAllBytes(cfgFile.toPath());
        String xml = new String(b);
        JSONObject obj = XML.toJSONObject(xml);
        JSONObject gmlIdobj = obj.getJSONObject("project").getJSONObject("kmlExport").getJSONObject("query").getJSONObject("gmlIds");
        gmlIdobj.put("id", this.inputs);
        String cfgData = XML.toString(obj);
        FileUtils.writeStringToFile(cfgFile, cfgData);
        */

        //System.out.println(obj);
        // Possible reading: obj.getJSONObject("project").getJSONObject("kmlExport").getJSONObject("query").getJSONObject("gmlIds").getString("id")
        /*
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(cfgFile);
        */

        /*
        URL url = XMLtoJsonConverter.class.getClassLoader().getResource("sample.xml");
        inputStream = url.openStream();
        String xml = IOUtils.toString(inputStream);
        JSON objJson = new XMLSerializer().read(xml);
        System.out.println("JSON data : " + objJson);
        */

        return cfgFile;
    }
}
