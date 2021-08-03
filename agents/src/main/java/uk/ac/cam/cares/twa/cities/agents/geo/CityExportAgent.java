package uk.ac.cam.cares.twa.cities.agents.geo;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.json.JSON;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

@WebServlet(
        urlPatterns = {
                CityExportAgent.URI_ACTION
        })
public class CityExportAgent extends JPSAgent {
        public static final String URI_ACTION = "/export/kml";
        public static final String KEY_GMLID = "gmlid";
        public static final String KEY_REQ_METHOD = "method";


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        if (validateInput(requestParams)) {
            //TODO
            try {
                URL targeturl = setupConfig();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
        //TODO
        return null;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        //TODO
        boolean error = true;
        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        if (!requestParams.getString(KEY_GMLID).isEmpty()){
                            error = false;   // no error
                        }
                    } catch (Exception e) {
                        throw new BadRequestException(e);
                    }
                }
            }
        }
        if (error) {
            throw new BadRequestException();
        }
        return !error;
    }

    private URL setupConfig() throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
        String projectCfg = "C:\\Users\\Shiying\\Documents\\CKG\\CitiesKG-git\\agents\\src\\test\\resources\\project.xml";

        File cfgFile = new File(projectCfg);
        byte[] b = Files.readAllBytes(cfgFile.toPath());
        String xml = new String(b);
        JSONObject obj = XML.toJSONObject(xml);
        System.out.println(obj);
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
        return targetURL;
    }

}
