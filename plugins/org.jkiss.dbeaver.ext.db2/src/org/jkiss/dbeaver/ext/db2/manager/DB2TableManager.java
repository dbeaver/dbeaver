/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

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

    @NotNull
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
    public DB2Table createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, DB2Schema db2Schema,
                                         Object copyFrom)
    {
        DB2Table table = new DB2Table(db2Schema, NEW_TABLE_NAME);
        try {
            setTableName(monitor, db2Schema, table);
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
            if (db2Table.getTablespace(new VoidProgressMonitor()) != null) {
                ddl.append(LINE_SEPARATOR);
                ddl.append(CLAUSE_IN_TS);
                ddl.append(getTablespaceName(db2Table.getTablespace(new VoidProgressMonitor())));
            }
            if (db2Table.getIndexTablespace(new VoidProgressMonitor()) != null) {
                ddl.append(LINE_SEPARATOR);
                ddl.append(CLAUSE_IN_TS_IX);
                ddl.append(getTablespaceName(db2Table.getIndexTablespace(new VoidProgressMonitor())));
            }
            if (db2Table.getLongTablespace(new VoidProgressMonitor()) != null) {
                ddl.append(LINE_SEPARATOR);
                ddl.append(CLAUSE_IN_TS_LONG);
                ddl.append(getTablespaceName(db2Table.getLongTablespace(new VoidProgressMonitor())));
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
    public void addStructObjectCreateActions(List<DBEPersistAction> actions, StructCreateCommand command)
    {
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
    public void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        DB2Table db2Table = command.getObject();

        if (command.getProperties().size() > 1) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(SQL_ALTER);
            sb.append(db2Table.getFullyQualifiedName(DBPEvaluationContext.DDL));
            sb.append(" ");

            appendTableModifiers(command.getObject(), command, sb);

            actionList.add(new SQLDatabasePersistAction(CMD_ALTER, sb.toString()));
        }

        DBEPersistAction commentAction = buildCommentAction(db2Table);
        if (commentAction != null) {
            actionList.add(commentAction);
        }
    }

    // ------
    // Rename
    // ------
    @Override
    public void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command)
    {
        String sql = String.format(SQL_RENAME_TABLE, command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL), DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName()));
        actions.add(
            new SQLDatabasePersistAction(CMD_RENAME, sql)
        );
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
            String commentSQL = String.format(SQL_COMMENT, db2Table.getFullyQualifiedName(DBPEvaluationContext.DDL), db2Table.getDescription());
            return new SQLDatabasePersistAction(CMD_COMMENT, commentSQL);
        } else {
            return null;
        }
    }
}
