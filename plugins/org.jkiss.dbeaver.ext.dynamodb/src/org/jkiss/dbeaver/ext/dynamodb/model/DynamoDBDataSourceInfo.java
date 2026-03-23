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
package org.jkiss.dbeaver.ext.dynamodb.model;

import org.jkiss.dbeaver.model.impl.AbstractDataSourceInfo;
import org.osgi.framework.Version;

public class DynamoDBDataSourceInfo extends AbstractDataSourceInfo {

    @Override
    public String getDatabaseProductName() {
        return "Amazon DynamoDB";
    }

    @Override
    public String getDatabaseProductVersion() {
        return "AWS SDK 2.29.51";
    }

    @Override
    public Version getDatabaseVersion() {
        return new Version(0, 0, 0);
    }

    @Override
    public String getDriverName() {
        return "Amazon DynamoDB AWS SDK v2";
    }

    @Override
    public String getDriverVersion() {
        return "2.29.51";
    }

    @Override
    public String getSchemaTerm() {
        return "Table";
    }

    @Override
    public String getProcedureTerm() {
        return "";
    }

    @Override
    public String getCatalogTerm() {
        return "";
    }

    @Override
    public boolean isDynamicMetadata() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return true;
    }

    @Override
    public boolean supportsResultSetLimit() {
        return true;
    }

    @Override
    public boolean supportsResultSetOrdering() {
        return false;
    }

    @Override
    public boolean needsTableMetaForColumnResolution() {
        return false;
    }
}
