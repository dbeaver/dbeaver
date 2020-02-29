/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oracle.ui.tools.sqldeveloper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIActivator;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ConfigImportWizardPageSqlDeveloper extends ConfigImportWizardPage {

    public static final String SQLD_HOME_FOLDER = "SQL Developer";
    public static final String SQLD_CONFIG_FILE = "connections.xml";
    public static final String SQLD_CONFIG_JSON_FILE = "connections.json";

    public static final String SQLD_SYSCONFIG_FOLDER = "system";
    public static final String SQLD_CONNECTIONS_FOLDER = "o.jdeveloper.db.connection";

    private static final Log log = Log.getLog(ConfigImportWizardPageSqlDeveloper.class);

    private final ImportDriverInfo oraDriver;

    protected ConfigImportWizardPageSqlDeveloper() {
        super("SQLDeveloper");
        setTitle("SQL Developer");
        setDescription("Import Oracle SQL Developer connections");
        setImageDescriptor(OracleUIActivator.getImageDescriptor("icons/sqldeveloper_big.png"));

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
                    throw new DBException(OracleUIMessages.config_import_wizard_page_sql_developer_label_installation_not_found);
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
            final File connectionsJsonFile = new File(connectionFolder, SQLD_CONFIG_JSON_FILE);
            final File connectionsFile = new File(connectionFolder, SQLD_CONFIG_FILE);
            if (connectionsFile.exists()) {
                parseConnections(connectionsFile, importData);
            } else if (connectionsJsonFile.exists()) {
                parseJsonConnections(connectionsJsonFile, importData);
            }

        }
    }

    public static class ConnectionDescription {
        @SerializedName("name")
        private String name;
        @SerializedName("type")
        private String type;
        @SerializedName("info")
        private ConnectionInfo info;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public ConnectionInfo getInfo() {
            return info;
        }

        public void setInfo(ConnectionInfo info) {
            this.info = info;
        }
    }

    public static class ConnectionList {
        @SerializedName("connections")
        private List<ConnectionDescription> connections = new ArrayList();

        public List<ConnectionDescription> getConnections() {
            return connections;
        }
    }

    public class ConnectionInfo {
        @SerializedName("role")
        private String role;
        @SerializedName("hostname")
        private String hostname;
        @SerializedName("port")
        private String port;
        @SerializedName("sid")
        private String sid;
        @SerializedName("serviceName")
        private String serviceName;
        @SerializedName("user")
        private String user;
        @SerializedName("customUrl")
        private String customUrl;
        @SerializedName("OS_AUTHENTICATION")
        private String OsAuth;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getHost() {
            return hostname;
        }

        public void setHost(String hostname) {
            this.hostname = hostname;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getSID() {
            return sid;
        }

        public void setSID(String sid) {
            this.sid = sid;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getUrl() {
            return customUrl;
        }

        public void setUrl(String customUrl) {
            this.customUrl = customUrl;
        }

        public String getOsAuth() {
            return OsAuth;
        }

        public void setOsAuth(String OS_AUTHENTICATION) {
            this.OsAuth = OS_AUTHENTICATION;
        }

    }

    private void parseJsonConnections(File connectionsFile, ImportData importData) throws JsonSyntaxException {
        try {
            Gson gson = new GsonBuilder().create();
            try (Reader br = new BufferedReader(new FileReader(connectionsFile))) {
                ConnectionList connResult = gson.fromJson(br, ConnectionList.class);
                for (ConnectionDescription conn : connResult.getConnections()) {
                    if (CommonUtils.isEmpty(conn.getName())) {
                        continue;
                    }

                    ConnectionInfo info = conn.getInfo();

                    if (CommonUtils.isEmpty(info.getHost()) && CommonUtils.isEmpty(info.getUrl())) {
                        continue;
                    }

                    String dbName = CommonUtils.isEmpty(info.getSID()) ? info.getServiceName() : info.getSID();

                    ImportConnectionInfo connectionInfo = new ImportConnectionInfo(oraDriver, null, conn.getName(), info.getUrl(), info.getHost(), info.getPort(), dbName, info.getHost(), null);
                    if (!CommonUtils.isEmpty(info.getSID())) {
                        connectionInfo.setProviderProperty(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.SID.name());
                    } else if (!CommonUtils.isEmpty(info.getServiceName())) {
                        connectionInfo.setProviderProperty(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.SERVICE.name());
                    }
                    if (CommonUtils.toBoolean(info.getOsAuth())) {
                        connectionInfo.setUser(OracleConstants.OS_AUTH_PROP);
                    }
                    if (!CommonUtils.isEmpty(info.getRole())) {
                        connectionInfo.setProviderProperty(OracleConstants.PROP_INTERNAL_LOGON, info.getRole());
                    }
                    if (!CommonUtils.isEmpty(conn.getType())) {
                        connectionInfo.setProviderProperty(OracleConstants.PROP_CONNECTION_TYPE, conn.getType());
                    }

                    importData.addConnection(connectionInfo);
                }
            }
        } catch (Exception e) {
            log.error("Configuration parse error", e);
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
