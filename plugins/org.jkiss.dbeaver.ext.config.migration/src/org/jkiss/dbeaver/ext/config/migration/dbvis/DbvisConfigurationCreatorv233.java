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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DbvisConfigurationCreatorv233 extends DbvisAbstractConfigurationCreator {
    private static final Log log = Log.getLog(DbvisConfigurationCreatorv233.class);
    
    public static final String VERSION = "version.24.3.x"; //$NON-NLS-1$
    public static final String CONFIG_FOLDER = "config233"; //$NON-NLS-1$
    public static final String CONFIG_FILE = "dbvis.xml"; //$NON-NLS-1$
    public static final String SSH_CONFIG_FILE = "sshservers.xml"; //$NON-NLS-1$

    // substitution map for current version of DBvisualiser
    public static final Map<String, String> dbvis2dbeaverDriverNames = new HashMap<>();
    static {
        dbvis2dbeaverDriverNames.put("H2 2.x Embedded", "H2 Embedded V.2"); //$NON-NLS-1$ $NON-NLS-2$
        dbvis2dbeaverDriverNames.put("Oracle 9i", "Oracle"); //$NON-NLS-1$ $NON-NLS-2$
        dbvis2dbeaverDriverNames.put("Db2 LUW", "Db2 for LUW"); //$NON-NLS-1$ $NON-NLS-2$
    }

    @Override
    public ImportData create(
        @NotNull ImportData importData,
        @NotNull File configFile
    ) throws DBException {
        try {
            Map<String, DbvisSshServerConfiguration> sshServerConfigurations = new LinkedHashMap<>();
            DbvisSshServerConfiguration sshConfiguration = null;
            File sshServersFile = new File(configFile.getParent(), SSH_CONFIG_FILE);
            if (sshServersFile.exists()) {
                Document sshConfigDocument = XMLUtils.parseDocument(sshServersFile);
                Element sshServersElement = XMLUtils.getChildElement(sshConfigDocument.getDocumentElement(), "SshServers");
                if (sshServersElement != null) {
                    for (Element sshServerElement : XMLUtils.getChildElementList(sshServersElement, "SshServer")) {
                        DbvisSshServerConfiguration sshConfig = new DbvisSshServerConfiguration();
                        sshConfig.setId(sshServerElement.getAttribute("id"));
                        sshConfig.setName(XMLUtils.getChildElementBody(sshServerElement, "Name"));
                        sshConfig.setDescription(XMLUtils.getChildElementBody(sshServerElement, "Description"));
                        sshConfig.setIsEnabled(XMLUtils.getChildElementBody(sshServerElement, "Enabled"));
                        sshConfig.setSshHost(XMLUtils.getChildElementBody(sshServerElement, "SshHost"));
                        sshConfig.setSshPort(XMLUtils.getChildElementBody(sshServerElement, "SshPort"));
                        sshConfig.setSshUserid(XMLUtils.getChildElementBody(sshServerElement, "SshUserid"));
                        sshConfig.setAuthenticationType(XMLUtils.getChildElementBody(sshServerElement, "AuthenticationType"));
                        sshConfig.setSshPrivateKeyFile(XMLUtils.getChildElementBody(sshServerElement, "SshPrivateKeyFile"));
                        sshConfig.setSshImplementationType(XMLUtils.getChildElementBody(sshServerElement, "SshImplementationType"));
                        sshConfig.setSshConfigFile(XMLUtils.getChildElementBody(sshServerElement, "SshConfigFile"));
                        sshConfig.setSshKnownHostsFile(XMLUtils.getChildElementBody(sshServerElement, "SshKnownHostsFile"));
                        sshConfig.setSshConnectTimeout(XMLUtils.getChildElementBody(sshServerElement, "SshConnectTimeout"));
                        sshServerConfigurations.put(sshConfig.getId(), sshConfig);
                    }
                }
            }
            Document configDocument = XMLUtils.parseDocument(configFile);
            Element databasesElement = XMLUtils.getChildElement(configDocument.getDocumentElement(), "Databases");
            if (databasesElement != null) {
                for (Element dbElement : XMLUtils.getChildElementList(databasesElement, "Database")) {
                    String alias = XMLUtils.getChildElementBody(dbElement, "Alias");
                    String url = XMLUtils.getChildElementBody(dbElement, "Url");
                    String driverName = XMLUtils.getChildElementBody(dbElement, "Driver");
                    String user = XMLUtils.getChildElementBody(dbElement, "Userid");
                    String password = null;
                    String hostName = null, port = null, database = null;
                    String type = XMLUtils.getChildElementBody(dbElement, "Type");
                    Element urlVarsElement = XMLUtils.getChildElement(dbElement, "UrlVariables");
                    ImportDriverInfo driver = null;
                    if (urlVarsElement != null) {
                        Element driverElement = XMLUtils.getChildElement(urlVarsElement, "Driver");
                        if (driverElement != null) {
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
                            builder.append(type)
                                .append("_")
                                .append(driverId)
                                .append("/")
                                .append("driverType")
                                .append("_")
                                .append(type)
                                .append("_")
                                .append(driverIdSegments[0])
                                .append(".xml");
                            File driverFile = new File(configFile.getParent(), builder.toString());
                            if (driverFile.exists()) {
                                Document driverTypeDocument = XMLUtils.parseDocument(driverFile);
                                Element driverTypeDocumentElement = driverTypeDocument.getDocumentElement();
                                String name = XMLUtils.getChildElementBody(driverTypeDocumentElement, "Label");
                                String sampleURL = XMLUtils.getChildElementBody(driverTypeDocumentElement, "URLFormat");
                                String identifier = XMLUtils.getChildElementBody(driverTypeDocumentElement, "Identifier");
                                if (!CommonUtils.isEmpty(name) && !CommonUtils.isEmpty(sampleURL)) {
                                    DriverDescriptor driverDescriptor = getDriverByName(name);
                                    if (driverDescriptor != null) {
                                        driver = new ImportDriverInfo(identifier, driverDescriptor.getName(), sampleURL,
                                            driverDescriptor.getDriverClassName());
                                        adaptSampleUrl(driver);
                                    } else {
                                        log.error("Driver descriptor not found for: " + driverName);
                                    }
                                }
                            } else {
                                if (!CommonUtils.isEmpty(driverName)) {
                                    DriverDescriptor driverDescriptor = getDriverByName(driverName);
                                    if (driverDescriptor != null) {
                                        driver = new ImportDriverInfo(driverDescriptor.getId(),
                                            driverDescriptor.getName(),
                                            driverDescriptor.getSampleURL(),
                                            driverDescriptor.getDriverClassName());
                                        adaptSampleUrl(driver);
                                    } else {
                                        log.error("Driver descriptor not found for: " + driverName);
                                    }
                                } else {
                                    log.error("Driver descriptor not found by path: " + driverFile.getAbsolutePath());
                                }
                            }
                            Element sshServers = XMLUtils.getChildElement(dbElement, "SshServers");
                            if (sshServers != null) {
                                for (Element sshServer : XMLUtils.getChildElementList(sshServers, "SshServer")) {
                                    String enabled = XMLUtils.getChildElementBody(sshServer, "Enabled");
                                    if (enabled.equals("true")) {
                                        sshConfiguration = sshServerConfigurations.get(sshServer.getAttribute("id"));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (CommonUtils.isEmpty(url) && CommonUtils.isEmpty(hostName)) {
                        hostName = "localhost"; //$NON-NLS-1$
                    }
                    if (!CommonUtils.isEmpty(alias) && !CommonUtils.isEmpty(driverName)
                        && (!CommonUtils.isEmpty(url) || !CommonUtils.isEmpty(hostName))) {
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
                        if (sshConfiguration != null) {
                            NetworkHandlerDescriptor sslHD = NetworkHandlerRegistry.getInstance().getDescriptor("ssh_tunnel");
                            DBWHandlerConfiguration sshHandler = new DBWHandlerConfiguration(sslHD, null);
                            sshHandler.setUserName(sshConfiguration.getSshUserid());
                            sshHandler.setSavePassword(true);
                            sshHandler.setProperty(DBWHandlerConfiguration.PROP_HOST, sshConfiguration.getSshHost());
                            sshHandler.setProperty(DBWHandlerConfiguration.PROP_PORT, sshConfiguration.getSshPort());
                            if (sshConfiguration.getAuthenticationType() == SSHConstants.AuthType.PUBLIC_KEY) {
                                sshHandler.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PUBLIC_KEY);
                                sshHandler.setProperty(SSHConstants.PROP_KEY_PATH, sshConfiguration.getSshPrivateKeyFile());
                            } else {
                                sshHandler.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PASSWORD);
                            }
                            sshHandler.setProperty(SSHConstants.PROP_IMPLEMENTATION, sshConfiguration.getSshImplementationType());
                            sshHandler.setEnabled(true);
                            connectionInfo.addNetworkHandler(sshHandler);
                        }
                        importData.addConnection(connectionInfo);
                    } else {
                        log.error("Can not extract data for: " + driverName);
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
    protected String substituteDriverName(String driverName) {
        String driversubstitutedName = dbvis2dbeaverDriverNames.get(driverName);
        if (driversubstitutedName == null) {
            driversubstitutedName = driverName;
        }
        return driversubstitutedName;
    }

}
