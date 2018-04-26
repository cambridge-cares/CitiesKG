package org.citydb.modules.database.gui.util;

import javax.swing.*;

public class ADEInfoRow {
    public static final ADEInfoRow DUMMY = new ADEInfoRow(null, "n/a", "n/a", false, false);
    private static final ImageIcon isSupported;
    private static final ImageIcon isNotSupported;

    static {
        isSupported = new ImageIcon(ADEInfoRow.class.getResource("/org/citydb/gui/images/common/done.png"));
        isNotSupported = new ImageIcon(ADEInfoRow.class.getResource("/org/citydb/gui/images/common/clear.png"));
    }

    private final String id;
    private final String name;
    private final String version;
    private boolean databaseSupport;
    private boolean impexpSupport;

    public ADEInfoRow(String id, String name, String version, boolean hasDBSupport, boolean hasImpexpSupport) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.databaseSupport = hasDBSupport;
        this.impexpSupport = hasImpexpSupport;
    }

    public Object getValueAt(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return name;
            case 1:
                return version;
            case 2:
                return databaseSupport ? isSupported : isNotSupported;
            case 3:
                return impexpSupport ? isSupported : isNotSupported;
            default:
                return null;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isDatabaseSupport() {
        return databaseSupport;
    }

    public void setDatabaseSupport(boolean databaseSupport) {
        this.databaseSupport = databaseSupport;
    }

    public boolean isImpexpSupport() {
        return impexpSupport;
    }

    public void setImpexpSupport(boolean impexpSupport) {
        this.impexpSupport = impexpSupport;
    }

    public static int getDefaultRowHeight() {
        return Math.max(UIManager.getFont("Table.font").getSize(), isSupported.getIconHeight()) + 2;
    }

}
