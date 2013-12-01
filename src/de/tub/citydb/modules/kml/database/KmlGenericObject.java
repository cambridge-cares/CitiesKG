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
package de.tub.citydb.modules.kml.database;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.media.j3d.GeometryArray;
import javax.vecmath.Point3d;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.opengis.kml._2.AltitudeModeEnumType;
import net.opengis.kml._2.BoundaryType;
import net.opengis.kml._2.LinearRingType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.LocationType;
import net.opengis.kml._2.ModelType;
import net.opengis.kml._2.MultiGeometryType;
import net.opengis.kml._2.OrientationType;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PolygonType;

import org.citygml.textureAtlas.TextureAtlasCreator;
import org.citygml.textureAtlas.image.ImageReader;
import org.citygml.textureAtlas.model.TextureImage;
import org.citygml.textureAtlas.model.TextureImagesInfo;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.appearance.Color;
import org.citygml4j.model.citygml.appearance.X3DMaterial;
import org.collada._2005._11.colladaschema.Accessor;
import org.collada._2005._11.colladaschema.Asset;
import org.collada._2005._11.colladaschema.BindMaterial;
import org.collada._2005._11.colladaschema.COLLADA;
import org.collada._2005._11.colladaschema.CommonColorOrTextureType;
import org.collada._2005._11.colladaschema.CommonFloatOrParamType;
import org.collada._2005._11.colladaschema.CommonNewparamType;
import org.collada._2005._11.colladaschema.Effect;
import org.collada._2005._11.colladaschema.Extra;
import org.collada._2005._11.colladaschema.FloatArray;
import org.collada._2005._11.colladaschema.FxSampler2DCommon;
import org.collada._2005._11.colladaschema.FxSurfaceCommon;
import org.collada._2005._11.colladaschema.FxSurfaceInitFromCommon;
import org.collada._2005._11.colladaschema.Geometry;
import org.collada._2005._11.colladaschema.Image;
import org.collada._2005._11.colladaschema.InputLocal;
import org.collada._2005._11.colladaschema.InputLocalOffset;
import org.collada._2005._11.colladaschema.InstanceEffect;
import org.collada._2005._11.colladaschema.InstanceGeometry;
import org.collada._2005._11.colladaschema.InstanceMaterial;
import org.collada._2005._11.colladaschema.InstanceWithExtra;
import org.collada._2005._11.colladaschema.LibraryEffects;
import org.collada._2005._11.colladaschema.LibraryGeometries;
import org.collada._2005._11.colladaschema.LibraryImages;
import org.collada._2005._11.colladaschema.LibraryMaterials;
import org.collada._2005._11.colladaschema.LibraryVisualScenes;
import org.collada._2005._11.colladaschema.Material;
import org.collada._2005._11.colladaschema.Mesh;
import org.collada._2005._11.colladaschema.ObjectFactory;
import org.collada._2005._11.colladaschema.Param;
import org.collada._2005._11.colladaschema.ProfileCOMMON;
import org.collada._2005._11.colladaschema.Source;
import org.collada._2005._11.colladaschema.Technique;
import org.collada._2005._11.colladaschema.Triangles;
import org.collada._2005._11.colladaschema.UpAxisType;
import org.collada._2005._11.colladaschema.Vertices;
import org.collada._2005._11.colladaschema.VisualScene;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.j3d.utils.geometry.GeometryInfo;

import de.tub.citydb.api.database.DatabaseGeometryConverter;
import de.tub.citydb.api.database.DatabaseSrs;
import de.tub.citydb.api.event.EventDispatcher;
import de.tub.citydb.api.geometry.GeometryObject;
import de.tub.citydb.api.geometry.GeometryObject.ElementType;
import de.tub.citydb.api.geometry.GeometryObject.GeometryType;
import de.tub.citydb.api.log.LogLevel;
import de.tub.citydb.config.Config;
import de.tub.citydb.config.project.kmlExporter.Balloon;
import de.tub.citydb.config.project.kmlExporter.ColladaOptions;
import de.tub.citydb.config.project.kmlExporter.DisplayForm;
import de.tub.citydb.config.project.kmlExporter.KmlExporter;
import de.tub.citydb.database.TypeAttributeValueEnum;
import de.tub.citydb.database.adapter.AbstractDatabaseAdapter;
import de.tub.citydb.database.adapter.TextureImageExportAdapter;
import de.tub.citydb.log.Logger;
import de.tub.citydb.modules.common.event.CounterEvent;
import de.tub.citydb.modules.common.event.CounterType;
import de.tub.citydb.modules.common.event.GeometryCounterEvent;
import de.tub.citydb.util.Util;

public abstract class KmlGenericObject {

	protected static final int POINT = 1;
	protected static final int LINE_STRING = 2;
	protected static final int EXTERIOR_POLYGON_RING = 1003;
	protected static final int INTERIOR_POLYGON_RING = 2003;

	protected static final int GEOMETRY_AMOUNT_WARNING = 10000;

	/** Tolerance after triangulation must be bigger than before triangulation since some points
	 * may deviate 0.00999999 before and 0.01000001 after. Using a single bigger tolerance value
	 * does not help since the effect repeats itself (0.01999999 vs. 0.0200001).
	 * 
	 * Tolerance after triangulation must not be much bigger than tolerance before, otherwise
	 * there is a risk of going up the wrong node tree when searching for a vertex
	 */
	private final static double TOLERANCE_BEFORE_TRIANGULATION = 0.015d; // this is very tolerant!!!
	private final static double TOLERANCE_AFTER_TRIANGULATION = 0.0150005d; // this is very tolerant!!!

	private final static String NO_TEXIMAGE = "default";

	private HashMap<Long, GeometryInfo> geometryInfos = new HashMap<Long, GeometryInfo>();
	// coordinates include texCoordinates, which geometryInfo does not
	// texCoordinates in geometryInfo would be float --> precision loss
	private NodeZ coordinateTree;

	// key is surfaceId, surfaceId is originally a Long, here we use an Object for compatibility with the textureAtlasAPI
	private HashMap<Object, String> texImageUris = new HashMap<Object, String>();
	// key is imageUri
	private HashMap<String, TextureImage> texImages = new HashMap<String, TextureImage>();
	// for images in unusual formats or wrapping textures. Most times it will be null.
	// key is imageUri
	private HashMap<String, Long> unsupportedTexImageIds = null;
	// key is surfaceId, surfaceId is originally a Long
	private HashMap<Long, X3DMaterial> x3dMaterials = null;

	private long id;
	private String gmlId;
	private BigInteger vertexIdCounter = new BigInteger("-1");
	protected VertexInfo firstVertexInfo = null;
	private VertexInfo lastVertexInfo = null;

	// origin of the relative coordinates for the object
	private double originX;
	private double originY;
	private double originZ;

	// placemark location in WGS84
	private double locationX;
	private double locationY;
	private double locationZ;

	private double zOffset;

	private boolean ignoreSurfaceOrientation = true;

	protected Connection connection;
	protected KmlExporterManager kmlExporterManager;
	protected net.opengis.kml._2.ObjectFactory kmlFactory;
	protected AbstractDatabaseAdapter databaseAdapter;
	protected TextureImageExportAdapter textureExportAdapter;
	protected DatabaseGeometryConverter geometryConverterAdapter;
	protected ElevationServiceHandler elevationServiceHandler;
	protected BalloonTemplateHandlerImpl balloonTemplateHandler;
	protected EventDispatcher eventDispatcher;
	protected Config config;

	protected int currentLod;
	protected DatabaseSrs dbSrs;
	protected X3DMaterial defaultX3dMaterial;

	private SimpleDateFormat dateFormatter;

	protected KmlGenericObject(Connection connection,
			KmlExporterManager kmlExporterManager,
			net.opengis.kml._2.ObjectFactory kmlFactory,
			AbstractDatabaseAdapter databaseAdapter,
			TextureImageExportAdapter textureExportAdapter,
			ElevationServiceHandler elevationServiceHandler,
			BalloonTemplateHandlerImpl balloonTemplateHandler,
			EventDispatcher eventDispatcher,
			Config config) {

		this.connection = connection;
		this.kmlExporterManager = kmlExporterManager;
		this.kmlFactory = kmlFactory;
		this.textureExportAdapter = textureExportAdapter;
		this.elevationServiceHandler = elevationServiceHandler;
		this.balloonTemplateHandler = balloonTemplateHandler;
		this.eventDispatcher = eventDispatcher;
		this.config = config;

		this.databaseAdapter = databaseAdapter;
		geometryConverterAdapter = databaseAdapter.getGeometryConverter();
		dbSrs = databaseAdapter.getConnectionMetaData().getReferenceSystem();

		dateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

		defaultX3dMaterial = new X3DMaterial();
		defaultX3dMaterial.setAmbientIntensity(0.2d);
		defaultX3dMaterial.setShininess(0.2d);
		defaultX3dMaterial.setTransparency(0d);
		defaultX3dMaterial.setDiffuseColor(getX3dColorFromString("0.8 0.8 0.8"));
		defaultX3dMaterial.setSpecularColor(getX3dColorFromString("1.0 1.0 1.0"));
		defaultX3dMaterial.setEmissiveColor(getX3dColorFromString("0.0 0.0 0.0"));
	}

	public abstract void read(KmlSplittingResult work);
	public abstract String getStyleBasisName();
	public abstract ColladaOptions getColladaOptions();
	public abstract Balloon getBalloonSettings();
	protected abstract List<DisplayForm> getDisplayForms();
	protected abstract String getHighlightingQuery();


	protected BalloonTemplateHandlerImpl getBalloonTemplateHandler() {
		return balloonTemplateHandler;
	}

