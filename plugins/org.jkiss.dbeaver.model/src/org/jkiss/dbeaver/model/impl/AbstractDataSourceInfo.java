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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.Collection;
import java.util.Map;

/**
 * AbstractDataSourceInfo
 */
public abstract class AbstractDataSourceInfo implements DBPDataSourceInfo
{
    @Override
    public boolean supportsTransactions() {
        return false;
    }

    @Override
    public boolean supportsTransactionsForDDL() {
        return supportsTransactions();
    }

    @Override
    public boolean supportsSavepoints() {
        return false;
    }

    @Override
    public boolean supportsReferentialIntegrity() {
        return false;
    }

    @Override
    public boolean supportsIndexes() {
        return false;
    }

    @Override
    public boolean supportsStoredCode() {
        return false;
    }

    @Override
    public Collection<DBPTransactionIsolation> getSupportedTransactionsIsolation() {
        return null;
    }

    @Override
    public boolean supportsResultSetLimit() {
        return false;
    }

    @Override
    public boolean supportsResultSetScroll() {
        return false;
    }

    @Override
    public boolean supportsResultSetOrdering() {
        return true;
    }

    @Override
    public boolean supportsNullableUniqueConstraints() {
        return false;
    }

    @Override
    public boolean isDynamicMetadata() {
        return false;
    }

    @Override
    public boolean supportsMultipleResults() {
        return false;
    }

    @Override
    public boolean isReadOnlyData()
    {
        return false;
    }

    @Override
    public boolean isReadOnlyMetaData()
    {
        return false;
    }

    @Override
    public Map<String, Object> getDatabaseProductDetails() {
        return null;
    }

    @Override
    public boolean isMultipleResultsFetchBroken() {
        return false;
    }

    @Override
    public boolean isMultipleResultsFailsOnMaxRows() {
        return false;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes() {
        return new DBSObjectType[0];
    }

    @Override
    public boolean needsTableMetaForColumnResolution() {
        return true;
    }

    @Override
    public boolean supportsStatementBinding() {
        return false;
    }

    @Override
    public boolean supportsBatchUpdates()
    {
        return false;
    }
}
