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
package org.jkiss.dbeaver.ext.config.migration.dbvis;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportDriverInfo;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

public class DbvisConfigurationCreatorv7 extends DbvisAbstractConfigurationCreator {
    public static final String VERSION = "version.7.x.x"; //$NON-NLS-1$
    public static final String CONFIG_FOLDER = "config70"; //$NON-NLS-1$
    public static final String CONFIG_FILE = "dbvis.xml"; //$NON-NLS-1$

    @Override
    public ImportData create(ImportData importData,
        File configFile) throws DBException {
        try {
            Document configDocument = XMLUtils.parseDocument(configFile);
            Element driversElement = XMLUtils.getChildElement(configDocument.getDocumentElement(), "Drivers");
            if (driversElement != null) {
                for (Element driverElement : XMLUtils.getChildElementList(driversElement, "Driver")) {
                    String name = XMLUtils.getChildElementBody(driverElement, "Name");
                    String sampleURL = XMLUtils.getChildElementBody(driverElement, "URLFormat");
                    String driverClass = XMLUtils.getChildElementBody(driverElement, "DefaultClass");
                    String lastName = XMLUtils.getChildElementBody(driverElement, "LastName");
                    // String lastVersion = XMLUtils.getChildElementBody(driverElement,
                    // "LastVersion");
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
                    //String passwordEncoded = XMLUtils.getChildElementBody(dbElement, "Password");
                    /*
                     * if (!CommonUtils.isEmpty(passwordEncoded)) { try { password = new
                     * String(Base64.decode(passwordEncoded), ContentUtils.DEFAULT_ENCODING); }
                     * catch (UnsupportedEncodingException e) { // Ignore } }
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
                    if (!CommonUtils.isEmpty(alias) && !CommonUtils.isEmpty(driverName)
                        && (!CommonUtils.isEmpty(url) || !CommonUtils.isEmpty(hostName))) {
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
        return importData;
    }

    @Override
    public String getConfigurationFile() {
        return CONFIG_FILE;
    }

    @Override
    public String getConfigurationFolder() {
        return CONFIG_FOLDER;
    }

    @Override
    protected String substituteDriverName(String name) {
        // not require to substitute
        return name;
    }

}
