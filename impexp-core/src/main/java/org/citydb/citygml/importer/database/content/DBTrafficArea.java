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
import java.sql.SQLException;
import java.sql.Types;

import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.transportation.AuxiliaryTrafficArea;
import org.citygml4j.model.citygml.transportation.TrafficArea;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;

public class DBTrafficArea extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private AttributeValueJoiner valueJoiner;

	public DBTrafficArea(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected String getTableName() {
		return TableEnum.TRAFFIC_AREA.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "trafficarea/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".traffic_area (id, objectclass_id, class, class_codespace, function, function_codespace, usage, usage_codespace, " +
				"surface_material, surface_material_codespace, lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id, " +
				"transportation_complex_id) values " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	}

	protected long doImport(TrafficArea trafficArea) throws CityGMLImportException, SQLException {
		return doImport(trafficArea, 0);
	}

	protected long doImport(AuxiliaryTrafficArea auxiliaryTrafficArea) throws CityGMLImportException, SQLException {
		return doImport(auxiliaryTrafficArea, 0);
	}

	public long doImport(TrafficArea trafficArea, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(trafficArea);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long trafficAreaId = cityObjectImporter.doImport(trafficArea,  featureType);

		// import traffic area information
		// primary id
		preparedStatement.setLong(1, trafficAreaId);

		// objectclass id
		preparedStatement.setLong(2, featureType.getObjectClassId());

		// tran:class
		if (trafficArea.isSetClazz() && trafficArea.getClazz().isSetValue()) {
			preparedStatement.setString(3, trafficArea.getClazz().getValue());
			preparedStatement.setString(4, trafficArea.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(3, Types.VARCHAR);
			preparedStatement.setNull(4, Types.VARCHAR);
		}

		// tran:function
		if (trafficArea.isSetFunction()) {
			valueJoiner.join(trafficArea.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(5, valueJoiner.result(0));
			preparedStatement.setString(6, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(5, Types.VARCHAR);
			preparedStatement.setNull(6, Types.VARCHAR);
		}

		// tran:usage
		if (trafficArea.isSetUsage()) {
			valueJoiner.join(trafficArea.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(7, valueJoiner.result(0));
			preparedStatement.setString(8, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(7, Types.VARCHAR);
			preparedStatement.setNull(8, Types.VARCHAR);
		}

		// tran:surface material
		if (trafficArea.isSetSurfaceMaterial() && trafficArea.getSurfaceMaterial().isSetValue()) {
			preparedStatement.setString(9, trafficArea.getSurfaceMaterial().getValue());
			preparedStatement.setString(10, trafficArea.getSurfaceMaterial().getCodeSpace());
		} else {
			preparedStatement.setNull(9, Types.VARCHAR);
			preparedStatement.setNull(10, Types.VARCHAR);
		}

		// tran:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				trafficArea.getLod2MultiSurface(),
				trafficArea.getLod3MultiSurface(),
				trafficArea.getLod4MultiSurface()
		}, new int[]{2, 3, 4}, "_multi_surface_id", 11);

		// reference to transportation complex
		if (parentId != 0)
			preparedStatement.setLong(14, parentId);
		else 
			preparedStatement.setNull(14, Types.NULL);

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.TRAFFIC_AREA);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(trafficArea, trafficAreaId, featureType);

		return trafficAreaId;
	}

	public long doImport(AuxiliaryTrafficArea auxiliaryTrafficArea, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(auxiliaryTrafficArea);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long auxiliaryTrafficAreaId = cityObjectImporter.doImport(auxiliaryTrafficArea, featureType);

		// import auxiliary traffic area information
		// primary id
		preparedStatement.setLong(1, auxiliaryTrafficAreaId);

		// objectclass id
		preparedStatement.setLong(2, featureType.getObjectClassId());

		// tran:class
		if (auxiliaryTrafficArea.isSetClazz() && auxiliaryTrafficArea.getClazz().isSetValue()) {
			preparedStatement.setString(3, auxiliaryTrafficArea.getClazz().getValue());
			preparedStatement.setString(4, auxiliaryTrafficArea.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(3, Types.VARCHAR);
			preparedStatement.setNull(4, Types.VARCHAR);
		}

		// tran:function
		if (auxiliaryTrafficArea.isSetFunction()) {
			valueJoiner.join(auxiliaryTrafficArea.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(5, valueJoiner.result(0));
			preparedStatement.setString(6, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(5, Types.VARCHAR);
			preparedStatement.setNull(6, Types.VARCHAR);
		}

		// tran:usage
		if (auxiliaryTrafficArea.isSetUsage()) {
			valueJoiner.join(auxiliaryTrafficArea.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(7, valueJoiner.result(0));
			preparedStatement.setString(8, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(7, Types.VARCHAR);
			preparedStatement.setNull(8, Types.VARCHAR);
		}

		// tran:surface material
		if (auxiliaryTrafficArea.isSetSurfaceMaterial() && auxiliaryTrafficArea.getSurfaceMaterial().isSetValue()) {
			preparedStatement.setString(9, auxiliaryTrafficArea.getSurfaceMaterial().getValue());
			preparedStatement.setString(10, auxiliaryTrafficArea.getSurfaceMaterial().getCodeSpace());
		} else {
			preparedStatement.setNull(9, Types.VARCHAR);
			preparedStatement.setNull(10, Types.VARCHAR);
		}

		// tran:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				auxiliaryTrafficArea.getLod2MultiSurface(),
				auxiliaryTrafficArea.getLod3MultiSurface(),
				auxiliaryTrafficArea.getLod4MultiSurface()
		}, new int[]{2, 3, 4}, "_multi_surface_id", 11);

		// reference to transportation complex
		if (parentId != 0)
			preparedStatement.setLong(14, parentId);
		else
			preparedStatement.setNull(14, Types.NULL);

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.TRAFFIC_AREA);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(auxiliaryTrafficArea, auxiliaryTrafficAreaId, featureType);

		return auxiliaryTrafficAreaId;
	}

}
