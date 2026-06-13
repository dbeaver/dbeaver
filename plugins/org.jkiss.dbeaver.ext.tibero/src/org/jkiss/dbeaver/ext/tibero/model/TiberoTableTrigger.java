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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

public class TiberoTableTrigger extends TiberoTrigger<TiberoTableBase> {

    private final TiberoSchema ownerSchema;
    private List<TiberoTriggerColumn> columns;

    public TiberoTableTrigger(@NotNull TiberoTableBase table, @NotNull String name) {
        super(table, name);
        this.ownerSchema = table.getContainer();
    }

    public TiberoTableTrigger(@NotNull TiberoTableBase table, @NotNull JDBCResultSet dbResult) {
        super(table, dbResult);
        String ownerName = JDBCUtils.safeGetStringTrimmed(dbResult, "OWNER");
        TiberoSchema schema = CommonUtils.isEmpty(ownerName) ? null : table.getDataSource().schemaCache.getCachedObject(ownerName);
        this.ownerSchema = schema == null ? table.getContainer() : schema;
    }

    @Override
    @Property(viewable = true, order = 4)
    public TiberoTableBase getTable() {
        return (TiberoTableBase) getParentObject();
    }

    @NotNull
    @Override
    public TiberoSchema getSchema() {
        return ownerSchema;
    }

    @Association
    @Nullable
    public Collection<TiberoTriggerColumn> getColumns() {
        return columns;
    }

    public void setColumns(@NotNull List<TiberoTriggerColumn> columns) {
        this.columns = columns;
    }
}
