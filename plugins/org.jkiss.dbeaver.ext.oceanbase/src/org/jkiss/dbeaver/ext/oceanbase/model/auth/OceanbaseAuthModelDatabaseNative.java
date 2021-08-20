/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.model.auth;

import java.util.Properties;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNativeCredentials;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class OceanbaseAuthModelDatabaseNative extends AuthModelDatabaseNative<AuthModelDatabaseNativeCredentials> {

    public static final String ID = "oceanbase_native";

    @Override
    public Object initAuthentication(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource,
            @NotNull AuthModelDatabaseNativeCredentials credentials, @NotNull DBPConnectionConfiguration configuration,
            @NotNull Properties connProperties) throws DBException {
        String userName = configuration.getUserName();
        if (!CommonUtils.isEmpty(userName) && !userName.contains("@")) {
            userName += "@" + configuration.getServerName();
        }

        credentials.setUserName(userName);
        return super.initAuthentication(monitor, dataSource, credentials, configuration, connProperties);
    }

}
