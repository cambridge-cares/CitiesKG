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

import org.apache.jena.graph.NodeFactory;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.SequenceEnum;
import org.citydb.database.schema.TableEnum;
import org.citydb.util.CoreConstants;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.generics.AbstractGenericAttribute;
import org.citygml4j.model.citygml.generics.DateAttribute;
import org.citygml4j.model.citygml.generics.DoubleAttribute;
import org.citygml4j.model.citygml.generics.GenericAttributeSet;
import org.citygml4j.model.citygml.generics.IntAttribute;
import org.citygml4j.model.citygml.generics.MeasureAttribute;
import org.citygml4j.model.citygml.generics.StringAttribute;
import org.citygml4j.model.citygml.generics.UriAttribute;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.citygml4j.model.gml.base.AbstractGML;

public class DBCityObjectGenericAttrib implements DBImporter {
	private final Connection batchConn;
	private final CityGMLImportManager importer;

	private PreparedStatement psAtomicGenericAttribute;
	private PreparedStatement psGenericAttributeSet;
	private PreparedStatement psGenericAttributeMember;
	private int batchCounter;
	//@todo Replace graph IRI and OntocityGML prefix with variables set on the GUI
	private static final String IRI_GRAPH_BASE = "http://localhost/berlin/";
	private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
	private static final String IRI_GRAPH_OBJECT_REL = "cityobjectgenericattrib/";
	private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;


	public DBCityObjectGenericAttrib(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		this.batchConn = batchConn;
		this.importer = importer;
		String param = "  ?;";

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		StringBuilder stmt = new StringBuilder();

		if (importer.isBlazegraph()) {
			stmt = getSPARQLStatement1(stmt);

		} else {
			stmt.append("insert into ").append(schema).append(".cityobject_genericattrib (id, parent_genattrib_id, " +
					"root_genattrib_id, attrname, datatype, genattribset_codespace, cityobject_id) values ")
					.append("(?, ?, ?, ?, ?, ?, ?)");
		}

		psGenericAttributeSet = batchConn.prepareStatement(stmt.toString());		

		stmt.setLength(0);

		if (importer.isBlazegraph()) {
			stmt = getSPARQLStatement2(stmt, importer);

			psGenericAttributeMember = batchConn.prepareStatement(stmt +
							SchemaManagerAdapter.ONTO_PARRENT_GENATTRIB_ID + param +
							SchemaManagerAdapter.ONTO_ROOT_GENATTRIB_ID + param + ".}}");

			psAtomicGenericAttribute = batchConn.prepareStatement(stmt +
							SchemaManagerAdapter.ONTO_PARRENT_GENATTRIB_ID + param +
							SchemaManagerAdapter.ONTO_ROOT_GENATTRIB_ID + param);
		} else {
			stmt.append("insert into ").append(schema).append(".cityobject_genericattrib (id, attrname, datatype," +
					" strval, intval, realval, urival, dateval, unit, cityobject_id, " +
					"parent_genattrib_id, root_genattrib_id) values ")
					.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ");
			psGenericAttributeMember = batchConn.prepareStatement(stmt + "?, ?)");
			psAtomicGenericAttribute = batchConn.prepareStatement(stmt + "null, " +
					importer.getDatabaseAdapter().getSQLAdapter()
							.getCurrentSequenceValue(SequenceEnum.CITYOBJECT_GENERICATTRIB_ID_SEQ.getName()) + ")");
		}

	}

