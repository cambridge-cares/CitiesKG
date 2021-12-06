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
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.SequenceEnum;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.util.CoreConstants;
import org.citygml4j.model.citygml.core.Address;
import org.citygml4j.model.citygml.core.AddressProperty;
import org.citygml4j.model.gml.base.AbstractGML;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.module.xal.XALModuleType;
import org.citygml4j.model.xal.CountryName;
import org.citygml4j.model.xal.LocalityName;
import org.citygml4j.model.xal.PostBoxNumber;
import org.citygml4j.model.xal.PostalCodeNumber;
import org.citygml4j.model.xal.ThoroughfareName;
import org.citygml4j.model.xal.ThoroughfareNumber;
import org.citygml4j.util.walker.XALWalker;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class DBAddress extends AbstractDBImporter {

	private DBAddressToBuilding addressToBuildingImporter;
	private DBAddressToBridge addressToBridgeImporter;
	private GeometryConverter geometryConverter;
	private XALAddressWalker addressWalker;

	private boolean importXALSource;
	private boolean hasGmlIdColumn;
	private String gmlIdCodespace;
	private boolean replaceGmlId;

	public DBAddress(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		addressToBuildingImporter = importer.getImporter(DBAddressToBuilding.class);
		addressToBridgeImporter = importer.getImporter(DBAddressToBridge.class);
		geometryConverter = importer.getGeometryConverter();
		addressWalker = new XALAddressWalker();
	}

	@Override
	protected void preconstructor(Config config) {
		importXALSource = config.getProject().getImporter().getAddress().isSetImportXAL();
		hasGmlIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(3, 1, 0) >= 0;
		replaceGmlId = config.getProject().getImporter().getGmlId().isUUIDModeReplace();

		if (hasGmlIdColumn) {
			gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
			if (gmlIdCodespace != null)
				gmlIdCodespace = "'" + gmlIdCodespace + "', ";
		}
	}

	@Override
	protected String getTableName() {
		return TableEnum.ADDRESS.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "address/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + SQL_SCHEMA + ".address (id, " + (hasGmlIdColumn ? "gmlid, " : "") + (gmlIdCodespace != null ? "gmlid_codespace, " : "") +
				"street, house_number, po_box, zip_code, city, country, multi_point, xal_source) values " +
				"(?, " + (hasGmlIdColumn ? "?, " : "") + (gmlIdCodespace != null ? gmlIdCodespace : "") + "?, ?, ?, ?, ?, ?, ?, ?)";
	}

	@Override
	protected String getSPARQLStatement(){
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
				(hasGmlIdColumn ? SchemaManagerAdapter.ONTO_GML_ID + param : "") +
				(gmlIdCodespace != null ? SchemaManagerAdapter.ONTO_GML_ID_CODESPACE + param : "") +
				SchemaManagerAdapter.ONTO_STREET + param +
				SchemaManagerAdapter.ONTO_HOUSE_NUMBER + param +
				SchemaManagerAdapter.ONTO_PO_BOX + param +
				SchemaManagerAdapter.ONTO_ZIP_CODE + param +
				SchemaManagerAdapter.ONTO_CITY + param +
				SchemaManagerAdapter.ONTO_COUNTRY + param +
				SchemaManagerAdapter.ONTO_MULTI_POINT + param +
				SchemaManagerAdapter.ONTO_XAL_SOURCE + param +
				".}" +
				"}";
		return stmt;
	}

	protected long doImport(Address address) throws CityGMLImportException, SQLException {
		if (!address.isSetXalAddress() || !address.getXalAddress().isSetAddressDetails()) {
			importer.logOrThrowErrorMessage(importer.getObjectSignature(address) + ": Skipping address due to missing xAL address details.");
			return 0;
		}

		FeatureType featureType = importer.getFeatureType(address);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		long addressId = importer.getNextSequenceValue(SequenceEnum.ADDRESS_ID_SEQ.getName());

		// gml:id
		if (address.isSetId())
			address.setLocalProperty(CoreConstants.OBJECT_ORIGINAL_GMLID, address.getId());

		if (replaceGmlId) {
			String gmlId = importer.generateNewGmlId();

			// mapping entry
			if (address.isSetId())
				importer.putObjectUID(address.getId(), addressId, gmlId, featureType.getObjectClassId());

			address.setId(gmlId);

		} else {
			if (address.isSetId())
				importer.putObjectUID(address.getId(), addressId, featureType.getObjectClassId());
			else
				address.setId(importer.generateNewGmlId());
		}

		int index = 1;

		if (importer.isBlazegraph()) {
			try {
				String uuid = address.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				preparedStatement.setURL(index++, url);
				preparedStatement.setURL(index++, url);
				address.setLocalProperty(CoreConstants.OBJECT_URIID, url);
			} catch (MalformedURLException e) {
				preparedStatement.setObject(index++, NodeFactory.createBlankNode());
			}
		} else {
		  preparedStatement.setLong(index++, addressId);
		}

		if (hasGmlIdColumn)
			preparedStatement.setString(index++, address.getId());

		// get address details
		addressWalker.reset();
		address.getXalAddress().getAddressDetails().accept(addressWalker);
		Object nullOrBlankNode;

		if (importer.isBlazegraph()) {
			if (addressWalker.street != null) {
				preparedStatement.setString(index++, addressWalker.street.toString());
			} else {
				setBlankNode(preparedStatement, index++);
			}
			if (addressWalker.houseNo  != null) {
				preparedStatement.setString(index++, addressWalker.houseNo.toString());
			} else {
				setBlankNode(preparedStatement, index++);
			}
			if (addressWalker.poBox != null) {
				preparedStatement.setString(index++, addressWalker.poBox.toString());
			} else {
				setBlankNode(preparedStatement, index++);
			}
			if (addressWalker.zipCode != null) {
				preparedStatement.setString(index++, addressWalker.zipCode.toString());
			} else {
				setBlankNode(preparedStatement, index++);
			}
			if (addressWalker.city != null) {
				preparedStatement.setString(index++, addressWalker.city.toString());
			} else {
				setBlankNode(preparedStatement, index++);
			}
			if (addressWalker.country != null) {
				preparedStatement.setString(index++, addressWalker.country.toString());
			} else {
				setBlankNode(preparedStatement, index++);
			}
		} else {
			preparedStatement.setString(index++, addressWalker.street != null ? addressWalker.street.toString() : null);
			preparedStatement.setString(index++, addressWalker.houseNo != null ? addressWalker.houseNo.toString() : null);
			preparedStatement.setString(index++, addressWalker.poBox != null ? addressWalker.poBox.toString() : null);
			preparedStatement.setString(index++, addressWalker.zipCode != null ? addressWalker.zipCode.toString() : null);
			preparedStatement.setString(index++, addressWalker.city != null ? addressWalker.city.toString() : null);
			preparedStatement.setString(index++, addressWalker.country != null ? addressWalker.country.toString() : null);
		}

		// multiPoint geometry
		index = importGeometryObjectProperty(address.getMultiPoint(), geometryConverter::getMultiPoint, index);

		// get XML representation of <xal:AddressDetails>
		String xalSource = null;
		if (importXALSource)
			xalSource = importer.marshalObject(address.getXalAddress().getAddressDetails(), XALModuleType.CORE);

		if (xalSource != null && !xalSource.isEmpty()) {
			preparedStatement.setString(index++, xalSource);
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, index++);
		}  else {
			preparedStatement.setNull(index++, Types.CLOB);
		}

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.ADDRESS);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(address, addressId, featureType);

		return addressId;
	}

	public void importBuildingAddress(Address address, long parentId) throws CityGMLImportException, SQLException {
		long addressId = doImport(address);
		if (addressId != 0)
			if (importer.isBlazegraph()) {
					addressToBuildingImporter.doImport((URL )address.getLocalProperty(CoreConstants.OBJECT_URIID),
							(URL) ((AbstractGML) ((AddressProperty) address.getParent()).getParent())
									.getLocalProperty(CoreConstants.OBJECT_URIID));
      } else {
        addressToBuildingImporter.doImport(addressId, parentId);
			}
	}

	public void importBridgeAddress(Address address, long parentId) throws CityGMLImportException, SQLException {
		long addressId = doImport(address);
		if (addressId != 0)
			addressToBridgeImporter.doImport(addressId, parentId);
	}

	private static final class XALAddressWalker extends XALWalker {
		private StringBuilder street;
		private StringBuilder houseNo;
		private StringBuilder poBox;
		private StringBuilder zipCode;
		private StringBuilder city;
		private StringBuilder country;

		@Override
		public void reset() {
			super.reset();
			street = houseNo = poBox = zipCode = city = country = null;
		}

		@Override
		public void visit(CountryName countryName) {
			if (country == null)
				country = new StringBuilder(countryName.getContent());
			else
				country.append(",").append(countryName.getContent());

			super.visit(countryName);
		}

		@Override
		public void visit(LocalityName localityName) {
			if (city == null)
				city = new StringBuilder(localityName.getContent());
			else
				city.append(",").append(localityName.getContent());

			super.visit(localityName);
		}

		@Override
		public void visit(PostalCodeNumber postalCodeNumber) {
			if (zipCode == null)
				zipCode = new StringBuilder(postalCodeNumber.getContent());
			else
				zipCode.append(",").append(postalCodeNumber.getContent());

			super.visit(postalCodeNumber);
		}

		@Override
		public void visit(ThoroughfareName thoroughfareName) {
			if (street == null)
				street = new StringBuilder(thoroughfareName.getContent());
			else
				street.append(",").append(thoroughfareName.getContent());

			super.visit(thoroughfareName);
		}

		@Override
		public void visit(ThoroughfareNumber thoroughfareNumber) {
			if (houseNo == null)
				houseNo = new StringBuilder(thoroughfareNumber.getContent());
			else
				houseNo.append(",").append(thoroughfareNumber.getContent());

			super.visit(thoroughfareNumber);
		}

		@Override
		public void visit(PostBoxNumber postBoxNumber) {
			if (poBox == null)
				poBox = new StringBuilder(postBoxNumber.getContent());
			else
				poBox.append(",").append(postBoxNumber.getContent());

			super.visit(postBoxNumber);
		}
	}

}
