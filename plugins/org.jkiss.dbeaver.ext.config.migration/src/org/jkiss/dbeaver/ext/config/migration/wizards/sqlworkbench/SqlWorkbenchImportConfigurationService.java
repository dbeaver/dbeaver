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
package org.jkiss.dbeaver.ext.config.migration.wizards.sqlworkbench;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportConfigurationException;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.Reader;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlWorkbenchImportConfigurationService {
    private static final Log log = Log.getLog(SqlWorkbenchImportConfigurationService.class);

    public static final SqlWorkbenchImportConfigurationService INSTANCE = new SqlWorkbenchImportConfigurationService();

    private final Set<String> keyWords;
    //for matching like: db1.dev.dbeaver.com:22
    private final Pattern urlPattern = Pattern.compile("^(?<host>[a-zA-Z0-9.-]+):(?<port>\\d+)$");
    private final Set<String> kerberosDriverTypes = Set.of("ldap_kerberos", "ldap_sasl_kerberos");
    private ImportDriverInfo mysqlDriver;

    private SqlWorkbenchImportConfigurationService() {

        keyWords = Set.of("", "hostName", "password", "port", "schema", "sshCompressionLevel", "sshHost", "sshKeyFile",
            "sshPassword", "sshUserName", "sslCA", "sslCert", "sslCipher", "sslKey", "useSSL", "userName", "kerberosMode", "krb5",
            "krb5cache", "mysqlplugindir");

        String idMysql = "mysql8";
        DriverUtils.getAllDrivers().stream()
            .filter(dbpDriver -> dbpDriver.getId().equals(idMysql))
            .findAny()
            .ifPresentOrElse(dbpDriver -> mysqlDriver = new ImportDriverInfo(
                    dbpDriver.getId(),
                    dbpDriver.getName(),
                    dbpDriver.getSampleURL(),
                    dbpDriver.getDriverClassName()),
                () -> mysqlDriver = new ImportDriverInfo(
                    idMysql,
                    "MySQL",
                    "jdbc:mysql://{host}[:{port}]/[{database}]",
                    "com.mysql.cj.jdbc.Driver"));

    }

    public void importXML(ImportData importData, Reader reader) throws XMLException {
        Document document = XMLUtils.parseDocument(reader);
        Collection<Element> allElements = XMLUtils.getChildElementList(XMLUtils.getChildElementList(document.getDocumentElement())
            .iterator().next());
        for (Element conElement : allElements) {
            importData.addConnection(parseConnection(conElement));
        }
    }

    private ImportConnectionInfo parseConnection(Element conElement) {

        String alias = getElementValueOrEmptyString(conElement, "name");
        Element parameterValues = getElemByNameAndTagKeyValue(conElement, "parameterValues");
        if (parameterValues == null) {
            throw new ImportConfigurationException("Can't find parameterValues tag in the xml");
        }
        String hostName = getElementValueOrEmptyString(parameterValues, "hostName");
        String port = getElementValueOrEmptyString(parameterValues, "port");
        String userName = getElementValueOrEmptyString(parameterValues, "userName");


        ImportConnectionInfo connectionInfo = new ImportConnectionInfo(
            mysqlDriver,
            null,
            alias,
            null,
            hostName,
            port,
            null,
            userName,
            null);

        configureAuthType(conElement, parameterValues, connectionInfo);
        configureSsh(parameterValues, connectionInfo);
        configureSsl(parameterValues, connectionInfo);
        configureDriverProperties(parameterValues, connectionInfo);
        return connectionInfo;
    }

    private void configureAuthType(Element conElement, Element parameterValues, ImportConnectionInfo connectionInfo) {
        String driver = getElementValueOrEmptyString(conElement, "driver");
        if (driver == null) {
            log.warn("Can't find driver tag in the xml");
            return;
        }
        String[] split = driver.split("\\.");
        String driverModel = split[split.length - 1];
        if (kerberosDriverTypes.contains(driverModel)) {
            connectionInfo.setAuthModelId("mysql_krb5");
        }
        String krbPathConfig = getElementValueOrEmptyString(parameterValues, "krb5");
        if (CommonUtils.isNotEmpty(krbPathConfig)) {
            connectionInfo.setAuthProperty("krb5.conf.path", krbPathConfig);
        }
    }

    private void configureDriverProperties(Element parameterValues, ImportConnectionInfo connectionInfo) {
        for (Element element : XMLUtils.getChildElementList(parameterValues)) {
            if (!keyWords.contains(element.getAttribute("key"))) {
                String driverPropertyValue = XMLUtils.getElementBody(element);
                if (CommonUtils.isNotEmpty(driverPropertyValue)) {
                    connectionInfo.setProperty(element.getAttribute("key"), driverPropertyValue);
                }
            }
        }
    }

    private void configureSsl(Element parameterValues, ImportConnectionInfo connectionInfo) {
        String sslCA = getElementValueOrEmptyString(parameterValues, "sslCA");
        String sslCert = getElementValueOrEmptyString(parameterValues, "sslCert");
        String sslCipher = getElementValueOrEmptyString(parameterValues, "sslCipher");
        String sslKey = getElementValueOrEmptyString(parameterValues, "sslKey");
        NetworkHandlerDescriptor sslHD = NetworkHandlerRegistry.getInstance().getDescriptor("mysql_ssl");
        DBWHandlerConfiguration sslHandler = new DBWHandlerConfiguration(sslHD, null);
        sslHandler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT, sslCA);
        sslHandler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT, sslCert);
        sslHandler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, sslKey);
        sslHandler.setProperty("ssl.cipher.suites", sslCipher);

        String isSslOff = "0";
        if (isSslOff.equals(getElementValueOrEmptyString(parameterValues, "useSSL"))) {
            sslHandler.setEnabled(false);
        } else {
            sslHandler.setEnabled(true);
        }
        connectionInfo.addNetworkHandler(sslHandler);
    }

    private void configureSsh(Element parameterValues, ImportConnectionInfo connectionInfo) {

        String sshHost = getElementValueOrEmptyString(parameterValues, "sshHost");
        if (CommonUtils.isEmpty(sshHost)) {
            return;
        }
        String sshPort = null;
        Matcher matcher = urlPattern.matcher(sshHost);
        if (matcher.matches()) {
            sshHost = matcher.group("host");
            sshPort = matcher.group("port");
        }
        String sshKeyFile = getElementValueOrEmptyString(parameterValues, "sshKeyFile");
        String sshUserName = getElementValueOrEmptyString(parameterValues, "sshUserName");

        NetworkHandlerDescriptor sslHD = NetworkHandlerRegistry.getInstance().getDescriptor("ssh_tunnel");
        DBWHandlerConfiguration sshHandler = new DBWHandlerConfiguration(sslHD, null);
        sshHandler.setUserName(sshUserName);
        sshHandler.setSavePassword(true);
        sshHandler.setProperty(DBWHandlerConfiguration.PROP_HOST, sshHost);
        sshHandler.setProperty(DBWHandlerConfiguration.PROP_PORT, sshPort);

        if (!CommonUtils.isEmpty(sshKeyFile)) {
            sshHandler.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PUBLIC_KEY);
            sshHandler.setProperty(SSHConstants.PROP_KEY_PATH, sshKeyFile);
        } else {
            sshHandler.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PASSWORD);
        }
        sshHandler.setProperty(SSHConstants.PROP_IMPLEMENTATION, "sshj");
        sshHandler.setEnabled(true);
        connectionInfo.addNetworkHandler(sshHandler);
    }

    @Nullable
    private String getElementValueOrEmptyString(@Nullable Element conElement, @NotNull String keyValue) {
        if (conElement == null) {
            return "";
        }
        Element element = getElemByNameAndTagKeyValue(conElement, keyValue);
        if (element == null) {
            return "";
        }
        return XMLUtils.getElementBody(element);
    }

    @Nullable
    private Element getElemByNameAndTagKeyValue(@Nullable Element conElement, String keyValue) {

        if (conElement == null) {
            return null;
        }
        for (Element element : XMLUtils.getChildElementList(conElement)) {
            if (keyValue.equals(element.getAttribute("key"))) {
                return element;
            }
        }
        return null;
    }
}
