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
