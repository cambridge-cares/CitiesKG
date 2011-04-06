package de.tub.citydb.components.kml;

import java.util.Locale;

import javax.xml.bind.JAXBContext;

import de.tub.citydb.components.kml.gui.preferences.KMLExportPreferences;
import de.tub.citydb.components.kml.gui.view.KMLExportView;
import de.tub.citydb.config.Config;
import de.tub.citydb.gui.ImpExpGui;
import de.tub.citydb.plugin.api.extensions.preferences.Preferences;
import de.tub.citydb.plugin.api.extensions.preferences.PreferencesExtension;
import de.tub.citydb.plugin.api.extensions.view.View;
import de.tub.citydb.plugin.api.extensions.view.ViewExtension;
import de.tub.citydb.plugin.internal.InternalPlugin;

public class KMLExportPlugin implements InternalPlugin, ViewExtension, PreferencesExtension {
	private KMLExportView view;
	private KMLExportPreferences preferences;
	
	public KMLExportPlugin(JAXBContext kmlContext, JAXBContext colladaContext, Config config, ImpExpGui mainView) {
		view = new KMLExportView(kmlContext, colladaContext, config, mainView);
		preferences = new KMLExportPreferences(mainView, config);
	}
		
	@Override
	public void init() {
		loadSettings();
	}

	@Override
	public void shutdown() {
		setSettings();
	}

	@Override
	public void switchLocale(Locale newLocale) {
		view.doTranslation();
		preferences.doTranslation();
	}

	@Override
	public Preferences getPreferences() {
		return preferences;
	}

	@Override
	public View getView() {
		return view;
	}
	
	@Override
	public void loadSettings() {
		view.loadSettings();
		preferences.loadSettings();
	}

	@Override
	public void setSettings() {
		view.setSettings();
		preferences.setSettings();
	}
	
}
