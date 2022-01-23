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

public abstract class AbstractDBImporter implements DBImporter {

	protected final CityGMLImportManager importer;
	protected final Connection batchConn;
	protected final PreparedStatement preparedStatement;

	protected final String TABLE_NAME;
	protected final String SQL_SCHEMA;
	protected final String PREFIX_ONTOCITYGML;
	protected final String IRI_GRAPH_BASE;
	protected final String IRI_GRAPH_OBJECT_REL;
	protected final String IRI_GRAPH_OBJECT;

	protected abstract String getTableName();

	protected abstract String getIriGraphObjectRel();

	// It is somewhat inelegant to have only some resources declared in the abstract class
	// but it is less unwieldy than passing them with each importGeometryProperty call.
	// I think it might be a good idea to have all importers declared here? But I will not do that
	// yet.
	protected DBSurfaceGeometry surfaceGeometryImporter;
	protected int nullGeometryType;
	protected String nullGeometryTypeName;

	protected int batchCounter;
	protected long objectId;

	// Constructs preparedStatement and sets importer and batchConn variables.
	public AbstractDBImporter(Connection batchConn, Config config, CityGMLImportManager importer)
			throws SQLException {
		this.importer = importer;
		this.batchConn = batchConn;
		preconstructor(config);
		nullGeometryType = importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType();
		nullGeometryTypeName =
				importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName();
		TABLE_NAME = getTableName();
		SQL_SCHEMA = importer.getDatabaseAdapter().getConnectionDetails().getSchema();
		PREFIX_ONTOCITYGML = importer.getOntoCityGmlPrefix();
		IRI_GRAPH_BASE = importer.getGraphBaseIri();
		IRI_GRAPH_OBJECT_REL = getIriGraphObjectRel();
		IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;
		String stmt = importer.isBlazegraph() ? getSPARQLStatement() : getSQLStatement();
		preparedStatement = batchConn.prepareStatement(stmt);
	}

	protected void preconstructor(Config config) {}

	// Generator functions for preparedStatement
	protected abstract String getSQLStatement();

	protected abstract String getSPARQLStatement();

	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			preparedStatement.executeBatch();
			batchCounter = 0;
		}
	}

	public void close() throws CityGMLImportException, SQLException {
		preparedStatement.close();
	}

	protected void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}

	protected <T extends AbstractGeometry> int importSurfaceGeometryProperties(
			GeometryProperty<T>[] geometryProperties, int[] lods, String columnSuffix, int index)
			throws CityGMLImportException, SQLException {
		for (int i = 0; i < lods.length; i++)
			index = importSurfaceGeometryProperty(geometryProperties[i], lods[i], columnSuffix, index);
		return index;
	}

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
						new DBXlinkSurfaceGeometry(TABLE_NAME, objectId, href, "lod" + lod + columnSuffix));
			}
		}
		return index;
	}

	protected <T extends GeometryProperty<?>> int importGeometryObjectProperties(
			T[] geometryProperties, Function<T, GeometryObject> converter, int index)
			throws CityGMLImportException, SQLException {
		for (T geometryProperty : geometryProperties)
			index = importGeometryObjectProperty(geometryProperty, converter, index);
		return index;
	}

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
