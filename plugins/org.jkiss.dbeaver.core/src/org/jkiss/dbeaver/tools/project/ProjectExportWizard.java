/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProjectExportWizard extends Wizard implements IExportWizard {

    private static final Log log = Log.getLog(ProjectExportWizard.class);

    private static final int COPY_BUFFER_SIZE = 5000;
    private static final String PROJECT_DESC_FILE = IProjectDescription.DESCRIPTION_FILE_NAME;
    private static final Set<String> IGNORED_RESOURCES = new HashSet<>();
    private ProjectExportWizardPage mainPage;

    static {
        IGNORED_RESOURCES.add(PROJECT_DESC_FILE);
        //IGNORED_RESOURCES.add(DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_PREFIX);
    }

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
            UIUtils.run(getContainer(), true, true, monitor -> {
                try {
                    exportProjects(monitor, exportData);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            DBWorkbench.getPlatformUI().showError(
                    "Export error",
                "Cannot export projects",
                ex.getTargetException());
            return false;
        }
        return true;
	}

    private void exportProjects(DBRProgressMonitor monitor, final ProjectExportData exportData)
        throws IOException, CoreException, InterruptedException
    {
        if (!exportData.getOutputFolder().exists()) {
            if (!exportData.getOutputFolder().mkdirs()) {
                throw new IOException("Cannot create directory '" + exportData.getOutputFolder().getAbsolutePath() + "'"); //$NON-NLS-2$
            }
        }

        String archiveName = exportData.getArchiveFileName() + ExportConstants.ARCHIVE_FILE_EXT;
        File archiveFile = new File(exportData.getOutputFolder(), archiveName);
        
        if (archiveFile.exists()) {
            boolean overwrite = DBWorkbench.getPlatformUI().confirmAction(
                CoreMessages.dialog_project_export_wizard_file_overwrite_window_title,
                NLS.bind(CoreMessages.dialog_project_export_wizard_file_overwrite_confirm, archiveName),
                true
            );

            if (!overwrite) {
                return;
            }
        }
        
        FileOutputStream exportStream = new FileOutputStream(archiveFile);

        try {
            ByteArrayOutputStream metaBuffer = new ByteArrayOutputStream(10000);
            ZipOutputStream archiveStream = new ZipOutputStream(exportStream);

            // Start meta
            XMLBuilder meta = new XMLBuilder(metaBuffer, GeneralUtils.UTF8_ENCODING);
            meta.startElement(ExportConstants.TAG_ARCHIVE);
            meta.addAttribute(ExportConstants.ATTR_VERSION, ExportConstants.ARCHIVE_VERSION_CURRENT);

            exportData.initExport(DBPPlatformDesktop.getInstance().getWorkspace(), meta, archiveStream);

            {
                // Export source info
                InetAddress localHost = RuntimeUtils.getLocalHostOrLoopback();
                meta.startElement(ExportConstants.TAG_SOURCE);
                meta.addAttribute(ExportConstants.ATTR_TIME, System.currentTimeMillis());
                meta.addAttribute(ExportConstants.ATTR_ADDRESS, localHost.getHostAddress());
                meta.addAttribute(ExportConstants.ATTR_HOST, localHost.getHostName());
                meta.endElement();
            }

            Map<DBPProject, Integer> resCountMap = new HashMap<>();
            monitor.beginTask(CoreMessages.dialog_project_export_wizard_monitor_collect_info, exportData.getProjectsToExport().size());
            for (RCPProject project : exportData.getProjectsToExport()) {
                // Add used drivers to export data
                final DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();
                if (dataSourceRegistry != null) {
                    for (DBPDataSourceContainer dataSourceDescriptor : dataSourceRegistry.getDataSources()) {
                        exportData.usedDrivers.add(dataSourceDescriptor.getDriver());
                    }
                }

                resCountMap.put(project, getChildCount(exportData, project.getEclipseProject()));
                monitor.worked(1);
            }
            monitor.done();

            {
                // Export projects
                try (var ignored = exportData.meta.startElement(ExportConstants.TAG_PROJECTS)) {
                    for (RCPProject project : exportData.getProjectsToExport()) {
                        monitor.beginTask(NLS.bind(CoreMessages.dialog_project_export_wizard_monitor_export_project, project.getName()), resCountMap.get(project));
                        try {
                            exportProject(monitor, exportData, project.getEclipseProject());
                        } finally {
                            monitor.done();
                        }
                    }
                }
            }

            if (exportData.isExportDrivers()) {
                // Export driver libraries
                Set<Path> libFiles = new HashSet<>();
                Map<String, Path> libPathMap = new HashMap<>();
                for (DBPDriver driver : exportData.usedDrivers) {
                    for (DBPDriverLibrary fileDescriptor : driver.getDriverLibraries()) {
                        final Path libraryFile = fileDescriptor.getLocalFile();
                        if (libraryFile != null && !fileDescriptor.isDisabled() && Files.exists(libraryFile)) {
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
                        final Path libFile = libPathMap.get(libPath);
                        if (Files.isDirectory(libFile)) {
                            continue;
                        }
                        // Check for file name duplications
                        final String libFileName = libFile.getFileName().toString();
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
                        try (InputStream is = Files.newInputStream(libFile)) {
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
        if (DBPPlatformDesktop.getInstance().getWorkspace().getResourceHandler(resource) == null) {
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

    private void exportProject(DBRProgressMonitor monitor, ProjectExportData exportData, IProject project) throws CoreException, IOException
    {
        monitor.subTask(project.getName());
        // Refresh project
        project.refreshLocal(IResource.DEPTH_INFINITE, RuntimeUtils.getNestedMonitor(monitor));

        // Write meta info
        exportData.meta.startElement(ExportConstants.TAG_PROJECT);
        exportData.meta.addAttribute(ExportConstants.ATTR_NAME, project.getName());
        exportData.meta.addAttribute(ExportConstants.ATTR_DESCRIPTION, project.getDescription().getComment());

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
        if (IGNORED_RESOURCES.contains(resource.getName())) {
            // Skip it
            return;
        }
        monitor.subTask(parentPath + resource.getName());

        exportData.meta.startElement(ExportConstants.TAG_RESOURCE);
        exportData.meta.addAttribute(ExportConstants.ATTR_NAME, resource.getName());

        if (resource instanceof IContainer) {
            // Add folder entry
            parentPath = parentPath + resource.getName() + "/"; //$NON-NLS-1$
            exportData.archiveStream.putNextEntry(new ZipEntry(parentPath));
            exportData.archiveStream.closeEntry();

            // Export children
            final IResource[] members = ((IContainer) resource).members(IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
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

}
