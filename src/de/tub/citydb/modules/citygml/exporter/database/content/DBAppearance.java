/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2013
 * Institute for Geodesy and Geoinformation Science
 * Technische Universitaet Berlin, Germany
 * http://www.gis.tu-berlin.de/
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package de.tub.citydb.modules.citygml.exporter.database.content;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.appearance.AbstractSurfaceData;
import org.citygml4j.model.citygml.appearance.AbstractTexture;
import org.citygml4j.model.citygml.appearance.Appearance;
import org.citygml4j.model.citygml.appearance.AppearanceProperty;
import org.citygml4j.model.citygml.appearance.Color;
import org.citygml4j.model.citygml.appearance.ColorPlusOpacity;
import org.citygml4j.model.citygml.appearance.GeoreferencedTexture;
import org.citygml4j.model.citygml.appearance.ParameterizedTexture;
import org.citygml4j.model.citygml.appearance.SurfaceDataProperty;
import org.citygml4j.model.citygml.appearance.TextureType;
import org.citygml4j.model.citygml.appearance.WrapMode;
import org.citygml4j.model.citygml.appearance.X3DMaterial;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.TransformationMatrix2x2;
import org.citygml4j.model.gml.base.StringOrRef;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.primitives.DirectPosition;
import org.citygml4j.model.gml.geometry.primitives.Point;
import org.citygml4j.model.gml.geometry.primitives.PointProperty;
import org.citygml4j.util.gmlid.DefaultGMLIdManager;
import org.citygml4j.xml.io.writer.CityGMLWriteException;

import de.tub.citydb.api.geometry.GeometryObject;
import de.tub.citydb.config.Config;
import de.tub.citydb.database.TypeAttributeValueEnum;
import de.tub.citydb.log.Logger;
import de.tub.citydb.modules.citygml.common.database.xlink.DBXlinkTextureFile;
import de.tub.citydb.modules.citygml.common.database.xlink.DBXlinkTextureFileEnum;
import de.tub.citydb.util.Util;

public class DBAppearance implements DBExporter {
	private final Logger LOG = Logger.getInstance();
	private final DBExporterManager dbExporterManager;
	private final Config config;
	private final Connection connection;
	private final DBExporterEnum type;

	private PreparedStatement psAppearance;
	private DBTextureParam textureParamExporter;
	private boolean exportTextureImage;
	private boolean uniqueFileNames;
	private String texturePath;
	private boolean useXLink;
	private boolean appendOldGmlId;
	private String gmlIdPrefix;
	private String pathSeparator;

	private HashSet<String> textureNameCache;

	public DBAppearance(DBExporterEnum type, Connection connection, Config config, DBExporterManager dbExporterManager) throws SQLException {
		if (type != DBExporterEnum.LOCAL_APPEARANCE && type != DBExporterEnum.GLOBAL_APPEARANCE)
			throw new IllegalArgumentException("Invalid type " + type + " for appearance exporter.");

		this.dbExporterManager = dbExporterManager;
		this.config = config;
		this.connection = connection;
		this.type = type;

		init();
	}

