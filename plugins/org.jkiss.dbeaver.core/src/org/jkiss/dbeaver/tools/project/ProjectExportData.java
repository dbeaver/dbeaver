/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.tools.project;

import org.eclipse.core.resources.IProject;
import org.jkiss.dbeaver.model.app.DBPProjectManager;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;


class ProjectExportData {

    private List<IProject> projects;
    private File outputFolder;
    private boolean exportDrivers;
    private String archiveFileName;

    DBPProjectManager projectRegistry;
    XMLBuilder meta;
    ZipOutputStream archiveStream;
    Set<DBPDriver> usedDrivers = new HashSet<>();

    public ProjectExportData(List<IProject> projects, File outputFolder, boolean exportDrivers, String archiveFileName)
    {
        this.projects = projects;
        this.outputFolder = outputFolder;
        this.exportDrivers = exportDrivers;
        this.archiveFileName = archiveFileName;
    }

    void initExport(DBPProjectManager projectRegistry, XMLBuilder meta, ZipOutputStream archiveStream)
    {
        this.projectRegistry = projectRegistry;
        this.meta = meta;
        this.archiveStream = archiveStream;
    }

    public List<IProject> getProjectsToExport()
    {
        return projects;
    }

    public File getOutputFolder()
    {
        return outputFolder;
    }

    public boolean isExportDrivers()
    {
        return exportDrivers;
    }

    public String getArchiveFileName()
    {
        return archiveFileName;
    }

}