	protected void setBalloonTemplateHandler(BalloonTemplateHandlerImpl balloonTemplateHandler) {
		this.balloonTemplateHandler = balloonTemplateHandler;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setGmlId(String gmlId) {
		this.gmlId = gmlId.replace(':', '_');
	}

	public String getGmlId() {
		return gmlId;
	}

	protected void setOriginX(double originX) {
		this.originX = originX;
	}

	protected double getOriginX() {
		return originX;
	}

	protected void setOriginY(double originY) {
		this.originY = originY;
	}

	protected double getOriginY() {
		return originY;
	}

	protected void setOriginZ(double originZ) {
		this.originZ = originZ;
	}

	protected double getOriginZ() {
		return originZ;
	}

	protected void setZOffset(double zOffset) {
		this.zOffset = zOffset;
	}

	protected double getZOffset() {
		return zOffset;
	}

	protected void setLocationX(double locationX) {
		this.locationX = locationX;
	}

	protected double getLocationX() {
		return locationX;
	}

	protected void setLocationY(double locationY) {
		this.locationY = locationY;
	}

	protected double getLocationY() {
		return locationY;
	}

	protected void setLocationZ(double locationZ) {
		this.locationZ = locationZ;
	}

	protected double getLocationZ() {
		return locationZ;
	}

	protected void setIgnoreSurfaceOrientation(boolean ignoreSurfaceOrientation) {
		this.ignoreSurfaceOrientation = ignoreSurfaceOrientation;
	}

	protected boolean isIgnoreSurfaceOrientation() {
		return ignoreSurfaceOrientation;
	}


	public COLLADA generateColladaTree() throws DatatypeConfigurationException{

		ObjectFactory colladaFactory = new ObjectFactory();

		// java and XML...
		DatatypeFactory df = DatatypeFactory.newInstance();
		XMLGregorianCalendar xmlGregorianCalendar = df.newXMLGregorianCalendar(new GregorianCalendar());
		xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);

		COLLADA	collada = colladaFactory.createCOLLADA();
		collada.setVersion("1.4.1");
		// --------------------------- asset ---------------------------

		Asset asset = colladaFactory.createAsset();
		asset.setCreated(xmlGregorianCalendar);
		asset.setModified(xmlGregorianCalendar);
		Asset.Unit unit = colladaFactory.createAssetUnit();
		unit.setName("meters");
		unit.setMeter(1.0);
		asset.setUnit(unit);
		asset.setUpAxis(UpAxisType.Z_UP);
		Asset.Contributor contributor = colladaFactory.createAssetContributor();
		// System.getProperty("line.separator") produces weird effects here
		contributor.setAuthoringTool(this.getClass().getPackage().getImplementationTitle() + ", version " +
				this.getClass().getPackage().getImplementationVersion() + "; " +
				this.getClass().getPackage().getImplementationVendor());
		asset.getContributor().add(contributor);
		collada.setAsset(asset);

		LibraryImages libraryImages = colladaFactory.createLibraryImages();
		LibraryMaterials libraryMaterials = colladaFactory.createLibraryMaterials();
		LibraryEffects libraryEffects = colladaFactory.createLibraryEffects();
		LibraryGeometries libraryGeometries = colladaFactory.createLibraryGeometries();
		LibraryVisualScenes libraryVisualScenes = colladaFactory.createLibraryVisualScenes();

		// --------------------------- geometry (constant part) ---------------------------
		Geometry geometry = colladaFactory.createGeometry();
		geometry.setId("geometry0");

		Source positionSource = colladaFactory.createSource();
		positionSource.setId("geometry0-position");

		FloatArray positionArray = colladaFactory.createFloatArray();
		positionArray.setId("geometry0-position-array");
		List<Double> positionValues = positionArray.getValue();
		positionSource.setFloatArray(positionArray);

		Accessor positionAccessor = colladaFactory.createAccessor();
		positionAccessor.setSource("#" + positionArray.getId());
		positionAccessor.setStride(new BigInteger("3"));
		Param paramX = colladaFactory.createParam();
		paramX.setType("float");
		paramX.setName("X");
		Param paramY = colladaFactory.createParam();
		paramY.setType("float");
		paramY.setName("Y");
		Param paramZ = colladaFactory.createParam();
		paramZ.setType("float");
		paramZ.setName("Z");
		positionAccessor.getParam().add(paramX);
		positionAccessor.getParam().add(paramY);
		positionAccessor.getParam().add(paramZ);
		Source.TechniqueCommon positionTechnique = colladaFactory.createSourceTechniqueCommon();
		positionTechnique.setAccessor(positionAccessor);
		positionSource.setTechniqueCommon(positionTechnique);

		Source texCoordsSource = colladaFactory.createSource();
		texCoordsSource.setId("geometry0-texCoords");

		FloatArray texCoordsArray = colladaFactory.createFloatArray();
		texCoordsArray.setId("geometry0-texCoords-array");
		List<Double> texCoordsValues = texCoordsArray.getValue();
		texCoordsSource.setFloatArray(texCoordsArray);

		Accessor texCoordsAccessor = colladaFactory.createAccessor();
		texCoordsAccessor.setSource("#" + texCoordsArray.getId());
		texCoordsAccessor.setStride(new BigInteger("2"));
		Param paramS = colladaFactory.createParam();
		paramS.setType("float");
		paramS.setName("S");
		Param paramT = colladaFactory.createParam();
		paramT.setType("float");
		paramT.setName("T");
		texCoordsAccessor.getParam().add(paramS);
		texCoordsAccessor.getParam().add(paramT);
		Source.TechniqueCommon texCoordsTechnique = colladaFactory.createSourceTechniqueCommon();
		texCoordsTechnique.setAccessor(texCoordsAccessor);
		texCoordsSource.setTechniqueCommon(texCoordsTechnique);

		Vertices vertices = colladaFactory.createVertices();
		vertices.setId("geometry0-vertex");
		InputLocal input = colladaFactory.createInputLocal();
		input.setSemantic("POSITION");
		input.setSource("#" + positionSource.getId());
		vertices.getInput().add(input);

		Mesh mesh = colladaFactory.createMesh();
		mesh.getSource().add(positionSource);
		mesh.getSource().add(texCoordsSource);
		mesh.setVertices(vertices);
		geometry.setMesh(mesh);
		libraryGeometries.getGeometry().add(geometry);
		BigInteger texCoordsCounter = BigInteger.ZERO;

		// --------------------------- visual scenes ---------------------------
		VisualScene visualScene = colladaFactory.createVisualScene();
		visualScene.setId("Building_" + gmlId);
		BindMaterial.TechniqueCommon techniqueCommon = colladaFactory.createBindMaterialTechniqueCommon();
		BindMaterial bindMaterial = colladaFactory.createBindMaterial();
		bindMaterial.setTechniqueCommon(techniqueCommon);
		InstanceGeometry instanceGeometry = colladaFactory.createInstanceGeometry();
		instanceGeometry.setUrl("#" + geometry.getId());
		instanceGeometry.setBindMaterial(bindMaterial);
		org.collada._2005._11.colladaschema.Node node = colladaFactory.createNode();
		node.getInstanceGeometry().add(instanceGeometry);
		visualScene.getNode().add(node);
		libraryVisualScenes.getVisualScene().add(visualScene);

		// --------------------------- now the variable part ---------------------------
		Triangles triangles = null;
		HashMap<String, Triangles> trianglesByTexImageName = new HashMap<String, Triangles>();

		// geometryInfos contains all surfaces, textured or not
		Set<Long> keySet = geometryInfos.keySet();
		Iterator<Long> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			Long surfaceId = iterator.next();
			String texImageName = texImageUris.get(surfaceId);
			X3DMaterial x3dMaterial = getX3dMaterial(surfaceId);
			boolean surfaceTextured = true;
			if (texImageName == null) {
				surfaceTextured = false;
				texImageName = (x3dMaterial != null) ?
						buildNameFromX3dMaterial(x3dMaterial):
							NO_TEXIMAGE; // <- should never happen
			}

			triangles = trianglesByTexImageName.get(texImageName);
			if (triangles == null) { // never worked on this image or material before

				// --------------------------- materials ---------------------------
				Material material = colladaFactory.createMaterial();
				material.setId(replaceExtensionWithSuffix(texImageName, "_mat"));
				InstanceEffect instanceEffect = colladaFactory.createInstanceEffect();
				instanceEffect.setUrl("#" + replaceExtensionWithSuffix(texImageName, "_eff"));
				material.setInstanceEffect(instanceEffect);
				libraryMaterials.getMaterial().add(material);

				// --------------------- effects common part 1 ---------------------
				Effect effect = colladaFactory.createEffect();
				effect.setId(replaceExtensionWithSuffix(texImageName, "_eff"));
				ProfileCOMMON profileCommon = colladaFactory.createProfileCOMMON();

				if (surfaceTextured) {
					// --------------------------- images ---------------------------
					Image image = colladaFactory.createImage();
					image.setId(replaceExtensionWithSuffix(texImageName, "_img"));
					image.setInitFrom(texImageName);
					libraryImages.getImage().add(image);

					// --------------------------- effects ---------------------------
					FxSurfaceInitFromCommon initFrom = colladaFactory.createFxSurfaceInitFromCommon();
					initFrom.setValue(image); // evtl. image.getId();
					FxSurfaceCommon surface = colladaFactory.createFxSurfaceCommon();
					surface.setType("2D"); // ColladaConstants.SURFACE_TYPE_2D
					surface.getInitFrom().add(initFrom);

					CommonNewparamType newParam1 = colladaFactory.createCommonNewparamType();
					newParam1.setSurface(surface);
					newParam1.setSid(replaceExtensionWithSuffix(texImageName, "_surface"));
					profileCommon.getImageOrNewparam().add(newParam1);

					FxSampler2DCommon sampler2D = colladaFactory.createFxSampler2DCommon();
					sampler2D.setSource(newParam1.getSid());
					CommonNewparamType newParam2 = colladaFactory.createCommonNewparamType();
					newParam2.setSampler2D(sampler2D);
					newParam2.setSid(replaceExtensionWithSuffix(texImageName, "_sampler"));
					profileCommon.getImageOrNewparam().add(newParam2);

					ProfileCOMMON.Technique profileCommonTechnique = colladaFactory.createProfileCOMMONTechnique();
					profileCommonTechnique.setSid("COMMON");
					ProfileCOMMON.Technique.Lambert lambert = colladaFactory.createProfileCOMMONTechniqueLambert();
					CommonColorOrTextureType.Texture texture = colladaFactory.createCommonColorOrTextureTypeTexture();
					texture.setTexture(newParam2.getSid());
					texture.setTexcoord("TEXCOORD"); // ColladaConstants.INPUT_SEMANTIC_TEXCOORD
					CommonColorOrTextureType ccott = colladaFactory.createCommonColorOrTextureType();
					ccott.setTexture(texture);
					lambert.setDiffuse(ccott);
					profileCommonTechnique.setLambert(lambert);
					profileCommon.setTechnique(profileCommonTechnique);
				}
				else {
					// --------------------------- effects ---------------------------
					ProfileCOMMON.Technique profileCommonTechnique = colladaFactory.createProfileCOMMONTechnique();
					profileCommonTechnique.setSid("COMMON");
					ProfileCOMMON.Technique.Lambert lambert = colladaFactory.createProfileCOMMONTechniqueLambert();

					CommonFloatOrParamType cfopt = colladaFactory.createCommonFloatOrParamType();
					CommonFloatOrParamType.Float cfoptf = colladaFactory.createCommonFloatOrParamTypeFloat();
					if (x3dMaterial.isSetShininess()) {
						cfoptf.setValue(x3dMaterial.getShininess());
						cfopt.setFloat(cfoptf);
						lambert.setReflectivity(cfopt);
					}

					if (x3dMaterial.isSetTransparency()) {
						cfopt = colladaFactory.createCommonFloatOrParamType();
						cfoptf = colladaFactory.createCommonFloatOrParamTypeFloat();
						cfoptf.setValue(x3dMaterial.getTransparency());
						cfopt.setFloat(cfoptf);
						lambert.setTransparency(cfopt);
					}

					if (x3dMaterial.isSetDiffuseColor()) {
						CommonColorOrTextureType.Color color = colladaFactory.createCommonColorOrTextureTypeColor();
						color.getValue().add(x3dMaterial.getDiffuseColor().getRed());
						color.getValue().add(x3dMaterial.getDiffuseColor().getGreen());
						color.getValue().add(x3dMaterial.getDiffuseColor().getBlue());
						color.getValue().add(1d); // alpha
						CommonColorOrTextureType ccott = colladaFactory.createCommonColorOrTextureType();
						ccott.setColor(color);
						lambert.setDiffuse(ccott);
					}

					if (x3dMaterial.isSetSpecularColor()) {
						CommonColorOrTextureType.Color color = colladaFactory.createCommonColorOrTextureTypeColor();
						color.getValue().add(x3dMaterial.getSpecularColor().getRed());
						color.getValue().add(x3dMaterial.getSpecularColor().getGreen());
						color.getValue().add(x3dMaterial.getSpecularColor().getBlue());
						color.getValue().add(1d); // alpha
						CommonColorOrTextureType ccott = colladaFactory.createCommonColorOrTextureType();
						ccott.setColor(color);
						lambert.setReflective(ccott);
					}

					if (x3dMaterial.isSetEmissiveColor()) {
						CommonColorOrTextureType.Color color = colladaFactory.createCommonColorOrTextureTypeColor();
						color.getValue().add(x3dMaterial.getEmissiveColor().getRed());
						color.getValue().add(x3dMaterial.getEmissiveColor().getGreen());
						color.getValue().add(x3dMaterial.getEmissiveColor().getBlue());
						color.getValue().add(1d); // alpha
						CommonColorOrTextureType ccott = colladaFactory.createCommonColorOrTextureType();
						ccott.setColor(color);
						lambert.setEmission(ccott);
					}

					profileCommonTechnique.setLambert(lambert);
					profileCommon.setTechnique(profileCommonTechnique);
				}

				// --------------------- effects common part 2 ---------------------
				Technique geTechnique = colladaFactory.createTechnique();
				geTechnique.setProfile("GOOGLEEARTH");

				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = factory.newDocumentBuilder();
					Document document = docBuilder.newDocument();
					factory.setNamespaceAware(true);
					Element doubleSided = document.createElementNS("http://www.collada.org/2005/11/COLLADASchema", "double_sided");
					doubleSided.setTextContent(ignoreSurfaceOrientation ? "1": "0");
					geTechnique.getAny().add(doubleSided);
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}

				Extra extra = colladaFactory.createExtra();
				extra.getTechnique().add(geTechnique);
				profileCommon.getExtra().add(extra);

				effect.getFxProfileAbstract().add(colladaFactory.createProfileCOMMON(profileCommon));

				libraryEffects.getEffect().add(effect);

				// --------------------------- triangles ---------------------------
				triangles = colladaFactory.createTriangles();
				triangles.setMaterial(replaceExtensionWithSuffix(texImageName, "_tri"));
				InputLocalOffset inputV = colladaFactory.createInputLocalOffset();
				inputV.setSemantic("VERTEX"); // ColladaConstants.INPUT_SEMANTIC_VERTEX
				inputV.setSource("#" + vertices.getId());
				inputV.setOffset(BigInteger.ZERO);
				triangles.getInput().add(inputV);
				if (surfaceTextured) {
					InputLocalOffset inputT = colladaFactory.createInputLocalOffset();
					inputT.setSemantic("TEXCOORD"); // ColladaConstants.INPUT_SEMANTIC_TEXCOORD
					inputT.setSource("#" + texCoordsSource.getId());
					inputT.setOffset(BigInteger.ONE);
					triangles.getInput().add(inputT);
				}

				trianglesByTexImageName.put(texImageName, triangles);
			}

