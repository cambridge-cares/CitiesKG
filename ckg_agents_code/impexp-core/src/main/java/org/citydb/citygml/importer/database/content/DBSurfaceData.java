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

import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceDataToTexImage;
import org.citydb.citygml.common.database.xlink.DBXlinkTextureAssociation;
import org.citydb.citygml.common.database.xlink.DBXlinkTextureAssociationTarget;
import org.citydb.citygml.common.database.xlink.DBXlinkTextureCoordList;
import org.citydb.citygml.common.database.xlink.DBXlinkTextureParam;
import org.citydb.citygml.common.database.xlink.DBXlinkTextureParamEnum;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.citygml.importer.util.ExternalFileChecker;
import org.citydb.citygml.importer.util.LocalAppearanceHandler;
import org.citydb.citygml.importer.util.LocalAppearanceHandler.SurfaceGeometryTarget;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.SequenceEnum;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.log.Logger;
import org.citydb.util.CoreConstants;
import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.appearance.AbstractSurfaceData;
import org.citygml4j.model.citygml.appearance.AbstractTexture;
import org.citygml4j.model.citygml.appearance.AbstractTextureParameterization;
import org.citygml4j.model.citygml.appearance.Appearance;
import org.citygml4j.model.citygml.appearance.Color;
import org.citygml4j.model.citygml.appearance.GeoreferencedTexture;
import org.citygml4j.model.citygml.appearance.ParameterizedTexture;
import org.citygml4j.model.citygml.appearance.SurfaceDataProperty;
import org.citygml4j.model.citygml.appearance.TexCoordGen;
import org.citygml4j.model.citygml.appearance.TexCoordList;
import org.citygml4j.model.citygml.appearance.TextureAssociation;
import org.citygml4j.model.citygml.appearance.TextureCoordinates;
import org.citygml4j.model.citygml.appearance.X3DMaterial;
import org.citygml4j.model.citygml.core.TransformationMatrix2x2;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.primitives.DirectPosition;
import org.citygml4j.model.gml.geometry.primitives.Point;
import org.citygml4j.model.gml.geometry.primitives.PointProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DBSurfaceData implements DBImporter {
	private final Logger log = Logger.getInstance();
	private final Connection batchConn;
	private  final CityGMLImportManager importer;

	private PreparedStatement psX3DMaterial;
	private PreparedStatement psParaTex;
	private PreparedStatement psGeoTex;
	private DBTextureParam textureParamImporter;
	private DBTexImage textureImageImporter;
	private DBAppearToSurfaceData appearToSurfaceDataImporter;
	private LocalAppearanceHandler localAppearanceHandler;
	private AttributeValueJoiner valueJoiner;
	private ExternalFileChecker externalFileChecker;
	private int batchCounter;

	private int dbSrid;
	private boolean replaceGmlId;
	private boolean affineTransformation;
	private int nullGeometryType;
	private String nullGeometryTypeName;
	private String PREFIX_ONTOCITYGML;
	private String IRI_GRAPH_BASE;
	private String IRI_GRAPH_OBJECT;
	private static final String IRI_GRAPH_OBJECT_REL = "surfacedata/";

	public DBSurfaceData(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		this.batchConn = batchConn;
		this.importer = importer;

		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		replaceGmlId = config.getProject().getImporter().getGmlId().isUUIDModeReplace();
		dbSrid = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getConnectionMetaData().getReferenceSystem().getSrid();
		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		nullGeometryType = importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType();
		nullGeometryTypeName = importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName();
		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
		if (gmlIdCodespace != null)
			gmlIdCodespace = "'" + gmlIdCodespace + "', ";

		String sparqlStmtPart = null;

		String x3dStmt = "insert into " + schema + ".surface_data (id, gmlid, " + (gmlIdCodespace != null ? "gmlid_codespace, " : "") + "name, name_codespace, description, is_front, objectclass_id, " +
				"x3d_shininess, x3d_transparency, x3d_ambient_intensity, x3d_specular_color, x3d_diffuse_color, x3d_emissive_color, x3d_is_smooth) values " +
				"(?, ?, " + (gmlIdCodespace != null ? gmlIdCodespace : "") + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		String param = "  ?;";

		if (importer.isBlazegraph()) {
			PREFIX_ONTOCITYGML = importer.getOntoCityGmlPrefix();
			IRI_GRAPH_BASE = importer.getGraphBaseIri();
			IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;
			sparqlStmtPart = getSPARQLStatement(gmlIdCodespace);
			x3dStmt = getx3dStmt(sparqlStmtPart);
		}

		psX3DMaterial = batchConn.prepareStatement(x3dStmt);

		String paraStmt = "insert into " + schema + ".surface_data (id, gmlid, " + (gmlIdCodespace != null ? "gmlid_codespace, " : "") + "name, name_codespace, description, is_front, objectclass_id, " +
				"tex_texture_type, tex_wrap_mode, tex_border_color) values " +
				"(?, ?, " + (gmlIdCodespace != null ? gmlIdCodespace : "") + "?, ?, ?, ?, ?, ?, ?, ?)";

		if (importer.isBlazegraph()) {
			paraStmt = getParaStmt(sparqlStmtPart);
		}

		psParaTex = batchConn.prepareStatement(paraStmt);

		String geoStmt = "insert into " + schema + ".surface_data (id, gmlid, " + (gmlIdCodespace != null ? "gmlid_codespace, " : "") + "name, name_codespace, description, is_front, objectclass_id, " +
				"tex_texture_type, tex_wrap_mode, tex_border_color, " +
				"gt_prefer_worldfile, gt_orientation, gt_reference_point) values " +
				"(?, ?, " + (gmlIdCodespace != null ? gmlIdCodespace : "") + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		if (importer.isBlazegraph()) {
			geoStmt = getGeoStmt(sparqlStmtPart);
		}

		psGeoTex = batchConn.prepareStatement(geoStmt);

		textureParamImporter = importer.getImporter(DBTextureParam.class);
		textureImageImporter = importer.getImporter(DBTexImage.class);
		appearToSurfaceDataImporter = importer.getImporter(DBAppearToSurfaceData.class);
		localAppearanceHandler = importer.getLocalAppearanceHandler();
		valueJoiner = importer.getAttributeValueJoiner();
		externalFileChecker = importer.getExternalFileChecker();
	}

	private String getSPARQLStatement(String gmlIdCodespace){
		String param = "  ?;";
		String sparqlStmtPart = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_GML_ID + param +
				(gmlIdCodespace != null ? SchemaManagerAdapter.ONTO_GML_ID_CODESPACE + param : "") +
				SchemaManagerAdapter.ONTO_NAME + param +
				SchemaManagerAdapter.ONTO_NAME_CODESPACE + param +
				SchemaManagerAdapter.ONTO_DESCRIPTION + param +
				SchemaManagerAdapter.ONTO_IS_FRONT + param +
				SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param
				;

		return sparqlStmtPart;
	}

	private String getGeoStmt(String sparqlStmtPart){
		String param = "  ?;";
		String geoStmt = sparqlStmtPart +
				SchemaManagerAdapter.ONTO_TEX_TEXTURE_TYPE + param +
				SchemaManagerAdapter.ONTO_TEX_WRAP_MODE + param +
				SchemaManagerAdapter.ONTO_TEX_BORDER_COLOR + param +
				SchemaManagerAdapter.ONTO_GT_PREFER_WORDFILE + param +
				SchemaManagerAdapter.ONTO_GT_ORIENTATION + param +
				SchemaManagerAdapter.ONTO_GT_REFERENCE_POINT + param +
				".}}";
		return geoStmt;
	}

	private String getParaStmt(String sparqlStmtPart){
		String param = "  ?;";
		String paraStmt = sparqlStmtPart +
				SchemaManagerAdapter.ONTO_TEX_TEXTURE_TYPE + param +
				SchemaManagerAdapter.ONTO_TEX_WRAP_MODE + param +
				SchemaManagerAdapter.ONTO_TEX_BORDER_COLOR + param +
				".}}";

		return paraStmt;

	}

	private String getx3dStmt(String sparqlStmtPart){
		String param = "  ?;";
		String x3dStmt = sparqlStmtPart +
				SchemaManagerAdapter.ONTO_X3D_SHININESS + param +
				SchemaManagerAdapter.ONTO_X3D_TRANSPARENCY + param +
				SchemaManagerAdapter.ONTO_X3D_AMBIENT_INTENSITY + param +
				SchemaManagerAdapter.ONTO_X3D_SPECULAR_COLOR + param +
				SchemaManagerAdapter.ONTO_X3D_DIFFUSE_COLOR + param +
				SchemaManagerAdapter.ONTO_X3D_EMISSIVE_COLOR + param +
				SchemaManagerAdapter.ONTO_X3D_IS_SMOOTH + param +
				".}}";

		return x3dStmt;
	}


	public long doImport(AbstractSurfaceData surfaceData, long parentId, boolean isLocalAppearance) throws CityGMLImportException, SQLException {
		PreparedStatement psSurfaceData;

		FeatureType featureType = importer.getFeatureType(surfaceData);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		if (surfaceData instanceof X3DMaterial)
			psSurfaceData = psX3DMaterial;
		else if (surfaceData instanceof ParameterizedTexture)
			psSurfaceData = psParaTex;
		else if (surfaceData instanceof GeoreferencedTexture)
			psSurfaceData = psGeoTex;
		else {
			importer.logOrThrowErrorMessage(importer.getObjectSignature(surfaceData) +
					": Unsupported surface data type.");
			return 0;
		}

		long surfaceDataId = importer.getNextSequenceValue(SequenceEnum.SURFACE_DATA_ID_SEQ.getName());

		// gml:id
		String origGmlId = surfaceData.getId();		
		if (replaceGmlId) {
			String gmlId = importer.generateNewGmlId();

			// mapping entry
			if (surfaceData.isSetId())
				importer.putObjectUID(surfaceData.getId(), surfaceDataId, gmlId, featureType.getObjectClassId());

			surfaceData.setId(gmlId);

		} else {
			if (surfaceData.isSetId())
				importer.putObjectUID(surfaceData.getId(), surfaceDataId, featureType.getObjectClassId());
			else
				surfaceData.setId(importer.generateNewGmlId());
		}

		int index = 0;


		// import surface data information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				URL url = new URL(IRI_GRAPH_OBJECT + surfaceData.getId() + "/");
				psSurfaceData.setURL(++index, url);
				psSurfaceData.setURL(++index, url);
				surfaceData.setLocalProperty(CoreConstants.OBJECT_URIID, url);
			} catch (MalformedURLException e) {
				setBlankNode(psSurfaceData, ++index);
				setBlankNode(psSurfaceData, ++index);
			}
    } else {
      psSurfaceData.setLong(++index, surfaceDataId);
		}

		psSurfaceData.setString(++index, surfaceData.getId());

		// gml:name
		if (surfaceData.isSetName()) {
			valueJoiner.join(surfaceData.getName(), Code::getValue, Code::getCodeSpace);
			psSurfaceData.setString(++index, valueJoiner.result(0));
			String codespace = valueJoiner.result(1);
			if (codespace == null && importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(psSurfaceData, ++index);
			setBlankNode(psSurfaceData, ++index);
		} else {
			psSurfaceData.setNull(++index, Types.VARCHAR);
			psSurfaceData.setNull(++index, Types.VARCHAR);
		}

		// gml:description
		if (surfaceData.isSetDescription()) {
			String description = surfaceData.getDescription().getValue();
			if (description != null)
				description = description.trim();

			psSurfaceData.setString(++index, description);
		} else if (importer.isBlazegraph()) {
			setBlankNode(psSurfaceData, ++index);
		} else {
			psSurfaceData.setNull(++index, Types.VARCHAR);
		}

		// app:isFront
		if (surfaceData.isSetIsFront() && !surfaceData.getIsFront())
			psSurfaceData.setInt(++index, 0);
		else
			psSurfaceData.setInt(++index, 1);

		// objectclass id
		psSurfaceData.setInt(++index, featureType.getObjectClassId());

		// fill other columns depending on the type
		String featureSignature = importer.getObjectSignature(surfaceData, origGmlId);
		if (surfaceData instanceof X3DMaterial) {
			X3DMaterial material = (X3DMaterial) surfaceData;

			// app:shininess
			if (material.isSetShininess()) {
				psSurfaceData.setDouble(++index, material.getShininess());
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.DOUBLE);
			}

			// app:transparency
			if (material.isSetTransparency()) {
				psSurfaceData.setDouble(++index, material.getTransparency());
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.DOUBLE);
			}

			// app:ambientIntensity
			if (material.isSetAmbientIntensity()) {
				psSurfaceData.setDouble(++index, material.getAmbientIntensity());
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.DOUBLE);
			}

			// app:specularColor
			if (material.isSetSpecularColor()) {
				Color color = material.getSpecularColor();
				String colorString = color.getRed() + " " + color.getGreen() + " " + color.getBlue();
				psSurfaceData.setString(++index, colorString);
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.VARCHAR);
			}

			// app:diffuseColor
			if (material.isSetDiffuseColor()) {
				Color color = material.getDiffuseColor();
				String colorString = color.getRed() + " " + color.getGreen() + " " + color.getBlue();
				psSurfaceData.setString(++index, colorString);
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.VARCHAR);
			}

			// app:emissiveColor
			if (material.isSetEmissiveColor()) {
				Color color = material.getEmissiveColor();
				String colorString = color.getRed() + " " + color.getGreen() + " " + color.getBlue();
				psSurfaceData.setString(++index, colorString);
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.VARCHAR);
			}


			if (material.isSetIsSmooth() && material.getIsSmooth()) {
				psSurfaceData.setInt(++index, 1);
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setInt(++index, 0);
			}


			psSurfaceData.addBatch();
			if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
				importer.executeBatch(TableEnum.SURFACE_DATA);

			if (material.isSetTarget()) {
				HashSet<String> duplicateTargets = new HashSet<>(material.getTarget().size());

				for (String target : material.getTarget()) {
					if (target != null && target.length() != 0) {

						// check for duplicate targets
						if (!duplicateTargets.add(target.replaceAll("^#", ""))) {
							log.debug(featureSignature + ": Skipping duplicate target '" + target + "'.");
							continue;
						}

						importer.propagateXlink(new DBXlinkTextureParam(
								surfaceDataId,
								target,
								DBXlinkTextureParamEnum.X3DMATERIAL));
					}
				}
			}
		}

		else {
			AbstractTexture absTex = (AbstractTexture)surfaceData;

			// handle texture image
			long texImageId;
			if (absTex.isSetImageURI()) {
				texImageId = textureImageImporter.doImport(absTex, surfaceDataId);
				if (texImageId != 0) {
					importer.propagateXlink(new DBXlinkSurfaceDataToTexImage(
							surfaceDataId, 
							texImageId));
				}
			}

			if (absTex.isSetTextureType()) {
				psSurfaceData.setString(++index, absTex.getTextureType().getValue());
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.VARCHAR);
			}

			if (absTex.isSetWrapMode()) {
				psSurfaceData.setString(++index, absTex.getWrapMode().getValue());
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.VARCHAR);
			}

			if (absTex.isSetBorderColor()) {
				psSurfaceData.setString(++index, valueJoiner.join(" ", absTex.getBorderColor().toList()));
			} else if (importer.isBlazegraph()) {
				setBlankNode(psSurfaceData, ++index);
			} else {
				psSurfaceData.setNull(++index, Types.VARCHAR);
			}

			// ParameterizedTexture
			if (surfaceData instanceof ParameterizedTexture) {
				psSurfaceData.addBatch();
				if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
					importer.executeBatch(TableEnum.SURFACE_DATA);

				ParameterizedTexture paraTex = (ParameterizedTexture)surfaceData;
				if (paraTex.isSetTarget()) {
					HashSet<String> duplicateTargets = new HashSet<>(paraTex.getTarget().size());
					int targetId = 0;

					for (TextureAssociation target : paraTex.getTarget()) {
						String targetURI = target.getUri();
						if (targetURI == null || targetURI.length() == 0)
							continue;

						// check for duplicate targets
						if (!duplicateTargets.add(targetURI.replaceAll("^#", ""))) {
							log.debug(featureSignature + ": Skipping duplicate target '" + targetURI + "'.");
							continue;
						}

						if (target.isSetTextureParameterization()) {
							AbstractTextureParameterization texPara = target.getTextureParameterization();
							String texParamGmlId = texPara.getId();

							if (texPara.getCityGMLClass() == CityGMLClass.TEX_COORD_GEN) {
								TexCoordGen texCoordGen = (TexCoordGen)texPara;
								if (!texCoordGen.isSetWorldToTexture())
									continue;

								Matrix worldToTexture = texCoordGen.getWorldToTexture().getMatrix();
								if (affineTransformation)
									worldToTexture = importer.getAffineTransformer().transformWorldToTexture(worldToTexture);

								String worldToTextureString = valueJoiner.join(" ", worldToTexture.toRowPackedList());

								boolean isResolved = false;
								if (isLocalAppearance) {
									// check whether we can query the target from the in-memory gml:id cache
									long surfaceGeometryId = importer.getGeometryIdFromMemory(targetURI.replaceAll("^#", ""));
									isResolved = surfaceGeometryId > 0;

									if (isResolved) {
										textureParamImporter.doImport(worldToTextureString, surfaceDataId, surfaceGeometryId);

										if (texParamGmlId != null) {
											// make sure xlinks to this texture parameterization can be resolved
											importer.propagateXlink(new DBXlinkTextureAssociationTarget(
													surfaceDataId,
													surfaceGeometryId,
													texParamGmlId));
										}
									}
								}

								if (!isResolved) {
									DBXlinkTextureParam xlink = new DBXlinkTextureParam(
											surfaceDataId,
											targetURI,
											DBXlinkTextureParamEnum.TEXCOORDGEN);

									xlink.setTextureParameterization(true);
									xlink.setTexParamGmlId(texParamGmlId);
									xlink.setWorldToTexture(worldToTextureString);

									importer.propagateXlink(xlink);
								}

							} else {
								TexCoordList texCoordList = (TexCoordList)texPara;
								if (!texCoordList.isSetTextureCoordinates())
									continue;

								targetId++;
								int texCoordId = 0;

								for (TextureCoordinates texCoord : texCoordList.getTextureCoordinates()) {
									String ring = texCoord.getRing();
									if (ring == null || ring.length() == 0 || !texCoord.isSetValue()) {
										continue;
									}

									String ringId = ring.replaceAll("^#", "");

									// check for duplicate references to the same geometry object
									if (!duplicateTargets.add(ringId)) {
										log.debug(featureSignature + ": Skipping duplicate target ring '" + ringId + "'.");
										continue;
									}

									// fix unclosed texture coordinates
									int nrOfCoord = texCoord.getValue().size();
									if (!texCoord.getValue().get(0).equals(texCoord.getValue().get(nrOfCoord - 2)) ||
											!texCoord.getValue().get(1).equals(texCoord.getValue().get(nrOfCoord - 1))) {
										texCoord.getValue().add(texCoord.getValue().get(0));
										texCoord.getValue().add(texCoord.getValue().get(1));
										log.debug(featureSignature + ": Fixed unclosed texture coordinates for ring '" + ringId + "'.");
									}

									// check for minimum number of texture coordinates
									if (texCoord.getValue().size() < 8) {
										importer.logOrThrowErrorMessage(featureSignature + ": Less than four texture coordinates for ring '" + ringId + "'.");
										continue;
									}

									// check for even number of texture coordinates
									if ((texCoord.getValue().size() & 1) == 1) {
										String msg = featureSignature + ": Odd number of texture coordinates for ring '" + ringId + "'.";
										if (!importer.isFailOnError()) {
											log.error(msg);
											continue;
										} else
											throw new CityGMLImportException(msg);
									}

									boolean isResolved = false;
									if (isLocalAppearance) {
										// announce texture coordinates to local appearance handler. this will return false
										// in case there is no corresponding linear ring
										isResolved = localAppearanceHandler.setTextureCoordinates(ringId, texCoord.getValue());
									}

									if (!isResolved) {
										// in case of a global appearance or lacking information for resolving
										// a local appearance, we send the target information to the cache
										double[] coordinates = new double[texCoord.getValue().size()];
										for (int i = 0; i < texCoord.getValue().size(); i++)
											coordinates[i] = texCoord.getValue().get(i);

										importer.propagateXlink(new DBXlinkTextureCoordList(
												surfaceDataId, 
												ringId, 
												texCoordId++ == 0 ? texParamGmlId : null, 
														GeometryObject.createPolygon(coordinates, 2, 0),
														targetId));
									}
								}

								if (isLocalAppearance) {
									for (SurfaceGeometryTarget tmp : localAppearanceHandler.getLocalContext()) {
										if (!tmp.isComplete()) {
											importer.logOrThrowErrorMessage(featureSignature + ": Not all rings in target geometry '" + targetURI + "' receive texture coordinates. Skipping target.");
											continue;
										}

										textureParamImporter.doImport(tmp, surfaceDataId);

										// make sure xlinks to this texture parameterization can be resolved
										if (texParamGmlId != null) {
											importer.propagateXlink(new DBXlinkTextureAssociationTarget(
													surfaceDataId,
													tmp.getSurfaceGeometryId(),
													texParamGmlId));
										}
									}

									localAppearanceHandler.clearLocalContext();
								}
							}

						} else {
							String href = target.getHref();

							if (href != null && href.length() != 0) {
								importer.propagateXlink(new DBXlinkTextureAssociation(
										surfaceDataId,
										href,
										targetURI));
							}
						}
					}
				}
			}

			// GeoreferencedTexture
			else if (surfaceData instanceof GeoreferencedTexture) {
				GeoreferencedTexture geoTex = (GeoreferencedTexture)surfaceData;

				// check for a world file if there is no inline georeference
				if (!geoTex.isSetOrientation() && !geoTex.isSetReferencePoint())
					processWorldFile(geoTex);

				psSurfaceData.setInt(++index, geoTex.isSetPreferWorldFile() && !geoTex.getPreferWorldFile() ? 0 : 1);

				if (geoTex.isSetOrientation()) {
					Matrix orientation = geoTex.getOrientation().getMatrix();
					if (affineTransformation)
						orientation = importer.getAffineTransformer().transformGeoreferencedTextureOrientation(orientation);

					psSurfaceData.setString(++index, valueJoiner.join(" ", orientation.toRowPackedList()));
				} else if (importer.isBlazegraph()) {
					setBlankNode(psSurfaceData, ++index);
				}  else {
					psSurfaceData.setNull(++index, Types.VARCHAR);
				}

				GeometryObject geom = null;
				if (geoTex.isSetReferencePoint()) {
					PointProperty property = geoTex.getReferencePoint();

					// the CityGML spec states that the referencePoint shall be 2d only
					if (property.isSetPoint()) {
						Point point = property.getPoint();
						List<Double> coords = point.toList3d();

						if (coords != null && !coords.isEmpty()) {
							if (affineTransformation)
								importer.getAffineTransformer().transformCoordinates(coords);

							geom = GeometryObject.createPoint(new double[]{coords.get(0), coords.get(1)}, 2, dbSrid);
						}
					} else {
						String href = property.getHref();
						if (href != null && href.length() != 0)
							importer.logOrThrowUnsupportedXLinkMessage(geoTex, Point.class, href);
					}
				}

				if (geom != null) {
					psSurfaceData.setObject(++index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geom, batchConn));
				} else if (importer.isBlazegraph()) {
					setBlankNode(psSurfaceData, ++index);
				} else {
					psSurfaceData.setNull(++index, nullGeometryType, nullGeometryTypeName);
				}

				psSurfaceData.addBatch();
				if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
					importer.executeBatch(TableEnum.SURFACE_DATA);

				if (geoTex.isSetTarget()) {
					HashSet<String> duplicateTargets = new HashSet<>(geoTex.getTarget().size());

					for (String target : geoTex.getTarget()) {
						if (target != null && target.length() != 0) {

							// check for duplicate targets
							if (!duplicateTargets.add(target.replaceAll("^#", ""))) {
								log.debug(featureSignature + ": Skipping duplicate target '" + target + "'.");
								continue;
							}

							boolean isResolved = false;
							if (isLocalAppearance) {
								// check whether we can query the target from the in-memory gml:id cache
								long surfaceGeometryId = importer.getGeometryIdFromMemory(target.replaceAll("^#", ""));
								isResolved = surfaceGeometryId > 0;
								if (isResolved)
									textureParamImporter.doImport(surfaceDataId, surfaceGeometryId);								
							}

							if (!isResolved) {
								importer.propagateXlink(new DBXlinkTextureParam(
										surfaceDataId,
										target,
										DBXlinkTextureParamEnum.GEOREFERENCEDTEXTURE
										));
							}
						}
					}
				}
			}
		}

		// appearance to surface data
		appearToSurfaceDataImporter.doImport(surfaceData.getLocalProperty(CoreConstants.OBJECT_URIID),
				((Appearance) ((SurfaceDataProperty) surfaceData.getParent()).getParent())
						.getLocalProperty(CoreConstants.OBJECT_URIID));
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(surfaceData, surfaceDataId, featureType);

		return surfaceDataId;
	}

	private void processWorldFile(GeoreferencedTexture geoTex) {
		String imageFileURI = geoTex.getImageURI();
		List<String> candidates = new ArrayList<>();

		// we assume the following naming scheme for world files:
		// 1) if the image file name has a 3-character extension (image.tif), the world file
		// has the same name followed by an extension containing the first and last letters
		// of the image's extension and ending with a 'w' (image.tfw).
		// 2) if the extension has more or less than 3 characters, including no extension at all,
		// then the world file name is formed by simply appending a 'w' to the image file name.

		// add candidate according to first scheme
		int index = imageFileURI.lastIndexOf('.');
		if (index != -1) {
			String name = imageFileURI.substring(0, index + 1);
			String extension = imageFileURI.substring(index + 1);
			if (extension.length() == 3)
				candidates.add(name + extension.substring(0, 1) + extension.substring(2, 3) + 'w');
		}

		// add candidate according to second scheme
		candidates.add(imageFileURI + "w");

		for (String candidate : candidates) {
			Path file = null;
			try {
				Map.Entry<String, String> fileInfo = externalFileChecker.getFileInfo(candidate);

				try {
					file = Paths.get(fileInfo.getKey());
				} catch (InvalidPathException ignored) {
					//
				}

				if (file == null || !file.isAbsolute())
					file = externalFileChecker.getInputFile().resolve(fileInfo.getKey());
			} catch (IOException e) {
				continue;
			}

			log.info("Processing world file: " + candidate);

			try (BufferedReader in = new BufferedReader(new InputStreamReader(Files.newInputStream(file)))) {
				double[] content = new double[6];
				int i = 0;

				String line;
				while ((line = in.readLine()) != null && i < content.length)
					content[i++] = Double.parseDouble(line);

				if (i == 6) {
					// interpretation of world file content taken from CityGML specification document version 1.0.0
					Matrix matrix = new Matrix(2, 2);
					matrix.setMatrix(Arrays.asList(content[0], content[2], content[1], content[3]));
					geoTex.setOrientation(new TransformationMatrix2x2(matrix));

					Point point = new Point();
					DirectPosition pos = new DirectPosition();
					pos.setValue(Arrays.asList(content[4], content[5]));
					point.setPos(pos);
					geoTex.setReferencePoint(new PointProperty(point));
				} else {
					log.error("Error while processing world file '" + candidate + "': Content could not be interpreted.");
					break;
				}
			} catch (IOException e) {
				log.error("Error while processing world file '" + candidate +"': " + e.getMessage());
				break;
			} catch (NumberFormatException e) {
				log.error("Error while processing world file '" + candidate +"': Content could not be interpreted.");
				break;
			}
		}
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psX3DMaterial.executeBatch();
			psParaTex.executeBatch();
			psGeoTex.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psX3DMaterial.close();
		psParaTex.close();
		psGeoTex.close();
	}

	/**
	 * Sets blank nodes on PreparedStatements. Used with SPARQL which does not support nulls.
	 */
	private void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}

}
