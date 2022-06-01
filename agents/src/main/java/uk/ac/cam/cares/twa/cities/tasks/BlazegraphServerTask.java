package uk.ac.cam.cares.twa.cities.tasks;

import com.bigdata.journal.IIndexManager;
import com.bigdata.journal.Journal;
import com.bigdata.rdf.sail.webapp.ConfigParams;
import com.bigdata.rdf.sail.webapp.NanoSparqlServer;
import com.bigdata.rdf.sail.webapp.StandaloneNanoSparqlServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import org.eclipse.jetty.server.Server;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Runnable task instantiating Blazegraph's {@link StandaloneNanoSparqlServer} and placing the
 * running instance in the {@link BlockingQueue} fot the other tasks to pick up when it is ready to
 * start working with it. The server is destroyed after being stopped by other tasks and this task
 * is stopped after that as well. All the necessary configuration files and system variables
 * required to start the server instance are set up by methods of this class.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class BlazegraphServerTask implements Runnable {

  public static final String PROPERTY_FILE = "RWStore.properties";
  public static final String PROPERTY_FILE_PATH = "../../../../../../../";
  public static final String JETTY_CFG_PATH = "jetty.xml";
  public static final String WAR_PATH = "war";
  public static final String NSS_PATH = "com/bigdata/rdf/sail/webapp/";
  public static final String SYS_PROP_JETTY = "jetty.home";
  public static final String NAMESPACE = "tmpkb";
  public static final String DEF_JOURNAL_NAME = "citiesKG.jnl";
  private final String FS = System.getProperty("file.separator");
  private final String journalPath;
  private final BlockingQueue<Server> queue;
  private Boolean stop = false;
  private Server server = null;
  public IIndexManager indexManager = null;

  public BlazegraphServerTask(BlockingQueue<Server> queue, String journalName) {
    this.queue = queue;
    this.journalPath = journalName;
  }

  public boolean isRunning() {
    return !stop;
  }

  public void stop() {
    stop = true;
  }

  @Override
  public void run() {
    while (isRunning()) {
      if (server == null) {
        try {
          String propFileAbsPath = setupPaths();
          File propFile = setupFiles(propFileAbsPath);
          String jettyXml = setupSystem(propFile.getAbsolutePath());
          server = setupServer(propFile.getAbsolutePath(), jettyXml);
          StandaloneNanoSparqlServer.awaitServerStart(server);
          queue.put(server);
        } catch (Exception e) {
          throw new JPSRuntimeException(e);
        }
      } else if (server.isStopped()) {
        ((Journal) indexManager).shutdownNow();
        server.destroy();
        stop();
      }
    }
  }

  /**
   * Creates path to a local Blazergaph RWStore.properties file based on the journal filename.
   *
   * @return - properties file path
   */
  private String setupPaths() {
    File journalFile = new File(journalPath);
    String propFileName = journalFile.getName().split("\\.")[0] + PROPERTY_FILE;
    String propFilePath = journalFile.getParent();

    return propFilePath + FS + propFileName;
  }

  /**
   * Creates local Blazergaph RWStore.properties file with appropriate journal file path variable.
   *
   * @param propFileAbsPath - path to a target properties file
   * @return - target properties file
   * @throws URISyntaxException - when properties file path could not be converted to URI
   * @throws IOException        - when read/write operations on files fail
   */
  private File setupFiles(String propFileAbsPath) throws URISyntaxException, IOException {
    Files.copy(Paths.get(
            Objects.requireNonNull(getClass().getResource(PROPERTY_FILE_PATH + PROPERTY_FILE)).toURI()),
        Paths.get(propFileAbsPath), REPLACE_EXISTING);  // Note Files.copy raises errors when the file on the destination exists
    Properties properties = new Properties();
    FileInputStream fin = new FileInputStream(propFileAbsPath);  // Save the filestream in a variable and close it properly after usage
    properties.load(fin);
    fin.close();
    properties.setProperty("com.bigdata.journal.AbstractJournal.file", new File(journalPath).getAbsolutePath());
    FileOutputStream fout = new FileOutputStream(propFileAbsPath);
    properties.store(fout, null);
    fout.close();
    File propFile = new File(propFileAbsPath);
    /*String data = FileUtils.readFileToString(propFile, String.valueOf(Charset.defaultCharset()));
    data = data.replace(DEF_JOURNAL_NAME, journalPath);
    FileUtils.writeStringToFile(propFile, data);*/

    return propFile;
  }

  /**
   * Sets system properties required to instantiate Blazegraph.
   *
   * @param propFileAbsPath - path to a target properties file
   * @return - jetty config path
   */
  private String setupSystem(String propFileAbsPath) {
    String jettyXml = Objects.requireNonNull(NanoSparqlServer.class.getResource(""))
        .toExternalForm()
        .replace(NSS_PATH, JETTY_CFG_PATH);
    String war = Objects.requireNonNull(NanoSparqlServer.class.getResource("")).toExternalForm()
        .replace(NSS_PATH, WAR_PATH);
    System.setProperty(SYS_PROP_JETTY, war);
    System.setProperty(NanoSparqlServer.SystemProperties.JETTY_XML, jettyXml);
    System.setProperty(NanoSparqlServer.SystemProperties.BIGDATA_PROPERTY_FILE, propFileAbsPath);

    return jettyXml;
  }

  /**
   * Creates an instance of StandaloneNanoSparqlServer.
   *
   * @param propFileAbsPath - path to a target properties file
   * @param jettyXml        - jetty config path
   * @return - instance of local StandaloneNanoSparqlServer
   * @throws Exception - when creating NanoSparqlServer instance fails
   */
  private Server setupServer(String propFileAbsPath, String jettyXml) throws Exception {
    LinkedHashMap<String, String> initParams = new LinkedHashMap<>();
    initParams.put(ConfigParams.PROPERTY_FILE, propFileAbsPath);
    initParams.put(ConfigParams.NAMESPACE, NAMESPACE);

    Properties properties = new Properties();
    FileInputStream fin = new FileInputStream(propFileAbsPath);  // Save the filestream in a variable and close it properly after usage
    properties.load(fin);
    fin.close();
    indexManager = new Journal(properties);

    return StandaloneNanoSparqlServer.newInstance(0, jettyXml, indexManager, initParams);
  }

}