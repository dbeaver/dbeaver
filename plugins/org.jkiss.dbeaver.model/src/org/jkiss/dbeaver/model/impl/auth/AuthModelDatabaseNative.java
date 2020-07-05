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

package org.jkiss.dbeaver.model.impl.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.auth.DBAAuthModel;
import org.jkiss.dbeaver.model.auth.DBAUserCredentialsProvider;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Properties;

/**
 * Database native auth model.
 *
 * No-op model. Leaves all configuration as is.
 */
public class AuthModelDatabaseNative implements DBAAuthModel<AuthModelDatabaseNativeCredentials> {

    public static final String ID = "native";

    public static final AuthModelDatabaseNative INSTANCE = new AuthModelDatabaseNative();

    @Override
    public void initCredentials(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull AuthModelDatabaseNativeCredentials credentials) throws DBException {
        if (dataSource instanceof DBAUserCredentialsProvider) {
            credentials.setUserName(((DBAUserCredentialsProvider) dataSource).getConnectionUserName(configuration));
            credentials.setUserPassword(((DBAUserCredentialsProvider) dataSource).getConnectionUserPassword(configuration));
        } else {
            credentials.setUserName(configuration.getUserName());
            credentials.setUserPassword(configuration.getUserPassword());
        }
        boolean allowsEmptyPassword = dataSource.getDriver().isAllowsEmptyPassword();
        if (credentials.getUserPassword() == null && allowsEmptyPassword) {
            credentials.setUserPassword("");
        }
    }

    @Override
    public void initAuthentication(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connectProps) throws DBException {
        String userName;
        String userPassword;
        if (dataSource instanceof DBAUserCredentialsProvider) {
            userName = ((DBAUserCredentialsProvider) dataSource).getConnectionUserName(configuration);
            userPassword = ((DBAUserCredentialsProvider) dataSource).getConnectionUserPassword(configuration);
        } else {
            userName = configuration.getUserName();
            userPassword = configuration.getUserPassword();
        }
        boolean allowsEmptyPassword = dataSource.getContainer().getDriver().isAllowsEmptyPassword();
        if (userPassword == null && allowsEmptyPassword) {
            userPassword = "";
        }

        if (!CommonUtils.isEmpty(userName)) {
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, userName);
        }
        if (!CommonUtils.isEmpty(userPassword) || (allowsEmptyPassword && !CommonUtils.isEmpty(userName))) {
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, userPassword);
        }
    }

    @Override
    public void endAuthentication(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties) {

    }

}
