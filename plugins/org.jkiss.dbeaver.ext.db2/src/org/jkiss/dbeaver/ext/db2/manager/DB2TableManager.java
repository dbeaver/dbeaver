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
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableForeignKey;
import org.jkiss.dbeaver.ext.db2.model.DB2TableUniqueKey;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DB2 Table Manager
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableManager extends SQLTableManager<DB2Table, DB2Schema> implements DBEObjectRenamer<DB2Table> {

    private static final String NEW_TABLE_NAME = "NEW_TABLE";

    private static final String SQL_ALTER = "ALTER TABLE ";
    private static final String SQL_RENAME_TABLE = "RENAME TABLE %s TO %s";
    private static final String SQL_COMMENT = "COMMENT ON TABLE %s IS '%s'";

    private static final String CLAUSE_IN_TS = "IN ";
    private static final String CLAUSE_IN_TS_IX = "INDEX IN ";
    private static final String CLAUSE_IN_TS_LONG = "LONG IN ";

    private static final String CMD_ALTER = "Alter Table";
    private static final String CMD_COMMENT = "Comment on Table";
    private static final String CMD_RENAME = "Rename Table";

    private static final String LINE_SEPARATOR = GeneralUtils.getDefaultLineSeparator();

    private static final Class<?>[] CHILD_TYPES = { DB2TableColumn.class, DB2TableUniqueKey.class, DB2TableForeignKey.class,
        DB2Index.class };

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2Schema, DB2Table> getObjectsCache(DB2Table object)
    {
        return object.getSchema().getTableCache();
    }

    // ------
    // Create
    // ------

    @Override
    public DB2Table createDatabaseObject(DBECommandContext context, DB2Schema db2Schema,
                                         Object copyFrom)
    {
        DB2Table table = new DB2Table(db2Schema, NEW_TABLE_NAME);
        try {
            setTableName(db2Schema, table);
        } catch (DBException e) {
            log.error(e);
        }
        return table;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void appendTableModifiers(DB2Table db2Table, NestedObjectCommand tableProps, StringBuilder ddl)
    {

        try {
            // Add Tablespaces infos
            if (db2Table.getTablespace(VoidProgressMonitor.INSTANCE) != null) {
                ddl.append(LINE_SEPARATOR);
                ddl.append(CLAUSE_IN_TS);
                ddl.append(getTablespaceName(db2Table.getTablespace(VoidProgressMonitor.INSTANCE)));
            }
            if (db2Table.getIndexTablespace(VoidProgressMonitor.INSTANCE) != null) {
                ddl.append(LINE_SEPARATOR);
                ddl.append(CLAUSE_IN_TS_IX);
                ddl.append(getTablespaceName(db2Table.getIndexTablespace(VoidProgressMonitor.INSTANCE)));
            }
            if (db2Table.getLongTablespace(VoidProgressMonitor.INSTANCE) != null) {
                ddl.append(LINE_SEPARATOR);
                ddl.append(CLAUSE_IN_TS_LONG);
                ddl.append(getTablespaceName(db2Table.getLongTablespace(VoidProgressMonitor.INSTANCE)));
            }
        } catch (DBException e) {
            // Never be here
            log.warn(e);
        }
    }

    private static String getTablespaceName(Object tablespace)
    {
        if (tablespace instanceof DB2Tablespace) {
            return ((DB2Tablespace) tablespace).getName();
        } else if (tablespace != null) {
            return String.valueOf(tablespace);
        } else {
            return null;
        }
    }

    @Override
    public DBEPersistAction[] makeStructObjectCreateActions(StructCreateCommand command)
    {
        // Eventually add Comment
        DBEPersistAction commentAction = buildCommentAction(command.getObject());
        if (commentAction == null) {
            return super.makeStructObjectCreateActions(command);
        } else {
            List<DBEPersistAction> actionList = new ArrayList<>(Arrays.asList(super
                .makeStructObjectCreateActions(command)));
            actionList.add(commentAction);
            return actionList.toArray(new DBEPersistAction[actionList.size()]);
        }
    }

    // ------
    // Alter
    // ------

    @Override
    public DBEPersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        DB2Table db2Table = command.getObject();

        List<DBEPersistAction> actions = new ArrayList<>(2);

        if (command.getProperties().size() > 1) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(SQL_ALTER);
            sb.append(db2Table.getFullQualifiedName());
            sb.append(" ");

            appendTableModifiers(command.getObject(), command, sb);

            actions.add(new SQLDatabasePersistAction(CMD_ALTER, sb.toString()));
        }

        DBEPersistAction commentAction = buildCommentAction(db2Table);
        if (commentAction != null) {
            actions.add(commentAction);
        }

        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

    // ------
    // Rename
    // ------
    @Override
    public DBEPersistAction[] makeObjectRenameActions(ObjectRenameCommand command)
    {
        String sql = String.format(SQL_RENAME_TABLE, command.getObject().getFullQualifiedName(), command.getNewName());
        return new DBEPersistAction[]{
            new SQLDatabasePersistAction(CMD_RENAME, sql)
        };
    }

    @Override
    public void renameObject(DBECommandContext commandContext, DB2Table object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

    // -------
    // Helpers
    // -------
    private DBEPersistAction buildCommentAction(DB2Table db2Table)
    {
        if (CommonUtils.isNotEmpty(db2Table.getDescription())) {
            String commentSQL = String.format(SQL_COMMENT, db2Table.getFullQualifiedName(), db2Table.getDescription());
            return new SQLDatabasePersistAction(CMD_COMMENT, commentSQL);
        } else {
            return null;
        }
    }
}
