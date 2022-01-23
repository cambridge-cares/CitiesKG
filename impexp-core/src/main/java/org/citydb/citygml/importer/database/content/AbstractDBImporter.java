package org.citydb.citygml.importer.database.content;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Types;

import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.geometry.GeometryObject;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;

import java.sql.PreparedStatement;

import org.citydb.config.Config;

import java.sql.Connection;
import java.util.function.Function;

/**
 * An abstract class which serves as the base for the DB* classes in org.citydb.citygml.importer.database.content,
 * implementing shared functionality and helper methods for the logic for geometry property imports.
 */
public abstract class AbstractDBImporter implements DBImporter {

	protected final CityGMLImportManager importer;
	protected final Connection batchConn;
	protected final PreparedStatement preparedStatement;

	protected final String tableName;
	protected final String sqlSchema;
	protected final String prefixOntoCityGML;
	protected final String iriGraphBase;
	protected final String iriGraphObjectRel;
	protected final String iriGraphObject;

	protected abstract String getTableName();

	protected abstract String getIriGraphObjectRel();

	// Import helpers and variables used in common methods implemented in AbstractDBImporter
	protected DBSurfaceGeometry surfaceGeometryImporter;
	protected int nullGeometryType;
	protected String nullGeometryTypeName;

	protected int batchCounter;
	protected long objectId;

	// Base constructor constructs preparedStatement and sets importer and batchConn variables.
	public AbstractDBImporter(Connection batchConn, Config config, CityGMLImportManager importer)
			throws SQLException {
		this.importer = importer;
		this.batchConn = batchConn;
		preconstructor(config);
		nullGeometryType = importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType();
		nullGeometryTypeName =
				importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName();
		tableName = getTableName();
		sqlSchema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();
		prefixOntoCityGML = importer.getOntoCityGmlPrefix();
		iriGraphBase = importer.getGraphBaseIri();
		iriGraphObjectRel = getIriGraphObjectRel();
		iriGraphObject = iriGraphBase + iriGraphObjectRel;
		String stmt = importer.isBlazegraph() ? getSPARQLStatement() : getSQLStatement();
		preparedStatement = batchConn.prepareStatement(stmt);
	}

	/**
	 * A method which is called at the start of the base constructor, to be overridden if subclasses need to
	 * perform actions in advance of getSPARQLStatement() or getSQLStatement() being called.
	 * @param config the Config which was passed to the class constructor.
	 */
	protected void preconstructor(Config config) {}

	/**
	 * Generator function for SQL statement to write data to database, to be implemented by subclasses.
	 * @return SQL statement for data import.
	 */
	protected String getSQLStatement() {
		return "SQL import not implemented for" + this.getClass().toString() + ".";
	}

	/**
	 * Generator function for SPARQL statement to write data to Blazegraph, to be implemented by subclasses.
	 * @return SPARQL statement for data import.
	 */
	protected String getSPARQLStatement() {
		return "SPARQL import not implemented for" + this.getClass().toString() + ".";
	}

