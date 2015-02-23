/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.tools.scripts;

import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.io.File;

/**
 * Import data
 */
class ScriptsImportData {

    private final File inputDir;
    private final String fileMasks;
    private final boolean overwriteFiles;
    private final DBNResource importDir;
    private final DBSDataSourceContainer dataSourceContainer;

    ScriptsImportData(File inputDir, String fileMasks, boolean overwriteFiles, DBNResource importDir, DBSDataSourceContainer dataSourceContainer)
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

    public DBSDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }
}
