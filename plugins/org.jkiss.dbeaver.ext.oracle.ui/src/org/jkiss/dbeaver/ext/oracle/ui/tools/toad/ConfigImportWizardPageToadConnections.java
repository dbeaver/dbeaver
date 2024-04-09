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
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIActivator;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.*;
import java.util.Collection;

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

            for (Element refElement : XMLUtils.getChildElementList(configDocument.getDocumentElement())) {
                if ("ConnectionHierarchy".equals(refElement.getNodeName())) {
                    Node firstChild = refElement.getFirstChild();
                    if (firstChild != null) {
                        Node nextSibling = firstChild.getNextSibling();
                        if (nextSibling != null) {
                            NamedNodeMap attributes = nextSibling.getAttributes();
                            if (attributes != null) {
                                Node name = attributes.getNamedItem("name");
                                if (name != null && "Oracle".equals(name.getNodeValue())) {
                                    Node nextSiblingFirstChild = nextSibling.getFirstChild();
                                    if (nextSiblingFirstChild != null) {
                                        Node childNextSibling = nextSiblingFirstChild.getNextSibling();
                                        if (childNextSibling != null && "Connections".equals(childNextSibling.getNodeName())) {
                                            Collection<Element> elementList = XMLUtils.getChildElementList((Element) childNextSibling);
                                            for (Element element : elementList) {
                                                // Doesn't work starting here
                                                element.getAttribute("Server");
                                                element.getAttribute("Host");
                                                element.getAttribute("Port");
                                                element.getAttribute("Sid");
                                                // <ConnectionType ServiceName="True">Direct</ConnectionType> - if Service Name used, not SID
                                                // <ConnectionType ServiceName="False">Direct</ConnectionType> - this or nothing if SIT used, not Service Name
                                                element.getAttribute("ConnectionMode");
                                                // <ConnectionMode>SYSOPER</ConnectionMode>
                                                // <ConnectionMode>SYSDBA</ConnectionMode>
                                                // <ConnectionMode>Default</ConnectionMode>
                                                // TOAD has different connection modes including SysASM and others. But these are three which we use

                                            }

                                            /*ImportConnectionInfo connectionInfo = new ImportConnectionInfo(oraDriver, null, conName, "url", host, port, dbName, user, null);
                                            if (!CommonUtils.isEmpty(sid)) {
                                                connectionInfo.setProviderProperty(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.SID.name());
                                            } else if (!CommonUtils.isEmpty(serviceName)) {
                                                connectionInfo.setProviderProperty(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.SERVICE.name());
                                            }
                                            if (!CommonUtils.isEmpty(role)) {
                                                connectionInfo.setProviderProperty(
                                                    OracleConstants.PROP_INTERNAL_LOGON,
                                                    CommonUtils.valueOf(OracleConnectionRole.class, role, OracleConnectionRole.NORMAL).getTitle());
                                            }
                                            importData.addConnection(connectionInfo);*/
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (XMLException e) {
            throw new DBException("Configuration parse error: " + e.getMessage());
        }
    }
}
