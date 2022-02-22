package uk.ac.cam.cares.twa.cities.tasks;

import com.opencsv.CSVReader;
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
import java.util.List;
import org.apache.jena.base.Sys;
import org.openrdf.query.algebra.Str;
import uk.ac.cam.cares.twa.cities.model.geo.KmlTiling;

public class JSONSorterTask {

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


  public static void main(String[] args) {

    String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\after_tiles_assignment\\";
    File directoryPath = new File(inputDir);
    String[] filelist = directoryPath.list();

    List<String[]> csvData = new ArrayList<>();
    List<String[]> csvList = new ArrayList<>();
    int numRow = 0;
    long start = System.currentTimeMillis();

    for (String inputfile : filelist) {
      try (CSVReader reader = new CSVReader(new FileReader(inputDir + inputfile))) {
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
        "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\" + "sorted_tiles_summary.csv"))) {
      writer.writeAll(csvList);

    } catch (IOException e) {
      e.printStackTrace();
    }
    
  }
}
