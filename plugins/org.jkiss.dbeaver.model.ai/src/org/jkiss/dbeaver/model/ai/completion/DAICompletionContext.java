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
package org.jkiss.dbeaver.model.ai.completion;

import org.eclipse.core.runtime.Assert;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DAICompletionContext {
    private final DAICompletionScope scope;
    private final List<DBSEntity> customEntities;
    private final DBSLogicalDataSource dataSource;
    private final DBCExecutionContext executionContext;

    private DAICompletionContext(
        @NotNull DAICompletionScope scope,
        @Nullable List<DBSEntity> customEntities,
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull DBCExecutionContext executionContext
    ) {
        this.scope = scope;
        this.customEntities = customEntities;
        this.dataSource = dataSource;
        this.executionContext = executionContext;
    }

    @NotNull
    public DAICompletionScope getScope() {
        return scope;
    }

    @NotNull
    public List<DBSEntity> getCustomEntities() {
        return Collections.unmodifiableList(Objects.requireNonNull(customEntities, "Scope is not custom"));
    }

    @NotNull
    public DBSLogicalDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    public static class Builder {
        private DAICompletionScope scope;
        private List<DBSEntity> customEntities;
        private DBSLogicalDataSource dataSource;
        private DBCExecutionContext executionContext;

        @NotNull
        public Builder setScope(@NotNull DAICompletionScope scope) {
            this.scope = scope;
            return this;
        }

        @NotNull
        public Builder setCustomEntities(@NotNull List<DBSEntity> customEntities) {
            this.customEntities = customEntities;
            return this;
        }

        @NotNull
        public Builder setDataSource(@NotNull DBSLogicalDataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        @NotNull
        public Builder setExecutionContext(@NotNull DBCExecutionContext executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        @NotNull
        public DAICompletionContext build() {
            Assert.isLegal(scope != null, "Scope must be specified");
            Assert.isLegal(scope != DAICompletionScope.CUSTOM || customEntities != null, "Custom entities must be specified when using custom scope");
            Assert.isLegal(dataSource != null, "Data source must be specified");
            Assert.isLegal(executionContext != null, "Execution context must be specified");

            return new DAICompletionContext(scope, customEntities, dataSource, executionContext);
        }
    }
}
