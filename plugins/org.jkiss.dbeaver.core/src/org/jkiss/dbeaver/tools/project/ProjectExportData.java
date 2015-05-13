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

package org.jkiss.dbeaver.tools.project;

import org.eclipse.core.resources.IProject;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.ProjectRegistry;
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

    ProjectRegistry projectRegistry;
    XMLBuilder meta;
    ZipOutputStream archiveStream;
    Set<DriverDescriptor> usedDrivers = new HashSet<DriverDescriptor>();

    public ProjectExportData(List<IProject> projects, File outputFolder, boolean exportDrivers, String archiveFileName)
    {
        this.projects = projects;
        this.outputFolder = outputFolder;
        this.exportDrivers = exportDrivers;
        this.archiveFileName = archiveFileName;
    }

    void initExport(ProjectRegistry projectRegistry, XMLBuilder meta, ZipOutputStream archiveStream)
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
