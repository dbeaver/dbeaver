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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQLServerTableReal
 */
public abstract class SQLServerTableReal extends SQLServerTableBase {
    SQLServerTableReal(@NotNull SQLServerSchema schema) {
        super(schema);
    }

    SQLServerTableReal(@NotNull SQLServerSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    @NotNull
    @Association
    public List<SQLServerTableTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().getTriggerCache().getAllObjects(monitor, getSchema()).stream()
            .filter(p -> p.getTable() == this)
            .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        getContainer().getTriggerCache().clearChildrenOf(this);
        return super.refreshObject(monitor);
    }
}
