package org.citydb.citygml.importer.database.content;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractGeometryConverterAdapter;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurface;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractDBImporterTest extends DBTest {

  @Test
  public void testImportSurfaceGeometryProperty() throws CityGMLImportException, SQLException, NoSuchFieldException, IllegalAccessException, MalformedURLException {

    DBBuilding building = new DBBuilding(batchConn, config, importer);

    // Mock prepared statement
    PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);
    Field preparedStatementField = AbstractDBImporter.class.getDeclaredField("preparedStatement");
    preparedStatementField.setAccessible(true);
    preparedStatementField.set(building, mockPreparedStatement);
    // Mock surface geometry importer
    building.surfaceGeometryImporter = Mockito.mock(DBSurfaceGeometry.class);
    // Mock geometryProperty to pass in
    MultiSurface mockPropertyObject = Mockito.mock(MultiSurface.class);
    Mockito.doReturn("mockedId").when(mockPropertyObject).getId();
    MultiSurfaceProperty mockProperty = Mockito.mock(MultiSurfaceProperty.class);
    Mockito.doReturn(true).when(mockProperty).isSetObject();
    Mockito.doReturn(mockPropertyObject).when(mockProperty).getObject();

    // Import non-null property with non-null object
    Mockito.doReturn(1L).when(building.surfaceGeometryImporter).doImport(Mockito.any(), Mockito.anyLong());
    assertEquals(6, building.importSurfaceGeometryProperty(mockProperty, 2, "_multi_surface_id", 5));
    Mockito.verify(mockPreparedStatement).setURL(6, new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + "mockedId/"));
    // Import null property
    Mockito.doReturn(1L).when(building.surfaceGeometryImporter).doImport(Mockito.any(), Mockito.anyLong());
    assertEquals(6, building.importSurfaceGeometryProperty(null, 2, "_multi_surface_id", 5));
    Mockito.verify(mockPreparedStatement).setObject(Mockito.eq(6), Mockito.argThat((arg) -> ((Node)arg).isBlank()));
    // Import non-null property with null object
    Mockito.doReturn(0L).when(building.surfaceGeometryImporter).doImport(Mockito.any(), Mockito.anyLong());
    assertEquals(6, building.importSurfaceGeometryProperty(mockProperty, 2, "_multi_surface_id", 5));
    Mockito.verify(mockPreparedStatement, Mockito.times(2)).setObject(Mockito.eq(6), Mockito.argThat((arg) -> ((Node)arg).isBlank()));

  }

  @Test
  public void importGeometryObjectProperty() throws CityGMLImportException, SQLException, NoSuchFieldException, IllegalAccessException, MalformedURLException {

    DBBuilding building = new DBBuilding(batchConn, config, importer);

    // Mock prepared statement
    PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);
    Field preparedStatementField = AbstractDBImporter.class.getDeclaredField("preparedStatement");
    preparedStatementField.setAccessible(true);
    preparedStatementField.set(building, mockPreparedStatement);
    // Mock geometryProperty to pass in
    MultiCurveProperty mockProperty = Mockito.mock(MultiCurveProperty.class);
    // Override importer getDatabaseObject
    AbstractGeometryConverterAdapter mockGeometryConverter = Mockito.mock(AbstractGeometryConverterAdapter.class);
    Mockito.doReturn(NodeFactory.createLiteral("literal geometry")).when(mockGeometryConverter).getDatabaseObject(Mockito.any(), Mockito.any());
    Field geometryConverterField = AbstractDatabaseAdapter.class.getDeclaredField("geometryAdapter");
    geometryConverterField.setAccessible(true);
    geometryConverterField.set(building.importer.getDatabaseAdapter(), mockGeometryConverter);

    // Import non-null property with non-null object
    assertEquals(6, building.importGeometryObjectProperty(mockProperty, (prop) -> Mockito.mock(GeometryObject.class), 5));
    Mockito.verify(mockPreparedStatement).setObject(Mockito.eq(6), Mockito.argThat((arg) -> ((Node)arg).getLiteralLexicalForm().equals("literal geometry")));
    // Import null property
    assertEquals(6, building.importGeometryObjectProperty(null, null, 5));
    Mockito.verify(mockPreparedStatement).setObject(Mockito.eq(6), Mockito.argThat((arg) -> ((Node)arg).isBlank()));
    // Import non-null property with null object
    assertEquals(6, building.importGeometryObjectProperty(mockProperty, (prop) -> null, 5));
    Mockito.verify(mockPreparedStatement, Mockito.times(2)).setObject(Mockito.eq(6), Mockito.argThat((arg) -> ((Node)arg).isBlank()));

  }

}
