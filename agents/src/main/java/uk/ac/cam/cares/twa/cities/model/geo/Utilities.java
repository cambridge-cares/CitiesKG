package uk.ac.cam.cares.twa.cities.model.geo;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utilities {

  private String[] filesList;
  private File currFile;
  private String outCsvFile = "sorted_summary";
  private String outFileExt = ".csv";
  private String outputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\";
  private String inputDir;

  public Utilities(String path) {

    this.filesList = getInputFiles(path);
    this.inputDir = getInputDir(path);
  }

  public static String[] getInputFiles(String path){
    File inputPath = new File(path);
    String[] filesList = null;

    if (inputPath.isFile()) {
      filesList = new String[1];
      filesList[0] = inputPath.getAbsolutePath();
    }
    else if (inputPath.isDirectory()) {
      File[] files = inputPath.listFiles();
      filesList = new String[inputPath.list().length];

      for (int i = 0; i < inputPath.list().length; ++i){
          filesList[i] = files[i].getAbsolutePath();
      }

    } else {
      System.out.println("Utilities.getInputFiles: The input does not exists!");
    }
    return filesList;
  }

  public static String getInputDir(String path){
    File inputPath = new File(path);
    String inputDir = null;

    if (inputPath.isFile()) {
      inputDir = inputPath.getParent();
    }
    else if (inputPath.isDirectory()) {
      inputDir = inputPath.getPath();
    } else {
      System.out.println("Utilities.getInputDir: The input does not exists!");
    }
    return inputDir;
  }

  public static HashMap convertList2Map(List<String[]> inputList){
    HashMap<String, String[]> csvMap = new HashMap<String, String[]>();

    for (String[] row : inputList){
      csvMap.put(row[0], row);
    }
    return csvMap;
  }

  public static int[] str2int(String inputstr){
    int[] output = new int[2];
    String[] splitStr = inputstr.split("#");
    output[0] = Integer.valueOf(splitStr[0]);
    output[1] = Integer.valueOf(splitStr[1]);  
    return output;
  }

  public static <T> String arr2str(T[] arr) {
    String output = "";
    String sep = "#";

    for (int j = 0; j < arr.length; j++) {
      output += String.valueOf(arr[j]);
      if (j != arr.length - 1) {
        output += sep;
      }
    }

    return output;
  }

  public static boolean isUnique (List<String> itemList) {
    Set<String> itemSet = new HashSet<>(itemList);
    boolean unique = false;
    if (itemSet.size() < itemList.size()) {
      unique = false;
    } else if (itemSet.size() == itemList.size()) {
      unique = true;
    }
    return unique;
  }

  public static void main(String[] args) {

    //String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\after_tiles_assignment\\";
    String inputFile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder_1\\tiles_summary.csv";
    Utilities jsonSorter = new Utilities(inputFile);
    File directoryPath = new File(inputFile);
    String[] filelist = directoryPath.list();

    List<String[]> csvData = new ArrayList<>();
    List<String[]> csvList = new ArrayList<>();
    int numRow = 0;
    long start = System.currentTimeMillis();

    CSVParser csvParser = new CSVParserBuilder().withEscapeChar('\0').build();
    for (String inputfile : jsonSorter.filesList) {
      try (CSVReader reader = new CSVReaderBuilder(new FileReader(jsonSorter.inputDir + "/" + inputfile)).withCSVParser(csvParser).build()) {
        String[] header = reader.readNext();
        csvData = reader.readAll();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (CsvException e) {
        e.printStackTrace();
      }
      numRow += csvData.size();
      csvList.addAll(csvData);
    }
    long finish = System.currentTimeMillis();
    System.out.println(numRow + " rows are processed.");
    System.out.println("Total reading time: " + (finish-start));


    Collections.sort(csvList, new Comparator<String[]>()
    {
      public int compare(String[] o1, String[] o2)
      {
        //compare two object and return an integer
        return o1[3].compareTo(o2[3]);}

      }
    );
//prints the sorted HashMap
    //HashMap<String, String[]> csvmap = JSONSorterTask.convertList2Map(csvList);
    System.out.println(csvList.get(3));

    try (CSVWriter writer = new CSVWriter(new FileWriter(
        "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\" + "sorted_tiles_summary.csv"))) {
      String[] header = {"gmlid", "envelope", "envelopeCentroid", "tiles", "filename"};
      writer.writeNext(header);
      writer.writeAll(csvList);

    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
