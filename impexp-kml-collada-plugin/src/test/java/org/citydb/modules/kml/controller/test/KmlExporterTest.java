package org.citydb.modules.kml.controller.test;

import net.opengis.kml._2.ExtendedDataType;
import org.citydb.citygml.importer.database.content.DBTest;
import org.citydb.config.Config;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.event.EventDispatcher;
import org.citydb.modules.kml.controller.KmlExporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class KmlExporterTest extends DBTest {

    // testing only newly added methods

    @Test
    public void testSetIriPrefixToExtendedData() {
        try {
            Method setIriPrefixToExtendedData = KmlExporter.class.getDeclaredMethod("setIriPrefixToExtendedData");
            setIriPrefixToExtendedData.setAccessible(true);

            KmlExporter exporter = new KmlExporter(JAXBContext.newInstance(), JAXBContext.newInstance(), new SchemaMapping(), new Config(), new EventDispatcher());
            ExtendedDataType actual = (ExtendedDataType) setIriPrefixToExtendedData.invoke(exporter);

            assertEquals(1, actual.getData().size());
            assertEquals("prefix", actual.getData().get(0).getName());
            assertEquals("http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/cityobject/", actual.getData().get(0).getValue());
        } catch (NoSuchMethodException | JAXBException | IllegalAccessException | InvocationTargetException e) {
            Assertions.fail();
        }
    }
}