			// --------------------------- geometry (variable part) ---------------------------
			GeometryInfo ginfo = geometryInfos.get(surfaceId);
			int outerRingCount = ginfo.getStripCounts()[0];
			ginfo.convertToIndexedTriangles();

			/*
			// the following seems to be buggy, so don't do it for now
			// generate normals, currently not used, but this is the recommended order
			NormalGenerator ng = new NormalGenerator();
			ng.generateNormals(ginfo);
			// stripify: merge triangles together into bigger triangles when possible
			Stripifier st = new Stripifier();
			st.stripify(ginfo);
			 */

			// fix a reversed orientation of the triangulated surface 
			// idea: get the first triangle edge on the outer ring and
			// check whether the order of the vertex indices is correct			
			int[] indexes = ginfo.getCoordinateIndices();
			byte[] edges = {0, 1, 1, 2, 2, 0};			
			boolean hasFound = false;

			for (int i = 0; !hasFound && i < indexes.length; i += 3) {			
				for (int j = 0; j < edges.length; j += 2) {
					int first = i + edges[j];
					int second = i + edges[j + 1]; 

					if (indexes[first] < outerRingCount && indexes[second] < outerRingCount && Math.abs(indexes[first] - indexes[second]) == 1) {
						hasFound = true;
						if (indexes[first] > indexes[second])
							ginfo.reverse();
						break;
					}
				}
			}

