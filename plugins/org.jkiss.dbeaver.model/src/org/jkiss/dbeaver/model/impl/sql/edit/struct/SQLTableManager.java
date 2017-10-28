/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JDBC table manager
 */
public abstract class SQLTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSObjectContainer>
    extends SQLStructEditor<OBJECT_TYPE, CONTAINER_TYPE>
{

    private static final String BASE_TABLE_NAME = "NewTable"; //$NON-NLS-1$

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected final void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand objectChangeCommand, Map<String, Object> options)
    {
        throw new IllegalStateException("addObjectCreateActions should never be called in struct editor");
    }

    @Override
    protected void addStructObjectCreateActions(List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options)
    {
        final OBJECT_TYPE table = command.getObject();
        final NestedObjectCommand tableProps = command.getObjectCommands().get(table);
        if (tableProps == null) {
            log.warn("Object change command not found"); //$NON-NLS-1$
            return;
        }
        final String tableName = CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true) ?
            table.getFullyQualifiedName(DBPEvaluationContext.DDL) : DBUtils.getQuotedIdentifier(table);

        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        StringBuilder createQuery = new StringBuilder(100);
        createQuery.append("CREATE TABLE ").append(tableName).append(" (").append(lineSeparator); //$NON-NLS-1$ //$NON-NLS-2$
        boolean hasNestedDeclarations = false;
        final Collection<NestedObjectCommand> orderedCommands = getNestedOrderedCommands(command);
        for (NestedObjectCommand nestedCommand : orderedCommands) {
            if (nestedCommand.getObject() == table) {
                continue;
            }
            if (excludeFromDDL(nestedCommand, orderedCommands)) {
                continue;
            }
            final String nestedDeclaration = nestedCommand.getNestedDeclaration(table, options);
            if (!CommonUtils.isEmpty(nestedDeclaration)) {
                // Insert nested declaration
                if (hasNestedDeclarations) {
                    createQuery.append(",").append(lineSeparator); //$NON-NLS-1$
                }
                createQuery.append("\t").append(nestedDeclaration); //$NON-NLS-1$
                hasNestedDeclarations = true;
            } else {
                // This command should be executed separately
                final DBEPersistAction[] nestedActions = nestedCommand.getPersistActions(options);
                if (nestedActions != null) {
                    Collections.addAll(actions, nestedActions);
                }
            }
        }

        createQuery.append(lineSeparator).append(")"); //$NON-NLS-1$
        appendTableModifiers(table, tableProps, createQuery);

        actions.add( 0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, createQuery.toString()) );
    }

    protected boolean excludeFromDDL(NestedObjectCommand command, Collection<NestedObjectCommand> orderedCommands) {
        return false;
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + (command.getObject().isView() ? "VIEW" : "TABLE") +
                " " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    protected void appendTableModifiers(OBJECT_TYPE table, NestedObjectCommand tableProps, StringBuilder ddl)
    {

    }

    protected void setTableName(DBRProgressMonitor monitor, CONTAINER_TYPE container, OBJECT_TYPE table) throws DBException {
        table.setName(getTableName(monitor, container));
    }

    protected String getTableName(DBRProgressMonitor monitor, CONTAINER_TYPE container) throws DBException {
        return getTableName(monitor, container, BASE_TABLE_NAME);
    }

    protected String getTableName(DBRProgressMonitor monitor, CONTAINER_TYPE container, String baseName) throws DBException {
        for (int i = 0; ; i++) {
            String tableName = DBObjectNameCaseTransformer.transformName(container.getDataSource(), i == 0 ? baseName : (baseName + "_" + i));
            DBSObject child = container.getChild(monitor, tableName);
            if (child == null) {
                return tableName;
            }
        }
    }

    public DBEPersistAction[] getTableDDL(DBRProgressMonitor monitor, OBJECT_TYPE table, Map<String, Object> options) throws DBException
    {
        final DBERegistry editorsRegistry = table.getDataSource().getContainer().getPlatform().getEditorsRegistry();
        SQLObjectEditor<DBSEntityAttribute, OBJECT_TYPE> tcm = getObjectEditor(editorsRegistry, DBSEntityAttribute.class);
        SQLObjectEditor<DBSTableConstraint, OBJECT_TYPE> pkm = getObjectEditor(editorsRegistry, DBSTableConstraint.class);
        SQLObjectEditor<DBSTableForeignKey, OBJECT_TYPE> fkm = getObjectEditor(editorsRegistry, DBSTableForeignKey.class);
        SQLObjectEditor<DBSTableIndex, OBJECT_TYPE> im = getObjectEditor(editorsRegistry, DBSTableIndex.class);

        StructCreateCommand command = makeCreateCommand(table);
        if (tcm != null) {
            // Aggregate nested column, constraint and index commands
            for (DBSEntityAttribute column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
                if (DBUtils.isHiddenObject(column)) {
                    continue;
                }
                command.aggregateCommand(tcm.makeCreateCommand(column));
            }
        }
        if (pkm != null) {
            try {
                for (DBSTableConstraint constraint : CommonUtils.safeCollection(table.getConstraints(monitor))) {
                    if (DBUtils.isHiddenObject(constraint)) {
                        continue;
                    }
                    command.aggregateCommand(pkm.makeCreateCommand(constraint));
                }
            } catch (DBException e) {
                // Ignore primary keys
                log.debug(e);
            }
        }
        if (fkm != null) {
            try {
                for (DBSEntityAssociation foreignKey : CommonUtils.safeCollection(table.getAssociations(monitor))) {
                    if (!(foreignKey instanceof DBSTableForeignKey) || DBUtils.isHiddenObject(foreignKey)) {
                        continue;
                    }
                    command.aggregateCommand(fkm.makeCreateCommand((DBSTableForeignKey) foreignKey));
                }
            } catch (DBException e) {
                // Ignore primary keys
                log.debug(e);
            }
        }
        if (im != null) {
            try {
                for (DBSTableIndex index : CommonUtils.safeCollection(table.getIndexes(monitor))) {
                    if (DBUtils.isHiddenObject(index)) {
                        continue;
                    }
                    command.aggregateCommand(im.makeCreateCommand(index));
                }
            } catch (DBException e) {
                // Ignore indexes
                log.debug(e);
            }
        }
        return command.getPersistActions(options);
    }

}

