/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import org.jkiss.utils.xml.XMLBuilder;
import org.eclipse.core.resources.IProject;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.ProjectRegistry;

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
