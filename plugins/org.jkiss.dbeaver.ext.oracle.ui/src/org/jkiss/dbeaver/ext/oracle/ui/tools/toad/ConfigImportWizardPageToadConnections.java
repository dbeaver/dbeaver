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
package org.jkiss.dbeaver.ext.oracle.ui.tools.toad;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.config.migration.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionRole;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIActivator;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigImportWizardPageToadConnections extends ConfigImportWizardPage {

    private final ImportDriverInfo oraDriver;

    protected ConfigImportWizardPageToadConnections() {
        super("Toad");
        setTitle("Toad");
        setDescription("Import Oracle Toad connections");
        setImageDescriptor(OracleUIActivator.getImageDescriptor("icons/toad_icon_big.png"));

        oraDriver = new ImportDriverInfo(
            null,
            "Oracle",
            "jdbc:oracle:thin:@{host}[:{port}]/{database}",
            "oracle.jdbc.OracleDriver");
    }

    @Override
    protected void loadConnections(ImportData importData) {
        importData.addDriver(oraDriver);
        ConfigImportWizardToad wizard = (ConfigImportWizardToad) getWizard();
        File inputFile = wizard.getInputFile();
        try (InputStream is = new FileInputStream(inputFile)) {
            try (Reader reader = new InputStreamReader(is)) {
                parseConnections(importData, reader);
            }
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
        }
    }

    private void parseConnections(@NotNull ImportData importData, @NotNull Reader reader) throws DBException {
        try {
            Document configDocument = XMLUtils.parseDocument(reader);

            for (Element refElement : XMLUtils.getChildElementList(configDocument.getDocumentElement(), "ConnectionHierarchy")) {
                for (Element dbElement : XMLUtils.getChildElementList(refElement, "DbPlatform")) {
                    if ("Oracle".equals(dbElement.getAttribute("name"))) {
                        for (Element consElement : XMLUtils.getChildElementList(dbElement, "Connections")) {
                            for (Element conElement : XMLUtils.getChildElementList(consElement, "Connection")) {
                                if ("Oracle".equals(conElement.getAttribute("type"))) {
                                    Map<String, String> attrMap = new LinkedHashMap<>();
                                    boolean isServiceType = false;
                                    for (Element attrElement : XMLUtils.getChildElementList(conElement)) {
                                        String elementBody = XMLUtils.getElementBody(attrElement);
                                        if (CommonUtils.isEmpty(elementBody)) {
                                            continue;
                                        }
                                        attrMap.put(attrElement.getTagName(), elementBody.trim());
                                        if ("ConnectionType".equals(attrElement.getTagName())) {
                                            isServiceType = "True".equals(attrElement.getAttribute("ServiceName"));
                                        }
                                    }

                                    String host = attrMap.get("Host");
                                    String port = attrMap.get("Port");
                                    String sid = attrMap.get("Sid");
                                    //String instanceName = attrMap.get("InstanceName");
                                    String user = attrMap.get("User");
                                    String alias = attrMap.get("Alias");
                                    String role = attrMap.get("ConnectionMode");
                                    String guid = attrMap.get("GUID");

                                    String sshHost = attrMap.get("SSHHost");

                                    DBWHandlerConfiguration sshHandler = null;
                                    if (!CommonUtils.isEmpty(sshHost)) {
                                        if (CommonUtils.isEmpty(host)) {
                                            host = sshHost;
                                        }
                                        String sshPort = attrMap.get("SSHPort");
                                        String sshUser = attrMap.get("SSHUser");
                                        String sshPassword = attrMap.get("SSHPassword");
                                        String sshPrivateKey = attrMap.get("SSHPrivateKey");
                                        String sshLocalPort = attrMap.get("SSHLocalPort");
                                        String sshRemotePort = attrMap.get("SSHRemotePort");
                                        String sshRemoteHost = attrMap.get("SSHRemoteHost");

                                        NetworkHandlerDescriptor sslHD = NetworkHandlerRegistry.getInstance().getDescriptor("ssh_tunnel");
                                        sshHandler = new DBWHandlerConfiguration(sslHD, null);
                                        sshHandler.setUserName(sshUser);
                                        sshHandler.setSavePassword(true);
                                        sshHandler.setProperty(DBWHandlerConfiguration.PROP_HOST, sshHost);
                                        sshHandler.setProperty(DBWHandlerConfiguration.PROP_PORT, sshPort);

                                        if (!CommonUtils.isEmpty(sshPrivateKey)) {
                                            sshHandler.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PUBLIC_KEY);
                                            sshHandler.setProperty(SSHConstants.PROP_KEY_PATH, sshPrivateKey);
                                        } else {
                                            sshHandler.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PASSWORD);
                                            sshHandler.setPassword(sshPassword);
                                        }
                                        sshHandler.setProperty(SSHConstants.PROP_IMPLEMENTATION, "sshj");
                                        sshHandler.setEnabled(true);
                                    }

                                    if (CommonUtils.isEmpty(alias)) {
                                        alias = sid;
                                    }
                                    if (CommonUtils.isEmpty(alias)) {
                                        alias = host;
                                    }
                                    if (CommonUtils.isEmpty(alias)) {
                                        alias = sshHost;
                                    }
                                    if (CommonUtils.isEmpty(alias)) {
                                        alias = guid;
                                    }

                                    ImportConnectionInfo connectionInfo = new ImportConnectionInfo(
                                        oraDriver,
                                        null,
                                        alias,
                                        null,
                                        host,
                                        port,
                                        sid,
                                        user,
                                        null);
                                    if (sshHandler != null) {
                                        connectionInfo.addNetworkHandler(sshHandler);
                                    }
                                    connectionInfo.setProviderProperty(
                                        OracleConstants.PROP_SID_SERVICE,
                                        isServiceType ?
                                            OracleConnectionType.SERVICE.name() :
                                            OracleConnectionType.SID.name());
                                    if (!CommonUtils.isEmpty(role)) {
                                        if ("Default".equals(role)) {
                                            role = OracleConnectionRole.NORMAL.name();
                                        }
                                        connectionInfo.setProviderProperty(
                                            OracleConstants.PROP_INTERNAL_LOGON,
                                            CommonUtils.valueOf(
                                                OracleConnectionRole.class,
                                                role,
                                                OracleConnectionRole.NORMAL).getTitle());
                                    }
                                    importData.addConnection(connectionInfo);
                                }
                            }
                        }
                    }
                }
            }
        } catch (XMLException e) {
            throw new DBException("Configuration parse error: " + e.getMessage(), e);
        }
    }
}
