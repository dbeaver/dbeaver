/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.IOUtils;
import net.sf.jkiss.utils.xml.XMLException;
import net.sf.jkiss.utils.xml.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceConstants;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ProjectImportWizard extends Wizard implements IImportWizard {

    static final Log log = LogFactory.getLog(ProjectImportWizard.class);

    private ProjectImportData data = new ProjectImportData();

    public ProjectImportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Project Import Wizard");
        setNeedsProgressMonitor(true);
    }

    public void addPages() {
        super.addPages();
        addPage(new ProjectImportWizardPageFile(data));
        //addPage(new ProjectImportWizardPageFinal(data));
    }

	@Override
	public boolean performFinish() {
        try {
            RuntimeUtils.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        importProjects(monitor);
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
                "Import error",
                "Cannot export projects",
                ex.getTargetException());
            return false;
        }
        UIUtils.showMessageBox(getShell(), "Project import", "Project(s) successfully imported", SWT.ICON_INFORMATION);
        return true;
	}

    private void importProjects(DBRProgressMonitor monitor) throws IOException, DBException
    {
        ZipFile zipFile = new ZipFile(data.getImportFile(), ZipFile.OPEN_READ);
        try {
            ZipEntry metaEntry = zipFile.getEntry(ExportConstants.META_FILENAME);
            if (metaEntry == null) {
                throw new DBException("Cannot find meta file");
            }
            final Map<String, String> libMap = new HashMap<String, String>();
            final Map<String, String> driverMap = new HashMap<String, String>();
            InputStream metaStream = zipFile.getInputStream(metaEntry);
            if (metaStream == null) {
                throw new DBException("Cannot open meta file '" + metaEntry.getName() + "'");
            }
            try {
                final Document metaDocument = XMLUtils.parseDocument(metaStream);
                {
                    // Read libraries map
                    final Element libsElement = XMLUtils.getChildElement(metaDocument.getDocumentElement(), ExportConstants.TAG_LIBRARIES);
                    if (libsElement != null) {
                        final Element[] libList = XMLUtils.getChildElementList(libsElement, DataSourceConstants.TAG_LIBRARY);
                        monitor.beginTask("Load driver libraries", libList.length);
                        for (Element libElement : libList) {
                            libMap.put(
                                libElement.getAttribute(ExportConstants.ATTR_PATH),
                                libElement.getAttribute(ExportConstants.ATTR_FILE));
                            monitor.worked(1);
                        }
                        monitor.done();
                    }
                }

                {
                    // Collect drivers to import
                    final Element driversElement = XMLUtils.getChildElement(metaDocument.getDocumentElement(), DataSourceConstants.TAG_DRIVERS);
                    if (driversElement != null) {
                        final Element[] driverList = XMLUtils.getChildElementList(driversElement, DataSourceConstants.TAG_DRIVER);
                        monitor.beginTask("Import drivers", driverList.length);
                        for (Element driverElement : driverList) {
                            if (monitor.isCanceled()) {
                                break;
                            }

                            importDriver(monitor, driverElement, zipFile, libMap, driverMap);
                            monitor.worked(1);
                        }
                        // Save drivers
                        DBeaverCore.getInstance().getDataSourceProviderRegistry().saveDrivers();
                        monitor.done();
                    }
                }

                {
                    // Import projects
                    final Element projectsElement = XMLUtils.getChildElement(metaDocument.getDocumentElement(), ExportConstants.TAG_PROJECTS);
                    if (projectsElement != null) {
                        final Element[] projectList = XMLUtils.getChildElementList(projectsElement, ExportConstants.TAG_PROJECT);
                        monitor.beginTask("Import projects", projectList.length);
                        for (Element projectElement : projectList) {
                            if (monitor.isCanceled()) {
                                break;
                            }

                            importProject(monitor, projectElement, zipFile, driverMap);
                            monitor.worked(1);
                        }
                        monitor.done();
                    }
                }

            } catch (XMLException e) {
                throw new DBException("Cannot parse meta file", e);
            } catch (CoreException e) {
                throw new DBException("Cannot persist project", e);
            } finally {
                metaStream.close();
            }
        }
        finally {
            zipFile.close();
        }
    }

    private DriverDescriptor importDriver(
        DBRProgressMonitor monitor,
        Element driverElement,
        ZipFile zipFile,
        Map<String, String> libMap,
        Map<String, String> driverMap) throws IOException, DBException
    {
        String providerId = driverElement.getAttribute(DataSourceConstants.ATTR_PROVIDER);
        String driverId = driverElement.getAttribute(DataSourceConstants.ATTR_ID);
        boolean isCustom = "true".equals(driverElement.getAttribute(DataSourceConstants.ATTR_CUSTOM));
        String driverName = driverElement.getAttribute(DataSourceConstants.ATTR_NAME);
        String driverClass = driverElement.getAttribute(DataSourceConstants.ATTR_CLASS);
        String driverURL = driverElement.getAttribute(DataSourceConstants.ATTR_URL);
        String driverDefaultPort = driverElement.getAttribute(DataSourceConstants.ATTR_PORT);
        String driverDescription = driverElement.getAttribute(DataSourceConstants.ATTR_DESCRIPTION);

        DataSourceProviderDescriptor dataSourceProvider = DBeaverCore.getInstance().getDataSourceProviderRegistry().getDataSourceProvider(providerId);
        if (dataSourceProvider == null) {
            throw new DBException("Cannot find data source provider '" + providerId + "' for driver '" + driverName + "'");
        }
        monitor.subTask("Load driver " + driverName);

        DriverDescriptor driver = null;
        if (!isCustom) {
            // Get driver by ID
            driver = dataSourceProvider.getDriver(driverId);
            if (driver == null) {
                log.warn("Driver '" + driverId + "' not found in data source provider '" + dataSourceProvider.getName() + "'");
            }
        }
        if (driver == null) {
            // Try to find existing driver by class name
            List<DriverDescriptor> matchedDrivers = new ArrayList<DriverDescriptor>();
            for (DriverDescriptor tmpDriver : dataSourceProvider.getDrivers()) {
                if (tmpDriver.getDriverClassName().equals(driverClass)) {
                    matchedDrivers.add(tmpDriver);
                }
            }
            if (matchedDrivers.size() == 1) {
                driver = matchedDrivers.get(0);
            } else if (!matchedDrivers.isEmpty()) {
                // Multiple drivers with the same class - tru to find driver with the same sample URL or with the same name
                for (DriverDescriptor tmpDriver : matchedDrivers) {
                    if (tmpDriver.getSampleURL().equals(driverURL) || tmpDriver.getName().equals(driverName)) {
                        driver = tmpDriver;
                        break;
                    }
                }
                if (driver == null) {
                    // Not found - lets use first one
                    log.warn("Ambiguous driver '" + driverName + "' - multiple drivers with class '" + driverClass + "' found. First one will be used");
                    driver = matchedDrivers.get(0);
                }
            }
        }
        if (driver == null) {
            // Create new driver
            driver = dataSourceProvider.createDriver();
            driver.setName(driverName);
            driver.setDescription(driverDescription);
            driver.setDriverClassName(driverClass);
            if (!CommonUtils.isEmpty(driverDefaultPort)) {
                driver.setDriverDefaultPort(CommonUtils.toInt(driverDefaultPort));
            }
            driver.setSampleURL(driverURL);
            driver.setModified(true);
            dataSourceProvider.addDriver(driver);
        }

        // Parameters and properties
        for (Element libElement : XMLUtils.getChildElementList(driverElement, DataSourceConstants.TAG_PARAMETER)) {
            driver.setDriverParameter(
                libElement.getAttribute(DataSourceConstants.ATTR_NAME),
                libElement.getAttribute(DataSourceConstants.ATTR_VALUE));
        }
        for (Element libElement : XMLUtils.getChildElementList(driverElement, DataSourceConstants.TAG_PROPERTY)) {
            driver.setConnectionProperty(
                libElement.getAttribute(DataSourceConstants.ATTR_NAME),
                libElement.getAttribute(DataSourceConstants.ATTR_VALUE));
        }

        // Add libraries (only for managable drivers with empty library list)
        if (driver.isManagable() && CommonUtils.isEmpty(driver.getLibraries())) {
            List<String> libraryList = new ArrayList<String>();
            final Element[] libList = XMLUtils.getChildElementList(driverElement, DataSourceConstants.TAG_LIBRARY);
            for (Element libElement : libList) {
                libraryList.add(libElement.getAttribute(DataSourceConstants.ATTR_PATH));
            }

            for (String libPath : libraryList) {
                File libFile = new File(libPath);
                if (libFile.exists()) {
                    // Just use path as-is (may be it is local re-import or local environments equal to export environment)
                    driver.addLibrary(libPath);
                } else {
                    // Get driver library from archive
                    String archiveLibEntry = libMap.get(libPath);
                    if (archiveLibEntry != null) {
                        ZipEntry libEntry = zipFile.getEntry(archiveLibEntry);
                        if (libEntry != null) {
                            // Extract driver to "drivers" folder
                            String libName = libFile.getName();
                            File contribFolder = DriverDescriptor.getDriversContribFolder();
                            if (!contribFolder.exists()) {
                                if (!contribFolder.mkdir()) {
                                    log.error("Cannot create drivers folder '" + contribFolder.getAbsolutePath() + "'");
                                    continue;
                                }
                            }
                            File importLibFile = new File(contribFolder, libName);
                            if (!importLibFile.exists()) {
                                FileOutputStream os = new FileOutputStream(importLibFile);
                                try {
                                    IOUtils.copyStream(zipFile.getInputStream(libEntry), os, IOUtils.DEFAULT_BUFFER_SIZE);
                                } finally {
                                    ContentUtils.close(os);
                                }
                            }
                            // Make relative path
                            String contribPath = contribFolder.getAbsolutePath();
                            String libAbsolutePath = importLibFile.getAbsolutePath();
                            String relativePath = libAbsolutePath.substring(contribPath.length());
                            while (relativePath.charAt(0) == '/' || relativePath.charAt(0) == '\\') {
                                relativePath = relativePath.substring(1);
                            }
                            driver.addLibrary(relativePath);
                        }
                    }
                }
            }
        }

        if (driver != null) {
            // Update driver map
            driverMap.put(driverId, driver.getId());
        }

        return driver;
    }

    private IProject importProject(DBRProgressMonitor monitor, Element projectElement, ZipFile zipFile, Map<String, String> driverMap)
        throws DBException, CoreException, IOException
    {
        String projectName = projectElement.getAttribute(ExportConstants.ATTR_NAME);
        String projectDescription = projectElement.getAttribute(ExportConstants.ATTR_DESCRIPTION);
        String targetProjectName = data.getTargetProjectName(projectName);
        if (targetProjectName == null) {
            targetProjectName = projectName;
        }

        IProject project = DBeaverCore.getInstance().getWorkspace().getRoot().getProject(targetProjectName);
        if (project.exists()) {
            throw new DBException("Project '" + targetProjectName + "' already exists");
        }

        monitor.subTask("Import project " + targetProjectName);

        final IProjectDescription description = DBeaverCore.getInstance().getWorkspace().newProjectDescription(project.getName());
        if (!CommonUtils.isEmpty(projectDescription)) {
            description.setComment(projectDescription);
        }
        project.create(description, monitor.getNestedMonitor());

        try {
            // Open project
            project.open(monitor.getNestedMonitor());

            // Set project properties
            loadResourceProperties(monitor, project, projectElement);
            project.setPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID, null);

            // Load resources
            importChildResources(
                monitor,
                project,
                projectElement,
                ExportConstants.DIR_PROJECTS + "/" + projectName + "/",
                zipFile);

            // Update driver references in datasources
            updateDriverReferences(monitor, project, driverMap);
        } catch (Exception e) {
            // Cleanup project which was partially imported
            try {
                project.delete(true, true, monitor.getNestedMonitor());
            } catch (CoreException e1) {
                log.error(e1);
            }
            throw new DBException("Error importing project resources", e);
        }

        // Add project to registry (it will also initialize this project)
        DBeaverCore.getInstance().getProjectRegistry().addProject(project);

        return project;
    }

    private void importChildResources(DBRProgressMonitor monitor, IContainer resource, Element resourceElement, String containerPath, ZipFile zipFile)
        throws DBException, IOException, CoreException
    {
        for (Element childElement : XMLUtils.getChildElementList(resourceElement, ExportConstants.TAG_RESOURCE)) {
            String childName = childElement.getAttribute(ExportConstants.ATTR_NAME);
            boolean isDirectory = "true".equals(childElement.getAttribute(ExportConstants.ATTR_DIRECTORY));
            String entryPath = containerPath + childName;
            if (isDirectory) {
                entryPath += "/";
            }
            final ZipEntry resourceEntry = zipFile.getEntry(entryPath);
            if (resourceEntry == null) {
                throw new DBException("Project resource '" + entryPath + "' not found in archive");
            }
            if (isDirectory != resourceEntry.isDirectory()) {
                throw new DBException("Directory '" + entryPath + "' stored as file in archive");
            }
            IResource childResource;
            if (isDirectory) {
                IFolder folder;
                if (resource instanceof IFolder) {
                    folder = ((IFolder)resource).getFolder(childName);
                } else if (resource instanceof IProject) {
                    folder = ((IProject)resource).getFolder(childName);
                } else {
                    throw new DBException("Unsupported container type '" + resource.getClass().getName() + "'");
                }
                folder.create(true, true, monitor.getNestedMonitor());
                childResource = folder;
                importChildResources(monitor, folder, childElement, entryPath, zipFile);
            } else {
                IFile file;
                if (resource instanceof IFolder) {
                    file = ((IFolder)resource).getFile(childName);
                } else if (resource instanceof IProject) {
                    file = ((IProject)resource).getFile(childName);
                } else {
                    throw new DBException("Unsupported container type '" + resource.getClass().getName() + "'");
                }
                file.create(zipFile.getInputStream(resourceEntry), true, monitor.getNestedMonitor());
                childResource = file;
            }
            loadResourceProperties(monitor, childResource, childElement);
        }
    }

    private void loadResourceProperties(DBRProgressMonitor monitor, IResource resource, Element element) throws CoreException, IOException
    {
        if (resource instanceof IFile) {
            final String charset = element.getAttribute(ExportConstants.ATTR_CHARSET);
            if (!CommonUtils.isEmpty(charset)) {
                ((IFile) resource).setCharset(charset, monitor.getNestedMonitor());
            }
        }
        for (Element attrElement : XMLUtils.getChildElementList(element, ExportConstants.TAG_ATTRIBUTE)) {
            String qualifier = attrElement.getAttribute(ExportConstants.ATTR_QUALIFIER);
            String name = attrElement.getAttribute(ExportConstants.ATTR_NAME);
            String value = attrElement.getAttribute(ExportConstants.ATTR_VALUE);
            if (!CommonUtils.isEmpty(qualifier) && !CommonUtils.isEmpty(name) && !CommonUtils.isEmpty(value)) {
                resource.setPersistentProperty(new QualifiedName(qualifier, name), value);
            }
        }
    }

    private void updateDriverReferences(DBRProgressMonitor monitor, IProject project, Map<String, String> driverMap) throws DBException, CoreException, IOException
    {
        final IFile configFile = project.getFile(DataSourceRegistry.CONFIG_FILE_NAME);
        if (configFile == null || !configFile.exists()) {
            throw new DBException("Cannot find configuration file '" + DataSourceRegistry.CONFIG_FILE_NAME + "'");
        }
        // Read and filter datasources config
        final InputStream configContents = configFile.getContents();
        String filteredContent;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(configContents, ContentUtils.DEFAULT_FILE_CHARSET));
            StringBuilder buffer = new StringBuilder();
            for (;;) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                buffer.append(line).append(ContentUtils.getDefaultLineSeparator());
            }
            filteredContent = buffer.toString();
            for (Map.Entry<String, String> entry : driverMap.entrySet()) {
                if (!entry.getKey().equals(entry.getValue())) {
                    filteredContent = filteredContent.replace("driver=\"" + entry.getKey() + "\"", "driver=\"" + entry.getValue() + "\"");
                }
            }
        }
        finally {
            ContentUtils.close(configContents);
        }
        // Update configuration
        configFile.setContents(
            new ByteArrayInputStream(filteredContent.getBytes(ContentUtils.DEFAULT_FILE_CHARSET)),
            true,
            false,
            monitor.getNestedMonitor());
    }


}
