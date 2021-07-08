package uk.ac.cam.cares.twa.cities.agents.geo;

import com.bigdata.rdf.sail.ExportKB;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.aws.AsynchronousWatcherService;
import uk.ac.cam.cares.jps.aws.WatcherCallback;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.aws.CreateFileWatcher;
import uk.ac.cam.cares.jps.base.util.CommandHelper;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


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
    public static final String URI_ACTION = "/import/citygml";
    public static final String KEY_REQ_METHOD = "method";
    public static final String KEY_REQ_URL = "requestUrl";
    public static final String KEY_DIRECTORY = "directory";
    public static final String KEY_SPLIT = "split";
    public static final String KEY_TARGET_URL = "targetURL";
    public static final String ARG_OUTDIR = "-outdir";
    public static final String ARG_FORMAT = "-format";
    public static final String NQ_OUTDIR = "quads";
    public static final String NQ_FORMAT = "N-Quads";
    public static final String NQ_FILENAME = "data.nq.gz";
    public static final String EXT_GZ = ".gz";
    private final String FS = System.getProperty("file.separator");
    public final int CHUNK_SIZE = 100;
    //@todo: ImpExp.main() seems to work better if there is only one thread of it at a time. It needs further investigation.
    public final int NUM_SERVER_THREADS = 1;
    public final int NUM_IMPORTER_THREADS = 1;
    private String requestUrl;
    private String targetUrl;
    private File importDir;
    File splitDir;
    private final ThreadPoolExecutor serverExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_SERVER_THREADS);
    private final ThreadPoolExecutor importerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        if (validateInput(requestParams)) {
            requestUrl = requestParams.getString(KEY_REQ_URL);
            targetUrl = requestParams.getString(KEY_TARGET_URL);
            if (requestUrl.contains(URI_LISTEN)) {
                importDir = listenToImport(requestParams.getString(KEY_DIRECTORY));
            } else if (requestUrl.contains(URI_ACTION)) {
                importDir = new File(requestParams.getString(AsynchronousWatcherService.KEY_WATCH));
                requestParams = new JSONObject(importFiles(importDir));
            }
        }

        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        boolean error = true;
        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_REQ_URL) && keys.contains(KEY_TARGET_URL)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        URL reqUrl = new URL(requestParams.getString(KEY_REQ_URL));
                        new URL(requestParams.getString(KEY_TARGET_URL));
                        if (reqUrl.getPath().contains(URI_LISTEN)) {
                            error = validateListenInput(requestParams, keys);
                        } else if (reqUrl.getPath().contains(URI_ACTION)) {
                            error = validateActionInput(requestParams, keys);
                        }
                    } catch (Exception e) {
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
     * Validates input specific to requests commint to URI_LISTEN
     *
     * @param requestParams - request body in JSON format
     * @param keys - request body keys
     *
     * @return boolean saying if request is valid or not
     */
    private boolean validateListenInput(JSONObject requestParams, Set<String> keys) {
        boolean error = true;
        if (keys.contains(KEY_DIRECTORY)) {
            File dir = new File((String) requestParams.get(KEY_DIRECTORY));
            if (dir.getAbsolutePath().length() > 0) {
                error = false;
            }
        }

        return error;
    }

    /**
     * Validates input specific to requests commint to URI_ACTION
     *
     * @param requestParams - request body in JSON format
     * @param keys - request body keys
     *
     * @return boolean saying if request is valid or not
     */
    private boolean validateActionInput(JSONObject requestParams, Set<String> keys) {
        boolean error = true;
        if (keys.contains(AsynchronousWatcherService.KEY_WATCH)) {
            File dir = new File((String) requestParams.get(AsynchronousWatcherService.KEY_WATCH));
            if (dir.isDirectory()) {
                error = false;
            }
        }

        return error;
    }

    /**
     * Starts agent listening process to the new file appearing events in a given directory.
     * Creates import directory, if it does not exist.
     * Asks Asynchronous Watching Service to start watching the directory.
     *
     * @param directoryName - which directory to look for new files
     * @return directory watched for new files to import
     *
     */
    private File listenToImport(String directoryName) {
        File dir = new File(directoryName);

        if (!dir.exists() && !dir.mkdirs()) {
            throw new JPSRuntimeException(new FileNotFoundException(directoryName));
        }

        try {
            JSONObject json = new JSONObject();
            String url = requestUrl.replace(URI_LISTEN, URI_ACTION);
            json.put(AsynchronousWatcherService.KEY_WATCH, directoryName);
            json.put(AsynchronousWatcherService.KEY_CALLBACK_URL, url);
            json.put(KEY_TARGET_URL, targetUrl);
            CreateFileWatcher watcher = new CreateFileWatcher(dir,
                    AsynchronousWatcherService.PARAM_TIMEOUT * AsynchronousWatcherService.TIMEOUT_MUL);
            WatcherCallback callback = watcher.getCallback(url, json.toString());
            watcher.setCallback(callback);
            watcher.setWatchAnyFile(true);
            watcher.start();
        } catch (Exception e) {
            throw new JPSRuntimeException(e);
        }

        return dir;
    }

    /**
     * Imports CityGML files present in a given directory
     *
     * @param importDir - contains CityGML files to import
     *
     * @return - import summary
     */
    private String importFiles(File importDir) {
        String imported = "";
        File[] dirContent = importDir.listFiles();

        if (dirContent != null) {
            for (File file : dirContent) {
                ArrayList<File> chunks = splitFile(file);
                for (File chunk : chunks) {
                    importChunk(chunk);
                }
            }
        }
        ArrayList<File> rejected = new ArrayList<>();

        for (File nqlFile: exportChunksToNquads(splitDir)) {
            if (changeUrlsInNQuadsFile(nqlFile, "", targetUrl)) {
                if (!uploadNQuadsFileToBlazegraphInstance(nqlFile, targetUrl + "sparql")) {
                    rejected.add(nqlFile);
                }
            } else {
                rejected.add(nqlFile);
            }
        }

        System.out.println("Import Done.");

        return imported;
    }

    /**
     * Splits CityGML files into smaller chunks in order to better manage the import process
     *
     * @param file - file to split
     * @return - list of CityGML chunk files
     */
    private ArrayList<File> splitFile(File file) {

        ArrayList<File> chunks = new ArrayList<>();

        splitDir = new File(file.getParent() + FS + KEY_SPLIT + new Date().getTime());
        splitDir.mkdir();
        String fileSrc = file.getPath();
        String fileDst = splitDir.getPath() + FS + file.getName();

        try {
            ArrayList<String> args = new ArrayList<>();
            args.add("python");
            //@TODO: change path
            args.add("/Users/arek/git/CARES/CitiesKG-git/utils/citygml_splitter.py");
            args.add(fileDst);
            args.add(String.valueOf(CHUNK_SIZE));
            Files.move(Paths.get(fileSrc), Paths.get(fileDst));
            CommandHelper.executeCommands(splitDir.getPath(), args);
            Iterator<File> files = Arrays.stream(splitDir.listFiles()).iterator();

            while (files.hasNext()) {
                File splitFile = files.next();
                if (!splitFile.getPath().equals(fileDst)) {
                    chunks.add(splitFile);
                 }
            }
        } catch (IOException e) {
            throw new JPSRuntimeException(e);
        }

        return chunks;
    }

    /**
     * Imports CityGML chunk file (for verification and detection,
     * if there are features not yet implemented in ImpExp tool in the chunk)
     *
     * @param file- chunk to import
     * @return - information about local import success
     */
    private void importChunk(File file) {
        BlazegraphServerTask serverTask = startBlazegraphInstance(file.getAbsolutePath());
        importToLocalBlazegraphInstance(serverTask, file);

    }

    /**
     * Starts local Blazegraph SPARQL server instance.
     *
     * @return - URL of the SPARQL update endpoint
     */
    private BlazegraphServerTask startBlazegraphInstance(String filepath) {
        BlazegraphServerTask task = new BlazegraphServerTask(filepath.replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_JNL));
        serverExecutor.execute(task);

        return task;
    }

    /**
     * Imports CityGML file into local Blazegraph instance.
     *
     * @param serverTask - an instance BlazegraphServerTask
     * @param file - file to import
     * @return
     */
    private ImporterTask importToLocalBlazegraphInstance(BlazegraphServerTask serverTask, File file) {
        ImporterTask task = new ImporterTask(serverTask, file);
        importerExecutor.execute(task);

        return task;
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
     * Exports data from Blazegraph journal files to n-quads format.
     *
     * @param journalDir - directory with jnl files
     * @return - exported n-quads files
     */
    private ArrayList<File> exportChunksToNquads(File journalDir) {
        ArrayList<File> nqFiles = new ArrayList<>();

        BlockingQueue<Runnable> queue = importerExecutor.getQueue();

        //Export for finished tasks when the queue is not empty
        while (!queue.isEmpty()) {
            try {
                for (File jnlFile: collectJournalFilesForNquadsExport(journalDir)) {
                    nqFiles.add(exportToNquadsFileFromJnlFile(jnlFile));
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new JPSRuntimeException(e);
            }
        }

        //After the queue is empty, last check on the directory
        for (File jnlFile: collectJournalFilesForNquadsExport(journalDir)) {
            nqFiles.add(exportToNquadsFileFromJnlFile(jnlFile));
        }

        return nqFiles;
    }

    /**
     * Produces a list of journal files suitable for esport to n-quads,
     * for which importing process already finished.
     *
     * @param journalDir
     * @return
     */
    private ArrayList<File>  collectJournalFilesForNquadsExport(File journalDir) {
        ArrayList<File> jnlFiles = new ArrayList<>();

        File[] dirJnlContent = journalDir.listFiles((dir, name) -> name.toLowerCase().endsWith(ImporterTask.EXT_FILE_JNL));
        for (File file : dirJnlContent) {
            File nqFile = new File(file.getAbsolutePath().replace(ImporterTask.EXT_FILE_JNL, ImporterTask.EXT_FILE_NQUADS));
            if (nqFile.isFile()) {
                jnlFiles.add(file);
            }
        }

        return jnlFiles;
    }

    /**
     * Exports individual journal file to n-quads file. Removes helper files after that.
     *
     * @param jnlFile
     * @return
     */
    private File exportToNquadsFileFromJnlFile(File jnlFile) {
        File nqFile = new File(jnlFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_JNL, ImporterTask.EXT_FILE_NQUADS));
        String nqDir = nqFile.getParent() + FS + NQ_OUTDIR;
        String[] args = {ARG_OUTDIR, nqDir,
                ARG_FORMAT, NQ_FORMAT,
                jnlFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_JNL, BlazegraphServerTask.PROPERTY_FILE)};
        try {
            ExportKB.main(args);
        } catch (Exception e) {
            throw new JPSRuntimeException(e);
        }
        File exportedNqFile = new File(nqDir + FS + BlazegraphServerTask.NAMESPACE + FS + NQ_FILENAME);
        File targetNqFile = new File(nqFile.getAbsolutePath() + EXT_GZ);
        exportedNqFile.renameTo(targetNqFile);
        exportedNqFile.delete();
        nqFile.delete();

        return targetNqFile;
    }

    /**
     * Find and replace on n-quads files to prepare them to contain URLs of the target system
     * instead of the local instance.
     *
     * @param nqFile - n-quads file to replace URLs in
     * @param from - string to replace
     * @param to - string to replace with
     * @return - information about replacement success
     */
    private boolean changeUrlsInNQuadsFile(File nqFile, String from, String to) {
        //@Todo: implementation
        boolean changed = false;
        if (from.isEmpty()) {
            from = getLocalSourceUrlFromProjectCfg(nqFile);
        }



        return changed;
    }

    /**
     * Extracts url of entities from the project config corresponding to a given n-quads file.
     *
     * @param nqFile - n-quads file
     * @return local url string for the entities in the n-quads file
     */
    private String getLocalSourceUrlFromProjectCfg(File nqFile) {
        String url = "";
        try {
            File projectCfg = new File(nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS + EXT_GZ,
                    ImporterTask.PROJECT_CONFIG));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document cfg = builder.parse(projectCfg);
            NodeList server = cfg.getElementsByTagName("server");
            NodeList port = cfg.getElementsByTagName("port");
            NodeList sid = cfg.getElementsByTagName("port");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new JPSRuntimeException(e);
        }

        return url;
    }

    /**
     * Imports n-quads file into a running Blazegraph instance.
     *
     * @param nqFile - n-quads file
     * @param blasegraphImportURL - URL of the Blazegraph instance
     * @return  - information about import success
     */
    private boolean uploadNQuadsFileToBlazegraphInstance(File nqFile, String blasegraphImportURL) {
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
