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
package org.jkiss.dbeaver.model.impl.dpi;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.local.LocalResultSet;
import org.jkiss.dbeaver.model.impl.local.LocalResultSetMeta;

import java.util.ArrayList;
import java.util.List;

public class DPIResultSet extends LocalResultSet<DBCStatement> {
    private final List<DPIResultSetColumn> meta = new ArrayList<>();

    public DPIResultSet(
        DBCSession session,
        DBCStatement statement
    ) {
        super(session, statement);
    }

    public DPIResultSet(
        @NotNull DBCSession session,
        @NotNull DBCStatement statement,
        @NotNull List<DPIResultSetColumn> meta,
        @NotNull List<Object[]> rows
    ) {
        super(session, statement);
        this.meta.addAll(meta);
        this.rows.addAll(rows);
    }

    public void addColumn(@NotNull DPIResultSetColumn column) {
        meta.add(column);
    }

    @NotNull
    @Override
    public LocalResultSetMeta getMeta() {
        return new LocalResultSetMeta(meta);
    }

    @NotNull
    public List<DPIResultSetColumn> getMetaColumns() {
        return meta;
    }

    public List<Object[]> getAllRows() {
        return rows;
    }

    @Override
    public void close() {
        curPosition = -1;
        rows.clear();
        meta.clear();
    }
}
