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
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
                        adaptSampleUrl(driver);
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
                            try {
                                adaptConnectionUrl(connectionInfo);
                            } catch (DBException e) {
                                setMessage(e.getMessage(), IMessageProvider.WARNING);
                            }
                            importData.addConnection(connectionInfo);
                        }
                    }
                }
            }

        } catch (XMLException e) {
            throw new DBException("Configuration parse error: " + e.getMessage());
        }
    }

    private static Pattern PATTERN_HOST = Pattern.compile("<server>");
    private static Pattern PATTERN_PORT = Pattern.compile("<port([0-9]*)>");
    private static Pattern PATTERN_DATABASE = Pattern.compile("<database>|<databaseName>|<sid>|<datasource>");

    private void adaptSampleUrl(ImportDriverInfo driverInfo)
    {
        int port = 0;
        String sampleURL = driverInfo.getSampleURL();
        sampleURL = PATTERN_HOST.matcher(sampleURL).replaceAll("{host}");
        final Matcher portMatcher = PATTERN_PORT.matcher(sampleURL);
        if (portMatcher.find()) {
            final String portString = portMatcher.group(1);
            if (!CommonUtils.isEmpty(portString)) {
                port = CommonUtils.toInt(portString);
            }
        }
        sampleURL = portMatcher.replaceAll("{port}");
        sampleURL = PATTERN_DATABASE.matcher(sampleURL).replaceAll("{database}");

        driverInfo.setSampleURL(sampleURL);
        if (port > 0) {
            driverInfo.setDefaultPort(port);
        }
    }

    private void adaptConnectionUrl(ImportConnectionInfo connectionInfo) throws DBException
    {
        final DriverDescriptor.MetaURL metaURL = DriverDescriptor.parseSampleURL(connectionInfo.getDriverInfo().getSampleURL());
        final String url = connectionInfo.getUrl();
        int sourceOffset = 0;
        List<String> urlComponents = metaURL.getUrlComponents();
        for (int i = 0, urlComponentsSize = urlComponents.size(); i < urlComponentsSize; i++) {
            String component = urlComponents.get(i);
            if (component.length() > 2 && component.charAt(0) == '{' && component.charAt(component.length() - 1) == '}' && metaURL.getAvailableProperties().contains(component.substring(1, component.length() - 1))) {
                // Property
                int partEnd;
                if (i < urlComponentsSize - 1) {
                    // Find next component
                    final String nextComponent = urlComponents.get(i + 1);
                    partEnd = url.indexOf(nextComponent, sourceOffset);
                    if (partEnd == -1) {
                        throw new DBException("Can't parse URL '" + url + "' - string '" + nextComponent + "' not found after '" + component);
                    }
                } else {
                    partEnd = url.length();
                }

                String propertyValue = url.substring(sourceOffset, partEnd);
                if (component.equals("{host}")) {
                    connectionInfo.setHost(propertyValue);
                } else if (component.equals("{port}")) {
                    connectionInfo.setPort(CommonUtils.toInt(propertyValue));
                } else if (component.equals("{database}")) {
                    connectionInfo.setDatabase(propertyValue);
                } else {
                    throw new DBException("Unsupported property " + component);
                }
                sourceOffset = partEnd;
            } else {
                // Static string
                sourceOffset += component.length();
            }
        }
    }

}
