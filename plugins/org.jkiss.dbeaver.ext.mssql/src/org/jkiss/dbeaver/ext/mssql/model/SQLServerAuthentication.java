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

import org.jkiss.dbeaver.ext.mssql.SQLServerMessages;

import java.util.Map;

/**
 * SQLServerAuthentication
 */
public enum SQLServerAuthentication {

    SQL_SERVER_PASSWORD(SQLServerMessages.authentication_sql_server_title, SQLServerMessages.authentication_sql_server_description, true, true),
    WINDOWS_INTEGRATED(SQLServerMessages.authentication_windows_title, SQLServerMessages.authentication_windows_description, true, false),
    AD_PASSWORD(SQLServerMessages.authentication_ad_password_title, SQLServerMessages.authentication_ad_password_description, false, true),
    AD_INTEGRATED(SQLServerMessages.authentication_ad_integrated_title, SQLServerMessages.authentication_ad_integrated_description, false, false),
    KERBEROS_INTEGRATED(SQLServerMessages.authentication_kerberos_title, SQLServerMessages.authentication_kerberos_description, false, true),
    OTHER(SQLServerMessages.authentication_other_title, SQLServerMessages.authentication_other_description, true, true)
    ;

    private final String title;
    private final String description;
    private final boolean supportsJTDS;
    private final boolean allowsPassword;

    SQLServerAuthentication(String title, String description, boolean supportsJTDS, boolean allowsPassword) {
        this.title = title;
        this.description = description;
        this.supportsJTDS = supportsJTDS;
        this.allowsPassword = allowsPassword;
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

    interface AuthInitializer {

        void initializeAuthentication(Map<String, String> properties);
    }
}
