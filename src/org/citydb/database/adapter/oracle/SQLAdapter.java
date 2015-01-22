package org.citydb.database.adapter.oracle;

import java.sql.Connection;
import java.sql.SQLException;

import org.citydb.api.geometry.BoundingBox;
import org.citydb.database.adapter.AbstractSQLAdapter;
import org.citydb.database.adapter.BlobExportAdapter;
import org.citydb.database.adapter.BlobExportAdapterImpl;
import org.citydb.database.adapter.BlobImportAdapter;
import org.citydb.database.adapter.BlobImportAdapterImpl;
import org.citydb.database.adapter.BlobType;
import org.citydb.modules.citygml.importer.database.content.DBSequencerEnum;

public class SQLAdapter extends AbstractSQLAdapter {
	
	@Override
	public String getInteger() {
		return "NUMBER";
	}

	@Override
	public String getSmallInt() {
		return "NUMBER";
	}

	@Override
	public String getBigInt() {
		return "NUMBER";
	}

	@Override
	public String getNumeric() {
		return "NUMBER";
	}

	@Override
	public String getNumeric(int precision) {
		return "NUMBER(" + precision + ")";
	}

	@Override
	public String getNumeric(int precision, int scale) {
		return "NUMBER(" + precision + "," + scale + ")";
	}

	@Override
	public String getReal() {
		return "BINARY_FLOAT";
	}

	@Override
	public String getDoublePrecision() {
		return "BINARY_DOUBLE";
	}

	@Override
	public String getCharacter(int nrOfChars) {
		return "CHAR(" + nrOfChars + ")";
	}

	@Override
	public String getCharacterVarying(int nrOfChars) {
		return "VARCHAR2(" + nrOfChars + ")";
	}
	
	@Override
	public String getPolygon2D() {
		return "MDSYS.SDO_GEOMETRY";
	}

	@Override
	public String getCreateUnloggedTable(String tableName, String columns) {
		StringBuilder builder = new StringBuilder()
		.append("create table ")
		.append(tableName).append(" ")
		.append(columns).append(" ")
		.append("nologging");

		return builder.toString();
	}

	@Override
	public String getCreateUnloggedTableAsSelectFrom( String targetTableName, String sourceTableName) {
		StringBuilder builder = new StringBuilder()
		.append("create table ")
		.append(targetTableName).append(" ")
		.append("nologging ")
		.append("as select * from ")
		.append(sourceTableName);

		return builder.toString();
	}

	@Override
	public String getNextSequenceValue(DBSequencerEnum sequence) {
		return new StringBuilder(getSequenceName(sequence)).append(".nextval").toString();
	}
	
	@Override
	public String getCurrentSequenceValue(DBSequencerEnum sequence) {
		return new StringBuilder(getSequenceName(sequence)).append(".currval").toString();
	}
	
	@Override
	public String getNextSequenceValuesQuery(DBSequencerEnum sequence) {
		return new StringBuilder("select * from table(")
		.append(resolveDatabaseOperationName("citydb_util.get_seq_values")).append("(")
		.append("'").append(getSequenceName(sequence)).append("'").append(",")
		.append("?").append("))").toString();
	}

	@Override
	public String getUnloggedIndexProperty() {
		return "nologging";
	}
	
	@Override
	public boolean requiresPseudoTableInSelect() {
		return true;
	}

	@Override
	public String getPseudoTableName() {
		return "dual";
	}
	
	@Override
	public int getMaximumNumberOfItemsForInOperator() {
		return 1000;
	}

	@Override
	public String getBoundingBoxPredicate(String attributeName, BoundingBox bbox, boolean overlap) {
		StringBuilder geometry = new StringBuilder()
		.append("MDSYS.SDO_GEOMETRY(2003, ").append(bbox.getSrs().getSrid()).append(", NULL, ")
		.append("MDSYS.SDO_ELEM_INFO_ARRAY(1, 1003, 3), ")
		.append("MDSYS.SDO_ORDINATE_ARRAY(")
		.append(bbox.getLowerLeftCorner().getX()).append(", ").append(bbox.getLowerLeftCorner().getY()).append(", ")
		.append(bbox.getUpperRightCorner().getX()).append(", ").append(bbox.getUpperRightCorner().getY()).append("))");

		StringBuilder predicate = new StringBuilder()
		.append("(SDO_INSIDE(").append(attributeName).append(", ").append(geometry).append(") = 'TRUE' ")
		.append("or SDO_COVEREDBY(").append(attributeName).append(", ").append(geometry).append(") = 'TRUE' ")
		.append("or SDO_EQUAL(").append(attributeName).append(", ").append(geometry).append(") = 'TRUE' ");

		if (!overlap)
			predicate.append(')');
		else {
			predicate.append("or SDO_OVERLAPBDYINTERSECT(").append(attributeName).append(", ").append(geometry).append(") = 'TRUE' ");
			predicate.append("or SDO_OVERLAPBDYDISJOINT(").append(attributeName).append(", ").append(geometry).append(") = 'TRUE' ");
			predicate.append("or SDO_CONTAINS(").append(attributeName).append(", ").append(geometry).append(") = 'TRUE')");
		}

		return predicate.toString();
	}

	@Override
	public boolean spatialPredicateRequiresNoIndexHint() {
		return true;
	}

	@Override
	public String getHierarchicalGeometryQuery() {
		return "select sg.*, LEVEL from SURFACE_GEOMETRY sg start with sg.ID=? connect by prior sg.ID=sg.PARENT_ID";
	}

	@Override
	public BlobImportAdapter getBlobImportAdapter(Connection connection, BlobType type) throws SQLException {
		return new BlobImportAdapterImpl(connection, type);
	}

	@Override
	public BlobExportAdapter getBlobExportAdapter(Connection connection, BlobType type) {
		return new BlobExportAdapterImpl(connection, type);
	}

}
