/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class SQLServerLoginPasswordManager implements DBAUserPasswordManager {

    private final SQLServerDataSource dataSource;

    SQLServerLoginPasswordManager(SQLServerDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void changeUserPassword(@NotNull DBRProgressMonitor monitor, @NotNull String loginName, @NotNull String newPassword, @NotNull String oldPassword) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Change user login password")) {
            session.enableLogging(false);
            JDBCUtils.executeSQL(session, "ALTER LOGIN " + DBUtils.getQuotedIdentifier(dataSource, loginName) + " WITH PASSWORD =" + SQLUtils.quoteString(dataSource, CommonUtils.notEmpty(newPassword)) +
                " OLD_PASSWORD =" + SQLUtils.quoteString(dataSource, CommonUtils.notEmpty(oldPassword)));
        } catch (SQLException e) {
            throw new DBCException(getPasswordPolicyErrorMessage(e), e);
        }
    }

    /**
     * Changes an expired password on the server by connecting with admin credentials
     * and executing ALTER LOGIN. This is necessary because the MSSQL JDBC driver does not
     * support changing expired passwords during login (no newPassword connection property),
     * and SQL Server completely blocks connections from users with expired passwords.
     */
    void changeExpiredPassword(
        @NotNull String connectionUrl,
        @NotNull Driver driverInstance,
        @NotNull DBPConnectionConfiguration connectionInfo,
        @NotNull String adminUser,
        @NotNull String adminPassword,
        @NotNull String loginName,
        @NotNull String newPassword
    ) throws DBException {
        Properties adminProps = new Properties();
        adminProps.put("user", adminUser);
        adminProps.put("password", CommonUtils.notEmpty(adminPassword));
        adminProps.put("integratedSecurity", "false");

        // Mirror the encrypt/SSL logic from SQLServerDataSource.getAllConnectionProperties:
        // If trust certificate is enabled, set it for the admin connection too
        boolean trustCertificate = CommonUtils.getBoolean(
            connectionInfo.getProviderProperty(SQLServerConstants.PROP_SSL_TRUST_SERVER_CERTIFICATE),
            false);
        if (trustCertificate) {
            adminProps.put(SQLServerConstants.PROP_DRIVER_TRUST_SERVER_CERTIFICATE, Boolean.TRUE.toString());
        }

        // If SSL handler is configured, use encrypt=true and copy SSL properties; otherwise default to encrypt=false
        DBWHandlerConfiguration sslConfig = connectionInfo.getHandler(SQLServerConstants.HANDLER_SSL);
        if (sslConfig != null && sslConfig.isEnabled()) {
            adminProps.put("encrypt", "true");

            // Copy trustStore settings (mirrors SQLServerDataSource.initSSL logic)
            String keystoreFileProp;
            if (CommonUtils.isEmpty(sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD))) {
                keystoreFileProp = sslConfig.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE);
            } else {
                keystoreFileProp = sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_KEYSTORE);
            }
            if (!CommonUtils.isEmpty(keystoreFileProp)) {
                adminProps.put("trustStore", keystoreFileProp);
            }

            String keystorePasswordProp;
            if (CommonUtils.isEmpty(sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD))) {
                keystorePasswordProp = sslConfig.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE_PASSWORD);
            } else {
                keystorePasswordProp = sslConfig.getPassword();
            }
            if (!CommonUtils.isEmpty(keystorePasswordProp)) {
                adminProps.put("trustStorePassword", keystorePasswordProp);
            }

            String hostnameProp = sslConfig.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE_HOSTNAME);
            if (!CommonUtils.isEmpty(hostnameProp)) {
                adminProps.put("hostNameInCertificate", hostnameProp);
            }
        } else {
            adminProps.put("encrypt", "false");
        }

        try (Connection adminConn = driverInstance.connect(connectionUrl, adminProps)) {
            if (adminConn == null) {
                throw new DBException("Failed to establish admin connection");
            }
            String sql = "ALTER LOGIN " + DBUtils.getQuotedIdentifier(dataSource, loginName)
                + " WITH PASSWORD = " + SQLUtils.quoteString(dataSource, newPassword);
            try (java.sql.Statement stmt = adminConn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (DBException e) {
            throw e;
        } catch (SQLException e) {
            throw new DBCException(getPasswordPolicyErrorMessage(e), e);
        } catch (Exception e) {
            throw new DBException("Failed to change password via admin connection", e);
        }
    }

    @NotNull
    static String getPasswordPolicyErrorMessage(@NotNull SQLException e) {
        String detail = switch (e.getErrorCode()) {
            case SQLServerConstants.EC_PASSWORD_TOO_SHORT -> ": password is too short";
            case SQLServerConstants.EC_PASSWORD_TOO_LONG -> ": password is too long";
            case SQLServerConstants.EC_PASSWORD_NOT_COMPLEX -> ": password is not complex enough";
            case SQLServerConstants.EC_PASSWORD_NOT_SATISFACTORY -> ": password does not meet policy requirements";
            case SQLServerConstants.EC_PASSWORD_RECENTLY_USED -> ": password was recently used";
            case SQLServerConstants.EC_PASSWORD_FILTER_REJECTED -> ": password was rejected by a password filter";
            default -> "";
        };
        return SQLServerMessages.password_change_error_message + detail;
    }
}
