/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

/**
 * StarRocks Table/View base class - abstract base for tables and views.
 * Implements catalog-aware fully qualified names.
 */
public abstract class StarRocksTableBase extends GenericTableBase {

    public StarRocksTableBase(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        super(container, tableName, tableType, dbResult);
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return (StarRocksDataSource) super.getDataSource();
    }

    @Nullable
    @Override
    public GenericCatalog getCatalog() {
        // Get catalog from the container hierarchy
        GenericStructContainer container = getContainer();
        if (container instanceof GenericSchema) {
            return ((GenericSchema) container).getCatalog();
        }
        return container.getCatalog();
    }

    @Nullable
    @Override
    public GenericSchema getSchema() {
        // The container should be the schema (StarRocksDatabase)
        GenericStructContainer container = getContainer();
        if (container instanceof GenericSchema) {
            return (GenericSchema) container;
        }
        return container.getSchema();
    }

    @Nullable
    public StarRocksCatalog getStarRocksCatalog() {
        return (StarRocksCatalog) getCatalog();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        // StarRocks always requires 3-level FQN: catalog.database.table
        // This is required for cross-catalog queries and data reading
        GenericCatalog catalog = getCatalog();
        GenericSchema schema = getSchema();

        if (catalog != null && schema != null) {
            // catalog.schema.table
            return DBUtils.getFullQualifiedName(
                getDataSource(),
                catalog,
                schema,
                this);
        } else if (schema != null) {
            // schema.table
            return DBUtils.getFullQualifiedName(
                getDataSource(),
                schema,
                this);
        }
        return DBUtils.getQuotedIdentifier(getDataSource(), getName());
    }
}
