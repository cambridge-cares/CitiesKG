package uk.ac.cam.cares.twa.cities.tasks;

import com.bigdata.rdf.sail.ExportKB;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

/**
 * Runnable task exporting an OntoCityGML model to N-Quads format {@link
 * <https://www.w3.org/TR/n-quads/>}. The model is exported from Blazegraph's journal file using
 * {@link ExportKB} and placed into the {@link BlockingQueue} to be picked up by any other tasks.
 * Local URLs are replaced in the target N-Quads file to make it ready for upload to the target
 * instance of Blazegraph with different URLs. All the helper files are removed by the methods in
 * this class as well. The task stops itself after the export process is finished and the target
 * N-Quads file is placed in the queue.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class NquadsExporterTask implements Runnable {

  public static final String ARG_OUTDIR = "-outdir";
  public static final String ARG_FORMAT = "-format";
  public static final String NQ_OUTDIR = "quads";
  public static final String NQ_FORMAT = "N-Quads";
  public static final String NQ_FILENAME = "data.nq.gz";
  public static final String EX_PROP_FILENAME = "kb.properties";
  public static final String EXT_GZ = ".gz";
  public static final String EXT_FILE_ZIP = ".zip";
  public static final String CFG_KEY_SERVER = "server";
  public static final String CFG_KEY_PORT = "port";
  public static final String CFG_KEY_SID = "sid";
  private final String FS = System.getProperty("file.separator");
  private final BlockingQueue<File> nqQueue;
  private final File importFile;
  private final String targetUrl;
  private Boolean stop = false;

  public NquadsExporterTask(BlockingQueue<File> nqQueue, File importFile, String targetUrl) {
    this.nqQueue = nqQueue;
    this.importFile = importFile;
    this.targetUrl = targetUrl;
  }

  public boolean isRunning() {
    return !stop;
  }

  public void stop() {
    stop = true;
  }

  @Override
  public void run() {
    File nqFile = new File(importFile.getAbsolutePath()
        .replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_NQUADS));
    while (isRunning()) {
      if (nqFile.isFile()) {
        try {
          File targetGzNqFile = exportToNquadsFileFromJnlFile(nqFile);
          String sourceUrl = getLocalSourceUrlFromProjectCfg(targetGzNqFile);
          File targetNqFile = changeUrlsInNQuadsFile(targetGzNqFile, sourceUrl, targetUrl);
          nqQueue.put(targetNqFile);
        } catch (InterruptedException | IOException e) {
          throw new JPSRuntimeException(e);
        }
        stop();
      }
    }
  }

  /**
   * Exports individual journal file to n-quads file. Removes helper files after that.
   *
   * @param nqFile empty N-quads input file identifying finished journal file
   * @return target N-quads file
   */
  private File exportToNquadsFileFromJnlFile(File nqFile) {
    File jnlFile = new File(
        nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS, ImporterTask.EXT_FILE_JNL));
    String nqDir = nqFile.getParent() + FS + NQ_OUTDIR;
    String propFilePath = jnlFile.getAbsolutePath()
        .replace(ImporterTask.EXT_FILE_JNL, BlazegraphServerTask.PROPERTY_FILE);
    String[] args = {ARG_OUTDIR, nqDir,
        ARG_FORMAT, NQ_FORMAT,
        propFilePath};
    File targetNqFile = new File(nqFile.getAbsolutePath() + EXT_GZ);
    try {
      ExportKB.main(args);
      File exportedNqFile = new File(
          nqDir + FS + BlazegraphServerTask.NAMESPACE + FS + NQ_FILENAME);
      File exportedPropFile = new File(
          nqDir + FS + BlazegraphServerTask.NAMESPACE + FS + EX_PROP_FILENAME);
      File propFile = new File(propFilePath);

      if (!exportedNqFile.renameTo(targetNqFile) ||
          !exportedPropFile.delete() ||
          !nqFile.delete() ||
          !jnlFile.delete() ||
          !propFile.delete()) {
        throw new IOException();
      }
    } catch (Exception e) {
      throw new JPSRuntimeException(e);
    }

    return targetNqFile;
  }

  /**
   * Extracts url of entities from the project config corresponding to a given n-quads file.
   *
   * @param nqFile - n-quads file
   * @return local url string for the entities in the n-quads file
   */
  private String getLocalSourceUrlFromProjectCfg(File nqFile) {
    String url = "/";
    try {
      File projectCfg = new File(
          nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS + EXT_GZ,
              ImporterTask.PROJECT_CONFIG));
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document cfg = builder.parse(projectCfg);
      String server = cfg.getElementsByTagName(CFG_KEY_SERVER).item(0).getTextContent();
      String port = cfg.getElementsByTagName(CFG_KEY_PORT).item(0).getTextContent();
      String sid = cfg.getElementsByTagName(CFG_KEY_SID).item(0).getTextContent();
      url = org.apache.http.HttpHost.DEFAULT_SCHEME_NAME.concat(":/").concat(
          url.concat(server).concat(":").concat(port).concat(sid));
      File cfgFile = new File(
          nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS + EXT_GZ,
              ImporterTask.PROJECT_CONFIG));
      if (!cfgFile.delete()) {
        throw new IOException();
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new JPSRuntimeException(e);
    }

    return url;
  }

  /**
   * Find and replace on n-quads files to prepare them to contain URLs of the target system instead
   * of the local instance.
   *
   * @param nqFile - n-quads file to replace URLs in
   * @param from   - string to replace
   * @param to     - string to replace with
   * @return - target N-Quads file with replaced URLs
   */
  private File changeUrlsInNQuadsFile(File nqFile, String from, String to) throws IOException {
    File targetNqFile = new File(
        nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS + EXT_GZ,
            ImporterTask.EXT_FILE_NQUADS));
    GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(nqFile));
    InputStreamReader reader = new InputStreamReader(gzis);
    BufferedReader in = new BufferedReader(reader);
    Iterator<String> it = in.lines().iterator();
    FileOutputStream fos = new FileOutputStream(targetNqFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos);
    BufferedWriter bw = new BufferedWriter(osw);
    String line;
    String replaced;

    while (it.hasNext()) {
      try {
        line = it.next();
      } catch (NoSuchElementException e) {
        throw new JPSRuntimeException(e);
      }
      replaced = line.replaceAll(from, to + "/");
      bw.write(replaced);
      bw.newLine();
    }

    bw.close();
    osw.close();

    return targetNqFile;
  }

}
