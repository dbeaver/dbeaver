/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.mssql.auth.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.util.Properties;

/**
 * SQLServerAuthentication.
 *
 * Deprecated. Replaced by auth models.
 */
@Deprecated
public enum SQLServerAuthentication {

    SQL_SERVER_PASSWORD(SQLServerMessages.authentication_sql_server_title, SQLServerMessages.authentication_sql_server_description, true, true, true, SQLServerAuthModelDatabase.ID),
    WINDOWS_INTEGRATED(SQLServerMessages.authentication_windows_title, SQLServerMessages.authentication_windows_description, true, false, false, SQLServerAuthModelWindows.ID),
    NTLM("NTLM", "NTLM authentication", true, true, true, SQLServerAuthModelNTLM.ID),
    AD_PASSWORD(SQLServerMessages.authentication_ad_password_title, SQLServerMessages.authentication_ad_password_description, false, true, true, SQLServerAuthModelADPassword.ID),
    AD_MSI(SQLServerMessages.authentication_ad_msi_title, SQLServerMessages.authentication_ad_msi_description, false, true, false, SQLServerAuthModelMSI.ID),
    AD_INTERACTIVE(SQLServerMessages.authentication_ad_interactive_title, SQLServerMessages.authentication_ad_interactive_description, false, true, false, SQLServerAuthModelMFA.ID),
    AD_INTEGRATED(SQLServerMessages.authentication_ad_integrated_title, SQLServerMessages.authentication_ad_integrated_description, false, false, false, SQLServerAuthModelADIntegrated.ID),
    KERBEROS_INTEGRATED(SQLServerMessages.authentication_kerberos_title, SQLServerMessages.authentication_kerberos_description, false, false, false, SQLServerAuthModelKerberos.ID),
    OTHER(SQLServerMessages.authentication_other_title, SQLServerMessages.authentication_other_description, true, true, true, SQLServerAuthModelCustom.ID)
    ;

    private final String title;
    private final String description;
    private final boolean supportsJTDS;
    private final boolean allowsPassword;
    private final boolean allowsUserName;
    private final String replacedByAuthModelId;

    SQLServerAuthentication(String title, String description, boolean supportsJTDS, boolean allowsUserName, boolean allowsPassword, String replacedByAuthModelId) {
        this.title = title;
        this.description = description;
        this.supportsJTDS = supportsJTDS;
        this.allowsUserName = allowsUserName;
        this.allowsPassword = allowsPassword;
        this.replacedByAuthModelId = replacedByAuthModelId;
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

    public boolean isAllowsUserName() {
        return allowsUserName;
    }

    public boolean isAllowsPassword() {
        return allowsPassword;
    }

    public String getReplacedByAuthModelId() {
        return replacedByAuthModelId;
    }

    public interface AuthInitializer {

        void initializeAuthentication(DBPConnectionConfiguration connectionInfo, Properties properties) throws DBCException;
    }
}
