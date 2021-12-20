package uk.ac.cam.cares.twa.cities;

import uk.ac.cam.cares.jps.base.query.sparql.PrefixToUrlMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrefixUtils {

  private static Map<String, String> prefixMap = new HashMap<>();
  private static final Pattern qualifiedNamePattern = Pattern.compile("([A-Za-z0-9]+):([A-Za-z0-9]+)");

  public static String insertPrefixStatements(String query) {
    // Find all prefixes referenced in the query
    Matcher matcher = qualifiedNamePattern.matcher(query);
    Set<String> prefixes = new HashSet<>();
    while(matcher.find()) prefixes.add(matcher.group(1));
    // Compile prefix statements
    StringBuilder prefixStatements = new StringBuilder();
    for (String prefix: prefixes) {
      String prefixStatement = getPrefixStatement(prefix);
      if(prefixStatement != null) prefixStatements.append(prefixStatement);
    }
    return prefixStatements + query;
  }

  public static String getPrefixStatement(String prefix) {
    String prefixUrl = getPrefixUrl(prefix);
    return prefixUrl == null ? null : String.format("PREFIX %s:<%s> \n", prefix, prefixUrl);
  }

  public static String getPrefixUrl(String prefix) {
    if(prefixMap.containsKey(prefix)) {
    } else if(PrefixToUrlMap.getPrefixUrl(prefix) != null){
      prefixMap.put(prefix, PrefixToUrlMap.getPrefixUrl(prefix));
    } else if(ResourceBundle.getBundle("config").containsKey("uri.prefix." + prefix)) {
      String uri = ResourceBundle.getBundle("config").getString("uri.prefix." + prefix);
      prefixMap.put(prefix, uri);
    } else {
      return null;
    }
    return prefixMap.get(prefix);
  }

  public static String expandQualifiedName(String expression) {
    String[] parts = expression.split(":", 2);
    if(parts.length > 1) {
      String prefixUrl = getPrefixUrl(parts[0]);
      if(prefixUrl != null) return prefixUrl + parts[1];
    }
    return expression;
  }

}
