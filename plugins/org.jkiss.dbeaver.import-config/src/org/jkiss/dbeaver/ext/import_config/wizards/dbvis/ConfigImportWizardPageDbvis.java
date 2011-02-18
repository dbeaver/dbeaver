/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards.dbvis;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.xml.XMLException;
import net.sf.jkiss.utils.xml.XMLUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.Activator;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;


public class ConfigImportWizardPageDbvis extends ConfigImportWizardPage {

    protected ConfigImportWizardPageDbvis()
    {
        super("DBVisualizer");
        setTitle("DBVisualizer");
        setDescription("Import DBVisualizer connections");
        setImageDescriptor(Activator.getImageDescriptor("icons/dbvis_big.png"));
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException
    {
        File homeFolder = RuntimeUtils.getUserHomeDir();
        File dbvisConfigHome = new File(homeFolder, ".dbvis");
        if (!dbvisConfigHome.exists()) {
            throw new DBException("DBVisualizer installation not found");
        }
        File configFolder = new File(dbvisConfigHome, "config70");
        if (!configFolder.exists()) {
            throw new DBException("Only DBVisualizer 7.x version is supported");
        }
        File configFile = new File(configFolder, "dbvis.xml");
        if (!configFile.exists()) {
            throw new DBException("DBVisualizer configuration file not found");
        }

        try {
            Document configDocument = XMLUtils.parseDocument(configFile);

            Element driversElement = XMLUtils.getChildElement(configDocument.getDocumentElement(), "Drivers");
            if (driversElement != null) {
                for (Element driverElement : XMLUtils.getChildElementList(driversElement, "Driver")) {
                    String name = XMLUtils.getChildElementBody(driverElement, "Name");
                    String sampleURL = XMLUtils.getChildElementBody(driverElement, "URLFormat");
                    String driverClass = XMLUtils.getChildElementBody(driverElement, "DefaultClass");
                    if (!CommonUtils.isEmpty(name) && !CommonUtils.isEmpty(sampleURL) && !CommonUtils.isEmpty(driverClass)) {
                        ImportDriverInfo driver = new ImportDriverInfo(null, name, sampleURL, driverClass);
                        importData.addDriver(driver);
                    }
                }
            }

            Element databasesElement = XMLUtils.getChildElement(configDocument.getDocumentElement(), "Databases");
            if (databasesElement != null) {
                for (Element dbElement : XMLUtils.getChildElementList(databasesElement, "Database")) {
                    String alias = XMLUtils.getChildElementBody(dbElement, "Alias");
                    String url = XMLUtils.getChildElementBody(dbElement, "Url");
                    String driverName = XMLUtils.getChildElementBody(dbElement, "Driver");
                    String user = XMLUtils.getChildElementBody(dbElement, "Userid");
                    if (!CommonUtils.isEmpty(alias) && !CommonUtils.isEmpty(url) && !CommonUtils.isEmpty(driverName)) {
                        ImportDriverInfo driver = importData.getDriver(driverName);
                        if (driver != null) {
                            ImportConnectionInfo connectionInfo = new ImportConnectionInfo(
                                driver,
                                dbElement.getAttribute("id"),
                                alias,
                                url,
                                null,
                                -1,
                                null,
                                user,
                                null);
                            importData.addConnection(connectionInfo);
                        }
                    }
                }
            }

        } catch (XMLException e) {
            throw new DBException("Configuration parse error: " + e.getMessage());
        }
    }
}
