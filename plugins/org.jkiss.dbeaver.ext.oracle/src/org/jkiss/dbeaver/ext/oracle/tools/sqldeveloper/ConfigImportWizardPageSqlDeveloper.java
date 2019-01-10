/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.oracle.tools.sqldeveloper;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.ext.oracle.Activator;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedHashMap;
import java.util.Map;


public class ConfigImportWizardPageSqlDeveloper extends ConfigImportWizardPage {

    public static final String SQLD_HOME_FOLDER = "SQL Developer";
    public static final String SQLD_CONFIG_FILE = "connections.xml";

    public static final String SQLD_SYSCONFIG_FOLDER = "system";
    public static final String SQLD_CONNECTIONS_FOLDER = "o.jdeveloper.db.connection.";

    private final ImportDriverInfo oraDriver;

    protected ConfigImportWizardPageSqlDeveloper()
    {
        super("SQLDeveloper");
        setTitle("SQL Developer");
        setDescription("Import Oracle SQL Developer connections");
        setImageDescriptor(Activator.getImageDescriptor("icons/sqldeveloper_big.png"));

        oraDriver = new ImportDriverInfo(null, "Oracle", "jdbc:oracle:thin:@{host}[:{port}]/{database}", "oracle.jdbc.OracleDriver");
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException {

        importData.addDriver(oraDriver);

        File homeFolder = RuntimeUtils.getUserHomeDir();
        File sqlDevHome = new File(homeFolder, "AppData/Roaming/" + SQLD_HOME_FOLDER);
        if (!sqlDevHome.exists()) {
            sqlDevHome = new File(homeFolder, "Application Data/" + SQLD_HOME_FOLDER);
            if (!sqlDevHome.exists()) {
                sqlDevHome = new File(homeFolder, ".sqldeveloper"); // On Linux
                if (!sqlDevHome.exists()) {
                    throw new DBException("SQL Developer installation not found");
                }
            }
        }
        final File[] sysConfFolders = sqlDevHome.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(SQLD_SYSCONFIG_FOLDER);
            }
        });
        if (sysConfFolders == null || sysConfFolders.length == 0) {
            throw new DBException("SQL Developer config not found");
        }
        for (File sysConfFolder : sysConfFolders) {
            final File[] connectionFolders = sysConfFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(SQLD_CONNECTIONS_FOLDER);
                }
            });
            if (connectionFolders == null || connectionFolders.length != 1) {
                continue;
            }
            final File connectionFolder = connectionFolders[0];
            final File connectionsFile = new File(connectionFolder, SQLD_CONFIG_FILE);
            if (!connectionsFile.exists()) {
                continue;
            }
            parseConnections(connectionsFile, importData);
        }
    }

    private void parseConnections(File connectionsFile, ImportData importData) throws DBException {
        try {
            Document configDocument = XMLUtils.parseDocument(connectionsFile);

            for (Element refElement : XMLUtils.getChildElementList(configDocument.getDocumentElement(), "Reference")) {
                final String conName = refElement.getAttribute("name");
                if (CommonUtils.isEmpty(conName)) {
                    continue;
                }

                final Map<String, String> propsMap = new LinkedHashMap<>();
                final Element refAddressesElement = XMLUtils.getChildElement(refElement, "RefAddresses");
                if (refAddressesElement != null) {
                    for (Element refAddr : XMLUtils.getChildElementList(refAddressesElement, "StringRefAddr")) {
                        String addrType = refAddr.getAttribute("addrType");
                        String addrContent = XMLUtils.getChildElementBody(refAddr, "Contents");
                        if (!CommonUtils.isEmpty(addrType) && !CommonUtils.isEmpty(addrContent)) {
                            propsMap.put(addrType, addrContent);
                        }
                    }
                }
                String host = propsMap.get("hostname");
                String port = propsMap.get("port");
                String sid = propsMap.get("sid");
                String serviceName = propsMap.get("serviceName");
                String user = propsMap.get("user");
                String role = propsMap.get("role");
                String osAuth = propsMap.get("OS_AUTHENTICATION");
                String url = propsMap.get("customUrl");

                if (CommonUtils.isEmpty(host) && CommonUtils.isEmpty(url)) {
                    continue;
                }
                String dbName = CommonUtils.isEmpty(sid) ? serviceName : sid;
                ImportConnectionInfo connectionInfo = new ImportConnectionInfo(oraDriver, null, conName, url, host, port, dbName, user, null);
                if (!CommonUtils.isEmpty(sid)) {
                    connectionInfo.setProviderProperty(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.SID.name());
                } else if (!CommonUtils.isEmpty(serviceName)) {
                    connectionInfo.setProviderProperty(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.SERVICE.name());
                }
                if (CommonUtils.toBoolean(osAuth)) {
                    connectionInfo.setUser(OracleConstants.OS_AUTH_PROP);
                }
                if (!CommonUtils.isEmpty(role)) {
                    connectionInfo.setProviderProperty(OracleConstants.PROP_INTERNAL_LOGON, role);
                }
                importData.addConnection(connectionInfo);
            }
        } catch (XMLException e) {
            throw new DBException("Configuration parse error: " + e.getMessage());
        }
    }

}