	/**
	 * Executes the importer's current batch.
	 */
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			preparedStatement.executeBatch();
			batchCounter = 0;
		}
	}

	/**
	 * Closes the importer's preparedStatement.
	 */
	public void close() throws CityGMLImportException, SQLException {
		preparedStatement.close();
	}

	/**
	 * Closes the importer's preparedStatement.
	 */
	protected void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}

	/**
	 * Imports an array of SurfaceGeometry properties. Internally calls importSurfaceGeometryProperty.
	 * @param geometryProperties the GeometryProperty objects to be imported.
	 * @param lods the respective levels of detail of the GeometryProperties, to build property names for xlinking.
	 * @param columnSuffix the suffix to be used to build property names for xlinking, e.g. "_multi_surface_id".
	 * @param index the property index which belongs to the first property; the others are assumed to be sequential.
	 * @return the index of the next property after the last geometry property.
	 */
	protected <T extends AbstractGeometry> int importSurfaceGeometryProperties(
			GeometryProperty<T>[] geometryProperties, int[] lods, String columnSuffix, int index)
			throws CityGMLImportException, SQLException {
		for (int i = 0; i < lods.length; i++)
			index = importSurfaceGeometryProperty(geometryProperties[i], lods[i], columnSuffix, index);
		return index;
	}

	/**
	 * Imports a single SurfaceGeometry property.
	 * @param geometryProperty the GeometryProperty to be imported.
	 * @param lod the level of detail of the GeometryProperty, to build a property name for xlinking.
	 * @param columnSuffix the suffix to be used to build the property name for xlinking, e.g. "_multi_surface_id".
	 * @param index the property index of the GeometryProperty.
	 * @return the index of the next property.
	 */
	protected <T extends AbstractGeometry> int importSurfaceGeometryProperty(
			GeometryProperty<T> geometryProperty, int lod, String columnSuffix, int index)
			throws CityGMLImportException, SQLException {
		if (geometryProperty == null) {
			if (importer.isBlazegraph()) setBlankNode(preparedStatement, ++index);
			else preparedStatement.setNull(++index, Types.NULL);
		} else if (geometryProperty.isSetObject()) {
			long geometryId =
					surfaceGeometryImporter.doImport(geometryProperty.getObject(), objectId);
			if (geometryId != 0) {
				if (importer.isBlazegraph()) {
					try {
						preparedStatement.setURL(
								++index,
								new URL(
										DBSurfaceGeometry.IRI_GRAPH_OBJECT
												+ geometryProperty.getObject().getId()
												+ "/"));
					} catch (MalformedURLException e) {
						new CityGMLImportException(e);
					}
				} else {
					preparedStatement.setLong(++index, geometryId);
				}
			} else {
				if (importer.isBlazegraph()) setBlankNode(preparedStatement, ++index);
				else preparedStatement.setNull(++index, Types.NULL);
			}
			geometryProperty.unsetGeometry();
		} else {
			String href = geometryProperty.getHref();
			if (href != null && href.length() != 0) {
				importer.propagateXlink(
						new DBXlinkSurfaceGeometry(tableName, objectId, href, "lod" + lod + columnSuffix));
			}
		}
		return index;
	}

	/**
	 * Imports an array of GeometryObject properties. Internally calls importGeometryObjectProperty.
	 * @param geometryProperties the objects to be imported.
	 * @param converter the converter which transforms the GeometryProperty into a GeometryObject.
	 * @param index the property index which belongs to the first property; the others are assumed to be sequential.
	 * @return the index of the next property after the last geometry property.
	 */
	protected <T extends GeometryProperty<?>> int importGeometryObjectProperties(
			T[] geometryProperties, Function<T, GeometryObject> converter, int index)
			throws CityGMLImportException, SQLException {
		for (T geometryProperty : geometryProperties)
			index = importGeometryObjectProperty(geometryProperty, converter, index);
		return index;
	}

	/**
	 * Imports an array of GeometryObject properties. Internally calls importGeometryObjectProperty.
	 * @param geometryProperty the object to be imported.
	 * @param converter the converter which transforms the GeometryProperty into a GeometryObject.
	 * @param index the property index of the property.
	 * @return the index of the next property.
	 */
	protected <T extends GeometryProperty<?>> int importGeometryObjectProperty(
			T geometryProperty, Function<T, GeometryObject> converter, int index)
			throws CityGMLImportException, SQLException {

		GeometryObject geometryObj = null;

		if (geometryProperty != null) {
			geometryObj = converter.apply(geometryProperty);
			geometryProperty.unsetGeometry();
		}

		if (geometryObj != null) {
			Object multiLineObj =
					importer
							.getDatabaseAdapter()
							.getGeometryConverter()
							.getDatabaseObject(geometryObj, batchConn);
			preparedStatement.setObject(++index, multiLineObj);
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else preparedStatement.setNull(++index, nullGeometryType, nullGeometryTypeName);
		}

		return index;
	}
}
