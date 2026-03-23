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
package org.jkiss.dbeaver.ext.dynamodb;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dynamodb.model.DynamoDBDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class DynamoDBDataSourceProvider implements DBPDataSourceProvider {

    @Override
    public void init(@NotNull DBPPlatform platform) {
    }

    @Override
    public long getFeatures() {
        return FEATURE_NONE;
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getConnectionProperties(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDriver driver,
            @NotNull DBPConnectionConfiguration connectionInfo) throws DBException {
        return new DBPPropertyDescriptor[0];
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDataSourceContainer container) throws DBException {
        return new DynamoDBDataSource(monitor, container);
    }

    @NotNull
    @Override
    public String getConnectionURL(
            @NotNull DBPDriver driver,
            @NotNull DBPConnectionConfiguration connectionInfo) {
        String region = connectionInfo.getProviderProperty(DynamoDBConstants.PROP_REGION);
        if (CommonUtils.isEmpty(region)) {
            region = connectionInfo.getServerName();
        }
        if (CommonUtils.isEmpty(region)) {
            region = DynamoDBConstants.DEFAULT_REGION;
        }
        return "dynamodb://" + region;
    }

    @Override
    public boolean providesDriverClasses() {
        return false;
    }
}
