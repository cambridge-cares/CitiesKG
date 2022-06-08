package uk.ac.cam.cares.twa.cities.tasks;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.io.FileUtils;
import org.citydb.ImpExp;
import org.eclipse.jetty.server.Server;
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
import uk.ac.cam.cares.twa.cities.agents.geo.CityExportAgent.Params;


public class ExporterExpTask implements Runnable {

  public static final String PROJECT_CONFIG = "project.xml";  // project config template
  public static final String EXT_FILE_KML = ".kml";
  public static final String PLACEHOLDER_GMLID = "{{gmlid}}";
  public static final String PLACEHOLDER_HOST = "{{host}}";
  public static final String PLACEHOLDER_PORT = "{{port}}";
  public static final String PLACEHOLDER_LOD = "{{lod}}";
  public static final String PLACEHOLDER_FOOTPRINT = "{{display1}}";
  public static final String PLACEHOLDER_EXTRUDED = "{{display2}}";
  public static final String PLACEHOLDER_GEOMETRY = "{{display3}}";
  public static final String PLACEHOLDER_COLLADA = "{{display4}}";
  public static final String PLACEHOLDER_NS = "{{namespace}}";
  public static final String ARG_SHELL = "-shell";
  public static final String ARG_KMLEXPORT = "-kmlExport";
  public static final String ARG_CFG = "-config";

  private final int lod;
  private Boolean stop = false;
  private final BlockingQueue<Params> taskParamsQueue;


  public ExporterExpTask(BlockingQueue<Params> taskParamsQueue) {
    this.taskParamsQueue = taskParamsQueue;
    this.lod = 5;   // by default: highest lod available
  }

  public boolean getStopStatus() {return stop;}

  public void stop() {
    stop = true;
  }

  public boolean isRunning() {
    return !stop;
  }

  @Override
  public void run() {
    File cfgfile;
    while (isRunning()) {
      try {
        while (!taskParamsQueue.isEmpty()) {
          Params taskParams = taskParamsQueue.take();
          cfgfile = setupConfig(taskParams);  // modify the gmlIds within the config file
          String[] args = {ARG_SHELL, ARG_KMLEXPORT, taskParams.outputPath,
              ARG_CFG,
              cfgfile.getAbsolutePath()}; // <-kmlExport filename> can take path with blank space, quoted with "" mark and <-kmlExport=filename> can only accept path without blank space
          ImpExp.main(args);
        }
      } catch (IOException | URISyntaxException | InterruptedException e) {
        e.printStackTrace();
        throw new JPSRuntimeException(e);
      } finally {
        System.out.println("Task Completed! Thread Name: " + Thread.currentThread().getName());
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
  private File setupConfig(Params taskparams) throws IOException, URISyntaxException {

    // Copy the template to the location of output
    File exportFile = new File(taskparams.outputPath);

    String projectCfg = exportFile.getAbsolutePath().replace(EXT_FILE_KML, "_" + PROJECT_CONFIG);
    Files.copy(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(PROJECT_CONFIG)).toURI()),
        Paths.get(projectCfg), StandardCopyOption.REPLACE_EXISTING);

    File cfgFile = new File(projectCfg);
    // Change the server info
    String cfgData = FileUtils.readFileToString(cfgFile, String.valueOf(Charset.defaultCharset()));
    cfgData = cfgData.replace(PLACEHOLDER_GMLID, createGmlids(taskparams.gmlIds));
    cfgData = cfgData.replace(PLACEHOLDER_HOST, String.valueOf(taskparams.serverInfo.get("host")));
    cfgData = cfgData.replace(PLACEHOLDER_PORT, String.valueOf(taskparams.serverInfo.get("port")));
    cfgData = cfgData.replace(PLACEHOLDER_NS, String.valueOf(taskparams.serverInfo.get("namespace")));

    // Change the lod in config file, by default: highest lod available = 5
    cfgData = cfgData.replace(PLACEHOLDER_LOD, String.valueOf(this.lod));
    // Change the displayform in config file
    cfgData = cfgData.replace(PLACEHOLDER_FOOTPRINT, String.valueOf(taskparams.displayMode[0]));
    cfgData = cfgData.replace(PLACEHOLDER_EXTRUDED, String.valueOf(taskparams.displayMode[1]));
    cfgData = cfgData.replace(PLACEHOLDER_GEOMETRY, String.valueOf(taskparams.displayMode[2]));
    cfgData = cfgData.replace(PLACEHOLDER_COLLADA, String.valueOf(taskparams.displayMode[3]));

    // Save the modified cfgFile
    FileUtils.writeStringToFile(cfgFile, cfgData);
    return cfgFile;
  }

  private String createGmlids (String[] gmlids) {

    StringBuilder sb = new StringBuilder();
    for (String gmlid : gmlids){
      sb.append("<id>");
      sb.append(gmlid);
      sb.append("</id>\n");
    }
    return sb.toString();
  }

}
