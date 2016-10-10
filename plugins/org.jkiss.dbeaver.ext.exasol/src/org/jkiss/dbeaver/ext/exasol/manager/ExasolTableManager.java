/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;

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

    private static final Class<?>[] CHILD_TYPES = {ExasolTableColumn.class, ExasolTableUniqueKey.class, ExasolTableForeignKey.class
    };

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public Class<?>[] getChildTypes() {
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
    public ExasolTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, ExasolSchema exasolSchema,
                                            Object copyFrom) {
        ExasolTable table = new ExasolTable(exasolSchema, NEW_TABLE_NAME);
        try {
            setTableName(monitor, exasolSchema, table);
        } catch (DBException e) {
            log.error(e);
        }
        return table;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void appendTableModifiers(ExasolTable exasolTable, NestedObjectCommand tableProps, StringBuilder ddl) {

    }

    @Override
    public void addStructObjectCreateActions(List<DBEPersistAction> actions, StructCreateCommand command) {
        super.addStructObjectCreateActions(actions, command);
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
    public void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command) {
        ExasolTable exasolTable = command.getObject();

        if (command.getProperties().size() > 1) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(SQL_ALTER);
            sb.append(exasolTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
            sb.append(" ");

            appendTableModifiers(command.getObject(), command, sb);

            actionList.add(new SQLDatabasePersistAction(CMD_ALTER, sb.toString()));
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
    public void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command) {
        String sql = String.format(SQL_RENAME_TABLE, command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL), command.getNewName());
        actions.add(
            new SQLDatabasePersistAction(CMD_RENAME, sql)
        );
    }

    @Override
    public void renameObject(DBECommandContext commandContext, ExasolTable object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
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
