package org.citydb;

import org.citydb.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImpExpConstants {
    public static final Path IMPEXP_HOME;
    public static final Path WORKING_DIR;
    public static final Path IMPEXP_DATA_DIR;
    public static final String SRS_TEMPLATES_DIR = "templates" + File.separator + "CoordinateReferenceSystems";
    public static final String PLUGINS_DIR = "plugins";
    public static final String ADE_EXTENSIONS_DIR = "ade-extensions";
    public static final String CONFIG_DIR = "config";
    public static final String PROJECT_FILE = "project.xml";
    public static final String GUI_FILE = "gui.xml";

    static {
        String impexpHomeEnv = System.getenv("APP_HOME");
        if (impexpHomeEnv == null) {
            Logger.getInstance().warn("Environment variable APP_HOME not set. " +
                    "Using current working directory instead.");
            impexpHomeEnv = ".";
        }

        String workingDirEnv = System.getenv("WORKING_DIR");
        if (workingDirEnv == null) {
            Logger.getInstance().warn("Environment variable WORKING_DIR not set. " +
                    "Using current working directory instead.");
            workingDirEnv = ".";
        }

        IMPEXP_HOME = Paths.get(impexpHomeEnv).normalize().toAbsolutePath();
        WORKING_DIR = Paths.get(workingDirEnv).normalize().toAbsolutePath();
        IMPEXP_DATA_DIR = Paths.get(System.getProperty("user.home"), "3dcitydb", "importer-exporter");
    }
}