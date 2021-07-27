package uk.ac.cam.cares.twa.cities.tasks;

import com.bigdata.rdf.sail.ExportKB;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;

public class NquadsExporterTask implements Runnable {
    public static final String ARG_OUTDIR = "-outdir";
    public static final String ARG_FORMAT = "-format";
    public static final String NQ_OUTDIR = "quads";
    public static final String NQ_FORMAT = "N-Quads";
    public static final String NQ_FILENAME = "data.nq.gz";
    public static final String EXT_GZ = ".gz";
    private final String FS = System.getProperty("file.separator");
    private Boolean stop = false;
    private final BlockingQueue<File>  nqQueue;
    private final File importFile;
    private final String  targetUrl;

    public NquadsExporterTask(BlockingQueue<File> nqQueue, File importFile, String targetUrl) {
        this.nqQueue = nqQueue;
        this.importFile = importFile;
        this.targetUrl = targetUrl;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        File nqFile = new File(importFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_NQUADS));
        while (!stop) {
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
     * @param nqFile
     * @return
     */
    private File exportToNquadsFileFromJnlFile(File nqFile) {
        File jnlFile = new File(nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS, ImporterTask.EXT_FILE_JNL));
        String nqDir = nqFile.getParent() + FS + NQ_OUTDIR;
        String propFilePath = jnlFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_JNL, BlazegraphServerTask.PROPERTY_FILE);
        String[] args = {ARG_OUTDIR, nqDir,
                ARG_FORMAT, NQ_FORMAT,
                propFilePath};
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
        jnlFile.delete();
        new File(propFilePath).delete();

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
            File projectCfg = new File(nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS + EXT_GZ,
                    ImporterTask.PROJECT_CONFIG));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document cfg = builder.parse(projectCfg);
            String server = cfg.getElementsByTagName("server").item(0).getTextContent();
            String port = cfg.getElementsByTagName("port").item(0).getTextContent();
            String sid = cfg.getElementsByTagName("sid").item(0).getTextContent();
            url = org.apache.http.HttpHost.DEFAULT_SCHEME_NAME.concat(":/").concat(
                    url.concat(server).concat(":").concat(port).concat(sid));
            new File(nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS + EXT_GZ,
                    ImporterTask.PROJECT_CONFIG)).delete();

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new JPSRuntimeException(e);
        }

        return url;
    }

    /**
     * Find and replace on n-quads files to prepare them to contain URLs of the target system
     * instead of the local instance.
     *
     * @param nqFile - n-quads file to replace URLs in
     * @param from - string to replace
     * @param to - string to replace with
     * @return - target N-Quads file with replaced URLs
     */
    private File changeUrlsInNQuadsFile(File nqFile, String from, String to) throws IOException {
        File targetNqFile  = new File(nqFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_NQUADS + EXT_GZ,
                ImporterTask.EXT_FILE_NQUADS));

        GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(nqFile));
        InputStreamReader reader = new InputStreamReader(gzis);
        BufferedReader in = new BufferedReader(reader);
        FileOutputStream fos = new FileOutputStream(targetNqFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        BufferedWriter bw = new BufferedWriter(osw);

        String readed;
        while ((readed = in.readLine()) != null) {
            bw.write(readed.replaceAll(from, to));
            bw.newLine();
        }

        osw.close();
        nqFile.delete();

        return targetNqFile;
    }

}
