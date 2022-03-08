package uk.ac.cam.cares.twa.cities.agents.geo;

import dev.jeka.core.api.file.JkPathTree;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.update.UpdateRequest;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.eclipse.jetty.server.Server;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.aws.AsynchronousWatcherService;
import uk.ac.cam.cares.jps.aws.CreateFileWatcher;
import uk.ac.cam.cares.jps.aws.WatcherCallback;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.util.CommandHelper;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;
import uk.ac.cam.cares.twa.cities.tasks.NquadsExporterTask;
import uk.ac.cam.cares.twa.cities.tasks.NquadsUploaderTask;


/**
 * A JPSAgent framework based 3D City Import Agent class used to import data into Semantic 3D City
 * Database {@link <a href="https://www.cares.cam.ac.uk/research/cities/">}, implemented as
 * Blazegraph backend and integrated into the Dynamic Geospatial Knowledge Graph of The World Avatar
 * project {@link <a href="https://www.theworldavatar.com/">}. The agent:
 * <p>
 * - listens to the presence of new files in a given directory - splits large city model files into
 * smaller chunks - imports chunks into local Blazegraph instances (for verification) - exports
 * locally imported data into n-quads format {@link <a https://www.w3.org/TR/n-quads/">} - imports
 * n-quads into target Blazegraph instance (The World Avatar) - archives audit trail
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
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
  public static final String KEY_SRID = "srid";
  public static final String KEY_SRSNAME = "srsName";
  public static final String SPLIT_SCRIPT = "citygml_splitter.py";
  public final int CHUNK_SIZE = 50;
  public static final int NUM_SERVER_THREADS = 2;
  //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
  public static final int NUM_IMPORTER_THREADS = 1;
  public static final String FS = System.getProperty("file.separator");
  public static final String CTYPE_SPARQLUPDATE = "application/sparql-update";
  private static final ExecutorService serverExecutor = Executors.newFixedThreadPool(NUM_SERVER_THREADS);
  private static final ExecutorService importerExecutor = Executors.newFixedThreadPool(
      NUM_IMPORTER_THREADS);
  private static final ExecutorService nqExportExecutor = Executors.newFixedThreadPool(NUM_SERVER_THREADS);
  private static final ExecutorService nqUploadExecutor = Executors.newFixedThreadPool(NUM_SERVER_THREADS);
  File splitDir;
  private String requestUrl;
  private String targetUrl;
  private File importDir;
  private String srid;
  private String srsname;

  public static final String OCGML_PREFIX = SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML.replace(":", "");
  public static final String OCGML_SCHEMA = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
  public static final String GRAPH_DATABASESRS = "/databasesrs/";
  public static final String QN_MARK = "?";
  public static final String SUB_SRID = "srid";
  public static final String SUB_SRSNAME = "srsname";
  public static final String OB_SRID = "currentSrid";
  public static final String OB_SRSNAME = "currentSrsname";

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    if (validateInput(requestParams)) {
      requestUrl = requestParams.getString(KEY_REQ_URL);
      targetUrl = requestParams.getString(KEY_TARGET_URL);
      // Ensure that targetUrl does not end with slash
      targetUrl = targetUrl.endsWith("/") ? targetUrl.substring(0, targetUrl.length()-1) : targetUrl;
      srid = requestParams.getString(KEY_SRID);
      srsname = requestParams.getString(KEY_SRSNAME);
      if (requestUrl.contains(URI_LISTEN)) {
        importDir = listenToImport(requestParams.getString(KEY_DIRECTORY));
      } else if (requestUrl.contains(URI_ACTION)) {
        String importedFiles = importFiles(new File(requestParams.getString(AsynchronousWatcherService.KEY_WATCH)));
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put("Imported Files", importedFiles);
        setDatabaseSrs();
        requestParams = jsonMessage;
      }
    }

    return requestParams;
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    boolean error = true;
    boolean errorSrs = true;
    if (!requestParams.isEmpty()) {
      Set<String> keys = requestParams.keySet();
      if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_REQ_URL) && keys.contains(
          KEY_TARGET_URL)) {
        if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
          try {
            URL reqUrl = new URL(requestParams.getString(KEY_REQ_URL));
            new URL(requestParams.getString(KEY_TARGET_URL));
            errorSrs = validateDatabaseSrsInput(requestParams, keys);
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

    if (error || errorSrs) {
      throw new BadRequestException();
    }

    return true;
  }

  /**
   * Validates input specific to requests commint to URI_LISTEN
   *
   * @param requestParams - request body in JSON format
   * @param keys          - request body keys
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
   * @param keys          - request body keys
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
   * Validates database srs inputs
   *
   * @param requestParams - request body in JSON format
   * @param keys          - request body keys
   * @return boolean saying if database srs is valid or not
   */
  private boolean validateDatabaseSrsInput(JSONObject requestParams, Set<String> keys) {
    boolean error = true;
    if ((keys.contains(KEY_SRID)) && (keys.contains(KEY_SRSNAME))) {
      String srid = requestParams.getString(KEY_SRID);
      String srsname = requestParams.getString(KEY_SRSNAME);
      try {
        Integer.parseInt(srid);
        if (!srsname.isEmpty()) {
          error = false;
        }
      } catch (NumberFormatException e) {
        throw new JPSRuntimeException(e);
      }
    }
    return error;
  }

  /**
   * Starts agent listening process to the new file appearing events in a given directory. Creates
   * import directory, if it does not exist. Asks Asynchronous Watching Service to start watching
   * the directory.
   *
   * @param directoryName - which directory to look for new files
   * @return directory watched for new files to import
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
      json.put(KEY_SRID, srid);
      json.put(KEY_SRSNAME, srsname);
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
   * @return - import summary
   */
  private String importFiles(File importDir) {
    StringBuilder imported = new StringBuilder();

    if (importDir != this.importDir) {
      this.importDir = importDir;
    }

    File[] dirContent = importDir.listFiles();

    if (Objects.requireNonNull(dirContent).length > 0) {
      for (File file : dirContent) {
        ArrayList<File> chunks = splitFile(file);
        for (File chunk : chunks) {
          try {
            importChunk(chunk);
            imported.append(chunk.getName()).append(" \n");
          } catch (Exception e) {
            throw new JPSRuntimeException(e);
          }
        }
      }
    }

    return imported.toString();
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
    if (!splitDir.exists() && !splitDir.mkdir()) {
      throw new JPSRuntimeException(new IOException(splitDir.getAbsolutePath()));
    }

    String fileSrc = file.getPath();
    String fileDst = splitDir.getPath() + FS + file.getName();

    try {
      ArrayList<String> args = new ArrayList<>();
      args.add("python");
      args.add(new File(
          Objects.requireNonNull(getClass().getClassLoader().getResource(SPLIT_SCRIPT)).toURI()).getAbsolutePath());
      args.add(fileDst);
      args.add(String.valueOf(CHUNK_SIZE));
      Files.move(Paths.get(fileSrc), Paths.get(fileDst)); //throws IOException
      System.out.println(args);
      CommandHelper.executeCommands(splitDir.getPath(), args);
      Iterator<File> files = Arrays.stream(Objects.requireNonNull(splitDir.listFiles())).iterator();

      while (files.hasNext()) {
        File splitFile = files.next();
        if (!splitFile.getPath().equals(fileDst)) {
          chunks.add(splitFile);
        }
      }
    } catch (IOException | URISyntaxException e) {
      throw new JPSRuntimeException(e);
    }

    return chunks;
  }

  /**
   * Imports CityGML chunk file (for verification and detection, if there are features not yet
   * implemented in ImpExp tool in the chunk)
   *
   * @param file- chunk to import
   */
  private void importChunk(File file) throws URISyntaxException {
    BlockingQueue<Server> localImportQueue = new LinkedBlockingDeque<>();
    BlockingQueue<File> remoteImportQueue = new LinkedBlockingDeque<>();
    if (!startBlazegraphInstance(localImportQueue, file.getAbsolutePath()).isRunning() ||
        !importToLocalBlazegraphInstance(localImportQueue, file).isRunning() ||
        !exportToNquads(remoteImportQueue, file).isRunning() ||
        !uploadNQuadsFileToBlazegraphInstance(remoteImportQueue, new URI(targetUrl)).isRunning()) {
      throw new JPSRuntimeException(new Exception(CityImportAgent.class.getName()));
    }

  }

  /**
   * Starts local Blazegraph NanoSparqlServer instance.
   *
   * @param queue - Blocking Queue to place the started server on so it can be picked up
   *              by other tasks.
   * @param filepath - path to target journal file for the server instance.
   * @return - running BlazegraphServerTask
   */
  private BlazegraphServerTask startBlazegraphInstance(BlockingQueue<Server> queue,
      String filepath) {
    BlazegraphServerTask task = new BlazegraphServerTask(queue,
        filepath.replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_JNL));
    serverExecutor.execute(task);

    return task;
  }

  /**
   * Imports CityGML file into local Blazegraph instance.
   *
   * @param queue - queue for instances of Blazegraph NanoSparqlServer
   * @param file  - file to import
   * @return ImporterTask - running task
   */
  private ImporterTask importToLocalBlazegraphInstance(BlockingQueue<Server> queue, File file) {
    ImporterTask task = new ImporterTask(queue, file);
    importerExecutor.execute(task);

    return task;
  }

  /**
   * Exports Blazegraph's journal file to N-Quads.
   *
   * @param queue queue for files to be picked up by other tasks.
   * @param file  - file to export
   * @return NquadsExporterTask - running task
   */
  private NquadsExporterTask exportToNquads(BlockingQueue<File> queue, File file) {
    NquadsExporterTask task = new NquadsExporterTask(queue, file, targetUrl);
    nqExportExecutor.execute(task);

    return task;
  }

  /**
   * Imports n-quads file into a running Blazegraph instance.
   *
   * @param queue               - n-quads file queue
   * @param blazegraphImportUri - URI of the target Blazegraph instance
   * @return - running NquadsUploaderTask
   */
  private NquadsUploaderTask uploadNQuadsFileToBlazegraphInstance(BlockingQueue<File> queue,
      URI blazegraphImportUri) {
    NquadsUploaderTask task = new NquadsUploaderTask(queue, blazegraphImportUri);
    nqUploadExecutor.execute(task);

    return task;
  }

  /**
   * Performs POST request to execute a SPARQL update at targetUrl endpoint. The SPARQL update:
   * 1. Creates a named graph 'databasesrs' and adds srid and srsname as triples if 'databasesrs' does not exist, or
   * 2. Replaces existing srid and srsname triples in an existing 'databasesrs' named graph
   */
  private void setDatabaseSrs() {
    String update = getSetDatabaseSrsUpdate().toString();
    try { HttpResponse<?> response = Unirest.post(targetUrl)
            .header(HTTP.CONTENT_TYPE, CTYPE_SPARQLUPDATE)
            .body(update)
            .socketTimeout(300000)
            .asEmpty();
        int respStatus = response.getStatus();
        if (respStatus != HttpURLConnection.HTTP_OK) {
          throw new HttpException(targetUrl + " " + respStatus);
        }
    } catch ( HttpException | UnirestException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Builds the SPARQL update statement for setDatabaseSrs
   *
   * @return UpdateRequest - update statement
   */
  private UpdateRequest getSetDatabaseSrsUpdate() {
    UpdateBuilder builder = new UpdateBuilder()
            .addPrefix(OCGML_PREFIX, OCGML_SCHEMA)
            .with(NodeFactory.createURI(targetUrl + GRAPH_DATABASESRS))
            .addDelete(QN_MARK + SUB_SRID, SchemaManagerAdapter.ONTO_SRID, QN_MARK + OB_SRID)
            .addDelete(QN_MARK + SUB_SRSNAME, SchemaManagerAdapter.ONTO_SRSNAME, QN_MARK + OB_SRSNAME)
            .addInsert(NodeFactory.createURI(targetUrl + "/"), SchemaManagerAdapter.ONTO_SRID, srid)
            .addInsert(NodeFactory.createURI(targetUrl + "/"),SchemaManagerAdapter.ONTO_SRSNAME, srsname)
            .addOptional(QN_MARK + SUB_SRID, SchemaManagerAdapter.ONTO_SRID, QN_MARK + OB_SRID)
            .addOptional(QN_MARK + SUB_SRSNAME, SchemaManagerAdapter.ONTO_SRSNAME, QN_MARK + OB_SRSNAME);
    UpdateRequest update = builder.buildRequest();
    return update;
  }

  /**
   * Creates an audit trail archive chunk .gml and .nq files generated during the import process.
   *
   * @param nqFile - N-Quads file which is checked to be the last one of the currently imported chunks.
   * @return - path to the audit trail archive.
   */
  public static String archiveImportFiles(File nqFile) {
    String archiveFilename = "";

    File nqDir = nqFile.getParentFile();
    if (nqDir.exists()) {
      File[] gmlFiles = nqDir.listFiles((dir, name) -> name.toLowerCase()
          .endsWith(ImporterTask.EXT_FILE_GML));
      File[] nqFiles = nqDir.listFiles((dir, name) -> name.toLowerCase()
          .endsWith(ImporterTask.EXT_FILE_NQUADS));

      if (Objects.requireNonNull(nqFiles).length > 0 && nqFiles.length == Objects.requireNonNull(
          gmlFiles).length - 1) {

          Path dirToZip = nqDir.toPath();
          Path zipFile = Paths
              .get(nqDir.getParent() + FS +nqDir.getName() + NquadsExporterTask.EXT_FILE_ZIP);
          JkPathTree.of(dirToZip).zipTo(zipFile);
          archiveFilename = zipFile.toString();
      }
    }

    return archiveFilename;
  }

}
