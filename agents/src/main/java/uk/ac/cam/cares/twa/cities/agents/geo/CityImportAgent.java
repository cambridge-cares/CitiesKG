package uk.ac.cam.cares.twa.cities.agents.geo;

import org.apache.http.protocol.HTTP;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.http.Http;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;


/**
 * A JPSAgent framework based 3D City Import Agent class used to import data into Semantic 3D City Database
 * {@link <a href="https://www.cares.cam.ac.uk/research/cities/">}, implemented as Blazegraph backend
 * and integrated into the Dynamic Geospatial Knowledge Graph of The World Avatar project
 * {@link <a href="https://www.theworldavatar.com/">}. The agent:
 *
 * - listens to the presence of new files in a given directory
 * - splits large city model files into smaller chunks
 * - imports chunks into local Blazegraph instances (for verification)
 * - exports locally imported data into n-quads format {@link <a https://www.w3.org/TR/n-quads/">}
 * - imports n-quads into target Blazegraph instance (The World Avatar)
 * - archives audit trail
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 *
 */
@WebServlet(
        urlPatterns = {
                CityImportAgent.URI_LISTEN,
                CityImportAgent.URI_ACTION
        })
public class CityImportAgent extends JPSAgent {

    public static final String URI_LISTEN = "/import/source";
    public static final String URI_ACTION = "/import/action";
    public static final String KEY_REQ_METHOD = "method";
    public static final String KEY_REQ_URL = "requestUrl";
    public static final String KEY_DIRECTORY = "directory";


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        if (validateInput(requestParams)) {
            if (requestParams.getString(KEY_REQ_URL).contains(URI_LISTEN)) {
                listenToImport(requestParams.getString(KEY_DIRECTORY));
            } else if (requestParams.getString(KEY_REQ_URL).contains(URI_ACTION)) {
                //@Todo: implementation
            }
        }
        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        boolean error = true;
        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_REQ_URL)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        URL reqUrl = new URL((String) requestParams.get(KEY_REQ_URL));
                        if (reqUrl.getPath().contains(URI_LISTEN) && keys.contains(KEY_DIRECTORY)) {
                            File dir = new File((String) requestParams.get(KEY_DIRECTORY));
                            if (dir.getAbsolutePath().length() > 0) {
                                error = false;
                            }
                        } else if (reqUrl.getPath().contains(URI_ACTION)) {
                            //@Todo: implementation
                        }
                    } catch (MalformedURLException e) {
                        throw new BadRequestException();
                    }
                }
            }
        }

        if (error) {
            throw new BadRequestException();
        }

        return true;
    }

    /**
     * Starts agent listening process to the new file appearing events in a given directory.
     *
     * @param directoryName - which directory to look for new files
     * @return information about listening process start
     */
    private boolean listenToImport(String directoryName) {
        //@Todo: implementation
        boolean listening = false;
        return listening;
    }

    /**
     * Imports CityGML files present in a given directory
     *
     * @param directoryName - contains CityGML files to import
     *
     * @return - import summary
     */
    private String importFiles(String directoryName) {
        //@Todo: implementation
        String imported = "";
        return imported;
    }

    /**
     * Splits CityGML files into smaller chunks in order to better manage the import process
     *
     * @param fileName - file to split
     * @param chunkSize - number of cityOBjectMebers in targer chunks after split
     *
     * @return - list of CityGML chunk files
     */
    private ArrayList splitFile(String fileName, int chunkSize) {
        //@Todo: implementation
        ArrayList chunks = new ArrayList<>();
        return chunks;
    }

    /**
     * Imports CityGML chunk file (for verification and detection,
     * if there are features not yet implemented in ImpExp tool in the chunk)
     *
     * @param fileName - chunk to import
     * @return - information about local import success
     */
    private boolean importChunk(String fileName) {
        //@Todo: implementation
        boolean imported = false;
        return imported;
    }

    /**
     * Starts local Blazegraph SPARQL server instance.
     *
     * @param port - SPARQL server port
     * @return - information about start success
     */
    private boolean startBlazegraphInstance(int port) {
        //@Todo: implementation
        boolean started = false;
        return started;
    }

    /**
     * Imports CityGML file into local Blazegraph instance.
     *
     * @param filename - file to import
     * @param port - port of local SPARQL server instance
     * @return
     */
    private String importToLocalBlazegraphInstance(String filename, int port) {
        //@Todo: implementation
        String importLog = "";
        return importLog;
    }

    /**
     * Stops local Blazegraph SPARQL server instance.
     *
     * @param port - SPARQL server port
     * @return - information about stop success
     */
    private boolean stopBlazegraphInstance(int port) {
        //@Todo: implementation
        boolean stopped = false;
        return stopped;
    }

    /**
     * Writes error log to a file.
     *
     * @param errorLog - error contents.
     */
    private void writeErrorLog(String errorLog) {
        //@Todo: implementation
    }

    /**
     * Exports data from Blazegraph journal file to n-quads format.
     *
     * @param journalFileName - file to export from
     * @return - exported n-quads file name
     */
    private String exportToNquads(String journalFileName) {
        //@Todo: implementation
        String nQuadsFileName = "";
        return nQuadsFileName;
    }

    /**
     * Find and replace on n-quads files to prepare them to contain URLs of the target system
     * instead of the local instance.
     *
     * @param filename - n-quads file to replace URLs in
     * @param from - string to replace
     * @param to - string to replace with
     * @return - information about replacement success
     */
    private boolean changeUrlsInNQuadsFile(String filename, String from, String to) {
        //@Todo: implementation
        boolean changed = false;
        return changed;
    }

    /**
     * Imports n-quads file into a running Blazegraph instance.
     *
     * @param fileName - n-quads file
     * @param blasegraphImportURL - URL of the Blazegraph instance
     * @return  - information about import success
     */
    private boolean uploadNQuadsFileToBlazegraphInstance(String fileName, String blasegraphImportURL) {
        //@Todo: implementation
        boolean uploaded = false;
        return uploaded;
    }

    /**
     * Creates an audit trail archive.
     *
     * @return - path to the audit trail archive.
     */
    private String archiveImportFiles() {
        //@Todo: implementation
        String archiveFilename = "";
        return archiveFilename;
    }

}
