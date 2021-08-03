package uk.ac.cam.cares.twa.cities.tasks;

import org.citydb.ImpExp;
import org.json.JSONObject;
import org.json.XML;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class ExporterTask implements Runnable {

    public static final String PROJECT_CONFIG = "project.xml";
    private final String inputs;
    private final String outputpath;


    public ExporterTask(String gmlIds, String outputpath) {
        this.inputs = gmlIds;
        this.outputpath = outputpath;
    }

    @Override
    public void run() {
        File cfgfile = null;
        try {
            cfgfile = setupConfig();  // modify the gmlIds within the config file
            String[] args = {"-shell", "-kmlExport=" + outputpath,
                    "-config=" + cfgfile.getAbsolutePath()};
            ImpExp.main(args);

        } catch (IOException e) {
            e.printStackTrace();
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
        String projectCfg = "C:\\Users\\Shiying\\Documents\\CKG\\CitiesKG-git\\agents\\src\\test\\resources\\project.xml";

        File cfgFile = new File(projectCfg);
        byte[] b = Files.readAllBytes(cfgFile.toPath());
        String xml = new String(b);
        JSONObject obj = XML.toJSONObject(xml);

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

        URL targetURL = null;
        //TODO: implementation
        return cfgFile;
    }
}
