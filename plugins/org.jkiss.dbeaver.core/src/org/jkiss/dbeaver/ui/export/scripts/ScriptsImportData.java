/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.scripts;

import org.jkiss.dbeaver.model.navigator.DBNResource;

import java.io.File;

/**
 * Import data
 */
class ScriptsImportData {

    private final File inputDir;
    private final String fileExtensions;
    private final DBNResource importDir;

    ScriptsImportData(File inputDir, String fileExtensions, DBNResource importDir)
    {
        this.inputDir = inputDir;
        this.fileExtensions = fileExtensions;
        this.importDir = importDir;
    }

    public File getInputDir()
    {
        return inputDir;
    }

    public String getFileExtensions()
    {
        return fileExtensions;
    }

    public DBNResource getImportDir()
    {
        return importDir;
    }
}
