/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import net.sf.jkiss.utils.IOUtils;
import net.sf.jkiss.utils.xml.XMLBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProjectExportWizard extends Wizard implements IExportWizard {

    static final Log log = LogFactory.getLog(ProjectExportWizard.class);

    public static final int COPY_BUFFER_SIZE = 5000;
    private ProjectExportWizardPage mainPage;

    public ProjectExportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Project Export Wizard"); //NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new ProjectExportWizardPage("Export project"); //NON-NLS-1
    }

    public void addPages() {
        super.addPages();
        addPage(mainPage);
    }

	@Override
	public boolean performFinish() {
        return exportProjects(
            mainPage.getProjectsToExport(),
            mainPage.getOutputFolder(),
            mainPage.isExportDrivers());
	}

    public boolean exportProjects(final List<IProject> projects, final File outputFolder, final boolean exportDrivers)
    {
        DBRRunnableWithProgress op = new DBRRunnableWithProgress()
        {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                if (!outputFolder.exists()) {
                    if (!outputFolder.mkdirs()) {
                        throw new InvocationTargetException(new IOException("Cannot create directory '" + outputFolder.getAbsolutePath() + "'"));
                    }
                }

                try {
                    String archiveName = getArchiveFileName(projects);
                    File archiveFile = new File(outputFolder, archiveName);
                    FileOutputStream exportStream = new FileOutputStream(archiveFile);

                    try {
                        ByteArrayOutputStream metaBuffer = new ByteArrayOutputStream(10000);
                        ZipOutputStream archiveStream = new ZipOutputStream(exportStream);
                        ExportData exportData;
                        {
                            // Start meta
                            XMLBuilder meta = new XMLBuilder(metaBuffer, ContentUtils.DEFAULT_FILE_CHARSET);
                            meta.startElement(ExportConstants.TAG_ARCHIVE);
                            meta.addAttribute(ExportConstants.ATTR_VERSION, ExportConstants.ARCHIVE_VERSION_CURRENT);

                            exportData = new ExportData(DBeaverCore.getInstance().getProjectRegistry(), meta, archiveStream);
                        }

                        Map<IProject, Integer> resCountMap = new HashMap<IProject, Integer>();
                        monitor.beginTask("Collect projects info", projects.size());
                        for (IProject project : projects) {
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
                            monitor.beginTask("Export drivers information", 1);
                            exportData.meta.startElement(ExportConstants.TAG_DRIVERS);
                            for (DriverDescriptor driver : exportData.usedDrivers) {
                                driver.serialize(exportData.meta, true);
                            }
                            exportData.meta.endElement();
                            monitor.done();
                        }

                        {
                            // Export projects
                            exportData.meta.startElement(ExportConstants.TAG_PROJECTS);
                            for (IProject project : projects) {
                                monitor.beginTask("Export project '" + project.getName() + "'", resCountMap.get(project));
                                try {
                                    exportProject(monitor, exportData, project);
                                } finally {
                                    monitor.done();
                                }
                            }
                            exportData.meta.endElement();
                        }

                        if (exportDrivers) {
                            // Export driver libraries
                            Set<File> libFiles = new HashSet<File>();
                            Map<String, File> libPathMap = new HashMap<String, File>();
                            for (DriverDescriptor driver : exportData.usedDrivers) {
                                for (DriverLibraryDescriptor libraryDescriptor : driver.getLibraries()) {
                                    final File libraryFile = libraryDescriptor.getLibraryFile();
                                    if (!libraryDescriptor.isDisabled() && libraryFile.exists()) {
                                        libFiles.add(libraryFile);
                                        libPathMap.put(libraryDescriptor.getPath(), libraryFile);
                                    }
                                }
                            }

                            if (!libFiles.isEmpty()) {
                                monitor.beginTask("Export driver libraries", libFiles.size());

                                final ZipEntry driversFolder = new ZipEntry(ExportConstants.DIR_DRIVERS + "/");
                                driversFolder.setComment("Database driver libraries");
                                exportData.archiveStream.putNextEntry(driversFolder);
                                exportData.archiveStream.closeEntry();

                                exportData.meta.startElement(ExportConstants.TAG_LIBRARIES);
                                Set<String> libFileNames = new HashSet<String>();
                                for (String libPath : libPathMap.keySet()) {
                                    final File libFile = libPathMap.get(libPath);
                                    // Check for file name duplications
                                    final String libFileName = libFile.getName();
                                    if (libFileNames.contains(libFileName)) {
                                        log.warn("Duplicate driver library file name: " + libFileName);
                                        continue;
                                    }
                                    libFileNames.add(libFileName);

                                    monitor.subTask(libFileName);

                                    exportData.meta.startElement(ExportConstants.TAG_LIBRARY);
                                    exportData.meta.addAttribute(ExportConstants.ATTR_PATH, libPath);
                                    exportData.meta.addAttribute(ExportConstants.ATTR_FILE, "drivers/" + libFileName);
                                    exportData.meta.endElement();

                                    final ZipEntry driverFile = new ZipEntry(ExportConstants.DIR_DRIVERS + "/" + libFileName);
                                    driverFile.setComment("Driver library");
                                    exportData.archiveStream.putNextEntry(driverFile);
                                    InputStream is = new FileInputStream(libFile);
                                    try {
                                        IOUtils.copyStream(is, exportData.archiveStream, COPY_BUFFER_SIZE);
                                    } finally {
                                        ContentUtils.close(is);
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
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            }
        };

        try {
            RuntimeUtils.run(getContainer(), true, true, op);
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

    String getArchiveFileName(List<IProject> projects)
    {
        String archiveName = "projects";
        if (projects.size() == 1) {
            archiveName = projects.get(0).getName();
        }
        archiveName += "-" + RuntimeUtils.getCurrentTimeStamp();
        archiveName += ExportConstants.ARCHIVE_FILE_EXT;
        return archiveName;
    }

    private int getChildCount(ExportData exportData, IResource resource) throws CoreException
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

    private void exportProject(DBRProgressMonitor monitor, ExportData exportData, IProject project) throws InterruptedException, CoreException, IOException
    {
        monitor.subTask(project.getName());

        // Write meta info
        exportData.meta.startElement(ExportConstants.TAG_PROJECT);
        exportData.meta.addAttribute(ExportConstants.ATTR_NAME, project.getName());
        exportData.meta.addAttribute(ExportConstants.ATTR_DESCRIPTION, project.getDescription().getComment());
        saveResourceProperties(project, exportData.meta);

        // Add project folder
        final String projectPath = ExportConstants.DIR_PROJECTS + "/" + project.getName() + "/";
        exportData.archiveStream.putNextEntry(new ZipEntry(projectPath));
        exportData.archiveStream.closeEntry();

        // Export resources
        for (IResource child : project.members()) {
            exportResourceTree(monitor, exportData, projectPath, child);
        }

        exportData.meta.endElement();

        monitor.worked(1);
    }

    private void exportResourceTree(DBRProgressMonitor monitor, ExportData exportData, String parentPath, IResource resource) throws CoreException, IOException
    {
        DBPResourceHandler resourceHandler = exportData.projectRegistry.getResourceHandler(resource);
        if (resourceHandler == null) {
            // Do not export garbage
            return;
        }
        monitor.subTask(parentPath + resource.getName());

        exportData.meta.startElement(ExportConstants.TAG_RESOURCE);
        exportData.meta.addAttribute(ExportConstants.ATTR_NAME, resource.getName());
        saveResourceProperties(resource, exportData.meta);

        if (resource instanceof IContainer) {
            // Add folder entry
            parentPath = parentPath + resource.getName() + "/";
            exportData.archiveStream.putNextEntry(new ZipEntry(parentPath));
            exportData.archiveStream.closeEntry();

            // Export children
            final IResource[] members = ((IContainer) resource).members();
            for (IResource child : members) {
                exportResourceTree(monitor, exportData, parentPath, child);
            }
        } else if (resource instanceof IFile) {
            // Add file to archive
            IFile file = (IFile)resource;
            exportData.archiveStream.putNextEntry(new ZipEntry(parentPath + resource.getName()));
            IOUtils.copyStream(file.getContents(), exportData.archiveStream, COPY_BUFFER_SIZE);
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
            if (contentDescription != null) {
                xml.addAttribute(ExportConstants.ATTR_CHARSET, contentDescription.getCharset());
                //xml.addAttribute(ExportConstants.ATTR_CHARSET, contentDescription.getContentType());
            }
        }
        for (Object entry : resource.getPersistentProperties().entrySet()) {
            Map.Entry propEntry = (Map.Entry) entry;
            xml.startElement(ExportConstants.TAG_ATTRIBUTE);
            final QualifiedName attrName = (QualifiedName) propEntry.getKey();
            xml.addAttribute(ExportConstants.ATTR_QUALIFIER, attrName.getQualifier());
            xml.addAttribute(ExportConstants.ATTR_NAME, attrName.getLocalName());
            xml.addAttribute(ExportConstants.ATTR_VALUE, (String) propEntry.getValue());
            xml.endElement();
        }
    }

    private static class ExportData {
        ProjectRegistry projectRegistry;
        XMLBuilder meta;
        ZipOutputStream archiveStream;
        Set<DriverDescriptor> usedDrivers = new HashSet<DriverDescriptor>();

        private ExportData(ProjectRegistry projectRegistry, XMLBuilder meta, ZipOutputStream archiveStream)
        {
            this.projectRegistry = projectRegistry;
            this.meta = meta;
            this.archiveStream = archiveStream;
        }
    }

}
