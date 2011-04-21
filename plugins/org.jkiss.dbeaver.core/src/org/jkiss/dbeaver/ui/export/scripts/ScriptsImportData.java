/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.scripts;

import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.io.File;

/**
 * Import data
 */
class ScriptsImportData {

    private final File inputDir;
    private final String fileMasks;
    private final DBNResource importDir;
    private final DBSDataSourceContainer dataSourceContainer;

    ScriptsImportData(File inputDir, String fileMasks, DBNResource importDir, DBSDataSourceContainer dataSourceContainer)
    {
        this.inputDir = inputDir;
        this.fileMasks = fileMasks;
        this.importDir = importDir;
        this.dataSourceContainer = dataSourceContainer;
    }

    public File getInputDir()
    {
        return inputDir;
    }

    public String getFileMasks()
    {
        return fileMasks;
    }

    public DBNResource getImportDir()
    {
        return importDir;
    }

    public DBSDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }
}
