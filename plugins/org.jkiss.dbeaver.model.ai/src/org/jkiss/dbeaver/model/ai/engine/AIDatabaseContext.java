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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.util.List;

public class AIDatabaseContext {
    private final DBSLogicalDataSource dataSource;
    private final AIDatabaseScope scope;
    private final List<DBSObject> customEntities;
    private final DBCExecutionContext executionContext;

    private AIDatabaseContext(
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull AIDatabaseScope scope,
        @Nullable List<DBSObject> customEntities,
        @NotNull DBCExecutionContext executionContext
    ) {
        this.dataSource = dataSource;
        this.scope = scope;
        this.customEntities = customEntities;
        this.executionContext = executionContext;
    }

    @NotNull
    public DBSLogicalDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    public AIDatabaseScope getScope() {
        return scope;
    }

    @Nullable
    public List<DBSObject> getCustomEntities() {
        return customEntities;
    }

    @NotNull
    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    public static class Builder {
        private DBSLogicalDataSource dataSource;
        private AIDatabaseScope scope;
        private List<DBSObject> customEntities;
        private DBCExecutionContext executionContext;

        public Builder(@NotNull DBSLogicalDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @NotNull
        public Builder setScope(@NotNull AIDatabaseScope scope) {
            this.scope = scope;
            return this;
        }

        @NotNull
        public Builder setCustomEntities(@NotNull List<DBSObject> customEntities) {
            this.customEntities = customEntities;
            return this;
        }

        @NotNull
        public Builder setExecutionContext(@NotNull DBCExecutionContext executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        @NotNull
        public AIDatabaseContext build() throws DBException {
            if (scope == null) {
                throw new DBException("Scope must be specified");
            }
            if (scope == AIDatabaseScope.CUSTOM && customEntities == null) {
                throw new DBException("Custom entities must be specified when using custom scope");
            }
            if (executionContext == null) {
                throw new DBException("Execution context must be specified");
            }
            DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
            if (dataSource.getCurrentCatalog() == null && contextDefaults != null) {
                DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
                dataSource.setCurrentCatalog(defaultCatalog == null ? null : defaultCatalog.getName());
            }
            if (dataSource.getCurrentSchema() == null && contextDefaults != null) {
                DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
                dataSource.setCurrentSchema(defaultSchema == null ? null : defaultSchema.getName());
            }

            return new AIDatabaseContext(dataSource, scope, customEntities, executionContext);
        }
    }

    public DBSObjectContainer getScopeObject() {
        DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
        if (contextDefaults == null) {
            return (DBSObjectContainer) executionContext.getDataSource();
        }

        DBSObjectContainer scoped = switch (getScope()) {
            case CURRENT_SCHEMA:
                if (contextDefaults.getDefaultSchema() != null) {
                    yield contextDefaults.getDefaultSchema();
                } else {
                    yield contextDefaults.getDefaultCatalog();
                }
            case CURRENT_DATABASE:
                yield contextDefaults.getDefaultCatalog();
            default:
                yield null;
        };

        return scoped != null ? scoped : (DBSObjectContainer) executionContext.getDataSource();
    }
}
