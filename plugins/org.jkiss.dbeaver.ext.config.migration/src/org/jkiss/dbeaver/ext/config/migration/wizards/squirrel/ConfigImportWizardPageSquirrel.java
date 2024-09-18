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

package org.jkiss.dbeaver.ext.config.migration.wizards.squirrel;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.config.migration.Activator;
import org.jkiss.dbeaver.ext.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ext.config.migration.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConfigImportWizardPageSquirrel extends ConfigImportWizardPage {

    public static final String SQL_HOME_FOLDER = ".squirrel-sql";
    public static final String SQL_ALIASES_FILE = "SQLAliases23.xml";
    public static final String SQL_DRIVERS_FILE = "SQLDrivers.xml";

    protected ConfigImportWizardPageSquirrel()
    {
        super(ImportConfigMessages.config_import_wizard_squirrel_name);
        setTitle(ImportConfigMessages.config_import_wizard_squirrel_name);
        setDescription(ImportConfigMessages.config_import_wizard_squirrel_description);
        setImageDescriptor(Activator.getImageDescriptor("icons/squirrel_big.png"));
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException
    {
        File homeFolder = RuntimeUtils.getUserHomeDir();
        File sqlConfigHome = new File(homeFolder, SQL_HOME_FOLDER);
        if (!sqlConfigHome.exists()) {
            throw new DBException(ImportConfigMessages.config_import_wizard_page_squirrel_label_installation_not_found);
        }
        File driversFile = new File(sqlConfigHome, SQL_DRIVERS_FILE);
        if (!driversFile.exists()) {
            throw new DBException("SQL Squirrel drivers configuration file not found. Possibly corrupted installation.");
        }
        File aliasesFile = new File(sqlConfigHome, SQL_ALIASES_FILE);
        if (!aliasesFile.exists()) {
            throw new DBException("SQL Squirrel configuration file not found. Possibly version older than 2.3 is installed.");
        }

        try {
            // Parse drivers

            Document driversDocument = XMLUtils.parseDocument(driversFile);
            for (Element driverElement : XMLUtils.getChildElementList(driversDocument.getDocumentElement(), "Bean")) {
                if (!"net.sourceforge.squirrel_sql.fw.sql.SQLDriver".equals(driverElement.getAttribute("Class"))) {
                    continue;
                }
                final Element driverIdentifier = XMLUtils.getChildElement(driverElement, "identifier");
                String driverId = driverIdentifier == null ? null : XMLUtils.getChildElementBody(driverIdentifier, "string");
                if (CommonUtils.isEmpty(driverId)) {
                    continue;
                }
                String name = XMLUtils.getChildElementBody(driverElement, "name");
                String driverClass = XMLUtils.getChildElementBody(driverElement, "driverClassName");
                String sampleURL = XMLUtils.getChildElementBody(driverElement, "url");
                //String webURL = XMLUtils.getChildElementBody(driverElement, "websiteUrl");

                if (!CommonUtils.isEmpty(name) && !CommonUtils.isEmpty(sampleURL) && !CommonUtils.isEmpty(driverClass)) {
                    ImportDriverInfo driver = new ImportDriverInfo(driverId, name, sampleURL, driverClass);
                    adaptSampleUrl(driver);

                    // Parse libraries
                    final Element jarFileNames = XMLUtils.getChildElement(driverElement, "jarFileNames");
                    if (jarFileNames != null) {
                        for (Element locationElement : XMLUtils.getChildElementList(jarFileNames, "Bean")) {
                            String path = XMLUtils.getChildElementBody(locationElement, "string");
                            if (!CommonUtils.isEmpty(path)) {
                                driver.addLibrary(path);
                            }
                        }
                    }

                    importData.addDriver(driver);
                }
            }

            // Parse aliases
            Document aliasesDocument = XMLUtils.parseDocument(aliasesFile);

            for (Element aliasElement : XMLUtils.getChildElementList(aliasesDocument.getDocumentElement(), "Bean")) {
                if (!"net.sourceforge.squirrel_sql.client.gui.db.SQLAlias".equals(aliasElement.getAttribute("Class"))) {
                    continue;
                }
                final Element driverIdentifier = XMLUtils.getChildElement(aliasElement, "driverIdentifier");
                String driverId = driverIdentifier == null ? null : XMLUtils.getChildElementBody(driverIdentifier, "string");
                if (CommonUtils.isEmpty(driverId)) {
                    continue;
                }
                final ImportDriverInfo driverInfo = importData.getDriverByID(driverId);
                if (driverInfo == null) {
                    continue;
                }
                String name = XMLUtils.getChildElementBody(aliasElement, "name");
                String url = XMLUtils.getChildElementBody(aliasElement, "url");
                String userName = XMLUtils.getChildElementBody(aliasElement, "userName");
                String password = XMLUtils.getChildElementBody(aliasElement, "password");
                ImportConnectionInfo connectionInfo = new ImportConnectionInfo(driverInfo, null, name, url, null, null, null, userName, password);
                importData.addConnection(connectionInfo);
            }
        } catch (XMLException e) {
            throw new DBException("Configuration parse error: " + e.getMessage());
        }

    }

    private static Pattern PATTERN_OPTIONAL = Pattern.compile("\\[|\\]");
    private static Pattern PATTERN_HOST = Pattern.compile("<server>|<server_name>|<hostname>|<host_name>|<host>", Pattern.CASE_INSENSITIVE);
    private static Pattern PATTERN_PORT = Pattern.compile("<port>|<port_number>|<(:?[0-9]+)>|(:[0-9]+)", Pattern.CASE_INSENSITIVE);
    private static Pattern PATTERN_HOST_PORT = Pattern.compile("<server:port>", Pattern.CASE_INSENSITIVE);
    private static Pattern PATTERN_DATABASE = Pattern.compile("dbname|<dbname>|<db-name>|<db_name>|<databaseName>|<database-name>|<database_name>|<database>|<full_db_path>|<pathname>|<alias>|<schema>|<default_schema>|<default-schema>", Pattern.CASE_INSENSITIVE);

    private void adaptSampleUrl(ImportDriverInfo driverInfo)
    {
        String port = null;
        String sampleURL = driverInfo.getSampleURL();
        int divPos = sampleURL.indexOf("?");
        if (divPos != -1) {
            sampleURL = sampleURL.substring(0, divPos);
        }
        divPos = sampleURL.indexOf(";");
        if (divPos != -1) {
            sampleURL = sampleURL.substring(0, divPos);
        }
        sampleURL = PATTERN_OPTIONAL.matcher(sampleURL).replaceAll("");
        sampleURL = PATTERN_HOST_PORT.matcher(sampleURL).replaceAll("{host}:{port}");
        sampleURL = PATTERN_HOST.matcher(sampleURL).replaceAll("{host}");

        String portReplacement = "{port}";
        final Matcher portMatcher = PATTERN_PORT.matcher(sampleURL);
        if (portMatcher.find()) {
            String portString = portMatcher.group(1);
            if (CommonUtils.isEmpty(portString)) {
                portString = portMatcher.group(2);
            }
            if (!CommonUtils.isEmpty(portString)) {
                if (portString.startsWith(":")) {
                    portReplacement = ":" + portReplacement;
                    portString = portString.substring(1);
                }
                port = portString;
            }
        }
        sampleURL = portMatcher.replaceAll(portReplacement);
        sampleURL = PATTERN_DATABASE.matcher(sampleURL).replaceAll("{database}");

        driverInfo.setSampleURL(sampleURL);
        if (port != null) {
            driverInfo.setDefaultPort(port);
        }
    }

}
