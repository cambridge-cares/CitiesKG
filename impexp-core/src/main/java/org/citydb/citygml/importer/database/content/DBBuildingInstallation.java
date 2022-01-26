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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.jena.graph.NodeFactory;
import org.citydb.citygml.common.database.xlink.DBXlinkBasic;
import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.util.CoreConstants;
import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.building.*;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;

public class DBBuildingInstallation extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBThematicSurface thematicSurfaceImporter;
	private GeometryConverter geometryConverter;
	private DBImplicitGeometry implicitGeometryImporter;
	private AttributeValueJoiner valueJoiner;
	private boolean affineTransformation;
	
	public DBBuildingInstallation(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		implicitGeometryImporter = importer.getImporter(DBImplicitGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		thematicSurfaceImporter = importer.getImporter(DBThematicSurface.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected String getTableName() {
		return TableEnum.BUILDING_INSTALLATION.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "buildinginstallation/";
	}

	@Override
	protected String getSQLStatement() {
		return  "insert into " + sqlSchema + ".building_installation (id, objectclass_id, class, class_codespace," +
				" function, function_codespace, usage, usage_codespace, building_id, room_id, " +
				"lod2_brep_id, lod3_brep_id, lod4_brep_id, lod2_other_geom, lod3_other_geom, lod4_other_geom, " +
				"lod2_implicit_rep_id, lod3_implicit_rep_id, lod4_implicit_rep_id, " +
				"lod2_implicit_ref_point, lod3_implicit_ref_point, lod4_implicit_ref_point, " +
				"lod2_implicit_transformation, lod3_implicit_transformation, lod4_implicit_transformation) values " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	}

	@Override
	protected String getSPARQLStatement() {
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + prefixOntoCityGML + "> " +
				"BASE <" + iriGraphBase + "> " +  // add BASE by SYL
				"INSERT DATA" +
				" { GRAPH <" + iriGraphObjectRel + "> " +
				"{ ? " + SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param +
				SchemaManagerAdapter.ONTO_CLASS + param +
				SchemaManagerAdapter.ONTO_CLASS_CODESPACE + param +
				SchemaManagerAdapter.ONTO_FUNCTION + param +
				SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE + param +
				SchemaManagerAdapter.ONTO_USAGE + param +
				SchemaManagerAdapter.ONTO_USAGE_CODESPACE + param +
				SchemaManagerAdapter.ONTO_BUILDING_ID + param +
				SchemaManagerAdapter.ONTO_ROOM_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD3_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD4_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_TRANSFORMATION + param +
		".}" +
				"}";

		return stmt;
	}

	protected long doImport(BuildingInstallation buildingInstallation) throws CityGMLImportException, SQLException {
		return doImport(buildingInstallation, null, 0);
	}

	protected long doImport(IntBuildingInstallation intBuildingInstallation) throws CityGMLImportException, SQLException {
		return doImport(intBuildingInstallation, null, 0);
	}

	public long doImport(BuildingInstallation buildingInstallation, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(buildingInstallation);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long buildingInstallationId = cityObjectImporter.doImport(buildingInstallation, featureType);

		int index = 0;
		URL objectURL = null;

		// import building information
		if (importer.isBlazegraph()) {
			try {
				String uuid = buildingInstallation.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				objectURL = new URL(iriGraphObject + uuid + "/");
			} catch (MalformedURLException e) {
				setBlankNode(preparedStatement, ++index);
			}
			preparedStatement.setURL(++index, objectURL);
			// primary id
			preparedStatement.setURL(++index, objectURL);
			buildingInstallation.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
		} else {
			// import building installation information
			// primary id
			preparedStatement.setLong(++index, buildingInstallationId);
		}

		// objectclass id

		preparedStatement.setLong(++index, featureType.getObjectClassId());

		// bldg:class
		if (buildingInstallation.isSetClazz() && buildingInstallation.getClazz().isSetValue()) {
			preparedStatement.setString(++index, buildingInstallation.getClazz().getValue());
			preparedStatement.setString(++index, buildingInstallation.getClazz().getCodeSpace());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:function
		if (buildingInstallation.isSetFunction()) {
			valueJoiner.join(buildingInstallation.getFunction(), Code::getValue, Code::getCodeSpace);
			String code = valueJoiner.result(0);
			String codespace = valueJoiner.result(1);

			if (code == null && importer.isBlazegraph()){
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, code);
			}

			if (codespace == null && importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:usage
		if (buildingInstallation.isSetUsage()) {
			valueJoiner.join(buildingInstallation.getUsage(), Code::getValue, Code::getCodeSpace);
			String code = valueJoiner.result(0);
			String codespace = valueJoiner.result(1);
			if (code == null && importer.isBlazegraph()){
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, code);
			}

			if (codespace == null && importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// parent id
		if (parent instanceof AbstractBuilding) {
			if (importer.isBlazegraph()) {
				preparedStatement.setURL(++index, (URL) parent.getLocalProperty("objectURL"));
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setLong(++index, parentId);
				preparedStatement.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			preparedStatement.setNull(++index, Types.NULL);
			preparedStatement.setLong(++index, parentId);
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setNull(++index, Types.NULL);
				preparedStatement.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXGeometry
		for (int i = 0; i < 3; i++) {
			GeometryProperty<? extends AbstractGeometry> geometryProperty = null;
			long geometryId = 0;
			GeometryObject geometryObject = null;

			switch (i) {
				case 0:
					geometryProperty = buildingInstallation.getLod2Geometry();
					break;
				case 1:
					geometryProperty = buildingInstallation.getLod3Geometry();
					break;
				case 2:
					geometryProperty = buildingInstallation.getLod4Geometry();
					break;
			}

			if (geometryProperty != null) {
				if (geometryProperty.isSetGeometry()) {
					AbstractGeometry abstractGeometry = geometryProperty.getGeometry();
					if (importer.isSurfaceGeometry(abstractGeometry))
						geometryId = surfaceGeometryImporter.doImport(abstractGeometry, buildingInstallationId);
					else if (importer.isPointOrLineGeometry(abstractGeometry))
						geometryObject = geometryConverter.getPointOrCurveGeometry(abstractGeometry);
					else
						importer.logOrThrowUnsupportedGeometryMessage(buildingInstallation, abstractGeometry);
					try {
						preparedStatement.setURL(
								++index - i,
								new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + geometryProperty.getGeometry().getId()));
					} catch (MalformedURLException e) {
						new CityGMLImportException(e);
					}
					geometryProperty.unsetGeometry();
				}   else {
					String href = geometryProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.BUILDING_INSTALLATION.getName(),
								buildingInstallationId,
								href,
								"lod" + (i + 2) + "_brep_id"));
					}
				}
			}

			if (geometryId != 0) {
				if (!importer.isBlazegraph()) {
					preparedStatement.setLong(++index, geometryId);
				}
			} else {
				if (importer.isBlazegraph()) {
					setBlankNode(preparedStatement, ++index - i);
				} else {
					preparedStatement.setNull(++index - i, Types.NULL);
				}
			}

			if (geometryObject != null) {
				preparedStatement.setObject(++index +2-i, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geometryObject, batchConn));
			} else if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index +2-i);
			} else
				preparedStatement.setNull(++index +2-i, nullGeometryType, nullGeometryTypeName);
		}

		// bldg:lodXImplicitRepresentation
		for (int i = 0; i < 3; i++) {
			ImplicitRepresentationProperty implicit = null;
			GeometryObject pointGeom = null;
			String matrixString = null;
			long implicitId = 0;

			switch (i) {
			case 0:
				implicit = buildingInstallation.getLod2ImplicitRepresentation();
				break;
			case 1:
				implicit = buildingInstallation.getLod3ImplicitRepresentation();
				break;
			case 2:
				implicit = buildingInstallation.getLod4ImplicitRepresentation();
				break;
			}

			if (implicit != null) {
				if (implicit.isSetObject()) {
					ImplicitGeometry geometry = implicit.getObject();

					// reference Point
					if (geometry.isSetReferencePoint())
						pointGeom = geometryConverter.getPoint(geometry.getReferencePoint());

					// transformation matrix
					if (geometry.isSetTransformationMatrix()) {
						Matrix matrix = geometry.getTransformationMatrix().getMatrix();
						if (affineTransformation)
							matrix = importer.getAffineTransformer().transformImplicitGeometryTransformationMatrix(matrix);

						matrixString = valueJoiner.join(" ", matrix.toRowPackedList());
					}

					// reference to IMPLICIT_GEOMETRY
					implicitId = implicitGeometryImporter.doImport(geometry);
				}
			}

			if (implicitId != 0) {
				preparedStatement.setLong(++index -2*i, implicitId);
			} else if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index -2*i);
			} else
				preparedStatement.setNull(++index -2*i, Types.NULL);

			if (pointGeom != null) {
				preparedStatement.setObject(++index +2-2*i, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
			} else if (importer.isBlazegraph()) {
					setBlankNode(preparedStatement, ++index +2-2*i);
			} else
				preparedStatement.setNull(++index +2-2*i, nullGeometryType, nullGeometryTypeName);

			if (matrixString != null) {
				preparedStatement.setString(++index +4-2*i, matrixString);
			} else if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index +4-2*i);
			} else
				preparedStatement.setNull(++index +4-2*i, Types.VARCHAR);
		}

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.BUILDING_INSTALLATION);

		// bldg:boundedBy
		if (buildingInstallation.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : buildingInstallation.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, buildingInstallation, buildingInstallationId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.THEMATIC_SURFACE.getName(),
								href,
								buildingInstallationId,
								"building_installation_id"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(buildingInstallation, buildingInstallationId, featureType);
		
		return buildingInstallationId;
	}

	public long doImport(IntBuildingInstallation intBuildingInstallation, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(intBuildingInstallation);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long intBuildingInstallationId = cityObjectImporter.doImport(intBuildingInstallation, featureType);

		int index = 0;
		URL objectURL = null;

		// import building information
		if (importer.isBlazegraph()) {
			try {
				String uuid = intBuildingInstallation.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				objectURL = new URL(iriGraphObject + uuid + "/");
			} catch (MalformedURLException e) {
				setBlankNode(preparedStatement, ++index);
			}
			preparedStatement.setURL(++index, objectURL);
			// primary id
			preparedStatement.setURL(++index, objectURL);
			intBuildingInstallation.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
		} else {
			// import interior building installation information
			// primary id
			preparedStatement.setLong(++index, intBuildingInstallationId);
		}


		// objectclass id
		preparedStatement.setLong(++index, featureType.getObjectClassId());

		// bldg:class
		if (intBuildingInstallation.isSetClazz() && intBuildingInstallation.getClazz().isSetValue()) {
			preparedStatement.setString(++index, intBuildingInstallation.getClazz().getValue());
			preparedStatement.setString(++index, intBuildingInstallation.getClazz().getCodeSpace());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		}	else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:function
		if (intBuildingInstallation.isSetFunction()) {
			valueJoiner.join(intBuildingInstallation.getFunction(), Code::getValue, Code::getCodeSpace);
			String code = valueJoiner.result(0);
			String codespace = valueJoiner.result(1);

			if (code == null && importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, code);
			}

			if (codespace == null && importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:usage
		if (intBuildingInstallation.isSetUsage()) {
			valueJoiner.join(intBuildingInstallation.getUsage(), Code::getValue, Code::getCodeSpace);
			String code = valueJoiner.result(0);
			String codespace = valueJoiner.result(1);
			if (code == null && importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, code);
			}

			if (codespace == null && importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// parent id
		if (parent instanceof AbstractBuilding) {
			if (importer.isBlazegraph()) {
				preparedStatement.setURL(++index, (URL) parent.getLocalProperty("objectURL"));
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setLong(++index, parentId);
				preparedStatement.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			preparedStatement.setNull(++index, Types.NULL);
			preparedStatement.setLong(++index, parentId);
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.NULL);
			preparedStatement.setNull(++index, Types.NULL);
		}	

		// bldg:lod4Geometry
		importer.setBlankNode(preparedStatement, ++index);
		importer.setBlankNode(preparedStatement, ++index);
		importer.setBlankNode(preparedStatement, ++index+1);
		importer.setBlankNode(preparedStatement, ++index+1);

		long geometryId = 0;
		GeometryObject geometryObject = null;

		if (intBuildingInstallation.isSetLod4Geometry()) {
			GeometryProperty<? extends AbstractGeometry> geometryProperty = intBuildingInstallation.getLod4Geometry();

			if (geometryProperty.isSetGeometry()) {
				AbstractGeometry abstractGeometry = geometryProperty.getGeometry();
				if (importer.isSurfaceGeometry(abstractGeometry))
					geometryId = surfaceGeometryImporter.doImport(abstractGeometry, intBuildingInstallationId);
				else if (importer.isPointOrLineGeometry(abstractGeometry))
					geometryObject = geometryConverter.getPointOrCurveGeometry(abstractGeometry);
				else 
					importer.logOrThrowUnsupportedGeometryMessage(intBuildingInstallation, abstractGeometry);
				try {
					preparedStatement.setURL(
							++index-2,
							new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + geometryProperty.getGeometry().getId()));
				} catch (MalformedURLException e) {
					new CityGMLImportException(e);
				}
				geometryProperty.unsetGeometry();
			} else {
				String href = geometryProperty.getHref();
				if (href != null && href.length() != 0) {
					importer.propagateXlink(new DBXlinkSurfaceGeometry(
							TableEnum.BUILDING_INSTALLATION.getName(),
							intBuildingInstallationId, 
							href, 
							"lod4_brep_id"));
				}
			}
		}

		if (geometryId != 0) {
			if(!importer.isBlazegraph()) {
				preparedStatement.setLong(++index-2, geometryId);
			}
		} else if(importer.isBlazegraph()) {
			setBlankNode(preparedStatement,++index-2);
		} else
			preparedStatement.setNull(++index-2, Types.NULL);

		if (geometryObject != null) {
			preparedStatement.setObject(++index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geometryObject, batchConn));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
		} else
			preparedStatement.setNull(++index, nullGeometryType, nullGeometryTypeName);

		// bldg:lod4ImplicitRepresentation
		importer.setBlankNode(preparedStatement, ++index);
		importer.setBlankNode(preparedStatement, ++index);
		importer.setBlankNode(preparedStatement, ++index+1);
		importer.setBlankNode(preparedStatement, ++index+1);
		importer.setBlankNode(preparedStatement, ++index+2);
		importer.setBlankNode(preparedStatement, ++index+2);

		GeometryObject pointGeom = null;
		String matrixString = null;
		long implicitId = 0;

		if (intBuildingInstallation.isSetLod4ImplicitRepresentation()) {
			ImplicitRepresentationProperty implicit = intBuildingInstallation.getLod4ImplicitRepresentation();

			if (implicit.isSetObject()) {
				ImplicitGeometry geometry = implicit.getObject();

				// reference Point
				if (geometry.isSetReferencePoint())
					pointGeom = geometryConverter.getPoint(geometry.getReferencePoint());

				// transformation matrix
				if (geometry.isSetTransformationMatrix()) {
					Matrix matrix = geometry.getTransformationMatrix().getMatrix();
					if (affineTransformation)
						matrix = importer.getAffineTransformer().transformImplicitGeometryTransformationMatrix(matrix);

					matrixString = valueJoiner.join(" ", matrix.toRowPackedList());
				}

				// reference to IMPLICIT_GEOMETRY
				implicitId = implicitGeometryImporter.doImport(geometry);
			}
		}

		if (implicitId != 0) {
			preparedStatement.setLong(++index -4, implicitId);
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index -4);
		} else
			preparedStatement.setNull(++index -4, Types.NULL);

		if (pointGeom != null) {
			preparedStatement.setObject(++index -2, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index -2);
		} else
			preparedStatement.setNull(++index -2, nullGeometryType, nullGeometryTypeName);

		if (matrixString != null) {
			preparedStatement.setString(++index, matrixString);
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
		} else
			preparedStatement.setNull(++index, Types.VARCHAR);

		// bldg:boundedBy
		if (intBuildingInstallation.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : intBuildingInstallation.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, intBuildingInstallation, intBuildingInstallationId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.THEMATIC_SURFACE.getName(),
								href,
								intBuildingInstallationId,
								"building_installation_id"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(intBuildingInstallation, intBuildingInstallationId, featureType);

		return intBuildingInstallationId;
	}

}
