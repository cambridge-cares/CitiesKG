package org.citydb.modules.database.gui.util;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ADETableModel extends AbstractTableModel {
    private final String[] columnNames = new String[]{"", "", "", ""};
    private final List<ADEInfoRow> data = new ArrayList<>();

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return ADEInfoRow.NO_ADES_ENTRY.getValueAt(columnIndex).getClass();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    public ADEInfoRow getRow(int rowIndex) {
        return rowIndex >= 0 && rowIndex < data.size() ? data.get(rowIndex) : ADEInfoRow.NO_ADES_ENTRY;
    }

    public void addRow(ADEInfoRow data) {
        int row = getRowCount();
        this.data.add(row, data);
        fireTableRowsInserted(row, row);
    }

    public boolean hasRows() {
        return !data.isEmpty();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex).getValueAt(columnIndex);
    }

    public void reset() {
        int rowCount = getRowCount();
        data.clear();
        if (rowCount != 0)
            fireTableRowsDeleted(0, rowCount - 1);
    }
}
