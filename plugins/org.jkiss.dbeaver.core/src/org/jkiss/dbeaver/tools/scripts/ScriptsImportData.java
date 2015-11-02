/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.tools.scripts;

import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.io.File;

/**
 * Import data
 */
class ScriptsImportData {

    private final File inputDir;
    private final String fileMasks;
    private final boolean overwriteFiles;
    private final DBNResource importDir;
    private final DBPDataSourceContainer dataSourceContainer;

    ScriptsImportData(File inputDir, String fileMasks, boolean overwriteFiles, DBNResource importDir, DBPDataSourceContainer dataSourceContainer)
    {
        this.inputDir = inputDir;
        this.fileMasks = fileMasks;
        this.overwriteFiles = overwriteFiles;
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

    public boolean isOverwriteFiles()
    {
        return overwriteFiles;
    }

    public DBNResource getImportDir()
    {
        return importDir;
    }

    public DBPDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }
}
