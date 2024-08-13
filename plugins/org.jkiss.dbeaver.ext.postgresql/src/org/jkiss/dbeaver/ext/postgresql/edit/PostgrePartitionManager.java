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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * PostgrePartitionManager
 */
public class PostgrePartitionManager extends PostgreTableManager {

    private static final Log log = Log.getLog(PostgrePartitionManager.class);

    private static final Class<? extends DBSObject>[] CHILD_TYPES_PART = CommonUtils.array(
        PostgreTableConstraint.class,
        PostgreTableForeignKey.class,
        PostgreIndex.class
    );

    protected PostgreTablePartition createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) {
        return new PostgreTablePartition((PostgreTable) container);
    }

    private String getParentTable(@NotNull DBRProgressMonitor monitor, @NotNull PostgreTablePartition partition) {

        List<PostgreTableBase> superTables;
        try {
            superTables = partition.getSuperTables(monitor);
        } catch (DBException e) {
            log.error("Unable to get parent", e);
            return "";//$NON-NLS-1$
        }

        if (superTables == null && partition.getPartitionOf() != null) {
            return partition.getPartitionOf().getFullyQualifiedName(DBPEvaluationContext.DDL);
        } else if (CommonUtils.isEmpty(superTables) || superTables.size() > 1) {
            log.error("Unable to get parent");
            return "";//$NON-NLS-1$
        }

        return superTables.get(0).getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

    @Override
    protected String beginCreateTableStatement(DBRProgressMonitor monitor, PostgreTableBase table, String tableName, Map<String, Object> options) {
        return "CREATE " + getCreateTableType(table) + " " + tableName + " PARTITION OF " + getParentTable(monitor, (PostgreTablePartition) table) + " ";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    protected boolean hasAttrDeclarations(PostgreTableBase table) {
        return false;
    }

    @Override
    protected boolean excludeFromDDL(NestedObjectCommand command, Collection<NestedObjectCommand> orderedCommands) {
        return !(command.getObject() instanceof PostgreTableConstraint) && !(command.getObject() instanceof PostgreIndex);
    }

    @Override
    public boolean canEditObject(PostgreTableBase object) {
        return object instanceof PostgreTablePartition;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return container instanceof PostgreTable;
    }

    @Override
    public boolean canDeleteObject(@NotNull PostgreTableBase object) {
        return true;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES_PART;
    }


}
