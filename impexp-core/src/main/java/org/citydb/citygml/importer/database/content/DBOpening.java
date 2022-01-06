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
import org.citygml4j.model.citygml.core.Address;
import org.citygml4j.model.citygml.core.AddressProperty;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.gml.base.AbstractGML;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;

public class DBOpening extends AbstractDBImporter {
	private boolean affineTransformation;
	private DBCityObject cityObjectImporter;
	private DBSurfaceGeometry surfaceGeometryImporter;
	private GeometryConverter geometryConverter;
	private DBImplicitGeometry implicitGeometryImporter;
	private DBOpeningToThemSurface openingToThemSurfaceImporter;
	private DBAddress addressImporter;
	private AttributeValueJoiner valueJoiner;

	public DBOpening(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		implicitGeometryImporter = importer.getImporter(DBImplicitGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		openingToThemSurfaceImporter = importer.getImporter(DBOpeningToThemSurface.class);
		addressImporter = importer.getImporter(DBAddress.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected String getTableName() {
		return TableEnum.OPENING.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "opening/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".opening (id, objectclass_id, address_id, " +
				"lod3_multi_surface_id, lod4_multi_surface_id, " +
				"lod3_implicit_rep_id, lod4_implicit_rep_id, " +
				"lod3_implicit_ref_point, lod4_implicit_ref_point, " +
				"lod3_implicit_transformation, lod4_implicit_transformation) values " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
				SchemaManagerAdapter.ONTO_ADDRESS_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_TRANSFORMATION + param +
				".}" +
				"}";

		return stmt;
	}

	protected long doImport(AbstractOpening opening) throws CityGMLImportException, SQLException {
		return doImport(opening, null, 0);
	}

	protected long doImport(AbstractOpening opening, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(opening);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long openingId = cityObjectImporter.doImport(opening, featureType);

		URL objectURL = null;
		int index = 0;
		// import opening information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				String uuid = opening.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				objectURL = new URL(iriGraphObject + uuid + "/");
			} catch (MalformedURLException e) {
				preparedStatement.setObject(++index, NodeFactory.createBlankNode());
			}
			// primary id
			preparedStatement.setURL(++index, objectURL);
			opening.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
		} else {
			preparedStatement.setLong(++index, openingId);
		}
		// primary id
		preparedStatement.setURL(++index, objectURL);
		// objectclass id
		preparedStatement.setInt(++index, featureType.getObjectClassId());

		// core:address
		long addressId = 0;
		if (opening instanceof Door) {
			Door door = (Door)opening;

			if (door.isSetAddress() && !door.getAddress().isEmpty()) {
				// unfortunately, we can just represent one address in the database...
				AddressProperty property = door.getAddress().get(0);
				Address address = property.getAddress();

				if (address != null) {
					addressId = addressImporter.doImport(address);
					property.unsetAddress();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								featureType.getTable(),
								openingId,
								href,
								"ADDRESS_ID"));
					}
				}
			}
		}

		if (addressId != 0)
			preparedStatement.setLong(++index, addressId);
		else if (importer.isBlazegraph())
			setBlankNode(preparedStatement, ++index);
		else
			preparedStatement.setNull(3, Types.NULL);

		// brid:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				opening.getLod3MultiSurface(),
				opening.getLod4MultiSurface()
		}, new int[]{3, 4}, "_multi_surface_id", 4);

		// bldg:lodXImplicitRepresentation
		for (int i = 0; i < 2; i++) {
			ImplicitRepresentationProperty implicit = null;
			GeometryObject pointGeom = null;
			String matrixString = null;
			long implicitId = 0;

			switch (i) {
			case 0:
				implicit = opening.getLod3ImplicitRepresentation();
				break;
			case 1:
				implicit = opening.getLod4ImplicitRepresentation();
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

					// reference to implicit geometry
					implicitId = implicitGeometryImporter.doImport(geometry);
				}
			}

			if (implicitId != 0)
				preparedStatement.setLong(++index, implicitId);
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, ++index);
			else
				preparedStatement.setNull(6 + i, Types.NULL);

			if (pointGeom != null)
				preparedStatement.setObject(++index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, ++index);
			else
				preparedStatement.setNull(8 + i, importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType(),
						importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName());

			if (matrixString != null)
				preparedStatement.setString(++index, matrixString);
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, ++index);
			else
				preparedStatement.setNull(10 + i, Types.VARCHAR);
		}

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.OPENING);

		if (parent instanceof AbstractBoundarySurface)
			if (importer.isBlazegraph()) {
				openingToThemSurfaceImporter.doImport((URL)opening.getLocalProperty(CoreConstants.OBJECT_URIID),
						(URL) ((AbstractGML) ((OpeningProperty) opening.getParent()).getParent())
								.getLocalProperty(CoreConstants.OBJECT_URIID));
			} else {
				openingToThemSurfaceImporter.doImport(openingId, parentId);
			}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(opening, openingId, featureType);

		return openingId;
	}

}
