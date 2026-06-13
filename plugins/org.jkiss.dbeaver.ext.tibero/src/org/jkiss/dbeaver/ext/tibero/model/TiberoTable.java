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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TiberoTable extends TiberoTableBase implements DBPScriptObject, DBPRefreshableObject {

    private String tableDDL;

    public TiberoTable(
        @NotNull TiberoSchema schema,
        @NotNull String name,
        @NotNull String tableType,
        @Nullable String description
    ) {
        super(schema, name, tableType, description);
    }

    @Override
    public boolean isView() {
        return false;
    }

    @NotNull
    @Override
    @Association
    public Collection<? extends DBSTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().indexCache.getObjects(monitor, getContainer(), this);
    }

    @NotNull
    @Override
    @Association
    public Collection<TiberoTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().constraintCache.getObjects(monitor, getContainer(), this);
    }

    @NotNull
    @Override
    @Association
    public Collection<TiberoTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
    }

    @NotNull
    @Override
    @Association
    public Collection<TiberoTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<TiberoTableForeignKey> refs = new ArrayList<>();
        for (TiberoTableForeignKey fk : getContainer().foreignKeyCache.getObjects(monitor, getContainer(), null)) {
            if (fk.getReferencedTable() == this) {
                refs.add(fk);
            }
        }
        return refs;
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(
        @NotNull DBRProgressMonitor monitor,
        @Nullable Map<String, Object> options
    ) throws DBException {
        if (tableDDL == null || (options != null && Boolean.TRUE.equals(options.get(OPTION_REFRESH)))) {
            tableDDL = TiberoUtils.loadObjectDDL(monitor, this, "TABLE", getContainer().getName());
        }
        return tableDDL;
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        tableDDL = null;
        return refreshTableObject(monitor);
    }
}
