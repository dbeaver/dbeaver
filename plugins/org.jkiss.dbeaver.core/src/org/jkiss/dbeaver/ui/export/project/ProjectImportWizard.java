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
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceConstants;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        UIUtils.showInfoBox(getShell(), "Project import", "Project(s) successfully imported");
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
            for (DriverDescriptor tmpDriver : dataSourceProvider.getDrivers()) {
                if (tmpDriver.getDriverClassName().equals(driverClass)) {
                    driver = tmpDriver;
                    break;
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

        // Add libraries (only for custom drivers with empty library list)
        if (driver.isCustom() && CommonUtils.isEmpty(driver.getLibraries())) {
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
                            driver.addLibrary(importLibFile.getAbsolutePath());
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

    private IProject importProject(DBRProgressMonitor monitor, Element projectElement, ZipFile zipFile, Map<String, String> driverMap) throws DBException
    {
        String projectName = projectElement.getAttribute(ExportConstants.ATTR_NAME);
        String projectDescription = projectElement.getAttribute(ExportConstants.ATTR_DESCRIPTION);
        String targetProjectName = data.getTargetProjectName(projectName);

        IProject project = DBeaverCore.getInstance().getWorkspace().getRoot().getProject(targetProjectName);
        if (project.exists()) {
            throw new DBException("Project '" + targetProjectName + "' already exists");
        }

        monitor.subTask("Import project " + targetProjectName);


        return project;
    }


}
