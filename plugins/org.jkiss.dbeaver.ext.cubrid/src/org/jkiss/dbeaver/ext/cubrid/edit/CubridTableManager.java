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
package org.jkiss.dbeaver.ext.cubrid.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.*;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CubridTableManager extends GenericTableManager implements DBEObjectRenamer<GenericTableBase> {
    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        CubridTableColumn.class,
        GenericUniqueKey.class,
        GenericTableForeignKey.class,
        CubridTableIndex.class
    );

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        CubridUser user = (CubridUser) container;
        CubridDataSource dataSource = (CubridDataSource) user.getDataSource();
        return !dataSource.isShard();
    }

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Nullable
    @Override
    public Collection<? extends DBSObject> getChildObjects(
        DBRProgressMonitor monitor,
        GenericTableBase object,
        Class<? extends DBSObject> childType
    ) throws DBException {
        if (childType == CubridTableColumn.class) {
            return object.getAttributes(monitor);
        }
        return super.getChildObjects(monitor, object, childType);
    }

    public void appendPartition(DBRProgressMonitor monitor, StringBuilder query, CubridTable table) throws DBException {
        List<CubridPartition> partitions = table.getPartitions(monitor);
        String type = partitions.getFirst().getTableType().toUpperCase();
        String key = partitions.getFirst().getExpression();
        CubridTableColumn column = (CubridTableColumn) table.getAttribute(monitor, key);

        query.append(String.format("PARTITION BY %s (%s)", type, key));
        if ("HASH".equals(type)) {
            query.append(" PARTITIONS ").append(partitions.size());
            return;
        }
        query.append(" (");
        for (CubridPartition partition : partitions) {
            String value = partition.getExpressionValues();
            query.append("\n\tPARTITION ").append(partition.getPartitionName());

            if ("RANGE".equals(type)) {
                query.append(" VALUES LESS THAN ");
                if ("MAXVALUE".equalsIgnoreCase(value)) {
                    query.append("MAXVALUE");
                } else {
                    query.append("(").append(DBPDataKind.NUMERIC == column.getDataKind() ?
                        value : SQLUtils.quoteString(partition, value)).append(")");
                }
            } else { //LIST
                query.append(" VALUES IN ");
                query.append("(").append(DBPDataKind.NUMERIC == column.getDataKind() ?
                    value : "'" + value.replaceAll(",\\s*", "', '") + "'").append(")");
            }
            if (!CommonUtils.isEmpty(partition.getDescription())) {
                query.append(" COMMENT ").append(SQLUtils.quoteString(partition, partition.getDescription()));
            }
            query.append(",");
        }
        query.deleteCharAt(query.length() - 1).append("\n)");
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (command.getProperties().size() > 1 || command.getProperty("schema") == null) {
            CubridTable table = (CubridTable) command.getObject();
            StringBuilder query = new StringBuilder("ALTER TABLE ");
            query.append(table.getContainer()).append(".").append(table.getName());
            appendTableModifiers(monitor, table, command, query, true, options);
            actionList.add(new SQLDatabasePersistAction(query.toString()));
        }
    }

    @Override
    protected void appendTableModifiers(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase genericTable,
        @NotNull NestedObjectCommand command,
        @NotNull StringBuilder query,
        boolean alter,
        @NotNull Map<String, Object> options
    ) throws DBException {
        CubridTable table = (CubridTable) genericTable;
        String delimiter = getDelimiter(options);
        String suffix = alter ? "," : delimiter;
        query.append(delimiter);
        if (!alter || command.hasProperty("reuseOID")) {
            if (table.getDataSource().isServerVersionAtLeast(11, 0)) {
                query.append(table.isReuseOID() ? "REUSE_OID" : "DONT_REUSE_OID").append(suffix);
            } else {
                query.append(table.isReuseOID() ? "REUSE_OID" + suffix : "");
            }
        }
        if ((!alter && table.getCollation().getName() != null) || (command.getProperty("charset") != null
            || command.getProperty("collation") != null)) {
            query.append("COLLATE ").append(table.getCollation().getName()).append(suffix);
        }
        if ((!alter || command.getProperty("autoIncrement") != null) && table.getAutoIncrement() > 0) {
            query.append("AUTO_INCREMENT = ").append(table.getAutoIncrement()).append(suffix);
        }
        if ((!alter && !CommonUtils.isEmpty(table.getDescription())) || command.hasProperty("description")) {
            query.append("COMMENT = ").append(SQLUtils.quoteString(table, CommonUtils.notEmpty(table.getDescription()))).append(suffix);
        }
        if (table.isPartitioned() && table.isPersisted()) {
            appendPartition(monitor, query, table);
            query.append(suffix);
        }
        if (!isCompact(options)) {
            query.deleteCharAt(query.length() - 1);
        }
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<GenericTableBase, PropertyHandler> command,
        @NotNull Map<String, Object> options
    ) {
        CubridTable table = (CubridTable) command.getObject();
        if (table.isPersisted() && table.getContainer() != table.getSchema()) {
            actions.add(
                new SQLDatabasePersistAction(
                    "Change Owner",
                    "ALTER TABLE " + table.getContainer() + "." + table.getName() + " OWNER TO " + table.getSchema()
                ));
        }
    }

    @Override
    protected void addObjectRenameActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectRenameCommand command,
        @NotNull Map<String, Object> options
    ) {
        CubridTable table = (CubridTable) command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "RENAME TABLE " + table.getContainer() + "." + command.getOldName() + " TO " + command.getNewName()
            ));
    }

    @Override
    public void renameObject(
        @NotNull DBECommandContext commandContext,
        @NotNull GenericTableBase object,
        @NotNull Map<String, Object> options,
        @NotNull String newName
    ) throws DBException {
        if (!((CubridDataSource) object.getDataSource()).isShard()) {
            processObjectRename(commandContext, object, options, newName);
        }
    }

    @Override
    public boolean canRenameObject(GenericTableBase object) {
        return !((CubridDataSource) object.getDataSource()).isShard();
    }

    @Override
    public boolean canEditObject(GenericTableBase object) {
        return !((CubridDataSource) object.getDataSource()).isShard();
    }

    @Override
    public boolean canDeleteObject(GenericTableBase object) {
        return !((CubridDataSource) object.getDataSource()).isShard();
    }
}
