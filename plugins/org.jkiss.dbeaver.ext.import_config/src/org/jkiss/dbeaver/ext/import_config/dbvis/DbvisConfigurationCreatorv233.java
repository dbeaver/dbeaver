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

package org.jkiss.dbeaver.ext.import_config.dbvis;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DbvisConfigurationCreatorv233 extends DbvisAbstractConfigurationCreator {

    public static final String VERSION = "version.24.3.x"; //$NON-NLS-1$
    public static final String CONFIG_FOLDER = "config233"; //$NON-NLS-1$
    public static final String CONFIG_FILE = "dbvis.xml"; //$NON-NLS-1$

    @Override
    public ImportData create(ImportData importData,
        File configFile) throws DBException {
        try {
            Document configDocument = XMLUtils.parseDocument(configFile);
            Element databasesElement = XMLUtils.getChildElement(configDocument.getDocumentElement(), "Databases");
            if (databasesElement != null) {
                for (Element dbElement : XMLUtils.getChildElementList(databasesElement, "Database")) {
                    String alias = XMLUtils.getChildElementBody(dbElement, "Alias");
                    String url = XMLUtils.getChildElementBody(dbElement, "Url");
                    String driverName = XMLUtils.getChildElementBody(dbElement, "Driver");
                    String user = XMLUtils.getChildElementBody(dbElement, "Userid");
                    String password = null;
                    //String passwordEncoded = XMLUtils.getChildElementBody(dbElement, "Password");
                    String hostName = null, port = null, database = null;
                    Element urlVarsElement = XMLUtils.getChildElement(dbElement, "UrlVariables");
                    if (urlVarsElement != null) {
                        Element driverElement = XMLUtils.getChildElement(urlVarsElement, "Driver");
                        if (driverElement != null) {
                            String templateId = driverElement.getAttribute("TemplateId");
                            String driverId = driverElement.getAttribute("DriverId");
                            String[] driverIdSegments = driverId.split("-");
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
                            StringBuilder builder = new StringBuilder("databaseinfo/user/driverTypes/");
                            builder.append(templateId)
                                .append("_")
                                .append(driverId)
                                .append("/")
                                .append("driverType")
                                .append("_")
                                .append(templateId)
                                .append("_")
                                .append(driverIdSegments[0])
                                .append(".xml");
                            File driverFile = new File(configFile.getParent(), builder.toString());
                            Document driverTypeDocument = XMLUtils.parseDocument(driverFile);
                            Element driverTypeDocumentElement = driverTypeDocument.getDocumentElement();
                            String name = XMLUtils.getChildElementBody(driverTypeDocumentElement, "Label");
                            String sampleURL = XMLUtils.getChildElementBody(driverTypeDocumentElement, "URLFormat");
                            String driverClass = XMLUtils.getChildElementBody(driverTypeDocumentElement, "DefaultClass");
                            
                            String identifier = XMLUtils.getChildElementBody(driverTypeDocumentElement, "Identifier");

                            String originId = XMLUtils.getChildElementBody(driverTypeDocumentElement, "OriginId");
                            if (!CommonUtils.isEmpty(name) && !CommonUtils.isEmpty(sampleURL) && !CommonUtils.isEmpty(driverClass)) {
                                ImportDriverInfo driver = new ImportDriverInfo(identifier, name, sampleURL, driverClass);
                                adaptSampleUrl(driver);
                                importData.addDriver(driver);
                            }
                            // SshServers
                            Map<String, Object> sshServerConfiguration = new HashMap<>();
                            File sshServersFile = new File(configFile.getParent(), "sshservers.xml");
                            Document sshConfigDocument = XMLUtils.parseDocument(sshServersFile);
                            Element sshDatabasesElement = XMLUtils.getChildElement(sshConfigDocument.getDocumentElement(), "DbVisualizer");
                            if (sshDatabasesElement != null) {
                            Element sshServersElement = XMLUtils.getChildElement(configDocument.getDocumentElement(), "SshServers");
                            if (sshServersElement != null) {
                                for (Element sshServerElement : XMLUtils.getChildElementList(sshDatabasesElement, "SshServer")) {
                                    String sshServerConfigurationId = sshServerElement.getAttribute("id");
                                    String sshEnabled = XMLUtils.getChildElementBody(sshServerElement, "Enabled");
                                    String SshHost = XMLUtils.getChildElementBody(sshServerElement, "SshHost");
                                    String SshPort = XMLUtils.getChildElementBody(sshServerElement, "SshPort");
                                    String SshUserid = XMLUtils.getChildElementBody(sshServerElement, "SshUserid");
                                    
                                }
                                
                            }}
                            
//                            for (Element dbElement : XMLUtils.getChildElementList(databasesElement, "Database")) {
//                            }
                            
                            
                            Element sshServers = XMLUtils.getChildElement(dbElement, "SshServers");
                            if (sshServers != null) {
                                DBWHandlerConfiguration sshHandler = null;
                                for (Element sshServer : XMLUtils.getChildElementList(sshServers, "SshServer")) {
                                    String sshServerId = sshServer.getAttribute("id");
                                    String enabled = XMLUtils.getChildElementBody(sshServer, "Enabled");
                                    if (enabled.equals("true")) {

                                    }
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

}
