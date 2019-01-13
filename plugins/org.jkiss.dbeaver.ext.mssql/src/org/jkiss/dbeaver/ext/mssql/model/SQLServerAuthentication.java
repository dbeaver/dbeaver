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
package org.jkiss.dbeaver.ext.mssql.model;

import org.ietf.jgss.*;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.utils.CommonUtils;

import java.util.Properties;

/**
 * SQLServerAuthentication
 */
public enum SQLServerAuthentication {

    SQL_SERVER_PASSWORD(SQLServerMessages.authentication_sql_server_title, SQLServerMessages.authentication_sql_server_description, true, true, (connectionInfo, properties) -> {
        properties.put(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY, String.valueOf(false));
        //properties.put(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION, SQLServerConstants.AUTH_SQL_SERVER_PASSWORD);

        setStandardCredentials(connectionInfo, properties);
    }),
    WINDOWS_INTEGRATED(SQLServerMessages.authentication_windows_title, SQLServerMessages.authentication_windows_description, true, false, (connectionInfo, properties) -> {
        properties.put(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY, String.valueOf(true));
    }),
    AD_PASSWORD(SQLServerMessages.authentication_ad_password_title, SQLServerMessages.authentication_ad_password_description, false, true, (connectionInfo, properties) -> {
        properties.put(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY, String.valueOf(false));
        properties.put(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION, SQLServerConstants.AUTH_ACTIVE_DIRECTORY_PASSWORD);

        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            properties.put("UserName", connectionInfo.getUserName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
            properties.put("Password", connectionInfo.getUserPassword());
        }
    }),
    AD_INTEGRATED(SQLServerMessages.authentication_ad_integrated_title, SQLServerMessages.authentication_ad_integrated_description, false, false, (connectionInfo, properties) -> {
        properties.put(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION, SQLServerConstants.AUTH_ACTIVE_DIRECTORY_INTEGRATED);
    }),
    KERBEROS_INTEGRATED(SQLServerMessages.authentication_kerberos_title, SQLServerMessages.authentication_kerberos_description, false, false, (connectionInfo, properties) -> {
        properties.put(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY, String.valueOf(true));
        properties.put(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION_SCHEME, SQLServerConstants.AUTH_SCHEME_KERBEROS);

        if (SQLServerConstants.USE_GSS) {
            // Disabled by default. Never really tested
            if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
                try {
                    GSSManager gssManager = GSSManager.getInstance();
                    GSSName name = gssManager.createName(connectionInfo.getUserName(), GSSName.NT_USER_NAME);
                    GSSCredential impersonatedUserCredential = gssManager.createCredential(name, GSSCredential.DEFAULT_LIFETIME, (Oid)null, GSSCredential.ACCEPT_ONLY);
                    properties.put("gsscredential", impersonatedUserCredential);
                } catch (GSSException e) {
                    throw new DBCException ("Error initializing GSS", e);
                }
            }
        }
    }),
    OTHER(SQLServerMessages.authentication_other_title, SQLServerMessages.authentication_other_description, true, true, (connectionInfo, properties) -> {
        // Set standard JDBC creds
        setStandardCredentials(connectionInfo, properties);
        // Nothing special
    })
    ;

    private static void setStandardCredentials(DBPConnectionConfiguration connectionInfo, Properties properties) {
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            properties.put(DBConstants.DATA_SOURCE_PROPERTY_USER, connectionInfo.getUserName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
            properties.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, connectionInfo.getUserPassword());
        }
    }

    private final String title;
    private final String description;
    private final boolean supportsJTDS;
    private final boolean allowsPassword;
    private final AuthInitializer initializer;

    SQLServerAuthentication(String title, String description, boolean supportsJTDS, boolean allowsPassword, AuthInitializer initializer) {
        this.title = title;
        this.description = description;
        this.supportsJTDS = supportsJTDS;
        this.allowsPassword = allowsPassword;
        this.initializer = initializer;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSupportsJTDS() {
        return supportsJTDS;
    }

    public boolean isAllowsPassword() {
        return allowsPassword;
    }

    public AuthInitializer getInitializer() {
        return initializer;
    }

    public interface AuthInitializer {

        void initializeAuthentication(DBPConnectionConfiguration connectionInfo, Properties properties) throws DBCException;
    }
}
