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
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.citygml.tunnel.AbstractBoundarySurface;
import org.citygml4j.model.citygml.tunnel.AbstractOpening;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;

public class DBTunnelOpening extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private GeometryConverter geometryConverter;
	private DBImplicitGeometry implicitGeometryImporter;
	private DBTunnelOpenToThemSrf openingToThemSurfaceImporter;
	private AttributeValueJoiner valueJoiner;
	private boolean affineTransformation;

	public DBTunnelOpening(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		implicitGeometryImporter = importer.getImporter(DBImplicitGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		openingToThemSurfaceImporter = importer.getImporter(DBTunnelOpenToThemSrf.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected String getTableName() {
		return TableEnum.TUNNEL_OPENING.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "tunnelopening/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".tunnel_opening (id, objectclass_id, lod3_multi_surface_id, lod4_multi_surface_id, " +
				"lod3_implicit_rep_id, lod4_implicit_rep_id, " +
				"lod3_implicit_ref_point, lod4_implicit_ref_point, " +
				"lod3_implicit_transformation, lod4_implicit_transformation) values " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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

		// import opening information
		// primary id
		preparedStatement.setLong(1, openingId);

		// objectclass id
		preparedStatement.setInt(2, featureType.getObjectClassId());

		// tun:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				opening.getLod3MultiSurface(),
				opening.getLod4MultiSurface()
		}, new int[]{3, 4}, "_multi_surface_id", 3);

		// tun:lodXImplicitRepresentation
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

					// reference to IMPLICIT_GEOMETRY
					implicitId = implicitGeometryImporter.doImport(geometry);
				}
			}

			if (implicitId != 0)
				preparedStatement.setLong(5 + i, implicitId);
			else
				preparedStatement.setNull(5 + i, Types.NULL);

			if (pointGeom != null)
				preparedStatement.setObject(7 + i, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
			else
				preparedStatement.setNull(7 + i, importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType(),
						importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName());

			if (matrixString != null)
				preparedStatement.setString(9 + i, matrixString);
			else
				preparedStatement.setNull(9 + i, Types.VARCHAR);
		}

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.TUNNEL_OPENING);

		if (parent instanceof AbstractBoundarySurface)
			openingToThemSurfaceImporter.doImport(openingId, parentId);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(opening, openingId, featureType);

		return openingId;
	}

}