	private void init() throws SQLException {
		textureNameCache = new HashSet<String>();
		exportTextureImage = config.getProject().getExporter().getAppearances().isSetExportTextureFiles();
		uniqueFileNames = config.getProject().getExporter().getAppearances().isSetUniqueTextureFileNames();

		texturePath = config.getInternal().getExportTextureFilePath();
		pathSeparator = config.getProject().getExporter().getAppearances().isTexturePathAbsolute() ?
				File.separator : "/";

		useXLink = config.getProject().getExporter().getXlink().getFeature().isModeXLink();
		if (!useXLink) {
			appendOldGmlId = config.getProject().getExporter().getXlink().getFeature().isSetAppendId();
			gmlIdPrefix = config.getProject().getExporter().getXlink().getFeature().getIdPrefix();
		}

		String getTextureImageContentLength = dbExporterManager.getDatabaseAdapter().getSQLAdapter().getTextureImageContentLength("TEX_IMAGE", "sd");
		
		StringBuilder query = new StringBuilder();
		if (!config.getInternal().isTransformCoordinates()) {		
			query.append("select app.ID as APP_ID, app.GMLID as APP_GMLID, app.NAME as APP_NAME, app.NAME_CODESPACE as APP_NAME_CODESPACE, app.DESCRIPTION as APP_DESCRIPTION, app.THEME, ")
			.append("sd.ID as SD_ID, sd.GMLID as SD_GMLID, sd.NAME as SD_NAME, sd.NAME_CODESPACE as SD_NAME_CODESPACE, sd.DESCRIPTION as SD_DESCRIPTION, sd.IS_FRONT, upper(sd.TYPE) as TYPE, ")
			.append("sd.X3D_SHININESS, sd.X3D_TRANSPARENCY, sd.X3D_AMBIENT_INTENSITY, sd.X3D_SPECULAR_COLOR, sd.X3D_DIFFUSE_COLOR, sd.X3D_EMISSIVE_COLOR, sd.X3D_IS_SMOOTH, ")
			.append("sd.TEX_IMAGE_URI, COALESCE(").append(getTextureImageContentLength).append(", 0) as DB_TEX_IMAGE_SIZE, sd.TEX_MIME_TYPE, lower(sd.TEX_TEXTURE_TYPE) as TEX_TEXTURE_TYPE, lower(sd.TEX_WRAP_MODE) as TEX_WRAP_MODE, sd.TEX_BORDER_COLOR, ")
			.append("sd.GT_PREFER_WORLDFILE, sd.GT_ORIENTATION, sd.GT_REFERENCE_POINT ")
			.append("from APPEARANCE app inner join APPEAR_TO_SURFACE_DATA a2s on app.ID = a2s.APPEARANCE_ID inner join SURFACE_DATA sd on sd.ID=a2s.SURFACE_DATA_ID where ");
		} else {
			int srid = config.getInternal().getExportTargetSRS().getSrid();
			String transformOrNull = dbExporterManager.getDatabaseAdapter().getSQLAdapter().resolveDatabaseOperationName("geodb_util.transform_or_null");

			query.append("select app.ID as APP_ID, app.GMLID as APP_GMLID, app.NAME as APP_NAME, app.NAME_CODESPACE as APP_NAME_CODESPACE, app.DESCRIPTION as APP_DESCRIPTION, app.THEME, ")
			.append("sd.ID as SD_ID, sd.GMLID as SD_GMLID, sd.NAME as SD_NAME, sd.NAME_CODESPACE as SD_NAME_CODESPACE, sd.DESCRIPTION as SD_DESCRIPTION, sd.IS_FRONT, upper(sd.TYPE) as TYPE, ")
			.append("sd.X3D_SHININESS, sd.X3D_TRANSPARENCY, sd.X3D_AMBIENT_INTENSITY, sd.X3D_SPECULAR_COLOR, sd.X3D_DIFFUSE_COLOR, sd.X3D_EMISSIVE_COLOR, sd.X3D_IS_SMOOTH, ")
			.append("sd.TEX_IMAGE_URI, COALESCE(").append(getTextureImageContentLength).append(", 0) as DB_TEX_IMAGE_SIZE, sd.TEX_MIME_TYPE, lower(sd.TEX_TEXTURE_TYPE) as TEX_TEXTURE_TYPE, lower(sd.TEX_WRAP_MODE) as TEX_WRAP_MODE, sd.TEX_BORDER_COLOR, ")
			.append("sd.GT_PREFER_WORLDFILE, sd.GT_ORIENTATION, ")
			.append(transformOrNull).append("(sd.GT_REFERENCE_POINT, ").append(srid).append(") AS GT_REFERENCE_POINT ")
			.append("from APPEARANCE app inner join APPEAR_TO_SURFACE_DATA a2s on app.ID = a2s.APPEARANCE_ID inner join SURFACE_DATA sd on sd.ID=a2s.SURFACE_DATA_ID where ");
		}

		if (type == DBExporterEnum.LOCAL_APPEARANCE)
			query.append("app.CITYOBJECT_ID=?");
		else
			query.append("app.ID=?");

		psAppearance = connection.prepareStatement(query.toString());
		
		textureParamExporter = (DBTextureParam)dbExporterManager.getDBExporter(
				type == DBExporterEnum.LOCAL_APPEARANCE ? DBExporterEnum.LOCAL_APPEARANCE_TEXTUREPARAM : DBExporterEnum.GLOBAL_APPEARANCE_TEXTUREPARAM);
	}

