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

package org.jkiss.dbeaver.ext.import_config.wizards.dbvis;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.Activator;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConfigImportWizardPageDbvis extends ConfigImportWizardPage {

    public static final String DBVIS_HOME_FOLDER = ".dbvis";
    public static final String DBVIS_CONFIG70_FOLDER = "config70";
    public static final String DBVIS_CONFIG_FILE = "dbvis.xml";

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
        File dbvisConfigHome = new File(homeFolder, DBVIS_HOME_FOLDER);
        if (!dbvisConfigHome.exists()) {
            throw new DBException("DBVisualizer installation not found");
        }
        File configFolder = new File(dbvisConfigHome, DBVIS_CONFIG70_FOLDER);
        if (!configFolder.exists()) {
            throw new DBException("Only DBVisualizer 7.x version is supported");
        }
        File configFile = new File(configFolder, DBVIS_CONFIG_FILE);
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
                    String lastName = XMLUtils.getChildElementBody(driverElement, "LastName");
                    //String lastVersion = XMLUtils.getChildElementBody(driverElement, "LastVersion");
                    if (!CommonUtils.isEmpty(name) && !CommonUtils.isEmpty(sampleURL) && !CommonUtils.isEmpty(driverClass)) {
                        ImportDriverInfo driver = new ImportDriverInfo(null, name, sampleURL, driverClass);
                        if (!CommonUtils.isEmpty(lastName)) {
                            driver.setDescription(lastName);
                        }
                        adaptSampleUrl(driver);

                        // Parse libraries
                        Element locationsElement = XMLUtils.getChildElement(driverElement, "Locations");
                        if (locationsElement != null) {
                            for (Element locationElement : XMLUtils.getChildElementList(locationsElement, "Location")) {
                                String path = XMLUtils.getChildElementBody(locationElement, "Path");
                                if (!CommonUtils.isEmpty(path)) {
                                    driver.addLibrary(path);
                                }
                            }
                        }

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
                    String password = null;
                    String passwordEncoded = XMLUtils.getChildElementBody(dbElement, "Password");
/*
                    if (!CommonUtils.isEmpty(passwordEncoded)) {
                        try {
                            password = new String(Base64.decode(passwordEncoded), ContentUtils.DEFAULT_CHARSET);
                        } catch (UnsupportedEncodingException e) {
                            // Ignore
                        }
                    }
*/
                    String hostName = null, port = null, database = null;
                    Element urlVarsElement = XMLUtils.getChildElement(dbElement, "UrlVariables");
                    if (urlVarsElement != null) {
                        Element driverElement = XMLUtils.getChildElement(urlVarsElement, "Driver");
                        if (driverElement != null) {
                            for (Element urlVarElement : XMLUtils.getChildElementList(driverElement, "UrlVariable")) {
                                final String varName = urlVarElement.getAttribute("UrlVariableName");
                                final String varValue = XMLUtils.getElementBody(urlVarElement);
                                if ("Server".equals(varName)) {
                                    hostName = varValue;
                                } else if ("Port".equals(varName)) {
                                    port = varValue;
                                } else if ("Database".equals(varName)) {
                                    database = varValue;
                                }
                            }
                        }
                    }
                    if (!CommonUtils.isEmpty(alias) && !CommonUtils.isEmpty(driverName) && (!CommonUtils.isEmpty(url) || !CommonUtils.isEmpty(hostName))) {
                        ImportDriverInfo driver = importData.getDriver(driverName);
                        if (driver != null) {
                            ImportConnectionInfo connectionInfo = new ImportConnectionInfo(
                                driver,
                                dbElement.getAttribute("id"),
                                alias,
                                url,
                                hostName,
                                port,
                                database,
                                user,
                                password);
                            importData.addConnection(connectionInfo);
                        }
                    }
                }
            }

        } catch (XMLException e) {
            throw new DBException("Configuration parse error: " + e.getMessage());
        }
    }

    private static Pattern PATTERN_PROTOCOL = Pattern.compile("<protocol>");
    private static Pattern PATTERN_HOST = Pattern.compile("<server>");
    private static Pattern PATTERN_PORT = Pattern.compile("<port([0-9]*)>");
    private static Pattern PATTERN_DATABASE = Pattern.compile("<database>|<databaseName>|<sid>|<datasource>");

    private void adaptSampleUrl(ImportDriverInfo driverInfo)
    {
        String port = null;
        String sampleURL = driverInfo.getSampleURL();
        sampleURL = PATTERN_PROTOCOL.matcher(sampleURL).replaceAll("{protocol}");
        sampleURL = PATTERN_HOST.matcher(sampleURL).replaceAll("{host}");
        final Matcher portMatcher = PATTERN_PORT.matcher(sampleURL);
        if (portMatcher.find()) {
            final String portString = portMatcher.group(1);
            if (!CommonUtils.isEmpty(portString)) {
                port = portString;
            }
        }
        sampleURL = portMatcher.replaceAll("{port}");
        sampleURL = PATTERN_DATABASE.matcher(sampleURL).replaceAll("{database}");

        driverInfo.setSampleURL(sampleURL);
        if (port != null) {
            driverInfo.setDefaultPort(port);
        }
    }

}