	private StringBuilder getSPARQLStatement1(StringBuilder stmt){
		String param = "  ?;";
		stmt.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_PARRENT_GENATTRIB_ID + param +
				SchemaManagerAdapter.ONTO_ROOT_GENATTRIB_ID + param +
				SchemaManagerAdapter.ONTO_ATTR_NAME + param +
				SchemaManagerAdapter.ONTO_DATA_TYPE + param +
				SchemaManagerAdapter.ONTO_GENATTRIBSET_CODESPACE + param +
				SchemaManagerAdapter.ONTO_CITY_OBJECT_ID + param +
				".}" +
				"}"
		);
		return stmt;
	}

	private StringBuilder getSPARQLStatement2(StringBuilder stmt, CityGMLImportManager importer) throws SQLException {
		String param = "  ?;";
		stmt.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_ATTR_NAME + param +
				SchemaManagerAdapter.ONTO_DATA_TYPE + param +
				SchemaManagerAdapter.ONTO_STR_VAL + param +
				SchemaManagerAdapter.ONTO_INT_VAL + param +
				SchemaManagerAdapter.ONTO_REAL_VAL + param +
				SchemaManagerAdapter.ONTO_URI_VAL + param +
				SchemaManagerAdapter.ONTO_DATE_VAL + param +
				SchemaManagerAdapter.ONTO_UNIT + param +
				SchemaManagerAdapter.ONTO_CITY_OBJECT_ID + param
		);
		return stmt;
	}

	public void doImport(AbstractGenericAttribute genericAttribute, long cityObjectId) throws CityGMLImportException, SQLException {
		doImport(genericAttribute, 0, 0, cityObjectId);
	}

	protected void doImport(AbstractGenericAttribute genericAttribute, long parentId, long rootId, long cityObjectId) throws CityGMLImportException, SQLException {
		boolean isBlazegraph = importer.getDatabaseAdapter().getDatabaseType().value().equals(DatabaseType.BLAZE.value());
		// attribute name may not be null
		if (!genericAttribute.isSetName())
			return;

		int index = 0;

		if (isBlazegraph) {
			++index;
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				psGenericAttributeSet.setURL(index, url);
				psAtomicGenericAttribute.setURL(index, url);
				psGenericAttributeMember.setURL(index, url);
				++index;
				psGenericAttributeSet.setURL(index, url);
				psAtomicGenericAttribute.setURL(index, url);
				psGenericAttributeMember.setURL(index, url);
			} catch (MalformedURLException e) {
				psGenericAttributeSet.setObject(index, NodeFactory.createBlankNode());
				psAtomicGenericAttribute.setObject(index, NodeFactory.createBlankNode());
				psGenericAttributeMember.setObject(index, NodeFactory.createBlankNode());
				++index;
				psGenericAttributeSet.setObject(index, NodeFactory.createBlankNode());
				psAtomicGenericAttribute.setObject(index, NodeFactory.createBlankNode());
				psGenericAttributeMember.setObject(index, NodeFactory.createBlankNode());
			}
		}

		if (genericAttribute.getCityGMLClass() == CityGMLClass.GENERIC_ATTRIBUTE_SET) {
			GenericAttributeSet attributeSet = (GenericAttributeSet)genericAttribute;

			// we do not import empty attribute sets
			if (attributeSet.getGenericAttribute().isEmpty())
				return;

			long attributeSetId = importer.getNextSequenceValue(SequenceEnum.CITYOBJECT_GENERICATTRIB_ID_SEQ.getName());
			if (rootId == 0)
				rootId = attributeSetId;

			psGenericAttributeSet.setLong(++index, attributeSetId);

			if (parentId != 0) {
				psGenericAttributeSet.setLong(++index, parentId);
			} else if (isBlazegraph) {
				setBlankNode(psGenericAttributeSet, ++index);
			}
			else
				psGenericAttributeSet.setNull(++index, Types.NULL);

			psGenericAttributeSet.setLong(++index, rootId);
			psGenericAttributeSet.setString(++index, attributeSet.getName());
			psGenericAttributeSet.setInt(++index, 7);
			psGenericAttributeSet.setString(++index, attributeSet.getCodeSpace());
			psGenericAttributeSet.setLong(++index, cityObjectId);


			psGenericAttributeSet.addBatch();
			if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
				importer.executeBatch(TableEnum.CITYOBJECT_GENERICATTRIB);

			// insert members of the attribute set
			for (AbstractGenericAttribute attribute : attributeSet.getGenericAttribute())
				doImport(attribute, attributeSetId, rootId, cityObjectId);			

		} else {
			@SuppressWarnings("resource")
			PreparedStatement ps = rootId == 0 ? psAtomicGenericAttribute : psGenericAttributeMember;
			ps.setString(++index, genericAttribute.getName());

			switch (genericAttribute.getCityGMLClass()) {
			case STRING_ATTRIBUTE:
				ps.setInt(++index, 1);

				StringAttribute stringAttribute = (StringAttribute)genericAttribute;
				if (stringAttribute.isSetValue()) {
					if (isBlazegraph) {
						ps.setString(++index, stringAttribute.getValue());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
					} else {
						ps.setString(3, stringAttribute.getValue());
					}
				} else if (isBlazegraph) {
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
				} else {
					ps.setNull(3, Types.VARCHAR);

					ps.setNull(4, Types.NULL);
					ps.setNull(5, Types.NULL);
					ps.setNull(6, Types.VARCHAR);
					ps.setNull(7, Types.DATE);
					ps.setNull(8, Types.VARCHAR);
				}
				break;
			case INT_ATTRIBUTE:
				ps.setInt(++index, 2);

				IntAttribute intAttribute = (IntAttribute)genericAttribute;
				if (intAttribute.isSetValue()) {
					if (isBlazegraph) {
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setInt(5, intAttribute.getValue());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
					} else {
						ps.setInt(4, intAttribute.getValue());
					}
				} else if (isBlazegraph) {
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
				} else {
					ps.setNull(4, Types.NULL);

					ps.setNull(3, Types.VARCHAR);
					ps.setNull(5, Types.NULL);
					ps.setNull(6, Types.VARCHAR);
					ps.setNull(7, Types.DATE);
					ps.setNull(8, Types.VARCHAR);
				}
				break;
			case DOUBLE_ATTRIBUTE:
				ps.setInt(++index, 3);

				DoubleAttribute doubleAttribute = (DoubleAttribute)genericAttribute;
				if (doubleAttribute.isSetValue()) {
					if (isBlazegraph) {
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setDouble(6, doubleAttribute.getValue());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
					} else {
						ps.setDouble(5, doubleAttribute.getValue());
					}
				} else if (isBlazegraph) {
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
				} else {
					ps.setNull(5, Types.NULL);

					ps.setNull(3, Types.VARCHAR);
					ps.setNull(4, Types.NULL);
					ps.setNull(6, Types.VARCHAR);
					ps.setNull(7, Types.DATE);
					ps.setNull(8, Types.VARCHAR);
				}
				break;
			case URI_ATTRIBUTE:
				ps.setInt(++index, 4);

				UriAttribute uriAttribute = (UriAttribute)genericAttribute;
				if (uriAttribute.isSetValue()) {
					if (isBlazegraph) {
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setString(7, uriAttribute.getValue());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
					} else {
						ps.setString(6, uriAttribute.getValue());
					}
				} else if (isBlazegraph) {
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
				} else {
					ps.setNull(6, Types.VARCHAR);

					ps.setNull(3, Types.VARCHAR);
					ps.setNull(4, Types.NULL);
					ps.setNull(5, Types.NULL);
					ps.setNull(7, Types.DATE);
					ps.setNull(8, Types.VARCHAR);
				}
				break;
			case DATE_ATTRIBUTE:
				ps.setInt(++index, 5);

				DateAttribute dateAttribute = (DateAttribute)genericAttribute;
				if (dateAttribute.isSetValue()) {
					if (isBlazegraph) {
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setTimestamp(8, Timestamp.valueOf(dateAttribute.getValue().atStartOfDay()));
						ps.setObject(++index, NodeFactory.createBlankNode());
					} else {
						ps.setTimestamp(7, Timestamp.valueOf(dateAttribute.getValue().atStartOfDay()));
					}
				} else if (isBlazegraph) {
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
				} else {
					ps.setNull(7, Types.TIMESTAMP);

					ps.setNull(3, Types.VARCHAR);
					ps.setNull(4, Types.NULL);
					ps.setNull(5, Types.NULL);
					ps.setNull(6, Types.VARCHAR);
					ps.setNull(8, Types.VARCHAR);
				}
				break;
			case MEASURE_ATTRIBUTE:
				ps.setInt(++index, 6);

				MeasureAttribute measureAttribute = (MeasureAttribute)genericAttribute;
				if (measureAttribute.isSetValue()) {
					if (isBlazegraph) {
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setDouble(6, measureAttribute.getValue().getValue());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setObject(++index, NodeFactory.createBlankNode());
						ps.setString(9, measureAttribute.getValue().getUom());
					} else {
						ps.setDouble(5, measureAttribute.getValue().getValue());
						ps.setString(8, measureAttribute.getValue().getUom());
					}
				} else if (isBlazegraph) {
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
					ps.setObject(++index, NodeFactory.createBlankNode());
				} else {
					ps.setNull(5, Types.NULL);
					ps.setNull(8, Types.VARCHAR);
				}

				if (!isBlazegraph) {
					ps.setNull(3, Types.VARCHAR);
					ps.setNull(4, Types.NULL);
					ps.setNull(6, Types.VARCHAR);
					ps.setNull(7, Types.DATE);
				}
				break;
			default:
				if (isBlazegraph) {
					ps.setObject(++index, NodeFactory.createBlankNode());
				} else {
					ps.setNull(++index, Types.NUMERIC);
				}
			}

			if (isBlazegraph && genericAttribute.isSetParent()) {
				ps.setURL(++index, (URL) ((AbstractGML)genericAttribute.getParent()).getLocalProperty(CoreConstants.OBJECT_URIID));
			} else {
				ps.setLong(++index, cityObjectId);
			}

			if (rootId != 0) {
				ps.setLong(++index, parentId);
				ps.setLong(++index, rootId);
			} else if (isBlazegraph) {
				ps.setObject(++index, NodeFactory.createBlankNode());
				ps.setObject(++index, NodeFactory.createBlankNode());
			}

			ps.addBatch();
			if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
				importer.executeBatch(TableEnum.CITYOBJECT_GENERICATTRIB);
		}
	}

	public void doImport(String attributeName, GeometryObject geometry, long cityObjectId) throws CityGMLImportException, SQLException {
		boolean isBlazegraph = importer.getDatabaseAdapter().getDatabaseType().value().equals(DatabaseType.BLAZE.value());
		if (attributeName == null || attributeName.length() == 0)
			return;

		psAtomicGenericAttribute.setString(1, attributeName);
		psAtomicGenericAttribute.setInt(2, 8);
		if (isBlazegraph) {
			psAtomicGenericAttribute.setObject(3, NodeFactory.createBlankNode());
			psAtomicGenericAttribute.setObject(4, NodeFactory.createBlankNode());
			psAtomicGenericAttribute.setObject(5, NodeFactory.createBlankNode());
			psAtomicGenericAttribute.setObject(6, NodeFactory.createBlankNode());
			psAtomicGenericAttribute.setObject(7, NodeFactory.createBlankNode());
			psAtomicGenericAttribute.setObject(8, NodeFactory.createBlankNode());
		} else {
			psAtomicGenericAttribute.setNull(3, Types.VARCHAR);
			psAtomicGenericAttribute.setNull(4, Types.NULL);
			psAtomicGenericAttribute.setNull(5, Types.NULL);
			psAtomicGenericAttribute.setNull(6, Types.VARCHAR);
			psAtomicGenericAttribute.setNull(7, Types.DATE);
			psAtomicGenericAttribute.setNull(8, Types.VARCHAR);
		}

		psAtomicGenericAttribute.setObject(9, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geometry, batchConn));
		psAtomicGenericAttribute.setLong(10, cityObjectId);

		psAtomicGenericAttribute.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.CITYOBJECT_GENERICATTRIB);
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psAtomicGenericAttribute.executeBatch();
			psGenericAttributeSet.executeBatch();
			psGenericAttributeMember.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psAtomicGenericAttribute.close();
		psGenericAttributeSet.close();
		psGenericAttributeMember.close();
	}

	/**
	 * Sets blank nodes on PreparedStatements. Used with SPARQL which does not support nulls.
	 */
	private void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}

}
