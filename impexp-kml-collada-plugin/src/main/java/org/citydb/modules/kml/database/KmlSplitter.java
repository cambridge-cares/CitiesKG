/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2019
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.modules.kml.database;

//import com.github.jsonldjava.utils.Obj;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.citydb.concurrent.WorkerPool;
import org.citydb.config.Config;
import org.citydb.config.geometry.BoundingBox;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.config.geometry.GeometryType;
import org.citydb.config.geometry.Position;
import org.citydb.config.project.database.Database;
import org.citydb.config.project.database.Database.PredefinedSrsName;
import org.citydb.config.project.database.DatabaseSrs;
import org.citydb.config.project.kmlExporter.DisplayForm;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.database.schema.mapping.MappingConstants;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.log.Logger;
import org.citydb.modules.kml.util.CityObject4JSON;
import org.citydb.query.Query;
import org.citydb.query.builder.QueryBuildException;
import org.citydb.query.builder.sql.BuildProperties;
import org.citydb.query.builder.sql.SQLQueryBuilder;
import org.citydb.query.filter.FilterException;
import org.citydb.query.filter.tiling.Tile;
import org.citydb.sqlbuilder.expression.PlaceHolder;
import org.citydb.sqlbuilder.schema.Table;
import org.citydb.sqlbuilder.select.Select;
import org.citydb.sqlbuilder.select.join.JoinFactory;
import org.citydb.sqlbuilder.select.operator.comparison.ComparisonFactory;
import org.citydb.sqlbuilder.select.operator.comparison.ComparisonName;
import org.citydb.util.Util;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citydb.config.project.database.DatabaseType; // added by Shiying

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class KmlSplitter {
	private final WorkerPool<KmlSplittingResult> dbWorkerPool;
	private final DisplayForm displayForm;
	private final Query query;
	private volatile boolean shouldRun = true;

	private AbstractDatabaseAdapter databaseAdapter;
	private Connection connection;
	private DatabaseSrs dbSrs;

	private String schema;
	private SchemaMapping schemaMapping;
	private SQLQueryBuilder builder;

	public KmlSplitter(SchemaMapping schemaMapping,
			WorkerPool<KmlSplittingResult> dbWorkerPool,
			Query query, 
			DisplayForm displayForm,
			Config config) throws SQLException {
		this.dbWorkerPool = dbWorkerPool;
		this.schemaMapping = schemaMapping;
		this.query = query;
		this.displayForm = displayForm;

		databaseAdapter = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter();
		connection = DatabaseConnectionPool.getInstance().getConnection();
		dbSrs = databaseAdapter.getConnectionMetaData().getReferenceSystem();		

		// try and change workspace for connection if needed
		if (databaseAdapter.hasVersioningSupport()) {
			Database database = config.getProject().getDatabase();
			databaseAdapter.getWorkspaceManager().gotoWorkspace(connection, 
					database.getWorkspaces().getKmlExportWorkspace());
		}

		schema = databaseAdapter.getConnectionDetails().getSchema();

		BuildProperties buildProperties = BuildProperties.defaults()
				.addProjectionColumn(MappingConstants.GMLID);

		builder = new SQLQueryBuilder(
				schemaMapping,
				databaseAdapter,
				buildProperties);
	}

	private void queryObjects() throws SQLException, QueryBuildException, FilterException {
		// tiling
		Tile activeTile = null;
		if (query.isSetTiling()) {
			activeTile = query.getTiling().getActiveTile();
			builder.getBuildProperties().addProjectionColumn(MappingConstants.ENVELOPE); // add envelop to the builder projectionColumns
		}

		// create query statement: Top-level Feature ID Queue
		Select select = builder.buildQuery(query);

		boolean is_Blazegraph = databaseAdapter.getDatabaseType().value().equals(DatabaseType.BLAZE.value());

		if (is_Blazegraph) {
			//For Blazegraph

			List<PlaceHolder<?>> placeHolders = select.getInvolvedPlaceHolders();
			int objectCount_sparql = 0;

			// for each gmlid, a sparql query will be executed and resultset will be extracted
			for (int i  = 0; i < placeHolders.size(); ++i) {
				Object gmlidUri = placeHolders.get(i).getValue();
				PreparedStatement stmt = databaseAdapter.getSQLAdapter().prepareStatement(select, connection);
				// Assign one gmlid, the predicateTokens
				if (((String)gmlidUri).contains("*")){
						// @TODO: as the preparedstatement will create different query for particular gmlid or *
				} else {
					stmt.setString(1, (String)gmlidUri);
					stmt.setString(2, (String)gmlidUri);
				}

				long startTime = System.currentTimeMillis();
				//System.out.println(stmt);  // only the parameterized query
				ResultSet rs = stmt.executeQuery();
				long endTime = System.currentTimeMillis();

				System.out.println("The first query getTopFeatureId takes: " + String.valueOf(endTime - startTime) + " milliseconds");

				ArrayList<Integer> numbers = new ArrayList<>(Arrays.asList(64, 4, 5, 7, 8, 9, 42, 43, 44, 45, 14, 46, 85, 21, 23, 26)); // correct solution

				while (rs.next() && shouldRun) {

					int objectClassId = rs.getInt(MappingConstants.OBJECTCLASS_ID);

					//if (numbers.contains(objectClassId)){
						String id_str = rs.getString(MappingConstants.ID);
					//	String[] elements = id_str.split("/");
					//	String gmlId = elements[elements.length-2];
						String gmlId = rs.getString(MappingConstants.GMLID);


					GeometryObject envelope = null;
					if (query.isSetTiling()) {
						Object geomObj = rs.getObject(MappingConstants.ENVELOPE);
						if (!rs.wasNull() && geomObj != null)
							envelope = databaseAdapter.getGeometryConverter().getEnvelope(geomObj);
					}

					// Note: This will lead to the implementation for Blazegraph in the same class
					addWorkToQueue(id_str, gmlId, objectClassId, envelope, activeTile, false);
					objectCount_sparql++;
					//}
				}

				if (query.isSetTiling())
					Logger.getInstance().debug(
							objectCount_sparql + " candidate objects found for Tile_" + activeTile.getX() + "_"
									+ activeTile.getY() + ".");
			}
		} else {
			// For SQL
			try (PreparedStatement stmt = databaseAdapter.getSQLAdapter()
					.prepareStatement(select, connection);
					ResultSet rs = stmt.executeQuery()) {
				int objectCount = 0;
				while (rs.next() && shouldRun) {
					long id = rs.getLong(MappingConstants.ID);
					String gmlId = rs.getString(MappingConstants.GMLID);
					int objectClassId = rs.getInt(MappingConstants.OBJECTCLASS_ID);

					GeometryObject envelope = null;
					if (query.isSetTiling()) {
						Object geomObj = rs.getObject(MappingConstants.ENVELOPE);
						if (!rs.wasNull() && geomObj != null)
							envelope = databaseAdapter.getGeometryConverter().getEnvelope(geomObj);
					}

					addWorkToQueue(id, gmlId, objectClassId, envelope, activeTile, false);
					objectCount++;
				}

				if (query.isSetTiling())
					Logger.getInstance().debug(
							objectCount + " candidate objects found for Tile_" + activeTile.getX() + "_"
									+ activeTile.getY() + ".");
			}
		}
	}

	public void startQuery() throws SQLException, QueryBuildException, FilterException {
		try {
			queryObjects();
		} finally {
			if (connection != null)
				connection.close();
		}
	}

	public void shutdown() {
		shouldRun = false;
	}

	/**
	 * Method only applies to Blazegraph: Add the query work with the TopFeatureID result for the geometry extraction (multi-threading)
	 *
	 */
	// For Blazegraph, "String id" instead of "long id"
	private void addWorkToQueue(String id, String gmlId, int objectClassId, GeometryObject envelope, Tile activeTile, boolean isCityObjectGroupMember) throws SQLException, FilterException {
		FeatureType featureType = schemaMapping.getFeatureType(objectClassId);

		// In order to avoid the duplication of export, cityobjectgroup members
		// should not be exported if it belongs to the feature types (except CityObjectGroup)
		// that have been already selected in the featureClass-Filter (ComplexFilter)
		if (isCityObjectGroupMember
				&& query.getFeatureTypeFilter().containsFeatureType(featureType)
				&& Util.getCityGMLClass(objectClassId) != CityGMLClass.CITY_OBJECT_GROUP)
			return;

		// 1) If only the feature type CityObjectGroup is checked, then all city
		// object groups and all their group members (independent of their
		// feature type) are exported.
		// 2) If further feature types are selected in addition to
		// CityObjectGroup, then only group members matching those feature types
		// are exported. Of course, all features that match the type selection
		// but are not group members are also exported.
		if (query.getFeatureTypeFilter().containsFeatureType(featureType)
				|| (isCityObjectGroupMember && query.getFeatureTypeFilter().size() == 1)) {

			// check whether center point of the feature's envelope is within the tile extent
			if (envelope != null && envelope.getGeometryType() == GeometryType.ENVELOPE) {
				double coordinates[] = envelope.getCoordinates(0);
				if (!activeTile.isOnTile(new org.citydb.config.geometry.Point(
								(coordinates[0] + coordinates[3]) / 2.0,
								(coordinates[1] + coordinates[4]) / 2.0,
								databaseAdapter.getConnectionMetaData().getReferenceSystem()),
						databaseAdapter))
					return;
			}

			// create json
			CityObject4JSON cityObject4Json = new CityObject4JSON(gmlId);
			cityObject4Json.setTileRow(activeTile != null ? activeTile.getX() : 0);
			cityObject4Json.setTileColumn(activeTile != null ? activeTile.getY() : 0);
			cityObject4Json.setEnvelope(getEnvelopeInWGS84(envelope));

			// put on work queue
			KmlSplittingResult splitter = new KmlSplittingResult(id, gmlId, objectClassId, cityObject4Json, displayForm);
			dbWorkerPool.addWork(splitter);

			if (splitter.getCityGMLClass() == CityGMLClass.CITY_OBJECT_GROUP) {
				Table cityObject = new Table("cityobject", schema);
				Table groupToCityObject = new Table("group_to_cityobject", schema);
				//PlaceHolder<Long> groupId = new PlaceHolder<>(id); // Add BY Shiying
				PlaceHolder<String> groupId = new PlaceHolder<>(id);

				Select select = new Select()
						.addProjection(cityObject.getColumn(MappingConstants.ID))
						.addProjection(cityObject.getColumn(MappingConstants.GMLID))
						.addProjection(cityObject.getColumn(MappingConstants.OBJECTCLASS_ID))
						.addProjection(cityObject.getColumn(MappingConstants.ENVELOPE))
						.addSelection(ComparisonFactory.in(
								cityObject.getColumn(MappingConstants.ID),
								new Select()
										.addProjection(cityObject.getColumn(MappingConstants.ID))
										.addJoin(JoinFactory.inner(cityObject, MappingConstants.ID, ComparisonName.EQUAL_TO, groupToCityObject.getColumn("cityobject_id")))
										.addSelection(ComparisonFactory.equalTo(groupToCityObject.getColumn("cityobjectgroup_id"), groupId))
						));

				try (PreparedStatement stmt = databaseAdapter.getSQLAdapter().prepareStatement(select, connection);
					 ResultSet rs = stmt.executeQuery()) {
					while (rs.next() && shouldRun) {
						long _id = rs.getLong(MappingConstants.ID);
						String _gmlId = rs.getString(MappingConstants.GMLID);
						int _objectClassId = rs.getInt(MappingConstants.OBJECTCLASS_ID);

						GeometryObject _envelope = null;
						Object geomObj = rs.getObject(MappingConstants.ENVELOPE);
						if (!rs.wasNull() && geomObj != null)
							_envelope = databaseAdapter.getGeometryConverter().getEnvelope(geomObj);

						// Recursion in CityObjectGroup
						addWorkToQueue(_id,  _gmlId, _objectClassId, _envelope, activeTile, true);
					}
				}
			}
		}
	}

	// For relational database
	private void addWorkToQueue(long id, String gmlId, int objectClassId, GeometryObject envelope, Tile activeTile, boolean isCityObjectGroupMember) throws SQLException, FilterException {
		FeatureType featureType = schemaMapping.getFeatureType(objectClassId);

		// In order to avoid the duplication of export, cityobjectgroup members
		// should not be exported if it belongs to the feature types (except CityObjectGroup) 
		// that have been already selected in the featureClass-Filter (ComplexFilter)
		if (isCityObjectGroupMember 
				&& query.getFeatureTypeFilter().containsFeatureType(featureType)
				&& Util.getCityGMLClass(objectClassId) != CityGMLClass.CITY_OBJECT_GROUP)
			return;

		// 1) If only the feature type CityObjectGroup is checked, then all city
		// object groups and all their group members (independent of their
		// feature type) are exported.
		// 2) If further feature types are selected in addition to
		// CityObjectGroup, then only group members matching those feature types
		// are exported. Of course, all features that match the type selection
		// but are not group members are also exported.
		if (query.getFeatureTypeFilter().containsFeatureType(featureType)
				|| (isCityObjectGroupMember && query.getFeatureTypeFilter().size() == 1)) {

			// check whether center point of the feature's envelope is within the tile extent
			if (envelope != null && envelope.getGeometryType() == GeometryType.ENVELOPE) {
				double coordinates[] = envelope.getCoordinates(0);
				if (!activeTile.isOnTile(new org.citydb.config.geometry.Point(
						(coordinates[0] + coordinates[3]) / 2.0,
						(coordinates[1] + coordinates[4]) / 2.0,
						databaseAdapter.getConnectionMetaData().getReferenceSystem()), 
						databaseAdapter))
					return;
			}

			// create json
			CityObject4JSON cityObject4Json = new CityObject4JSON(gmlId);
			cityObject4Json.setTileRow(activeTile != null ? activeTile.getX() : 0);
			cityObject4Json.setTileColumn(activeTile != null ? activeTile.getY() : 0);
			cityObject4Json.setEnvelope(getEnvelopeInWGS84(envelope));

			// put on work queue
			KmlSplittingResult splitter = new KmlSplittingResult(id, gmlId, objectClassId, cityObject4Json, displayForm);
			dbWorkerPool.addWork(splitter);

			if (splitter.getCityGMLClass() == CityGMLClass.CITY_OBJECT_GROUP) {
				Table cityObject = new Table("cityobject", schema);
				Table groupToCityObject = new Table("group_to_cityobject", schema);
				PlaceHolder<Long> groupId = new PlaceHolder<>(id);

				Select select = new Select()
						.addProjection(cityObject.getColumn(MappingConstants.ID))
						.addProjection(cityObject.getColumn(MappingConstants.GMLID))
						.addProjection(cityObject.getColumn(MappingConstants.OBJECTCLASS_ID))
						.addProjection(cityObject.getColumn(MappingConstants.ENVELOPE))
						.addSelection(ComparisonFactory.in(
						cityObject.getColumn(MappingConstants.ID),
						new Select()
						.addProjection(cityObject.getColumn(MappingConstants.ID))
						.addJoin(JoinFactory.inner(cityObject, MappingConstants.ID, ComparisonName.EQUAL_TO, groupToCityObject.getColumn("cityobject_id")))
						.addSelection(ComparisonFactory.equalTo(groupToCityObject.getColumn("cityobjectgroup_id"), groupId))
						));

				try (PreparedStatement stmt = databaseAdapter.getSQLAdapter().prepareStatement(select, connection);
					 ResultSet rs = stmt.executeQuery()) {
					while (rs.next() && shouldRun) {
						long _id = rs.getLong(MappingConstants.ID);
						String _gmlId = rs.getString(MappingConstants.GMLID);
						int _objectClassId = rs.getInt(MappingConstants.OBJECTCLASS_ID);

						GeometryObject _envelope = null;
						Object geomObj = rs.getObject(MappingConstants.ENVELOPE);
						if (!rs.wasNull() && geomObj != null)
							_envelope = databaseAdapter.getGeometryConverter().getEnvelope(geomObj);

						// Recursion in CityObjectGroup
						addWorkToQueue(_id,  _gmlId, _objectClassId, _envelope, activeTile, true);
					}
				}
			}
		}
	}

	private double[] getEnvelopeInWGS84(GeometryObject envelope) throws SQLException {
		if (envelope == null)
			return null;

		double[] coordinates = envelope.getCoordinates(0);
		BoundingBox bbox = new BoundingBox(new Position(coordinates[0], coordinates[1]), new Position(coordinates[3], coordinates[4]));
		BoundingBox wgs84 = databaseAdapter.getUtil().transformBoundingBox(bbox, dbSrs, Database.PREDEFINED_SRS.get(PredefinedSrsName.WGS84_2D));

		double[] result = new double[6];
		result[0] = wgs84.getLowerCorner().getX();
		result[1] = wgs84.getLowerCorner().getY();
		result[2] = 0;
		result[3] = wgs84.getUpperCorner().getX();
		result[4] = wgs84.getUpperCorner().getY();
		result[5] = 0;

		return result;
	}

}