	public void read(AbstractCityObject cityObject, long cityObjectId) throws SQLException {
		final List<Long> appearanceIds = new ArrayList<Long>();
		ResultSet rs = null;

		try {
			psAppearance.setLong(1, cityObjectId);
			rs = psAppearance.executeQuery();

			long currentAppearanceId = 0;
			Appearance appearance = null;

			while (rs.next()) {
				long appearanceId = rs.getLong("APP_ID");

				if (appearanceId != currentAppearanceId) {
					currentAppearanceId = appearanceId;

					int index = appearanceIds.indexOf(appearanceId);
					if (index == -1) {
						appearance = new Appearance();
						getAppearancePropeties(appearance, rs);

						// add appearance to cityobject
						cityObject.addAppearance(new AppearanceProperty(appearance));
						dbExporterManager.updateFeatureCounter(CityGMLClass.APPEARANCE);

						appearanceIds.add(appearanceId);
					} else
						appearance = cityObject.getAppearance().get(index).getAppearance();
				}

				// add surface data to appearance
				addSurfaceData(appearance, rs);
			}
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	public boolean read(DBSplittingResult splitter) throws SQLException, CityGMLWriteException {
		ResultSet rs = null;

		try {
			Appearance appearance = new Appearance();
			boolean isInited = false;

			long appearanceId = splitter.getPrimaryKey();
			psAppearance.setLong(1, appearanceId);
			rs = psAppearance.executeQuery();

			while (rs.next()) {
				if (!isInited) {
					getAppearancePropeties(appearance, rs);
					textureNameCache.clear();
					isInited = true;
				}

				// add surface data to appearance
				addSurfaceData(appearance, rs);
			}
			
			if (appearance.isSetSurfaceDataMember()) {
				dbExporterManager.print(appearance);
				dbExporterManager.updateFeatureCounter(CityGMLClass.APPEARANCE);
				return true;
			} 

			return false;
		} finally {
			if (rs != null)
				rs.close();
		}
	}
	
	private void getAppearancePropeties(Appearance appearance, ResultSet rs) throws SQLException {
		String gmlId = rs.getString("APP_GMLID");
		if (gmlId != null)
			appearance.setId(gmlId);

		String gmlName = rs.getString("APP_NAME");
		String gmlNameCodespace = rs.getString("APP_NAME_CODESPACE");

		Util.dbGmlName2featureName(appearance, gmlName, gmlNameCodespace);

		String description = rs.getString("APP_DESCRIPTION");
		if (description != null) {
			StringOrRef stringOrRef = new StringOrRef();
			stringOrRef.setValue(description);
			appearance.setDescription(stringOrRef);
		}

		String theme = rs.getString("THEME");
		if (theme != null)
			appearance.setTheme(theme);
	}

	private void addSurfaceData(Appearance appearance, ResultSet rs) throws SQLException {
		long surfaceDataId = rs.getLong("SD_ID");
		if (rs.wasNull())
			return;
		
		String surfaceDataType = rs.getString("TYPE");
		if (rs.wasNull() || surfaceDataType == null || surfaceDataType.length() == 0)
			return;

		AbstractSurfaceData surfaceData = null;		
		if (surfaceDataType.equals(TypeAttributeValueEnum.X3D_MATERIAL.toString().toUpperCase()))
			surfaceData = new X3DMaterial();
		else if (surfaceDataType.equals(TypeAttributeValueEnum.PARAMETERIZED_TEXTURE.toString().toUpperCase()))
			surfaceData = new ParameterizedTexture();
		else if (surfaceDataType.equals(TypeAttributeValueEnum.GEOREFERENCED_TEXTURE.toString().toUpperCase()))
			surfaceData = new GeoreferencedTexture();
		else
			return;

		String gmlId = rs.getString("SD_GMLID");
		if (gmlId != null) {
			// process xlink
			if (dbExporterManager.lookupAndPutGmlId(gmlId, surfaceDataId, CityGMLClass.ABSTRACT_SURFACE_DATA)) {
				if (useXLink) {
					SurfaceDataProperty surfaceDataProperty = new SurfaceDataProperty();
					surfaceDataProperty.setHref("#" + gmlId);

					appearance.addSurfaceDataMember(surfaceDataProperty);
					return;
				} else {
					String newGmlId = DefaultGMLIdManager.getInstance().generateUUID(gmlIdPrefix);
					if (appendOldGmlId)
						newGmlId += '-' + gmlId;

					gmlId = newGmlId;
				}
			}

			surfaceData.setId(gmlId);
		}
		
		// retrieve targets. if there are no targets we suppress this surface data object
		boolean hasTargets = textureParamExporter.read(surfaceData, surfaceDataId);
		if (!hasTargets)
			return;

		String gmlName = rs.getString("SD_NAME");
		String gmlNameCodespace = rs.getString("SD_NAME_CODESPACE");

		Util.dbGmlName2featureName(surfaceData, gmlName, gmlNameCodespace);

		String description = rs.getString("SD_DESCRIPTION");
		if (description != null) {
			StringOrRef stringOrRef = new StringOrRef();
			stringOrRef.setValue(description);
			surfaceData.setDescription(stringOrRef);
		}

		int isFront = rs.getInt("IS_FRONT");
		if (!rs.wasNull() && isFront == 0)
			surfaceData.setIsFront(false);

		if (surfaceData.getCityGMLClass() == CityGMLClass.X3D_MATERIAL) {
			X3DMaterial material = (X3DMaterial)surfaceData;

			double shininess = rs.getDouble("X3D_SHININESS");
			if (!rs.wasNull())
				material.setShininess(shininess);

			double transparency = rs.getDouble("X3D_TRANSPARENCY");
			if (!rs.wasNull())
				material.setTransparency(transparency);

			double ambientIntensity = rs.getDouble("X3D_AMBIENT_INTENSITY");
			if (!rs.wasNull())
				material.setAmbientIntensity(ambientIntensity);

			for (int i = 0; i < 3; i++) {
				String columnName = null;

				switch (i) {
				case 0:
					columnName = "X3D_SPECULAR_COLOR";
					break;
				case 1:
					columnName = "X3D_DIFFUSE_COLOR";
					break;
				case 2:
					columnName = "X3D_EMISSIVE_COLOR";
					break;
				}

				String colorString = rs.getString(columnName);
				if (colorString != null) {
					List<Double> colorList = Util.string2double(colorString, "\\s+");

					if (colorList != null && colorList.size() >= 3) {
						Color color = new Color(colorList.get(0), colorList.get(1), colorList.get(2));

						switch (i) {
						case 0:
							material.setSpecularColor(color);
							break;
						case 1:
							material.setDiffuseColor(color);
							break;
						case 2:
							material.setEmissiveColor(color);
							break;
						}
					} else {
						// database entry is incorrect
					}
				}
			}

			int isSmooth = rs.getInt("X3D_IS_SMOOTH");
			if (!rs.wasNull() && isSmooth == 1)
				material.setIsSmooth(true);
		}

		else if (surfaceData.getCityGMLClass() == CityGMLClass.PARAMETERIZED_TEXTURE ||
				surfaceData.getCityGMLClass() == CityGMLClass.GEOREFERENCED_TEXTURE) {
			AbstractTexture absTex = (AbstractTexture)surfaceData;

			long dbImageSize = rs.getLong("DB_TEX_IMAGE_SIZE");
			String imageURI = rs.getString("TEX_IMAGE_URI");
			if (imageURI != null) {
				if (uniqueFileNames) {
					String extension = Util.getFileExtension(imageURI);
					imageURI = "tex" + surfaceDataId + (extension != null ? "." + extension : "");
				}

				File file = new File(imageURI);
				String fileName = file.getName();
				if (texturePath != null)
					fileName = texturePath + pathSeparator + fileName;

				absTex.setImageURI(fileName);

				// export texture image from database
				if (exportTextureImage && (uniqueFileNames || !textureNameCache.contains(imageURI))) {
					if (dbImageSize > 0) {
						DBXlinkTextureFile xlink = new DBXlinkTextureFile(
								surfaceDataId,
								file.getName(),
								DBXlinkTextureFileEnum.TEXTURE_IMAGE);
						dbExporterManager.propagateXlink(xlink);
					} else {
						StringBuilder msg = new StringBuilder(Util.getFeatureSignature(
								absTex.getCityGMLClass(), 
								absTex.getId()));
						msg.append(": Skipping 0 byte texture file ' ");
						msg.append(imageURI);
						msg.append("'.");

						LOG.warn(msg.toString());
					}

					if (!uniqueFileNames)
						textureNameCache.add(imageURI);
				}
			}

			String mimeType = rs.getString("TEX_MIME_TYPE");
			if (mimeType != null)
				absTex.setMimeType(new Code(mimeType));

			String textureType = rs.getString("TEX_TEXTURE_TYPE");
			if (textureType != null) {
				TextureType type = TextureType.fromValue(textureType);
				absTex.setTextureType(type);
			}

			String wrapMode = rs.getString("TEX_WRAP_MODE");
			if (wrapMode != null) {
				WrapMode mode = WrapMode.fromValue(wrapMode);
				absTex.setWrapMode(mode);
			}

			String borderColorString = rs.getString("TEX_BORDER_COLOR");
			if (borderColorString != null) {
				List<Double> colorList = Util.string2double(borderColorString, "\\s+");

				if (colorList != null && colorList.size() >= 4) {
					ColorPlusOpacity borderColor = new ColorPlusOpacity(
							colorList.get(0), colorList.get(1), colorList.get(2), colorList.get(3)						
							);

					absTex.setBorderColor(borderColor);
				} else {
					// database entry incorrect
				}
			}
		}

		if (surfaceData.getCityGMLClass() == CityGMLClass.GEOREFERENCED_TEXTURE) {
			GeoreferencedTexture geoTex = (GeoreferencedTexture)surfaceData;

			int preferWorldFile = rs.getInt("GT_PREFER_WORLDFILE");
			if (!rs.wasNull() && preferWorldFile == 0)
				geoTex.setPreferWorldFile(false);

			String orientationString = rs.getString("GT_ORIENTATION");
			if (orientationString != null) {
				List<Double> m = Util.string2double(orientationString, "\\s+");

				if (m != null && m.size() >= 4) {
					Matrix matrix = new Matrix(2, 2);
					matrix.setMatrix(m.subList(0, 4));

					geoTex.setOrientation(new TransformationMatrix2x2(matrix));
				}
			}

			Object referencePointObj = rs.getObject("GT_REFERENCE_POINT");
			if (!rs.wasNull() && referencePointObj != null) {
				GeometryObject pointObj = dbExporterManager.getDatabaseAdapter().getGeometryConverter().getPoint(referencePointObj);				

				if (pointObj != null) {
					double[] point = pointObj.getCoordinates(0);
					Point referencePoint = new Point();

					List<Double> value = new ArrayList<Double>();
					value.add(point[0]);
					value.add(point[1]);

					DirectPosition pos = new DirectPosition();
					pos.setValue(value);
					pos.setSrsDimension(2);
					referencePoint.setPos(pos);

					PointProperty pointProperty = new PointProperty(referencePoint);
					geoTex.setReferencePoint(pointProperty);
				}
			}
		}
		
		// finally add surface data to appearance
		SurfaceDataProperty surfaceDataProperty = new SurfaceDataProperty();
		surfaceDataProperty.setSurfaceData(surfaceData);
		appearance.addSurfaceDataMember(surfaceDataProperty);

		return;
	}

	public void clearLocalCache() {
		textureNameCache.clear();
	}

	@Override
	public void close() throws SQLException {
		psAppearance.close();
	}

	@Override
	public DBExporterEnum getDBExporterType() {
		return type;
	}

}