			GeometryArray gArray = ginfo.getGeometryArray();
			Point3d coordPoint = new Point3d();
			for(int i = 0; i < gArray.getVertexCount(); i++){
				gArray.getCoordinate(i, coordPoint);

				VertexInfo vertexInfo = getVertexInfoForXYZ(coordPoint.x, coordPoint.y, coordPoint.z);
				if (vertexInfo == null || (surfaceTextured && vertexInfo.getTexCoords(surfaceId) == null)) {
					// no node or wrong node found
					// use best fit only in extreme cases (it is slow)
					if (surfaceTextured) {
						vertexInfo = getVertexInfoBestFitForXYZ(coordPoint.x, coordPoint.y, coordPoint.z, surfaceId);
					}
					else  {
						vertexInfo = getVertexInfoBestFitForXYZ(coordPoint.x, coordPoint.y, coordPoint.z);
					}
				}
				triangles.getP().add(vertexInfo.getVertexId());

				if (surfaceTextured) {
					TexCoords texCoords = vertexInfo.getTexCoords(surfaceId);
					if (texCoords != null) {
						// trying to save some texture points
						int indexOfT = texCoordsValues.indexOf(texCoords.getT()); 
						if (indexOfT > 0 && indexOfT%2 == 1 && // avoid coincidences
								texCoordsValues.get(indexOfT - 1).equals(texCoords.getS())) {
							triangles.getP().add(new BigInteger(String.valueOf((indexOfT - 1)/2)));
						}
						else {
							texCoordsValues.add(new Double(texCoords.getS()));
							texCoordsValues.add(new Double(texCoords.getT()));
							triangles.getP().add(texCoordsCounter);
							texCoordsCounter = texCoordsCounter.add(BigInteger.ONE);
							// no triangleCounter++ since it is BigInteger
						}
					}
					else { // should never happen
						triangles.getP().add(texCoordsCounter); // wrong data is better than triangles out of sync
						Logger.getInstance().log(LogLevel.DEBUG, 
								"texCoords not found for (" + coordPoint.x + ", " + coordPoint.y + ", "
										+ coordPoint.z + "). TOLERANCE = " + TOLERANCE_AFTER_TRIANGULATION);
					}
				}
			}
		}

		VertexInfo vertexInfoIterator = firstVertexInfo;
		while (vertexInfoIterator != null) {
			// undo trick for very close coordinates
			positionValues.add(new Double(reducePrecisionForXorY((vertexInfoIterator.getX() - originX)/100)));
			positionValues.add(new Double(reducePrecisionForXorY((vertexInfoIterator.getY() - originY)/100)));
			positionValues.add(new Double(reducePrecisionForZ((vertexInfoIterator.getZ() - originZ)/100)));
			vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
		} 
		positionArray.setCount(new BigInteger(String.valueOf(positionValues.size()))); // gotta love BigInteger!
		texCoordsArray.setCount(new BigInteger(String.valueOf(texCoordsValues.size())));
		positionAccessor.setCount(positionArray.getCount().divide(positionAccessor.getStride()));
		texCoordsAccessor.setCount(texCoordsArray.getCount().divide(texCoordsAccessor.getStride()));

		Set<String> trianglesKeySet = trianglesByTexImageName.keySet();
		Iterator<String> trianglesIterator = trianglesKeySet.iterator();
		while (trianglesIterator.hasNext()) {
			String texImageName = trianglesIterator.next();
			triangles = trianglesByTexImageName.get(texImageName);
			triangles.setCount(new BigInteger(String.valueOf(triangles.getP().size()/(3*triangles.getInput().size()))));
			if (texImageName.startsWith(NO_TEXIMAGE)) { // materials first, textures last
				mesh.getLinesOrLinestripsOrPolygons().add(0, triangles);
			}
			else {
				mesh.getLinesOrLinestripsOrPolygons().add(triangles);
			}
			InstanceMaterial instanceMaterial = colladaFactory.createInstanceMaterial();
			instanceMaterial.setSymbol(triangles.getMaterial());
			instanceMaterial.setTarget("#" + replaceExtensionWithSuffix(texImageName, "_mat"));
			techniqueCommon.getInstanceMaterial().add(instanceMaterial);
		}

		// this method's name is really like this...
		List<Object> libraries = collada.getLibraryAnimationsOrLibraryAnimationClipsOrLibraryCameras();

		if (!libraryImages.getImage().isEmpty()) { // there may be buildings with no textures at all
			libraries.add(libraryImages);
		}
		libraries.add(libraryMaterials);
		libraries.add(libraryEffects);
		libraries.add(libraryGeometries);
		libraries.add(libraryVisualScenes);

		InstanceWithExtra instanceWithExtra = colladaFactory.createInstanceWithExtra();
		instanceWithExtra.setUrl("#" + visualScene.getId());
		COLLADA.Scene scene = colladaFactory.createCOLLADAScene();
		scene.setInstanceVisualScene(instanceWithExtra);
		collada.setScene(scene);

		return collada;
	}

	private String replaceExtensionWithSuffix (String imageName, String suffix) {
		int indexOfExtension = imageName.lastIndexOf('.');
		if (indexOfExtension != -1) {
			imageName = imageName.substring(0, indexOfExtension);
		}
		return imageName + suffix;
	}
	/*
	protected HashMap<Object, String> getTexImageUris(){
		return texImageUris;
	}
	 */
	protected void addGeometryInfo(long surfaceId, GeometryInfo geometryInfo){
		geometryInfos.put(new Long(surfaceId), geometryInfo);
	}

	protected int getGeometryAmount(){
		return geometryInfos.size();
	}
	/*
	protected GeometryInfo getGeometryInfo(long surfaceId){
		return geometryInfos.get(new Long(surfaceId));
	}
	 */
	protected void addX3dMaterial(long surfaceId, X3DMaterial x3dMaterial){
		if (x3dMaterial == null) return;
		if (x3dMaterial.isSetAmbientIntensity()
				|| x3dMaterial.isSetShininess()
				|| x3dMaterial.isSetTransparency()
				|| x3dMaterial.isSetDiffuseColor()
				|| x3dMaterial.isSetSpecularColor()
				|| x3dMaterial.isSetEmissiveColor()) {

			if (x3dMaterials == null) {
				x3dMaterials = new HashMap<Long, X3DMaterial>();
			}
			x3dMaterials.put(new Long(surfaceId), x3dMaterial);
		}
	}

	protected X3DMaterial getX3dMaterial(long surfaceId) {
		X3DMaterial x3dMaterial = null;
		if (x3dMaterials != null) {
			x3dMaterial = x3dMaterials.get(new Long(surfaceId));
		}
		return x3dMaterial;
	}

	protected void addTexImageUri(long surfaceId, String texImageUri){
		if (texImageUri != null) {
			texImageUris.put(new Long(surfaceId), texImageUri);
		}
	}

	protected void addTexImage(String texImageUri, TextureImage texImage){
		if (texImage != null) {
			texImages.put(texImageUri, texImage);
		}
	}

	protected void removeTexImage(String texImageUri){
		texImages.remove(texImageUri);
	}

	public HashMap<String, TextureImage> getTexImages(){
		return texImages;
	}

	protected TextureImage getTexImage(String texImageUri){
		TextureImage texImage = null;
		if (texImages != null) {
			texImage = texImages.get(texImageUri);
		}
		return texImage;
	}

	protected void addUnsupportedTexImageId(String texImageUri, long surfaceDataId){
		if (surfaceDataId < 0) {
			return;
		}
		if (unsupportedTexImageIds == null) {
			unsupportedTexImageIds = new HashMap<String, Long>();
		}
		unsupportedTexImageIds.put(texImageUri, surfaceDataId);
	}

	public HashMap<String, Long> getUnsupportedTexImageIds(){
		return unsupportedTexImageIds;
	}

	protected long getUnsupportedTexImageId(String texImageUri){
		long surfaceDataId = -1;
		if (unsupportedTexImageIds != null) {
			Long tmp = unsupportedTexImageIds.get(texImageUri);
			if (tmp != null)
				surfaceDataId = tmp.longValue();
		}
		return surfaceDataId;
	}

	protected void setVertexInfoForXYZ(long surfaceId, double x, double y, double z, TexCoords texCoordsForThisSurface){
		vertexIdCounter = vertexIdCounter.add(BigInteger.ONE);
		VertexInfo vertexInfo = new VertexInfo(vertexIdCounter, x, y, z);
		vertexInfo.addTexCoords(surfaceId, texCoordsForThisSurface);
		NodeZ nodeToInsert = new NodeZ(z, new NodeY(y, new NodeX(x, vertexInfo)));
		if (coordinateTree == null) {
			coordinateTree =  nodeToInsert;
			firstVertexInfo = vertexInfo;
			lastVertexInfo = vertexInfo;
		}
		else {
			insertNode(coordinateTree, nodeToInsert);
		}
	}

	private VertexInfo getVertexInfoForXYZ(double x, double y, double z){
		NodeY rootY = (NodeY) getValue(z, coordinateTree);
		NodeX rootX = (NodeX) getValue(y, rootY);
		VertexInfo vertexInfo = (VertexInfo) getValue(x, rootX);
		return vertexInfo;
	}

	private void insertNode(Node currentBasis, Node nodeToInsert) {
		int compareKeysResult = compareKeys(nodeToInsert.key, currentBasis.key, TOLERANCE_BEFORE_TRIANGULATION);
		if (compareKeysResult > 0) {
			if (currentBasis.rightArc == null){
				currentBasis.setRightArc(nodeToInsert);
				linkCurrentVertexInfoToLastVertexInfo(nodeToInsert);
			}
			else {
				insertNode(currentBasis.rightArc, nodeToInsert);
			}
		}
		else if (compareKeysResult < 0) {
			if (currentBasis.leftArc == null){
				currentBasis.setLeftArc(nodeToInsert);
				linkCurrentVertexInfoToLastVertexInfo(nodeToInsert);
			}
			else {
				insertNode(currentBasis.leftArc, nodeToInsert);
			}
		}
		else {
			replaceOrAddValue(currentBasis, nodeToInsert);
		}
	}

	private Object getValue(double key, Node currentBasis) {
		if (currentBasis == null) {
			return null;
		}
		int compareKeysResult = compareKeys(key, currentBasis.key, TOLERANCE_AFTER_TRIANGULATION);
		if (compareKeysResult > 0) {
			return getValue(key, currentBasis.rightArc);
		}
		else if (compareKeysResult < 0) {
			return getValue(key, currentBasis.leftArc);
		}
		return currentBasis.value;
	}


	private VertexInfo getVertexInfoBestFitForXYZ(double x, double y, double z, long surfaceId) {
		VertexInfo result = null;
		VertexInfo vertexInfoIterator = firstVertexInfo;
		double distancePow2 = Double.MAX_VALUE;
		double currentDistancePow2;
		while (vertexInfoIterator != null) {
			if (vertexInfoIterator.getTexCoords(surfaceId) != null) {
				currentDistancePow2 = Math.pow(x - (float)vertexInfoIterator.getX(), 2) + 
						Math.pow(y - (float)vertexInfoIterator.getY(), 2) +
						Math.pow(z - (float)vertexInfoIterator.getZ(), 2);
				if (currentDistancePow2 < distancePow2) {
					distancePow2 = currentDistancePow2;
					result = vertexInfoIterator;
				}
			}
			vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
		}
		if (result == null) {
			result = getVertexInfoBestFitForXYZ(x, y, z);
		}
		return result;
	}

	private VertexInfo getVertexInfoBestFitForXYZ(double x, double y, double z) {
		VertexInfo result = null;
		VertexInfo vertexInfoIterator = firstVertexInfo;
		double distancePow2 = Double.MAX_VALUE;
		double currentDistancePow2;
		while (vertexInfoIterator != null) {
			currentDistancePow2 = Math.pow(x - (float)vertexInfoIterator.getX(), 2) + 
					Math.pow(y - (float)vertexInfoIterator.getY(), 2) +
					Math.pow(z - (float)vertexInfoIterator.getZ(), 2);
			if (currentDistancePow2 < distancePow2) {
				distancePow2 = currentDistancePow2;
				result = vertexInfoIterator;
			}
			vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
		}
		return result;
	}

	private void replaceOrAddValue(Node currentBasis, Node nodeToInsert) {
		if (nodeToInsert.value instanceof VertexInfo) {
			VertexInfo vertexInfoToInsert = (VertexInfo)nodeToInsert.value;
			if (currentBasis.value == null) { // no vertexInfo yet for this point
				currentBasis.value = nodeToInsert.value;
				linkCurrentVertexInfoToLastVertexInfo(vertexInfoToInsert);
			}
			else {
				vertexIdCounter = vertexIdCounter.subtract(BigInteger.ONE);
				((VertexInfo)currentBasis.value).addTexCoordsFrom(vertexInfoToInsert);
			}
		}
		else { // Node
			insertNode((Node)currentBasis.value, (Node)nodeToInsert.value);
		}
	}

	private void linkCurrentVertexInfoToLastVertexInfo (Node node) {
		while (!(node.value instanceof VertexInfo)) {
			node = (Node)node.value;
		}
		linkCurrentVertexInfoToLastVertexInfo((VertexInfo)node.value);
	}

	private void linkCurrentVertexInfoToLastVertexInfo (VertexInfo currentVertexInfo) {
		lastVertexInfo.setNextVertexInfo(currentVertexInfo);
		lastVertexInfo = currentVertexInfo;
	}

	private int compareKeys (double key1, double key2, double tolerance){
		int result = 0;
		if (Math.abs(key1 - key2) > tolerance) {
			result = key1 > key2 ? 1 : -1;
		}
		return result;
	}

	public void appendObject (KmlGenericObject objectToAppend) {

		VertexInfo vertexInfoIterator = objectToAppend.firstVertexInfo;
		while (vertexInfoIterator != null) {
			if (vertexInfoIterator.getAllTexCoords() == null) {
				this.setVertexInfoForXYZ(-1, // dummy
						vertexInfoIterator.getX(),
						vertexInfoIterator.getY(),
						vertexInfoIterator.getZ(),
						null);
			}
			else {
				Set<Long> keySet = vertexInfoIterator.getAllTexCoords().keySet();
				Iterator<Long> iterator = keySet.iterator();
				while (iterator.hasNext()) {
					Long surfaceId = iterator.next();
					this.setVertexInfoForXYZ(surfaceId,
							vertexInfoIterator.getX(),
							vertexInfoIterator.getY(),
							vertexInfoIterator.getZ(),
							vertexInfoIterator.getTexCoords(surfaceId));
				}
			}
			vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
		} 

		Set<Long> keySet = objectToAppend.geometryInfos.keySet();
		Iterator<Long> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			Long surfaceId = iterator.next();
			this.addX3dMaterial(surfaceId, objectToAppend.getX3dMaterial(surfaceId));
			String imageUri = objectToAppend.texImageUris.get(surfaceId);
			this.addTexImageUri(surfaceId, imageUri);
			this.addTexImage(imageUri, objectToAppend.getTexImage(imageUri));
			this.addUnsupportedTexImageId(imageUri, objectToAppend.getUnsupportedTexImageId(imageUri));
			this.addGeometryInfo(surfaceId, objectToAppend.geometryInfos.get(surfaceId));
		}

		// adapt id accordingly
		int indexOf_to_ = this.gmlId.indexOf("_to_");
		String ownLowerLimit = "";
		String ownUpperLimit = "";
		if (indexOf_to_ != -1) { // already more than one building in here
			ownLowerLimit = this.gmlId.substring(0, indexOf_to_);
			ownUpperLimit = this.gmlId.substring(indexOf_to_ + 4);
		}
		else {
			ownLowerLimit = this.gmlId;
			ownUpperLimit = ownLowerLimit;
		}

		int btaIndexOf_to_ = objectToAppend.gmlId.indexOf("_to_");
		String btaLowerLimit = "";
		String btaUpperLimit = "";
		if (btaIndexOf_to_ != -1) { // already more than one building in there
			btaLowerLimit = objectToAppend.gmlId.substring(0, btaIndexOf_to_);
			btaUpperLimit = objectToAppend.gmlId.substring(btaIndexOf_to_ + 4);
		}
		else {
			btaLowerLimit = objectToAppend.gmlId;
			btaUpperLimit = btaLowerLimit;
		}

		ownLowerLimit = ownLowerLimit.compareTo(btaLowerLimit)<0 ? ownLowerLimit: btaLowerLimit;
		ownUpperLimit = ownUpperLimit.compareTo(btaUpperLimit)>0 ? ownUpperLimit: btaUpperLimit;

		this.setGmlId(String.valueOf(ownLowerLimit) + "_to_" + ownUpperLimit);
	}


	public void createTextureAtlas(int packingAlgorithm, double imageScaleFactor, boolean pots) throws SQLException, IOException {

		if (texImages.size() < 2) {
			// building has not enough textures or they are in an unknown image format 
			return;
		}

		useExternalTAGenerator(packingAlgorithm, imageScaleFactor, pots);
	}

	private void useExternalTAGenerator(int packingAlgorithm, double scaleFactor, boolean pots) throws SQLException, IOException {
		TextureAtlasCreator taCreator = new TextureAtlasCreator();
		TextureImagesInfo tiInfo = new TextureImagesInfo();
		tiInfo.setTexImageURIs(texImageUris);
		tiInfo.setTexImages(texImages);

		// texture coordinates
		HashMap<Object, String> tiInfoCoords = new HashMap<Object, String>();

		Set<Object> sgIdSet = texImageUris.keySet();
		Iterator<Object> sgIdIterator = sgIdSet.iterator();
		while (sgIdIterator.hasNext()) {
			Long sgId = (Long) sgIdIterator.next();
			VertexInfo vertexInfoIterator = firstVertexInfo;
			while (vertexInfoIterator != null) {
				if (vertexInfoIterator.getAllTexCoords() != null &&
						vertexInfoIterator.getAllTexCoords().containsKey(sgId)) {
					double s = vertexInfoIterator.getTexCoords(sgId).getS();
					double t = vertexInfoIterator.getTexCoords(sgId).getT();
					String tiInfoCoordsForSgId = tiInfoCoords.get(sgId);
					tiInfoCoordsForSgId = (tiInfoCoordsForSgId == null) ?
							"" :
								tiInfoCoordsForSgId + " ";	
					tiInfoCoords.put(sgId, tiInfoCoordsForSgId + String.valueOf(s) + " " + String.valueOf(t));
				}
				vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
			}
		} 

		tiInfo.setTexCoordinates(tiInfoCoords);

		taCreator.setUsePOTS(pots);
		taCreator.setScaleFactor(scaleFactor);

		// create texture atlases
		taCreator.convert(tiInfo, packingAlgorithm);

		sgIdIterator = sgIdSet.iterator();
		while (sgIdIterator.hasNext()) {
			Long sgId = (Long) sgIdIterator.next();
			StringTokenizer texCoordsTokenized = new StringTokenizer(tiInfoCoords.get(sgId), " ");
			VertexInfo vertexInfoIterator = firstVertexInfo;
			while (texCoordsTokenized.hasMoreElements() &&
					vertexInfoIterator != null) {
				if (vertexInfoIterator.getAllTexCoords() != null && 
						vertexInfoIterator.getAllTexCoords().containsKey(sgId)) {
					vertexInfoIterator.getTexCoords(sgId).setS(Double.parseDouble(texCoordsTokenized.nextToken()));
					vertexInfoIterator.getTexCoords(sgId).setT(Double.parseDouble(texCoordsTokenized.nextToken()));
				}
				vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
			}
		} 
	}

	public void resizeAllImagesByFactor (double factor) throws SQLException, IOException {
		if (texImages.size() == 0) { // building has no textures at all
			return;
		}

		Set<String> keySet = texImages.keySet();
		Iterator<String> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			String imageName = iterator.next();
			BufferedImage imageToResize = texImages.get(imageName).getBufferedImage();
			if (imageToResize.getWidth()*factor < 1 || imageToResize.getHeight()*factor < 1) {
				continue;
			}
			BufferedImage resizedImage = getScaledInstance(imageToResize,
					(int)(imageToResize.getWidth()*factor),
					(int)(imageToResize.getHeight()*factor),
					RenderingHints.VALUE_INTERPOLATION_BILINEAR,
					true);
			texImages.put(imageName, new TextureImage(resizedImage));
		}

	}


	/**
	 * Convenience method that returns a scaled instance of the
	 * provided {@code BufferedImage}.
	 *
	 * @param img the original image to be scaled
	 * @param targetWidth the desired width of the scaled instance,
	 *    in pixels
	 * @param targetHeight the desired height of the scaled instance,
	 *    in pixels
	 * @param hint one of the rendering hints that corresponds to
	 *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality if true, this method will use a multi-step
	 *    scaling technique that provides higher quality than the usual
	 *    one-step technique (only useful in downscaling cases, where
	 *    {@code targetWidth} or {@code targetHeight} is
	 *    smaller than the original dimensions, and generally only when
	 *    the {@code BILINEAR} hint is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	private BufferedImage getScaledInstance(BufferedImage img,
			int targetWidth,
			int targetHeight,
			Object hint,
			boolean higherQuality) {

		int type = (img.getTransparency() == Transparency.OPAQUE) ?
				BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = (BufferedImage)img;
		int w, h;
		if (higherQuality) {
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = img.getWidth();
			h = img.getHeight();
		} 
		else {
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}

		do {
			if (higherQuality && w > targetWidth) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}

			if (higherQuality && h > targetHeight) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}

			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
		}
		while (w != targetWidth || h != targetHeight);

		return ret;
	}

	private String buildNameFromX3dMaterial(X3DMaterial x3dMaterial) {
		String name = NO_TEXIMAGE;
		if (x3dMaterial.isSetAmbientIntensity()) { name = name + "_ai_" + x3dMaterial.getAmbientIntensity();}
		if (x3dMaterial.isSetShininess()) { name = name + "_sh_" + x3dMaterial.getShininess();}
		if (x3dMaterial.isSetTransparency()) { name = name + "_tr_" + x3dMaterial.getTransparency();}
		if (x3dMaterial.isSetDiffuseColor()) { name = name + "_dc_r_" + x3dMaterial.getDiffuseColor().getRed()
				+ "_g_" + x3dMaterial.getDiffuseColor().getGreen()
				+ "_b_" + x3dMaterial.getDiffuseColor().getBlue();}
		if (x3dMaterial.isSetSpecularColor()) { name = name + "_sc_r_" + x3dMaterial.getSpecularColor().getRed()
				+ "_g_" + x3dMaterial.getSpecularColor().getGreen()
				+ "_b_" + x3dMaterial.getSpecularColor().getBlue();}
		if (x3dMaterial.isSetEmissiveColor()) { name = name + "_ec_r_" + x3dMaterial.getEmissiveColor().getRed()
				+ "_g_" + x3dMaterial.getEmissiveColor().getGreen()
				+ "_b_" + x3dMaterial.getEmissiveColor().getBlue();}
		return name;
	}

	protected List<Point3d> setOrigins() {
		originZ = Double.MAX_VALUE;
		List<Point3d> coords = new ArrayList<Point3d>();
		VertexInfo vertexInfoIterator = firstVertexInfo;
		while (vertexInfoIterator != null) {
			if (vertexInfoIterator.getZ() < originZ) { // origin must be a point with the lowest z-coordinate
				originX = vertexInfoIterator.getX();
				originY = vertexInfoIterator.getY();
				originZ = vertexInfoIterator.getZ();
				coords.clear();
				Point3d point3d = new Point3d(originX, originY, originZ);
				coords.add(point3d);
			}
			if (vertexInfoIterator.getZ() == originZ) {
				Point3d point3d = new Point3d(vertexInfoIterator.getX(), vertexInfoIterator.getY(), vertexInfoIterator.getZ());
				coords.add(point3d);
			}
			vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
		}
		return coords;
	}

	protected static double reducePrecisionForXorY (double originalValue) {
		double newValue = originalValue; // + 0.00000005d;
		//		if (decimalDigits != 0) {
		//			double factor = Math.pow(10, decimalDigits);
		double factor = Math.pow(10, 7);
		newValue = Math.rint(newValue*factor);
		newValue = newValue/factor;
		//		}
		return newValue;
	}

	protected static double reducePrecisionForZ (double originalValue) {
		double newValue = originalValue; // + 0.0005d;
		//		if (decimalDigits != 0) {
		//			double factor = Math.pow(10, decimalDigits);
		double factor = Math.pow(10, 4);
		newValue = Math.rint(newValue*factor);
		newValue = newValue/factor;
		//		}
		return newValue;
	}

	protected List<PlacemarkType> createPlacemarksForFootprint(ResultSet rs, KmlSplittingResult work) throws SQLException {

		List<PlacemarkType> placemarkList = new ArrayList<PlacemarkType>();
		PlacemarkType placemark = kmlFactory.createPlacemarkType();
		placemark.setName(work.getGmlId());
		placemark.setId(DisplayForm.FOOTPRINT_PLACEMARK_ID + placemark.getName());

		if (work.getDisplayForm().isHighlightingEnabled()) {
			placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.FOOTPRINT_STR + "Style");
		}
		else {
			placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.FOOTPRINT_STR + "Normal");
		}

		if (getBalloonSettings().isIncludeDescription()) {
			addBalloonContents(placemark, work.getId());
		}
		MultiGeometryType multiGeometry = kmlFactory.createMultiGeometryType();
		placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));

		PolygonType polygon = null; 
		while (rs.next()) {
			Object buildingGeometryObj = rs.getObject(1); 

			if (!rs.wasNull() && buildingGeometryObj != null) {
				eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));

				GeometryObject groundSurface = convertToWGS84(geometryConverterAdapter.getGeometry(buildingGeometryObj));
				if (groundSurface.getGeometryType() != GeometryType.POLYGON && groundSurface.getGeometryType() != GeometryType.MULTI_POLYGON)
					return placemarkList;

				int dim = groundSurface.getDimension();

				for (int i = 0; i < groundSurface.getNumElements(); i++) {
					LinearRingType linearRing = kmlFactory.createLinearRingType();
					BoundaryType boundary = kmlFactory.createBoundaryType();
					boundary.setLinearRing(linearRing);

					if (groundSurface.getElementType(i) == ElementType.EXTERIOR_LINEAR_RING) {
						polygon = kmlFactory.createPolygonType();
						polygon.setTessellate(true);
						polygon.setExtrude(false);
						polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.CLAMP_TO_GROUND));
						polygon.setOuterBoundaryIs(boundary);
						multiGeometry.getAbstractGeometryGroup().add(kmlFactory.createPolygon(polygon));
					} else if (polygon != null)
						polygon.getInnerBoundaryIs().add(boundary);

					// order points counter-clockwise
					double[] ordinatesArray = groundSurface.getCoordinates(i);
					for (int j = ordinatesArray.length - dim; j >= 0; j = j-dim)
						linearRing.getCoordinates().add(String.valueOf(ordinatesArray[j] + "," + ordinatesArray[j+1] + ",0"));
				}
			}
		}
		if (polygon != null) { // if there is at least some content
			placemarkList.add(placemark);
		}
		return placemarkList;
	}

	protected List<PlacemarkType> createPlacemarksForExtruded(ResultSet rs,
			KmlSplittingResult work,
			double measuredHeight,
			boolean reversePointOrder) throws SQLException {

		List<PlacemarkType> placemarkList = new ArrayList<PlacemarkType>();
		PlacemarkType placemark = kmlFactory.createPlacemarkType();
		placemark.setName(work.getGmlId());
		placemark.setId(DisplayForm.EXTRUDED_PLACEMARK_ID + placemark.getName());
		if (work.getDisplayForm().isHighlightingEnabled()) {
			placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.EXTRUDED_STR + "Style");
		}
		else {
			placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.EXTRUDED_STR + "Normal");
		}
		if (getBalloonSettings().isIncludeDescription()) {
			addBalloonContents(placemark, work.getId());
		}
		MultiGeometryType multiGeometry = kmlFactory.createMultiGeometryType();
		placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));

		PolygonType polygon = null; 
		while (rs.next()) {
			Object buildingGeometryObj = rs.getObject(1); 

			if (!rs.wasNull() && buildingGeometryObj != null) {
				eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));

				GeometryObject groundSurface = convertToWGS84(geometryConverterAdapter.getGeometry(buildingGeometryObj));
				if (groundSurface.getGeometryType() != GeometryType.POLYGON && groundSurface.getGeometryType() != GeometryType.MULTI_POLYGON)
					return placemarkList;

				int dim = groundSurface.getDimension();

				for (int i = 0; i < groundSurface.getNumElements(); i++) {
					LinearRingType linearRing = kmlFactory.createLinearRingType();
					BoundaryType boundary = kmlFactory.createBoundaryType();
					boundary.setLinearRing(linearRing);

					if (groundSurface.getElementType(i) == ElementType.EXTERIOR_LINEAR_RING) {
						polygon = kmlFactory.createPolygonType();
						polygon.setTessellate(true);
						polygon.setExtrude(true);
						polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
						polygon.setOuterBoundaryIs(boundary);
						multiGeometry.getAbstractGeometryGroup().add(kmlFactory.createPolygon(polygon));
					} else
						polygon.getInnerBoundaryIs().add(boundary);

					double[] ordinatesArray = groundSurface.getCoordinates(i);
					if (reversePointOrder) {
						for (int j = 0; j < ordinatesArray.length; j = j+dim)
							linearRing.getCoordinates().add(String.valueOf(ordinatesArray[j] + "," + ordinatesArray[j+1] + "," + measuredHeight));

					} else if (polygon != null)
						// order points counter-clockwise
						for (int j = ordinatesArray.length - dim; j >= 0; j = j-dim)
							linearRing.getCoordinates().add(String.valueOf(ordinatesArray[j] + "," + ordinatesArray[j+1] + "," + measuredHeight));
				}
			}
		}
		if (polygon != null) { // if there is at least some content
			placemarkList.add(placemark);
		}
		return placemarkList;
	}


	protected List<PlacemarkType> createPlacemarksForGeometry(ResultSet rs,
			KmlSplittingResult work) throws SQLException{
		return createPlacemarksForGeometry(rs, work, false, false);
	}

	private List<PlacemarkType> createPlacemarksForGeometry(ResultSet rs,
			KmlSplittingResult work,
			boolean includeGroundSurface,
			boolean includeClosureSurface) throws SQLException {

		HashMap<String, MultiGeometryType> multiGeometries = new HashMap<String, MultiGeometryType>();
		MultiGeometryType multiGeometry = null;
		PolygonType polygon = null;

		double zOffset = getZOffsetFromConfigOrDB(work.getId());
		List<Point3d> lowestPointCandidates = getLowestPointsCoordinates(rs, (zOffset == Double.MAX_VALUE));
		rs.beforeFirst(); // return cursor to beginning
		if (zOffset == Double.MAX_VALUE) {
			zOffset = getZOffsetFromGEService(work.getId(), lowestPointCandidates);
		}
		double lowestZCoordinate = convertPointCoordinatesToWGS84(new double[] {
				lowestPointCandidates.get(0).x/100, // undo trick for very close coordinates
				lowestPointCandidates.get(0).y/100,	
				lowestPointCandidates.get(0).z/100}) [2];

		while (rs.next()) {
			//			Long surfaceId = rs.getLong("id");

			String surfaceType = rs.getString("type");
			if (surfaceType != null && !surfaceType.endsWith("Surface")) {
				surfaceType = surfaceType + "Surface";
			}

			if ((!includeGroundSurface && TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_GROUND_SURFACE).toString().equalsIgnoreCase(surfaceType)) ||
					(!includeClosureSurface && TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_CLOSURE_SURFACE).toString().equalsIgnoreCase(surfaceType)))	{
				continue;
			}

			Object buildingGeometryObj = rs.getObject(1); 
			GeometryObject surface = convertToWGS84(geometryConverterAdapter.getPolygon(buildingGeometryObj));

			eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));

			polygon = kmlFactory.createPolygonType();
			switch (config.getProject().getKmlExporter().getAltitudeMode()) {
			case ABSOLUTE:
				polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.ABSOLUTE));
				break;
			case RELATIVE:
				polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
				break;
			}

			// just in case surfaceType == null
			boolean probablyRoof = true;
			double nx = 0;
			double ny = 0;
			double nz = 0;

			for (int i = 0; i < surface.getNumElements(); i++) {
				LinearRingType linearRing = kmlFactory.createLinearRingType();
				BoundaryType boundary = kmlFactory.createBoundaryType();
				boundary.setLinearRing(linearRing);

				if (i == 0)
					polygon.setOuterBoundaryIs(boundary);
				else
					polygon.getInnerBoundaryIs().add(boundary);

				// order points clockwise
				double[] ordinatesArray = surface.getCoordinates(i);
				for (int j = 0; j < ordinatesArray.length; j = j+3) {
					linearRing.getCoordinates().add(String.valueOf(reducePrecisionForXorY(ordinatesArray[j]) + "," 
							+ reducePrecisionForXorY(ordinatesArray[j+1]) + ","
							+ reducePrecisionForZ(ordinatesArray[j+2] + zOffset)));

					probablyRoof = probablyRoof && (reducePrecisionForZ(ordinatesArray[j+2] - lowestZCoordinate) > 0);
					// not touching the ground

					if (currentLod == 1) { // calculate normal
						int current = j;
						int next = j+3;
						if (next >= ordinatesArray.length) next = 0;
						nx = nx + ((ordinatesArray[current+1] - ordinatesArray[next+1]) * (ordinatesArray[current+2] + ordinatesArray[next+2])); 
						ny = ny + ((ordinatesArray[current+2] - ordinatesArray[next+2]) * (ordinatesArray[current] + ordinatesArray[next])); 
						nz = nz + ((ordinatesArray[current] - ordinatesArray[next]) * (ordinatesArray[current+1] + ordinatesArray[next+1]));
					}
				}
			}

			if (currentLod == 1) { // calculate normal
				double value = Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (value == 0) { // not a surface, but a line
					continue;
				}
				nx = nx / value;
				ny = ny / value;
				nz = nz / value;
			}

			if (surfaceType == null) {
				surfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();
				switch (currentLod) {
				case 1:
					if (probablyRoof && (nz > 0.999)) {
						surfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_ROOF_SURFACE).toString();
					}
					break;
				case 2:
					if (probablyRoof) {
						surfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_ROOF_SURFACE).toString();
					}
					break;
				}
			}

			multiGeometry = multiGeometries.get(surfaceType);
			if (multiGeometry == null) {
				multiGeometry = kmlFactory.createMultiGeometryType();
				multiGeometries.put(surfaceType, multiGeometry);
			}
			multiGeometry.getAbstractGeometryGroup().add(kmlFactory.createPolygon(polygon));

		}

		List<PlacemarkType> placemarkList = new ArrayList<PlacemarkType>();
		Set<String> keySet = multiGeometries.keySet();
		Iterator<String> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			String surfaceType = iterator.next();
			PlacemarkType placemark = kmlFactory.createPlacemarkType();
			placemark.setName(work.getGmlId() + "_" + surfaceType);
			placemark.setId(DisplayForm.GEOMETRY_PLACEMARK_ID + placemark.getName());
			if (work.isBuilding())
				placemark.setStyleUrl("#" + surfaceType + "Normal");
			else
				placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.GEOMETRY_STR + "Normal");
			if (getBalloonSettings().isIncludeDescription() &&
					!work.getDisplayForm().isHighlightingEnabled()) { // avoid double description
				addBalloonContents(placemark, work.getId());
			}
			multiGeometry = multiGeometries.get(surfaceType);
			placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));
			placemarkList.add(placemark);
		}
		return placemarkList;
	}

	protected void fillGenericObjectForCollada(ResultSet rs) throws SQLException {

		String selectedTheme = config.getProject().getKmlExporter().getAppearanceTheme();
		int texImageCounter = 0;

		while (rs.next()) {
			long surfaceRootId = rs.getLong(1);
			for (String colladaQuery: Queries.COLLADA_GEOMETRY_AND_APPEARANCE_FROM_ROOT_ID) { // parent surfaces come first
				PreparedStatement psQuery = null;
				ResultSet rs2 = null;

				try {
					psQuery = connection.prepareStatement(colladaQuery);
					psQuery.setLong(1, surfaceRootId);
					//				psQuery.setString(2, selectedTheme);
					rs2 = psQuery.executeQuery();

					while (rs2.next()) {
						String theme = rs2.getString("theme");

						Object buildingGeometryObj = rs2.getObject(1); 
						// surfaceId is the key to all Hashmaps in object
						long surfaceId = rs2.getLong("id");
						long surfaceDataId = rs2.getLong("sd_id");
						long parentId = rs2.getLong("parent_id");

						if (buildingGeometryObj == null) { // root or parent
							if (selectedTheme.equalsIgnoreCase(theme)) {
								X3DMaterial x3dMaterial = new X3DMaterial();
								fillX3dMaterialValues(x3dMaterial, rs2);
								// x3dMaterial will only added if not all x3dMaterial members are null
								addX3dMaterial(surfaceId, x3dMaterial);
							}
							else if (theme == null) { // no theme for this parent surface
								if (getX3dMaterial(parentId) != null) { // material for parent's parent known
									addX3dMaterial(surfaceId, getX3dMaterial(parentId));
								}
							}
							continue; 
						}

						// from hier on it is an elementary surfaceMember
						eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));

						String texImageUri = null;
						StringTokenizer texCoordsTokenized = null;

						if (selectedTheme.equals(KmlExporter.THEME_NONE)) {
							addX3dMaterial(surfaceId, defaultX3dMaterial);
						}
						else if	(!selectedTheme.equalsIgnoreCase(theme) && // no surface data for this surface and theme
								getX3dMaterial(parentId) != null) { // material for parent surface known
							addX3dMaterial(surfaceId, getX3dMaterial(parentId));
						}
						else {
							texImageUri = rs2.getString("tex_image_uri");
							String texCoords = rs2.getString("texture_coordinates");

							if (texImageUri != null && texImageUri.trim().length() != 0
									&&  texCoords != null && texCoords.trim().length() != 0) {

								int fileSeparatorIndex = Math.max(texImageUri.lastIndexOf("\\"), texImageUri.lastIndexOf("/")); 
								texImageUri = "_" + texImageUri.substring(fileSeparatorIndex + 1);

								addTexImageUri(surfaceId, texImageUri);
								if ((getUnsupportedTexImageId(texImageUri) == -1) && (getTexImage(texImageUri) == null)) { 
									// not already marked as wrapping texture && not already read in
									TextureImage texImage = null;
									try {
										texImage = ImageReader.read(textureExportAdapter.getInStream(rs2, "tex_image", texImageUri));
									}
									catch (IOException ioe) {}
									if (texImage != null) { // image in JPEG, PNG or another usual format
										addTexImage(texImageUri, texImage);
									}
									else {
										addUnsupportedTexImageId(texImageUri, surfaceDataId);
									}

									texImageCounter++;
									if (texImageCounter > 20) {
										eventDispatcher.triggerEvent(new CounterEvent(CounterType.TEXTURE_IMAGE, texImageCounter, this));
										texImageCounter = 0;
									}
								}

								texCoords = texCoords.replaceAll(";", " "); // substitute of ; for internal ring
								texCoordsTokenized = new StringTokenizer(texCoords.trim(), " ");
							}
							else {
								X3DMaterial x3dMaterial = new X3DMaterial();
								fillX3dMaterialValues(x3dMaterial, rs2);
								// x3dMaterial will only added if not all x3dMaterial members are null
								addX3dMaterial(surfaceId, x3dMaterial);
								if (getX3dMaterial(surfaceId) == null) {
									// untextured surface and no x3dMaterial -> default x3dMaterial (gray)
									addX3dMaterial(surfaceId, defaultX3dMaterial);
								}
							}
						}

						GeometryObject surface = geometryConverterAdapter.getPolygon(buildingGeometryObj);
						GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);

						int contourCount = surface.getNumElements();
						int[] stripCountArray = new int[contourCount];
						int[] countourCountArray = {contourCount};

						// last point of polygons in gml is identical to first and useless for GeometryInfo
						double[] giOrdinatesArray = new double[surface.getNumCoordinates() - (contourCount * 3)];
						int i = 0;

						for (int currentContour = 0; currentContour < surface.getNumElements(); currentContour++) {
							double[] ordinatesArray = surface.getCoordinates(currentContour);
							for (int j = 0; j < ordinatesArray.length - 3; j = j+3, i = i+3) {

								giOrdinatesArray[i] = ordinatesArray[j] * 100; // trick for very close coordinates
								giOrdinatesArray[i+1] = ordinatesArray[j+1] * 100;
								giOrdinatesArray[i+2] = ordinatesArray[j+2] * 100;

								TexCoords texCoordsForThisSurface = null;
								if (texCoordsTokenized != null && texCoordsTokenized.hasMoreTokens()) {
									double s = Double.parseDouble(texCoordsTokenized.nextToken());
									double t = Double.parseDouble(texCoordsTokenized.nextToken());
									texCoordsForThisSurface = new TexCoords(s, t);
								}
								setVertexInfoForXYZ(surfaceId,
										giOrdinatesArray[i],
										giOrdinatesArray[i+1],
										giOrdinatesArray[i+2],
										texCoordsForThisSurface);
							}
							stripCountArray[currentContour] = (ordinatesArray.length - 3) / 3;
							if (texCoordsTokenized != null && texCoordsTokenized.hasMoreTokens()) {
								texCoordsTokenized.nextToken(); // geometryInfo ignores last point in a polygon
								texCoordsTokenized.nextToken(); // keep texture coordinates in sync
							}
						}
						gi.setCoordinates(giOrdinatesArray);
						gi.setContourCounts(countourCountArray);
						gi.setStripCounts(stripCountArray);
						addGeometryInfo(surfaceId, gi);
					}
				}
				catch (SQLException sqlEx) {
					Logger.getInstance().error("SQL error while querying city object: " + sqlEx.getMessage());
				}
				finally {
					if (rs2 != null)
						try { rs2.close(); } catch (SQLException e) {}
					if (psQuery != null)
						try { psQuery.close(); } catch (SQLException e) {}
				}
			}
		}

		// count rest images
		eventDispatcher.triggerEvent(new CounterEvent(CounterType.TEXTURE_IMAGE, texImageCounter, this));
	}

	public PlacemarkType createPlacemarkForColladaModel() throws SQLException {
		PlacemarkType placemark = kmlFactory.createPlacemarkType();
		placemark.setName(getGmlId());
		placemark.setId(DisplayForm.COLLADA_PLACEMARK_ID + placemark.getName());

		DisplayForm colladaDisplayForm = null;
		for (DisplayForm displayForm: getDisplayForms()) {
			if (displayForm.getForm() == DisplayForm.COLLADA) {
				colladaDisplayForm = displayForm;
				break;
			}
		}

		if (getBalloonSettings().isIncludeDescription() 
				&& !colladaDisplayForm.isHighlightingEnabled()) { // avoid double description

			ColladaOptions colladaOptions = getColladaOptions();
			if (!colladaOptions.isGroupObjects() || colladaOptions.getGroupSize() == 1) {
				addBalloonContents(placemark, getId());
			}
		}

		ModelType model = kmlFactory.createModelType();
		LocationType location = kmlFactory.createLocationType();

		switch (config.getProject().getKmlExporter().getAltitudeMode()) {
		case ABSOLUTE:
			model.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.ABSOLUTE));
			break;
		case RELATIVE:
			model.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
			break;
		}

		location.setLatitude(getLocationY());
		location.setLongitude(getLocationX());
		location.setAltitude(getLocationZ() + reducePrecisionForZ(getZOffset()));
		model.setLocation(location);

		// correct heading value
		double lat1 = Math.toRadians(getLocationY());
		// undo trick for very close coordinates
		double[] dummy = convertPointCoordinatesToWGS84(new double[] {getOriginX()/100, getOriginY()/100 - 20, getOriginZ()/100});
		double lat2 = Math.toRadians(dummy[1]);
		double dLon = Math.toRadians(dummy[0] - getLocationX());
		double y = Math.sin(dLon) * Math.cos(lat2);
		double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
		double bearing = Math.toDegrees(Math.atan2(y, x));
		bearing = (bearing + 180) % 360;

		OrientationType orientation = kmlFactory.createOrientationType();
		orientation.setHeading(reducePrecisionForZ(bearing));
		model.setOrientation(orientation);

		LinkType link = kmlFactory.createLinkType();
		if (config.getProject().getKmlExporter().isOneFilePerObject() &&
				!config.getProject().getKmlExporter().isExportAsKmz() &&
				config.getProject().getKmlExporter().getFilter().getComplexFilter().getTiledBoundingBox().getActive().booleanValue())
		{
			link.setHref(getGmlId() + ".dae");
		}
		else {
			// File.separator would be wrong here, it MUST be "/"
			link.setHref(getGmlId() + "/" + getGmlId() + ".dae");
		}
		model.setLink(link);

		placemark.setAbstractGeometryGroup(kmlFactory.createModel(model));
		return placemark;
	}


	protected List<PlacemarkType> createPlacemarksForHighlighting(KmlSplittingResult work) throws SQLException {

		List<PlacemarkType> placemarkList= new ArrayList<PlacemarkType>();

		PlacemarkType placemark = kmlFactory.createPlacemarkType();
		placemark.setStyleUrl("#" + getStyleBasisName() + work.getDisplayForm().getName() + "Style");
		placemark.setName(work.getGmlId());
		placemark.setId(DisplayForm.GEOMETRY_HIGHLIGHTED_PLACEMARK_ID + placemark.getName());
		placemarkList.add(placemark);

		if (getBalloonSettings().isIncludeDescription()) {
			addBalloonContents(placemark, work.getId());
		}

		MultiGeometryType multiGeometry =  kmlFactory.createMultiGeometryType();
		placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));

		PreparedStatement getGeometriesStmt = null;
		ResultSet rs = null;

		double hlDistance = work.getDisplayForm().getHighlightingDistance();

		try {
			getGeometriesStmt = connection.prepareStatement(getHighlightingQuery(),
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);

			for (int i = 1; i <= getGeometriesStmt.getParameterMetaData().getParameterCount(); i++) {
				getGeometriesStmt.setLong(i, work.getId());
			}
			rs = getGeometriesStmt.executeQuery();

			double zOffset = getZOffsetFromConfigOrDB(work.getId());
			if (zOffset == Double.MAX_VALUE) {
				List<Point3d> lowestPointCandidates = getLowestPointsCoordinates(rs, (zOffset == Double.MAX_VALUE));
				rs.beforeFirst(); // return cursor to beginning
				zOffset = getZOffsetFromGEService(work.getId(), lowestPointCandidates);
			}

			while (rs.next()) {
				Object unconvertedObj = rs.getObject(1);
				GeometryObject unconvertedSurface = geometryConverterAdapter.getPolygon(unconvertedObj);
				if (unconvertedSurface == null || unconvertedSurface.getNumElements() == 0)
					return null;

				double[] ordinatesArray = unconvertedSurface.getCoordinates(0);
				double nx = 0;
				double ny = 0;
				double nz = 0;

				for (int current = 0; current < ordinatesArray.length - 3; current = current+3) {
					int next = current+3;
					if (next >= ordinatesArray.length - 3) next = 0;
					nx = nx + ((ordinatesArray[current+1] - ordinatesArray[next+1]) * (ordinatesArray[current+2] + ordinatesArray[next+2])); 
					ny = ny + ((ordinatesArray[current+2] - ordinatesArray[next+2]) * (ordinatesArray[current] + ordinatesArray[next])); 
					nz = nz + ((ordinatesArray[current] - ordinatesArray[next]) * (ordinatesArray[current+1] + ordinatesArray[next+1])); 
				}

				double value = Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (value == 0) { // not a surface, but a line
					continue;
				}
				nx = nx / value;
				ny = ny / value;
				nz = nz / value;

				for (int i = 0; i < unconvertedSurface.getNumElements(); i++) {
					ordinatesArray = unconvertedSurface.getCoordinates(i);
					for (int j = 0; j < ordinatesArray.length; j = j + 3) {
						// coordinates = coordinates + hlDistance * (dot product of normal vector and unity vector)
						ordinatesArray[j] = ordinatesArray[j] + hlDistance * nx;
						ordinatesArray[j+1] = ordinatesArray[j+1] + hlDistance * ny;
						ordinatesArray[j+2] = ordinatesArray[j+2] + zOffset + hlDistance * nz;
					}
				}

				// now convert to WGS84
				GeometryObject surface = convertToWGS84(unconvertedSurface);
				unconvertedSurface = null;

				PolygonType polygon = kmlFactory.createPolygonType();
				switch (config.getProject().getKmlExporter().getAltitudeMode()) {
				case ABSOLUTE:
					polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.ABSOLUTE));
					break;
				case RELATIVE:
					polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
					break;
				}
				multiGeometry.getAbstractGeometryGroup().add(kmlFactory.createPolygon(polygon));

				for (int i = 0; i < surface.getNumElements(); i++) {
					LinearRingType linearRing = kmlFactory.createLinearRingType();
					BoundaryType boundary = kmlFactory.createBoundaryType();
					boundary.setLinearRing(linearRing);

					if (i == 0)
						polygon.setOuterBoundaryIs(boundary);
					else
						polygon.getInnerBoundaryIs().add(boundary);

					// order points clockwise
					ordinatesArray = surface.getCoordinates(i);
					for (int j = 0; j < ordinatesArray.length; j = j+3)
						linearRing.getCoordinates().add(String.valueOf(reducePrecisionForXorY(ordinatesArray[j]) + "," 
								+ reducePrecisionForXorY(ordinatesArray[j+1]) + ","
								+ reducePrecisionForZ(ordinatesArray[j+2])));
				}
			}
		}
		catch (Exception e) {
			Logger.getInstance().warn("Exception when generating highlighting geometry of object " + work.getGmlId());
			e.printStackTrace();
		}
		finally {
			if (rs != null) rs.close();
			if (getGeometriesStmt != null) getGeometriesStmt.close();
		}

		return placemarkList;
	}

	private String getBalloonContentFromGenericAttribute(long id) {

		String balloonContent = null;
		String genericAttribName = "Balloon_Content"; 
		PreparedStatement selectQuery = null;
		ResultSet rs = null;

		try {
			// look for the value in the DB
			selectQuery = connection.prepareStatement(Queries.GET_STRVAL_GENERICATTRIB_FROM_ID);
			selectQuery.setLong(1, id);
			selectQuery.setString(2, genericAttribName);
			rs = selectQuery.executeQuery();
			if (rs.next()) {
				balloonContent = rs.getString(1);
			}
		}
		catch (Exception e) {}
		finally {
			try {
				if (rs != null) rs.close();
				if (selectQuery != null) selectQuery.close();
			}
			catch (Exception e2) {}
		}
		return balloonContent;
	}

	protected void addBalloonContents(PlacemarkType placemark, long id) {
		try {
			switch (getBalloonSettings().getBalloonContentMode()) {
			case GEN_ATTRIB:
				String balloonTemplate = getBalloonContentFromGenericAttribute(id);
				if (balloonTemplate != null) {
					if (getBalloonTemplateHandler() == null) { // just in case
						setBalloonTemplateHandler(new BalloonTemplateHandlerImpl((File) null, connection));
					}
					placemark.setDescription(getBalloonTemplateHandler().getBalloonContent(balloonTemplate, id, currentLod));
				}
				break;
			case GEN_ATTRIB_AND_FILE:
				balloonTemplate = getBalloonContentFromGenericAttribute(id);
				if (balloonTemplate != null) {
					placemark.setDescription(getBalloonTemplateHandler().getBalloonContent(balloonTemplate, id, currentLod));
					break;
				}
			case FILE :
				if (getBalloonTemplateHandler() != null) {
					placemark.setDescription(getBalloonTemplateHandler().getBalloonContent(id, currentLod));
				}
				break;
			}
		}
		catch (Exception e) { } // invalid balloons are silently discarded
	}

	protected void fillX3dMaterialValues (X3DMaterial x3dMaterial, ResultSet rs) throws SQLException {

		double ambientIntensity = rs.getDouble("x3d_ambient_intensity");
		if (!rs.wasNull()) {
			x3dMaterial.setAmbientIntensity(ambientIntensity);
		}
		double shininess = rs.getDouble("x3d_shininess");
		if (!rs.wasNull()) {
			x3dMaterial.setShininess(shininess);
		}
		double transparency = rs.getDouble("x3d_transparency");
		if (!rs.wasNull()) {
			x3dMaterial.setTransparency(transparency);
		}
		Color color = getX3dColorFromString(rs.getString("x3d_diffuse_color"));
		if (color != null) {
			x3dMaterial.setDiffuseColor(color);
		}
		color = getX3dColorFromString(rs.getString("x3d_specular_color"));
		if (color != null) {
			x3dMaterial.setSpecularColor(color);
		}
		color = getX3dColorFromString(rs.getString("x3d_emissive_color"));
		if (color != null) {
			x3dMaterial.setEmissiveColor(color);
		}
		x3dMaterial.setIsSmooth(rs.getInt("x3d_is_smooth") == 1);
	}

	private Color getX3dColorFromString(String colorString) {
		Color color = null;
		if (colorString != null) {
			List<Double> colorList = Util.string2double(colorString, "\\s+");

			if (colorList != null && colorList.size() >= 3) {
				color = new Color(colorList.get(0), colorList.get(1), colorList.get(2));
			}
		}
		return color;
	}

	protected double getZOffsetFromConfigOrDB (long id) {

		double zOffset = Double.MAX_VALUE;;

		switch (config.getProject().getKmlExporter().getAltitudeOffsetMode()) {
		case NO_OFFSET:
			zOffset = 0;
			break;
		case CONSTANT:
			zOffset = config.getProject().getKmlExporter().getAltitudeOffsetValue();
			break;
		case GENERIC_ATTRIBUTE:
			PreparedStatement selectQuery = null;
			ResultSet rs = null;
			String genericAttribName = "GE_LoD" + currentLod + "_zOffset";
			try {
				// first look for the value in the DB
				selectQuery = connection.prepareStatement(Queries.GET_STRVAL_GENERICATTRIB_FROM_ID);
				selectQuery.setLong(1, id);
				selectQuery.setString(2, genericAttribName);
				rs = selectQuery.executeQuery();
				if (rs.next()) {
					String strVal = rs.getString(1);
					if (strVal != null) { // use value in DB 
						StringTokenizer attributeTokenized = new StringTokenizer(strVal, "|");
						attributeTokenized.nextToken(); // skip mode
						zOffset = Double.parseDouble(attributeTokenized.nextToken());
					}
				}
			}
			catch (Exception e) {}
			finally {
				try {
					if (rs != null) rs.close();
					if (selectQuery != null) selectQuery.close();
				}
				catch (Exception e2) {}
			}
		}

		return zOffset;
	}

	protected double getZOffsetFromGEService (long id, List<Point3d> candidates) {

		double zOffset = 0;

		if (config.getProject().getKmlExporter().isCallGElevationService()) { // allowed to query
			PreparedStatement insertQuery = null;
			ResultSet rs = null;
			/*
			PreparedStatement checkQuery = null;
			ResultSet rs2 = null;
			 */
			try {
				// convert candidate points to WGS84
				double[] coords = new double[candidates.size()*3];
				int index = 0;
				for (Point3d point3d: candidates) {
					coords[index++] = point3d.x / 100; // undo trick for very close coordinates
					coords[index++] = point3d.y / 100;
					coords[index++] = point3d.z / 100;
				}
				GeometryObject geomObj = convertToWGS84(GeometryObject.createCurve(coords, 3, dbSrs.getSrid()));
				coords = geomObj.getCoordinates(0);

				Logger.getInstance().info("Getting zOffset from Google's elevation API for " + getGmlId() + " with " + candidates.size() + " points.");
				zOffset = elevationServiceHandler.getZOffset(coords);
				/*
				checkQuery = connection.prepareStatement(Queries.GET_ID_FROM_GMLID);
				checkQuery.setString(1, gmlId);
				rs2 = checkQuery.executeQuery();
				rs2.next();
				long id = rs2.getLong(1);

				if (rs2.next()) {
					Logger.getInstance().warn("gml:id value " + gmlId + " is used for more than one object in the 3DCityDB; zOffset was not stored.");
			    }
				else {
				 */
				// save result in DB for next time
				String genericAttribName = "GE_LoD" + currentLod + "_zOffset";
				insertQuery = connection.prepareStatement(Queries.INSERT_GE_ZOFFSET(databaseAdapter.getSQLAdapter()));
				insertQuery.setString(1, genericAttribName);
				String strVal = "Auto|" + zOffset + "|" + dateFormatter.format(new Date(System.currentTimeMillis()));
				insertQuery.setString(2, strVal);
				insertQuery.setLong(3, id);
				rs = insertQuery.executeQuery();
				//				}
			}
			catch (Exception e) {
				//				if (e.getMessage().startsWith("ORA-01427")) { // single-row subquery returns more than one row 
				//					Logger.getInstance().warn("gml:id value " + gmlId + " is used for more than one object in the 3DCityDB; zOffset was not stored.");
				//				}
			}
			finally {
				try {
					if (rs != null) rs.close();
					if (insertQuery != null) insertQuery.close();
					/*
					if (rs2 != null) rs2.close();
					if (checkQuery != null) checkQuery.close();
					 */
				}
				catch (Exception e2) {}
			}
		}

		return zOffset;
	}

	protected List<Point3d> getLowestPointsCoordinates(ResultSet rs, boolean willCallGEService) throws SQLException {
		double currentlyLowestZCoordinate = Double.MAX_VALUE;
		List<Point3d> coords = new ArrayList<Point3d>();

		rs.next();

		do {
			GeometryObject buildingGeometryObj = geometryConverterAdapter.getGeometry(rs.getObject(1));

			// we are only interested in the z coordinate 
			for (int i = 0; i < buildingGeometryObj.getNumElements(); i++) {
				double[] ordinatesArray = buildingGeometryObj.getCoordinates(i);
				
				for (int j = 2; j < ordinatesArray.length; j = j+3) {
					if (ordinatesArray[j] < currentlyLowestZCoordinate) {
						coords.clear();
						Point3d point3d = new Point3d(ordinatesArray[j-2], ordinatesArray[j-1], ordinatesArray[j]);
						coords.add(point3d);
						currentlyLowestZCoordinate = point3d.z;
					}
					if (willCallGEService && ordinatesArray[j] == currentlyLowestZCoordinate) {
						Point3d point3d = new Point3d(ordinatesArray[j-2], ordinatesArray[j-1], ordinatesArray[j]);
						if (!coords.contains(point3d)) {
							coords.add(point3d);
						}
					}
				}
			}

			if (!rs.next())	break;
		}
		while (true);

		for (Point3d point3d: coords) {
			point3d.x = point3d.x * 100; // trick for very close coordinates
			point3d.y = point3d.y * 100;
			point3d.z = point3d.z * 100;
		}
		return coords;
	}

	protected double[] convertPointCoordinatesToWGS84(double[] coords) throws SQLException {
		double[] pointCoords = null;
		GeometryObject convertedPointGeom = null;

		// this is a nasty hack for Oracle. In Oracle, transforming a single point to WGS84 does not change
		// its z-value, whereas transforming a series of vertices does affect their z-value
		switch (databaseAdapter.getDatabaseType()) {
		case ORACLE:
			convertedPointGeom = convertToWGS84(GeometryObject.createCurve(coords, coords.length, dbSrs.getSrid()));
			break;
		case POSTGIS:
			convertedPointGeom = convertToWGS84(GeometryObject.createPoint(coords, coords.length, dbSrs.getSrid()));
			break;
		}

		if (convertedPointGeom != null)
			pointCoords = convertedPointGeom.getCoordinates(0);

		return pointCoords;
	}

	protected GeometryObject convertToWGS84(GeometryObject geomObj) throws SQLException {
		GeometryObject convertedGeomObj = null;
		PreparedStatement convertStmt = null;
		ResultSet rs2 = null;
		try {
			convertStmt = (dbSrs.is3D() &&  geomObj.getDimension() == 3) ? connection.prepareStatement(Queries.TRANSFORM_GEOMETRY_TO_WGS84_3D(databaseAdapter.getSQLAdapter())) : 
				connection.prepareStatement(Queries.TRANSFORM_GEOMETRY_TO_WGS84(databaseAdapter.getSQLAdapter()));

			// now convert to WGS84
			Object unconverted = geometryConverterAdapter.getDatabaseObject(geomObj, connection);
			if (unconverted == null)
				return null;

			convertStmt.setObject(1, unconverted);
			rs2 = convertStmt.executeQuery();
			while (rs2.next()) {
				// ColumnName is SDO_CS.TRANSFORM(JGeometry, 4326)
				convertedGeomObj = geometryConverterAdapter.getGeometry(rs2.getObject(1));
			}
		}
		catch (Exception e) {
			Logger.getInstance().warn("Exception when converting geometry to WGS84");
			e.printStackTrace();
		}
		finally {
			try {
				if (rs2 != null) rs2.close();
				if (convertStmt != null) convertStmt.close();
			}
			catch (Exception e2) {}
		}

		if (config.getProject().getKmlExporter().isUseOriginalZCoords()) {
			double[][] originalCoords = geomObj.getCoordinates();			
			double[][] convertedCoords = convertedGeomObj.getCoordinates();

			for (int i = 0; i < originalCoords.length; i++) {
				for (int j = 2; j < originalCoords[i].length; j += 3)
					convertedCoords[i][j] = originalCoords[i][j];
			}
		}

		return convertedGeomObj;
	}

	/*
	private List<PlacemarkType> createPlacemarkForEachSurfaceGeometry(OracleResultSet rs,
			String gmlId,
			boolean includeGroundSurface) throws SQLException {

		PlacemarkType placemark = null; 
		List<PlacemarkType> placemarkList = new ArrayList<PlacemarkType>();

		double zOffset = getZOffsetFromConfigOrDB(gmlId);
		List<Point3d> lowestPointCandidates = getLowestPointsCoordinates(rs, (zOffset == Double.MAX_VALUE));
		rs.beforeFirst(); // return cursor to beginning
		if (zOffset == Double.MAX_VALUE) {
			zOffset = getZOffsetFromGEService(gmlId, lowestPointCandidates);
		}
		double lowestZCoordinate = convertPointCoordinatesToWGS84(new double[] {
				lowestPointCandidates.get(0).x/100, // undo trick for very close coordinates
				lowestPointCandidates.get(0).y/100,	
				lowestPointCandidates.get(0).z/100}) [2];

		while (rs.next()) {
			String surfaceType = rs.getString("type");
			if (surfaceType != null && !surfaceType.endsWith("Surface")) {
				surfaceType = surfaceType + "Surface";
			}

			STRUCT buildingGeometryObj = (STRUCT)rs.getObject(1); 
			long surfaceId = rs.getLong("id");
			// results are ordered by surface type
			if (!includeGroundSurface && TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.GROUND_SURFACE).toString().equalsIgnoreCase(surfaceType)) {
				continue;
			}

			JGeometry originalSurface = JGeometry.load(buildingGeometryObj);
			double[] originalOrdinatesArray = originalSurface.getOrdinatesArray();
			if (originalOrdinatesArray == null) {
				continue;
			}

			// convert original surface to WGS84
			JGeometry originalSurfaceWGS84 = convertToWGS84(originalSurface);
			double[] originalOrdinatesArrayWGS84 = originalSurfaceWGS84.getOrdinatesArray();

			// create Placemark for every Polygon
			placemark = kmlFactory.createPlacemarkType();
			placemark.setName(gmlId + "_" + String.valueOf(surfaceId));
			placemark.setId(DisplayForm.GEOMETRY_PLACEMARK_ID + placemark.getName());
			placemark.setStyleUrl("#" + surfaceType + "Normal");

			//			if (config.getProject().getKmlExporter().isIncludeDescription() &&
			//					!config.getProject().getKmlExporter().isGeometryHighlighting()) { // avoid double description
			//				addBalloonContents(placemark, gmlId);
			//			}

			placemarkList.add(placemark);

			PolygonType polygon = kmlFactory.createPolygonType();
			switch (config.getProject().getKmlExporter().getAltitudeMode()) {
			case ABSOLUTE:
				polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.ABSOLUTE));
				break;
			case RELATIVE:
				polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
				break;
			}
			placemark.setAbstractGeometryGroup(kmlFactory.createPolygon(polygon));

			boolean probablyRoof = true;

			for (int i = 0; i < originalSurfaceWGS84.getElemInfo().length; i = i+3) {
				LinearRingType linearRing = kmlFactory.createLinearRingType();
				BoundaryType boundary = kmlFactory.createBoundaryType();
				boundary.setLinearRing(linearRing);
				if (originalSurfaceWGS84.getElemInfo()[i+1] == EXTERIOR_POLYGON_RING) {
					polygon.setOuterBoundaryIs(boundary);
				}
				else { // INTERIOR_POLYGON_RING
					polygon.getInnerBoundaryIs().add(boundary);
				}

				int startNextRing = ((i+3) < originalSurfaceWGS84.getElemInfo().length) ? 
						originalSurfaceWGS84.getElemInfo()[i+3] - 1: // still holes to come
							originalOrdinatesArrayWGS84.length; // default

						// order points clockwise
						for (int j = originalSurfaceWGS84.getElemInfo()[i] - 1; j < startNextRing; j = j+3) {
							linearRing.getCoordinates().add(String.valueOf(reducePrecisionForXorY(originalOrdinatesArrayWGS84[j]) + "," 
									+ reducePrecisionForXorY(originalOrdinatesArrayWGS84[j+1]) + ","
									+ reducePrecisionForZ(originalOrdinatesArrayWGS84[j+2] + zOffset)));

							probablyRoof = probablyRoof && (reducePrecisionForZ(originalOrdinatesArrayWGS84[j+2] - lowestZCoordinate) > 0);
							// not touching the ground
						}

						if (surfaceType == null) {
							String likelySurfaceType = (probablyRoof && currentLod < 3) ?
									TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.ROOF_SURFACE).toString().toString() :
										TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.WALL_SURFACE).toString();
									placemark.setStyleUrl("#" + likelySurfaceType + "Normal");
						}

			}

		}

		return placemarkList;
	}


	private List<PlacemarkType> createPlacemarkForEachHighlingtingGeometry(KmlSplittingResult work) throws SQLException {

		PlacemarkType highlightingPlacemark = null; 
		List<PlacemarkType> placemarkList = new ArrayList<PlacemarkType>();

		PreparedStatement getGeometriesStmt = null;
		OracleResultSet rs = null;

		double hlDistance = work.getDisplayForm().getHighlightingDistance();

		try {
			getGeometriesStmt = connection.prepareStatement(Queries.getSingleBuildingHighlightingQuery(currentLod),
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);

			for (int i = 1; i <= getGeometriesStmt.getParameterMetaData().getParameterCount(); i++) {
				getGeometriesStmt.setString(i, work.getGmlId());
			}
			rs = (OracleResultSet)getGeometriesStmt.executeQuery();

			double zOffset = getZOffsetFromConfigOrDB(work.getGmlId());
			List<Point3d> lowestPointCandidates = getLowestPointsCoordinates(rs, (zOffset == Double.MAX_VALUE));
			rs.beforeFirst(); // return cursor to beginning
			if (zOffset == Double.MAX_VALUE) {
				zOffset = getZOffsetFromGEService(work.getGmlId(), lowestPointCandidates);
			}

			while (rs.next()) {
//				String surfaceType = rs.getString("type");
//				if (!surfaceType.endsWith("Surface")) {
//					surfaceType = surfaceType + "Surface";
//				}
				//	results are ordered by surface type
//				if (!includeGroundSurface && CityGMLClass.GROUNDSURFACE.toString().equalsIgnoreCase(surfaceType)) {
//					continue;
//				}

				STRUCT buildingGeometryObj = (STRUCT)rs.getObject(1); 
				long surfaceId = rs.getLong("id");

				JGeometry originalSurface = JGeometry.load(buildingGeometryObj);
				double[] ordinatesArray = originalSurface.getOrdinatesArray();
				if (ordinatesArray == null) {
					continue;
				}

				int contourCount = originalSurface.getElemInfo().length/3;
				// remove normal-irrelevant points
				int startContour1 = originalSurface.getElemInfo()[0] - 1;
				int endContour1 = (contourCount == 1) ? 
						ordinatesArray.length: // last
							originalSurface.getElemInfo()[3] - 1; // holes are irrelevant for normal calculation
				// last point of polygons in gml is identical to first and useless for GeometryInfo
				endContour1 = endContour1 - 3;

				double nx = 0;
				double ny = 0;
				double nz = 0;

				for (int current = startContour1; current < endContour1; current = current+3) {
					int next = current+3;
					if (next >= endContour1) next = 0;
					nx = nx + ((ordinatesArray[current+1] - ordinatesArray[next+1]) * (ordinatesArray[current+2] + ordinatesArray[next+2])); 
					ny = ny + ((ordinatesArray[current+2] - ordinatesArray[next+2]) * (ordinatesArray[current] + ordinatesArray[next])); 
					nz = nz + ((ordinatesArray[current] - ordinatesArray[next]) * (ordinatesArray[current+1] + ordinatesArray[next+1])); 
				}

				double value = Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (value == 0) { // not a surface, but a line
					continue;
				}
				nx = nx / value;
				ny = ny / value;
				nz = nz / value;

				double factor = 1.5; // 0.5 inside Global Highlighting; 1.5 outside Global Highlighting;

				for (int i = 0; i < ordinatesArray.length; i = i + 3) {
					// coordinates = coordinates + hlDistance * (dot product of normal vector and unity vector)
					ordinatesArray[i] = ordinatesArray[i] + hlDistance * factor * nx;
					ordinatesArray[i+1] = ordinatesArray[i+1] + hlDistance * factor * ny;
					ordinatesArray[i+2] = ordinatesArray[i+2] + hlDistance * factor * nz;
				}

				// now convert highlighting to WGS84
				JGeometry highlightingSurfaceWGS84 = convertToWGS84(originalSurface);
				double[] highlightingOrdinatesArrayWGS84 = highlightingSurfaceWGS84.getOrdinatesArray();

				// create highlighting Placemark for every Polygon
				highlightingPlacemark = kmlFactory.createPlacemarkType();
				highlightingPlacemark.setName(work.getGmlId() + "_" + String.valueOf(surfaceId));
				highlightingPlacemark.setId(DisplayForm.GEOMETRY_HIGHLIGHTED_PLACEMARK_ID + highlightingPlacemark.getName());
				highlightingPlacemark.setStyleUrl("#" + work.getDisplayForm().getName() + "Style");

//				if (config.getProject().getKmlExporter().isIncludeDescription() &&
//						!config.getProject().getKmlExporter().isGeometryHighlighting()) { // avoid double description
//					addBalloonContents(placemark, gmlId);
//				}

				placemarkList.add(highlightingPlacemark);

				PolygonType highlightingPolygon = kmlFactory.createPolygonType();
				switch (config.getProject().getKmlExporter().getAltitudeMode()) {
				case ABSOLUTE:
					highlightingPolygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.ABSOLUTE));
					break;
				case RELATIVE:
					highlightingPolygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
					break;
				}
				highlightingPlacemark.setAbstractGeometryGroup(kmlFactory.createPolygon(highlightingPolygon));

				for (int i = 0; i < highlightingSurfaceWGS84.getElemInfo().length; i = i+3) {
					LinearRingType highlightingLinearRing = kmlFactory.createLinearRingType();
					BoundaryType highlightingBoundary = kmlFactory.createBoundaryType();
					highlightingBoundary.setLinearRing(highlightingLinearRing);
					if (highlightingSurfaceWGS84.getElemInfo()[i+1] == EXTERIOR_POLYGON_RING) {
						highlightingPolygon.setOuterBoundaryIs(highlightingBoundary);
					}
					else { // INTERIOR_POLYGON_RING
						highlightingPolygon.getInnerBoundaryIs().add(highlightingBoundary);
					}

					int startNextRing = ((i+3) < highlightingSurfaceWGS84.getElemInfo().length) ? 
							highlightingSurfaceWGS84.getElemInfo()[i+3] - 1: // still holes to come
								highlightingOrdinatesArrayWGS84.length; // default

							// order points clockwise
							for (int j = highlightingSurfaceWGS84.getElemInfo()[i] - 1; j < startNextRing; j = j+3) {
								highlightingLinearRing.getCoordinates().add(String.valueOf(reducePrecisionForXorY(highlightingOrdinatesArrayWGS84[j]) + "," 
										+ reducePrecisionForXorY(highlightingOrdinatesArrayWGS84[j+1]) + ","
										+ reducePrecisionForZ(highlightingOrdinatesArrayWGS84[j+2] + zOffset)));
							}

				}
			}
		}
		catch (Exception e) {
			Logger.getInstance().warn("Exception when generating highlighting geometry of building " + work.getGmlId());
			e.printStackTrace();
		}
		finally {
			if (rs != null) rs.close();
			if (getGeometriesStmt != null) getGeometriesStmt.close();
		}

		return placemarkList;
	}
	 */

	protected static byte[] hexStringToByteArray(String hex) {
		// padding if needed
		if (hex.length()/2 != (hex.length()+1)/2) {
			hex = "0" + hex;
		}

		byte[] bytes = new byte[hex.length()/2];
		try {
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}
		return bytes;
	}

	protected class Node{
		double key;
		Object value;
		Node rightArc;
		Node leftArc;

		protected Node(double key, Object value){
			this.key = key;
			this.value = value;
		}

		protected void setLeftArc(Node leftArc) {
			this.leftArc = leftArc;
		}

		protected Node getLeftArc() {
			return leftArc;
		}

		protected void setRightArc (Node rightArc) {
			this.rightArc = rightArc;
		}

		protected Node getRightArc() {
			return rightArc;
		}

	}

	protected class NodeX extends Node{
		protected NodeX(double key, Object value){
			super(key, value);
		}
	}
	protected class NodeY extends Node{
		protected NodeY(double key, Object value){
			super(key, value);
		}
	}
	protected class NodeZ extends Node{
		protected NodeZ(double key, Object value){
			super(key, value);
		}
	}


}
