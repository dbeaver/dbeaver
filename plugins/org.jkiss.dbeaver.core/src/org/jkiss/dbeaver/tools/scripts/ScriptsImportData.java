/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.tools.scripts;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNResource;

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
