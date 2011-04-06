package de.tub.citydb.components.database.gui.view;

import java.awt.Component;

import javax.swing.Icon;

import de.tub.citydb.components.database.gui.view.components.DatabasePanel;
import de.tub.citydb.config.Config;
import de.tub.citydb.config.internal.Internal;
import de.tub.citydb.gui.ImpExpGui;
import de.tub.citydb.plugin.api.extensions.view.View;

public class DatabaseView implements View {
	private final DatabasePanel component;
	
	public DatabaseView(Config config, ImpExpGui mainView) {
		component = new DatabasePanel(config, mainView);
	}
	
	@Override
	public String getTitle() {
		return Internal.I18N.getString("main.tabbedPane.database");
	}

	@Override
	public Component getViewComponent() {
		return component;
	}

	@Override
	public String getToolTip() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return null;
	}
	
	public void loadSettings() {
		component.loadSettings();
	}
	
	public void setSettings() {
		component.setSettings();
	}
	
	public void doTranslation() {
		component.doTranslation();
	}
	
	public void connect() {
		component.connect();
	}
	
	public void disconnect() {
		component.disconnect();
	}

}
