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

import org.citydb.citygml.common.database.xlink.DBXlinkBasic;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.transportation.AuxiliaryTrafficArea;
import org.citygml4j.model.citygml.transportation.AuxiliaryTrafficAreaProperty;
import org.citygml4j.model.citygml.transportation.TrafficArea;
import org.citygml4j.model.citygml.transportation.TrafficAreaProperty;
import org.citygml4j.model.citygml.transportation.TransportationComplex;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.complexes.GeometricComplex;
import org.citygml4j.model.gml.geometry.complexes.GeometricComplexProperty;
import org.citygml4j.model.gml.geometry.primitives.AbstractGeometricPrimitive;
import org.citygml4j.model.gml.geometry.primitives.GeometricPrimitiveProperty;

public class DBTransportationComplex extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBTrafficArea trafficAreaImporter;
	private GeometryConverter geometryConverter;
	private AttributeValueJoiner valueJoiner;

	public DBTransportationComplex(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		trafficAreaImporter = importer.getImporter(DBTrafficArea.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected String getTableName() {
		return TableEnum.TRANSPORTATION_COMPLEX.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "transportationcomplex/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".transportation_complex (id, objectclass_id, class, class_codespace, function, function_codespace, usage, usage_codespace, " +
				"lod0_network, lod1_multi_surface_id, lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id) values " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	}

	protected long doImport(TransportationComplex transportationComplex) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(transportationComplex);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long transportationComplexId = cityObjectImporter.doImport(transportationComplex, featureType);		

		// import transportation complex information
		// primary id
		preparedStatement.setLong(1, transportationComplexId);

		// objectclass id
		preparedStatement.setInt(2, featureType.getObjectClassId());

		// tran:class
		if (transportationComplex.isSetClazz() && transportationComplex.getClazz().isSetValue()) {
			preparedStatement.setString(3, transportationComplex.getClazz().getValue());
			preparedStatement.setString(4, transportationComplex.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(3, Types.VARCHAR);
			preparedStatement.setNull(4, Types.VARCHAR);
		}

		// tran:function
		if (transportationComplex.isSetFunction()) {
			valueJoiner.join(transportationComplex.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(5, valueJoiner.result(0));
			preparedStatement.setString(6, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(5, Types.VARCHAR);
			preparedStatement.setNull(6, Types.VARCHAR);
		}

		// tran:usage
		if (transportationComplex.isSetUsage()) {
			valueJoiner.join(transportationComplex.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(7, valueJoiner.result(0));
			preparedStatement.setString(8, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(7, Types.VARCHAR);
			preparedStatement.setNull(8, Types.VARCHAR);
		}

		// tran:lod0Network
		GeometryObject multiCurve = null;

		if (transportationComplex.isSetLod0Network()) {
			GeometricComplex aggregate = new GeometricComplex();

			for (GeometricComplexProperty complexProperty : transportationComplex.getLod0Network()) {
				// for lod0Network we just consider appropriate curve geometries

				if (complexProperty.isSetCompositeCurve()) {
					GeometricPrimitiveProperty primitiveProperty = new GeometricPrimitiveProperty(complexProperty.getCompositeCurve());
					aggregate.addElement(primitiveProperty);
				} 

				else if (complexProperty.getGeometricComplex() != null) {
					GeometricComplex complex = complexProperty.getGeometricComplex();        			

					if (complex.isSetElement()) {
						for (GeometricPrimitiveProperty primitiveProperty : complex.getElement()) {        					
							if (primitiveProperty.isSetGeometricPrimitive()) {        						
								AbstractGeometricPrimitive primitive = primitiveProperty.getGeometricPrimitive();

								switch (primitive.getGMLClass()) {
								case LINE_STRING:
								case COMPOSITE_CURVE:
								case ORIENTABLE_CURVE:
								case CURVE:
									aggregate.addElement(primitiveProperty);
									break;
								default:
									// geometry type not supported by lod0Network
								}
							}
						}
					}
				}

				// we do not support XLinks or further geometry types so far
			}

			transportationComplex.unsetLod0Network();

			if (aggregate.isSetElement() && !aggregate.getElement().isEmpty())   		
				multiCurve = geometryConverter.getCurveGeometry(aggregate);
		}

		if (multiCurve != null)
			preparedStatement.setObject(9, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(multiCurve, batchConn));
		else
			preparedStatement.setNull(9, nullGeometryType, nullGeometryTypeName);

		// tran:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				transportationComplex.getLod1MultiSurface(),
				transportationComplex.getLod2MultiSurface(),
				transportationComplex.getLod3MultiSurface(),
				transportationComplex.getLod4MultiSurface()
		}, new int[]{1, 2, 3, 4}, "_multi_surface_id", 10);

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.TRANSPORTATION_COMPLEX);

		// AuxiliaryTrafficArea
		if (transportationComplex.isSetAuxiliaryTrafficArea()) {
			for (AuxiliaryTrafficAreaProperty property : transportationComplex.getAuxiliaryTrafficArea()) {
				AuxiliaryTrafficArea trafficArea = property.getAuxiliaryTrafficArea();

				if (trafficArea != null) {
					trafficAreaImporter.doImport(trafficArea, transportationComplexId);
					property.unsetAuxiliaryTrafficArea();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TRAFFIC_AREA.getName(),
								href,
								transportationComplexId,
								"transportation_complex_id"));
					}
				}
			}
		}

		// TrafficArea
		if (transportationComplex.isSetTrafficArea()) {
			for (TrafficAreaProperty property : transportationComplex.getTrafficArea()) {
				TrafficArea trafficArea = property.getTrafficArea();

				if (trafficArea != null) {
					trafficAreaImporter.doImport(trafficArea, transportationComplexId);
					property.unsetTrafficArea();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TRAFFIC_AREA.getName(),
								href,
								transportationComplexId,
								"transportation_complex_id"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(transportationComplex, transportationComplexId, featureType);

		return transportationComplexId;
	}

}
