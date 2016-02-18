/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DB2 Table Column Manager
 * 
 * @author Denis Forveille
 */
public class DB2TableColumnManager extends SQLTableColumnManager<DB2TableColumn, DB2TableBase> implements DBEObjectRenamer<DB2TableColumn> {

    private static final String SQL_ALTER = "ALTER TABLE %s ALTER COLUMN %s ";
    private static final String SQL_COMMENT = "COMMENT ON COLUMN %s.%s IS '%s'";
    private static final String SQL_REORG = "CALL SYSPROC.ADMIN_CMD('REORG TABLE %s')";

    private static final String CLAUSE_SET_TYPE = " SET DATA TYPE ";
    private static final String CLAUSE_SET_NULL = " SET NOT NULL";
    private static final String CLAUSE_DROP_NULL = " DROP NOT NULL";

    private static final String CMD_ALTER = "Alter Column";
    private static final String CMD_COMMENT = "Comment on Column";
    private static final String CMD_REORG = "Reorg table";

    private static final String LINE_SEPARATOR = GeneralUtils.getDefaultLineSeparator();

    // -----------------
    // Business Contract
    // -----------------
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2TableColumn> getObjectsCache(DB2TableColumn object)
    {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache((DB2Table) object.getParentObject());
    }

    @Override
    public boolean canEditObject(DB2TableColumn object)
    {
        // Edit is only availabe for DB2Table and not for other kinds of tables (View, MQTs, Nicknames..)
        DB2TableBase db2TableBase = object.getParentObject();
        if ((db2TableBase != null) & (db2TableBase.getClass().equals(DB2Table.class))) {
            return true;
        } else {
            return false;
        }
    }

    // ------
    // Create
    // ------

    @Override
    protected DB2TableColumn createDatabaseObject(DBECommandContext context, DB2TableBase parent,
                                                  Object copyFrom)
    {
        DB2TableColumn column = new DB2TableColumn(parent);
        column.setName(getNewColumnName(context, parent));
        return column;
    }

    // -----
    // Alter
    // -----
    @Override
    protected DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        DB2TableColumn db2Column = command.getObject();

        List<DBEPersistAction> actions = new ArrayList<>(3);

        String sqlAlterColumn = String.format(SQL_ALTER, db2Column.getTable().getFullQualifiedName(), computeDeltaSQL(command));
        actions.add(new SQLDatabasePersistAction(CMD_ALTER, sqlAlterColumn));

        // Comment
        DBEPersistAction commentAction = buildCommentAction(db2Column);
        if (commentAction != null) {
            actions.add(commentAction);
        }

        // Be Safe, Add a reorg action
        actions.add(buildReorgAction(db2Column));

        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

    // -------
    // Helpers
    // -------
    private String computeDeltaSQL(ObjectChangeCommand command)
    {

        if (command.getProperties().isEmpty()) {
            return "";
        }

        if (log.isDebugEnabled()) {
            for (Map.Entry<Object, Object> entry : command.getProperties().entrySet()) {
                log.debug(entry.getKey() + "=" + entry.getValue());
            }
        }

        DB2TableColumn column = command.getObject();

        StringBuilder sb = new StringBuilder(128);
        sb.append(column.getName());

        Boolean required = (Boolean) command.getProperty("required");
        if (required != null) {
            sb.append(LINE_SEPARATOR);
            if (required) {
                sb.append(CLAUSE_SET_NULL);
            } else {
                sb.append(CLAUSE_DROP_NULL);
            }
        }

        String type = (String) command.getProperty("type");
        if (type != null) {
            sb.append(LINE_SEPARATOR);
            sb.append(CLAUSE_SET_TYPE);
            sb.append(type);
        }

        return sb.toString();
    }

    private DBEPersistAction buildCommentAction(DB2TableColumn db2Column)
    {
        if (CommonUtils.isNotEmpty(db2Column.getDescription())) {
            String tableName = db2Column.getTable().getFullQualifiedName();
            String columnName = db2Column.getName();
            String comment = db2Column.getDescription();
            String commentSQL = String.format(SQL_COMMENT, tableName, columnName, comment);
            return new SQLDatabasePersistAction(CMD_COMMENT, commentSQL);
        } else {
            return null;
        }
    }

    private DBEPersistAction buildReorgAction(DB2TableColumn db2Column)
    {
        String tableName = db2Column.getTable().getFullQualifiedName();
        String reorgSQL = String.format(SQL_REORG, tableName);
        return new SQLDatabasePersistAction(CMD_REORG, reorgSQL);
    }

    @Override
    public void renameObject(DBECommandContext commandContext, DB2TableColumn object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected DBEPersistAction[] makeObjectRenameActions(ObjectRenameCommand command)
    {
        final DB2TableColumn column = command.getObject();

        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER TABLE " + column.getTable().getFullQualifiedName() + " RENAME COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO " + command.getNewName())
        };
    }
}
