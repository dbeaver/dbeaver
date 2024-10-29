/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Karl
 */


public class ExasolTableManager extends SQLTableManager<ExasolTable, ExasolSchema> implements DBEObjectRenamer<ExasolTable> {

    private static final String NEW_TABLE_NAME = "NEW_TABLE";

    private static final String SQL_ALTER = "ALTER TABLE ";
    private static final String SQL_RENAME_TABLE = "RENAME TABLE %s TO %s";
    private static final String SQL_COMMENT = "COMMENT ON TABLE %s IS '%s'";


    private static final String CMD_ALTER = "Alter Table";
    private static final String CMD_COMMENT = "Comment on Table";
    private static final String CMD_RENAME = "Rename Table";

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
            ExasolTableColumn.class,
            ExasolTableUniqueKey.class,
            ExasolTableForeignKey.class,
            ExasolTableIndex.class
    );

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Nullable
    @Override
    public DBSObjectCache<ExasolSchema, ExasolTable> getObjectsCache(ExasolTable object) {
        return object.getSchema().getTableCache();
    }

    // ------
    // Create
    // ------

    @Override
    public ExasolTable createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object exasolSchema,
                                            Object copyFrom, @NotNull Map<String, Object> options) {
        ExasolTable table = new ExasolTable((ExasolSchema) exasolSchema, NEW_TABLE_NAME);
        setNewObjectName(monitor, (ExasolSchema) exasolSchema, table);
        return table;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void appendTableModifiers(DBRProgressMonitor monitor, ExasolTable exasolTable, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {

    }

    @Override
    public void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
        super.addStructObjectCreateActions(monitor, executionContext, actions, command, options);
        // Eventually add Comment
        DBEPersistAction commentAction = buildCommentAction(command.getObject());
        if (commentAction != null) {
            actions.add(commentAction);
        }
    }

    // ------
    // Alter
    // ------

    @Override
    public void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) {
        ExasolTable exasolTable = command.getObject();

        if (command.getProperties().size() > 0) {
        	
			if (command.getProperties().containsKey("hasPartitionKey") 
					&& ((command.getProperties().get("hasPartitionKey").toString()).equals("false")) )
			{
				actionList.add(new SQLDatabasePersistAction("ALTER TABLE " + exasolTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + " DROP PARTITION KEYS"));
			} else if (command.getProperties().size() > 1) {
			
			StringBuilder sb = new StringBuilder(128);
			sb.append(SQL_ALTER);
			sb.append(exasolTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
			sb.append(" ");

			appendTableModifiers(monitor, command.getObject(), command, sb, true);

			actionList.add(new SQLDatabasePersistAction(CMD_ALTER, sb.toString()));
			}
        }

        DBEPersistAction commentAction = buildCommentAction(exasolTable);
        if (commentAction != null) {
            actionList.add(commentAction);
        }
    }

    // ------
    // Rename
    // ------
    @Override
    public void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        String sql = String.format(SQL_RENAME_TABLE,
            DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "." + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()),
            DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName()));
        actions.add(
            new SQLDatabasePersistAction(CMD_RENAME, sql)
        );
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull ExasolTable object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    // -------
    // Helpers
    // -------
    private DBEPersistAction buildCommentAction(ExasolTable exasolTable) {
        if (CommonUtils.isNotEmpty(exasolTable.getDescription())) {
            String commentSQL = String.format(SQL_COMMENT, exasolTable.getFullyQualifiedName(DBPEvaluationContext.DDL), exasolTable.getDescription());
            return new SQLDatabasePersistAction(CMD_COMMENT, commentSQL);
        } else {
            return null;
        }
    }
    
}
