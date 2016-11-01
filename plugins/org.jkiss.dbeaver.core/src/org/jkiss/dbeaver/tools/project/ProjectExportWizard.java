/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.dbeaver.Log;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProjectExportWizard extends Wizard implements IExportWizard {

    private static final Log log = Log.getLog(ProjectExportWizard.class);

    public static final int COPY_BUFFER_SIZE = 5000;
    public static final String PROJECT_DESC_FILE = ".project";
    private ProjectExportWizardPage mainPage;

    public ProjectExportWizard() {
	}

	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(CoreMessages.dialog_project_export_wizard_window_title);
        setNeedsProgressMonitor(true);
        mainPage = new ProjectExportWizardPage(CoreMessages.dialog_project_export_wizard_main_page);
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(mainPage);
    }

	@Override
	public boolean performFinish() {
        final ProjectExportData exportData = mainPage.getExportData();
        try {
            DBeaverUI.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        exportProjects(monitor, exportData);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                "Export error",
                "Cannot export projects",
                ex.getTargetException());
            return false;
        }
        return true;
	}

    public void exportProjects(DBRProgressMonitor monitor, final ProjectExportData exportData)
        throws IOException, CoreException, InterruptedException
    {
        if (!exportData.getOutputFolder().exists()) {
            if (!exportData.getOutputFolder().mkdirs()) {
                throw new IOException("Cannot create directory '" + exportData.getOutputFolder().getAbsolutePath() + "'"); //$NON-NLS-2$
            }
        }

        String archiveName = exportData.getArchiveFileName() + ExportConstants.ARCHIVE_FILE_EXT;
        File archiveFile = new File(exportData.getOutputFolder(), archiveName);
        FileOutputStream exportStream = new FileOutputStream(archiveFile);

        try {
            ByteArrayOutputStream metaBuffer = new ByteArrayOutputStream(10000);
            ZipOutputStream archiveStream = new ZipOutputStream(exportStream);

            // Start meta
            XMLBuilder meta = new XMLBuilder(metaBuffer, GeneralUtils.UTF8_ENCODING);
            meta.startElement(ExportConstants.TAG_ARCHIVE);
            meta.addAttribute(ExportConstants.ATTR_VERSION, ExportConstants.ARCHIVE_VERSION_CURRENT);

            exportData.initExport(DBeaverCore.getInstance().getProjectRegistry(), meta, archiveStream);

            {
                // Export source info
                meta.startElement(ExportConstants.TAG_SOURCE);
                meta.addAttribute(ExportConstants.ATTR_TIME, Long.valueOf(System.currentTimeMillis()));
                meta.addAttribute(ExportConstants.ATTR_ADDRESS, InetAddress.getLocalHost().getHostAddress());
                meta.addAttribute(ExportConstants.ATTR_HOST, InetAddress.getLocalHost().getHostName());
                meta.endElement();
            }

            Map<IProject, Integer> resCountMap = new HashMap<>();
            monitor.beginTask(CoreMessages.dialog_project_export_wizard_monitor_collect_info, exportData.getProjectsToExport().size());
            for (IProject project : exportData.getProjectsToExport()) {
                // Add used drivers to export data
                final DataSourceRegistry dataSourceRegistry = exportData.projectRegistry.getDataSourceRegistry(project);
                if (dataSourceRegistry != null) {
                    for (DataSourceDescriptor dataSourceDescriptor : dataSourceRegistry.getDataSources()) {
                        exportData.usedDrivers.add(dataSourceDescriptor.getDriver());
                    }
                }

                resCountMap.put(project, getChildCount(exportData, project));
                monitor.worked(1);
            }
            monitor.done();

            {
                // Export drivers meta
                monitor.beginTask(CoreMessages.dialog_project_export_wizard_monitor_export_driver_info, 1);
                exportData.meta.startElement(RegistryConstants.TAG_DRIVERS);
                for (DriverDescriptor driver : exportData.usedDrivers) {
                    driver.serialize(exportData.meta, true);
                }
                exportData.meta.endElement();
                monitor.done();
            }

            {
                // Export projects
                exportData.meta.startElement(ExportConstants.TAG_PROJECTS);
                for (IProject project : exportData.getProjectsToExport()) {
                    monitor.beginTask(NLS.bind(CoreMessages.dialog_project_export_wizard_monitor_export_project, project.getName()), resCountMap.get(project));
                    try {
                        exportProject(monitor, exportData, project);
                    } finally {
                        monitor.done();
                    }
                }
                exportData.meta.endElement();
            }

            if (exportData.isExportDrivers()) {
                // Export driver libraries
                Set<File> libFiles = new HashSet<>();
                Map<String, File> libPathMap = new HashMap<>();
                for (DriverDescriptor driver : exportData.usedDrivers) {
                    for (DBPDriverLibrary fileDescriptor : driver.getDriverLibraries()) {
                        final File libraryFile = fileDescriptor.getLocalFile();
                        if (libraryFile != null && !fileDescriptor.isDisabled() && libraryFile.exists()) {
                            libFiles.add(libraryFile);
                            libPathMap.put(fileDescriptor.getPath(), libraryFile);
                        }
                    }
                }

                if (!libFiles.isEmpty()) {
                    monitor.beginTask(CoreMessages.dialog_project_export_wizard_monitor_export_libraries, libFiles.size());

                    final ZipEntry driversFolder = new ZipEntry(ExportConstants.DIR_DRIVERS + "/"); //$NON-NLS-1$
                    driversFolder.setComment("Database driver libraries"); //$NON-NLS-1$
                    exportData.archiveStream.putNextEntry(driversFolder);
                    exportData.archiveStream.closeEntry();

                    exportData.meta.startElement(ExportConstants.TAG_LIBRARIES);
                    Set<String> libFileNames = new HashSet<>();
                    for (String libPath : libPathMap.keySet()) {
                        final File libFile = libPathMap.get(libPath);
                        // Check for file name duplications
                        final String libFileName = libFile.getName();
                        if (libFileNames.contains(libFileName)) {
                            log.warn("Duplicate driver library file name: " + libFileName); //$NON-NLS-1$
                            continue;
                        }
                        libFileNames.add(libFileName);

                        monitor.subTask(libFileName);

                        exportData.meta.startElement(RegistryConstants.TAG_FILE);
                        exportData.meta.addAttribute(ExportConstants.ATTR_PATH, libPath);
                        exportData.meta.addAttribute(ExportConstants.ATTR_FILE, "drivers/" + libFileName); //$NON-NLS-1$
                        exportData.meta.endElement();

                        final ZipEntry driverFile = new ZipEntry(ExportConstants.DIR_DRIVERS + "/" + libFileName); //$NON-NLS-1$
                        driverFile.setComment("Driver library"); //$NON-NLS-1$
                        exportData.archiveStream.putNextEntry(driverFile);
                        try (InputStream is = new FileInputStream(libFile)) {
                            IOUtils.copyStream(is, exportData.archiveStream, COPY_BUFFER_SIZE);
                        }

                        exportData.archiveStream.closeEntry();

                        monitor.worked(1);
                    }
                    exportData.meta.endElement();

                    monitor.done();
                }
            }

            // Add meta to archive
            {
                exportData.meta.endElement();
                exportData.meta.flush();
                archiveStream.putNextEntry(new ZipEntry(ExportConstants.META_FILENAME));
                archiveStream.write(metaBuffer.toByteArray());
                archiveStream.closeEntry();
            }

            // Finish archive creation
            archiveStream.finish();
        } finally {
            ContentUtils.close(exportStream);
        }
}

    private int getChildCount(ProjectExportData exportData, IResource resource) throws CoreException
    {
        if (exportData.projectRegistry.getResourceHandler(resource) == null) {
            return 0;
        }
        int childCount = 1;
        if (resource instanceof IContainer) {
            for (IResource child : ((IContainer) resource).members()) {
                childCount += getChildCount(exportData, child);
            }
        }
        return childCount;
    }

    private void exportProject(DBRProgressMonitor monitor, ProjectExportData exportData, IProject project) throws InterruptedException, CoreException, IOException
    {
        monitor.subTask(project.getName());
        // Refresh project
        project.refreshLocal(IResource.DEPTH_INFINITE, RuntimeUtils.getNestedMonitor(monitor));

        // Write meta info
        exportData.meta.startElement(ExportConstants.TAG_PROJECT);
        exportData.meta.addAttribute(ExportConstants.ATTR_NAME, project.getName());
        exportData.meta.addAttribute(ExportConstants.ATTR_DESCRIPTION, project.getDescription().getComment());
        saveResourceProperties(project, exportData.meta);

        // Add project folder
        final String projectPath = ExportConstants.DIR_PROJECTS + "/" + project.getName() + "/"; //$NON-NLS-1$ //$NON-NLS-2$
        exportData.archiveStream.putNextEntry(new ZipEntry(projectPath));
        exportData.archiveStream.closeEntry();

        // Export resources
        for (IResource child : project.members(IContainer.INCLUDE_HIDDEN)) {
            exportResourceTree(monitor, exportData, projectPath, child);
        }

        exportData.meta.endElement();

        monitor.worked(1);
    }

    private void exportResourceTree(DBRProgressMonitor monitor, ProjectExportData exportData, String parentPath, IResource resource) throws CoreException, IOException
    {
        if (resource.getName().equals(PROJECT_DESC_FILE)) {
            // Skip it
            return;
        }
        monitor.subTask(parentPath + resource.getName());

        exportData.meta.startElement(ExportConstants.TAG_RESOURCE);
        exportData.meta.addAttribute(ExportConstants.ATTR_NAME, resource.getName());
        saveResourceProperties(resource, exportData.meta);

        if (resource instanceof IContainer) {
            // Add folder entry
            parentPath = parentPath + resource.getName() + "/"; //$NON-NLS-1$
            exportData.archiveStream.putNextEntry(new ZipEntry(parentPath));
            exportData.archiveStream.closeEntry();

            // Export children
            final IResource[] members = ((IContainer) resource).members();
            for (IResource child : members) {
                if (child.isLinked()) {
                    continue;
                }
                exportResourceTree(monitor, exportData, parentPath, child);
            }
        } else if (resource instanceof IFile) {
            // Add file to archive
            IFile file = (IFile)resource;
            exportData.archiveStream.putNextEntry(new ZipEntry(parentPath + resource.getName()));
            try (InputStream is = file.getContents()) {
                IOUtils.copyStream(is, exportData.archiveStream, COPY_BUFFER_SIZE);
            }
            exportData.archiveStream.closeEntry();
        } else {
            // Just skip it
        }

        exportData.meta.endElement();

        monitor.worked(1);
    }

    private void saveResourceProperties(IResource resource, XMLBuilder xml) throws CoreException, IOException
    {
        if (resource instanceof IFile) {
            final IContentDescription contentDescription = ((IFile) resource).getContentDescription();
            if (contentDescription != null && contentDescription.getCharset() != null) {
                xml.addAttribute(ExportConstants.ATTR_CHARSET, contentDescription.getCharset());
                //xml.addAttribute(ExportConstants.ATTR_CHARSET, contentDescription.getContentType());
            }
        } else if (resource instanceof IFolder) {
            xml.addAttribute(ExportConstants.ATTR_DIRECTORY, true);
        }
        for (Object entry : resource.getPersistentProperties().entrySet()) {
            Map.Entry<?, ?> propEntry = (Map.Entry<?,?>) entry;
            xml.startElement(ExportConstants.TAG_ATTRIBUTE);
            final QualifiedName attrName = (QualifiedName) propEntry.getKey();
            xml.addAttribute(ExportConstants.ATTR_QUALIFIER, attrName.getQualifier());
            xml.addAttribute(ExportConstants.ATTR_NAME, attrName.getLocalName());
            xml.addAttribute(ExportConstants.ATTR_VALUE, (String) propEntry.getValue());
            xml.endElement();
        }
    }

}
