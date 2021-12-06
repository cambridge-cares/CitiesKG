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
package org.citydb.citygml.importer.database.content;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.citydb.citygml.common.database.xlink.DBXlinkBasic;
import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.waterbody.AbstractWaterBoundarySurface;
import org.citygml4j.model.citygml.waterbody.BoundedByWaterSurfaceProperty;
import org.citygml4j.model.citygml.waterbody.WaterBody;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;

public class DBWaterBody extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBWaterBoundarySurface boundarySurfaceImporter;
	private GeometryConverter geometryConverter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;

	public DBWaterBody(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		boundarySurfaceImporter = importer.getImporter(DBWaterBoundarySurface.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected void preconstructor(Config config) {
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
	}
	
	@Override
	protected String getTableName() {
		return TableEnum.WATERBODY.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "waterbody/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + SQL_SCHEMA + ".waterbody (id, class, class_codespace, function, function_codespace, usage, usage_codespace, " +
				"lod0_multi_curve, lod1_multi_curve, lod0_multi_surface_id, lod1_multi_surface_id, " +
				"lod1_solid_id, lod2_solid_id, lod3_solid_id, lod4_solid_id" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	@Override
	protected String getSPARQLStatement() {
		return "NOT IMPLEMENTED.";
	}

	protected long doImport(WaterBody waterBody) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(waterBody);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long waterBodyId = cityObjectImporter.doImport(waterBody, featureType);

		// import water body information
		// primary id
		preparedStatement.setLong(1, waterBodyId);

		// wtr:class
		if (waterBody.isSetClazz() && waterBody.getClazz().isSetValue()) {
			preparedStatement.setString(2, waterBody.getClazz().getValue());
			preparedStatement.setString(3, waterBody.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.VARCHAR);
		}

		// wtr:function
		if (waterBody.isSetFunction()) {
			valueJoiner.join(waterBody.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(4, valueJoiner.result(0));
			preparedStatement.setString(5, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// wtr:usage
		if (waterBody.isSetUsage()) {
			valueJoiner.join(waterBody.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(6, valueJoiner.result(0));
			preparedStatement.setString(7, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// wtr:lodXMultiCurve
		importGeometryObjectProperties(new MultiCurveProperty[]{
				waterBody.getLod0MultiCurve(),
				waterBody.getLod1MultiCurve(),
		}, geometryConverter::getMultiCurve, 8);

		// wtr:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				waterBody.getLod0MultiSurface(),
				waterBody.getLod1MultiSurface()
		}, new int[]{0, 1}, "_multi_surface_id", 10);

		// wtr:lodXSolid
		importSurfaceGeometryProperties(new SolidProperty[]{
				waterBody.getLod1Solid(),
				waterBody.getLod2Solid(),
				waterBody.getLod3Solid(),
				waterBody.getLod4Solid()
		}, new int[]{1, 2, 3, 4}, "_solid_id", 12);

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(16, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.WATERBODY);

		// wtr:boundedBy
		if (waterBody.isSetBoundedBySurface()) {
			for (BoundedByWaterSurfaceProperty property : waterBody.getBoundedBySurface()) {
				AbstractWaterBoundarySurface boundarySurface = property.getWaterBoundarySurface();

				if (boundarySurface != null) {
					boundarySurfaceImporter.doImport(boundarySurface, waterBody, waterBodyId);
					property.unsetWaterBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.WATERBOD_TO_WATERBND_SRF.getName(),
								waterBodyId,
								"WATERBODY_ID",
								href,
								"WATERBOUNDARY_SURFACE_ID"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(waterBody, waterBodyId, featureType);

		return waterBodyId;
	}

}
