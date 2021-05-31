package org.citydb.database.adapter.blazegraph;

/*
//import de.hsmainz.cs.semgis.arqextension.geometry.attribute.Area;
//import de.hsmainz.cs.semgis.arqextension.geometry.relation.Union;
//import de.hsmainz.cs.semgis.arqextension.geometry.transform.Force2D;
import io.github.galbiston.geosparql_jena.implementation.GeometryWrapperFactory;
import io.github.galbiston.geosparql_jena.implementation.datatype.WKTDatatype;
import io.github.galbiston.geosparql_jena.implementation.vocabulary.SRS_URI;
import io.github.galbiston.geosparql_jena.spatial.filter_functions.TransformSRSFF;
import org.apache.jena.sparql.expr.NodeValue;
import org.locationtech.jts.geom.Coordinate;

import java.util.LinkedList;
import java.util.List;

public class GeoJena {
    public static void main(String[] args){
        String st_geometry1 = "82106.765 454221.659 -0.526,82107.759 454227.024 -0.526,82108.99 454233.752 -0.526,82109.056 454234.11 -0.526," +
                "82109.301 454235.449 -0.526,82110.361 454241.194 -0.526,82110.597 454242.533 -0.526,82111.667 454248.272 -0.526,82111.79 454248.937 -0.526," +
                "82111.586 454248.97 -0.526,82112.523 454254.297 -0.526,82112.779 454254.933 -0.526,82113.133 454255.521 -0.526,82113.576 454256.045 -0.526," +
                "82114.095 454256.492 -0.526,82114.679 454256.852 -0.526,82115.312 454257.115 -0.526,82115.979 454257.275 -0.526,82116.679 454257.347 -0.526," +
                "82117.38 454257.306 -0.526,82118.066 454257.153 -0.526,82118.719 454256.891 -0.526,82119.321 454256.528 -0.526,82119.857 454256.073 -0.526," +
                "82120.313 454255.538 -0.526,82120.672 454254.897 -0.526,82120.41 454254.77 -0.526,82139.425 454221.057 -0.526,82139.823 454220.351 -0.526," +
                "82140.059 454219.694 -0.526,82140.175 454219.006 -0.526,82140.168 454218.309 -0.526,82140.038 454217.623 -0.526,82139.79 454216.971 -0.526," +
                "82139.43 454216.373 -0.526,82138.971 454215.848 -0.526,82138.429 454215.447 -0.526,82137.847 454215.151 -0.526,82137.222 454214.961 -0.526," +
                "82136.574 454214.882 -0.526,82135.922 454214.916 -0.526,82134.585 454215.166 -0.526,82132.371 454215.483 -0.526,82131.031 454215.731 -0.526," +
                "82130.68 454215.789 -0.526,82125.286 454216.795 -0.526,82123.94 454217.036 -0.526,82118.21 454218.086 -0.526,82116.869 454218.345 -0.526," +
                "82111.134 454219.407 -0.526,82109.79 454219.651 -0.526,82107.919 454219.98 -0.526,82106.507 454220.236 -0.526,82106.765 454221.659 -0.526";

        String st_geometry2 = "82107.919 454219.98 130.672,82109.79 454219.651 130.672,82111.134 454219.407 130.672,82116.869 454218.345 130.672,82118.21 454218.086 130.672," +
                "82123.94 454217.036 130.672,82125.286 454216.795 130.672,82130.68 454215.789 130.672,82131.031 454215.731 130.672,82132.371 454215.483 130.672,82134.585 454215.166 130.672," +
                "82135.922 454214.916 130.672,82136.574 454214.882 130.672,82137.222 454214.961 130.672,82137.847 454215.151 130.672,82138.429 454215.447 130.672,82138.971 454215.848 130.672," +
                "82139.43 454216.373 130.672,82139.79 454216.971 130.672,82140.038 454217.623 130.672,82140.168 454218.309 130.672,82140.175 454219.006 130.672,82140.059 454219.694 130.672," +
                "82139.823 454220.351 130.672,82139.425 454221.057 130.672,82120.41 454254.77 130.672,82120.672 454254.897 130.672,82120.313 454255.538 130.672,82119.857 454256.073 130.672," +
                "82119.321 454256.528 130.672,82118.719 454256.891 130.672,82118.066 454257.153 130.672,82117.38 454257.306 130.672,82116.679 454257.347 130.672,82115.979 454257.275 130.672," +
                "82115.312 454257.115 130.672,82114.679 454256.852 130.672,82114.095 454256.492 130.672,82113.576 454256.045 130.672,82113.133 454255.521 130.672,82112.779 454254.933 130.672," +
                "82112.523 454254.297 130.672,82111.586 454248.97 130.672,82111.79 454248.937 130.672,82111.667 454248.272 130.672,82110.597 454242.533 130.672,82110.361 454241.194 130.672," +
                "82109.301 454235.449 130.672,82109.056 454234.11 130.672,82108.99 454233.752 130.672,82107.759 454227.024 130.672,82106.765 454221.659 130.672,82106.507 454220.236 130.672," +
                "82107.919 454219.98 130.672";

        NodeValue srsgeom = createNode(st_geometry1);
        NodeValue dstSrid = NodeValue.makeString(SRS_URI.DEFAULT_WKT_CRS84);
        TransformSRSFF instance = new TransformSRSFF();
        NodeValue result = instance.exec(srsgeom, dstSrid);
        System.out.println(result.toString());
        //assertEquals(srsgeom, result);

/*
        // Force2D
        Force2D instance1 = new Force2D();
        NodeValue result1 = instance1.exec(srsgeom);
        System.out.println(result1.toString());

        // AREA
        Area instance2 = new Area();
        NodeValue result2 = instance2.exec(srsgeom);
        System.out.println(result2.toString());

        // ST_UNION
        NodeValue v1geom = createNode(st_geometry1);
        NodeValue v2geom = createNode(st_geometry2);

        NodeValue v1geom2D = instance1.exec(v1geom);
        NodeValue v2geom2D = instance1.exec(v2geom);
        Union instance3 = new Union();
        NodeValue uniongeom = instance3.exec(v1geom2D, v2geom2D);
        System.out.println(uniongeom.toString());


    }

            */

/*
    public static NodeValue createNode(String geometry){
        String[] pointXYZList = geometry.split(",");

        List<Coordinate> coords=new LinkedList<>();

        for (String s : pointXYZList) {
            String[] pointXYZ = s.split(" ");
            coords.add(new Coordinate(Double.valueOf(pointXYZ[0]), Double.valueOf(pointXYZ[1]), Double.valueOf(pointXYZ[2])));
        }

        return GeometryWrapperFactory.createPolygon(coords, WKTDatatype.URI).asNodeValue();

    }
}*/
