/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * Oracle table manager
 */
public class OracleTableManager extends SQLTableManager<OracleTable, OracleSchema> implements DBEObjectRenamer<OracleTable> {

    private static final Class<?>[] CHILD_TYPES = {
        OracleTableColumn.class,
        OracleTableConstraint.class,
        OracleTableForeignKey.class,
        OracleTableIndex.class
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTable> getObjectsCache(OracleTable object)
    {
        return (DBSObjectCache) object.getSchema().tableCache;
    }

    @Override
    protected OracleTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        OracleTable table = new OracleTable(parent, "");
        try {
            setTableName(monitor, parent, table);
        } catch (DBException e) {
            log.error(e);
        }
        return table; //$NON-NLS-1$
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        if (command.getProperties().size() > 1 || command.getProperty("comment") == null) {
            StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
            query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" "); //$NON-NLS-1$
            appendTableModifiers(command.getObject(), command, query);
            actionList.add(new SQLDatabasePersistAction(query.toString()));
        }
    }

    @Override
    protected void addObjectExtraActions(List<DBEPersistAction> actions, NestedObjectCommand<OracleTable, PropertyHandler> command) {
        if (command.getProperty("comment") != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS '" + SQLUtils.escapeString(command.getObject().getComment()) + "'"));
        }
    }

    @Override
    protected void appendTableModifiers(OracleTable table, NestedObjectCommand tableProps, StringBuilder ddl)
    {
        // ALTER
        if (tableProps.getProperty("tablespace") != null) { //$NON-NLS-1$
            Object tablespace = table.getTablespace();
            if (tablespace instanceof OracleTablespace) {
                if (table.isPersisted()) {
                    ddl.append("\nMOVE TABLESPACE ").append(((OracleTablespace) tablespace).getName()); //$NON-NLS-1$
                } else {
                    ddl.append("\nTABLESPACE ").append(((OracleTablespace) tablespace).getName()); //$NON-NLS-1$
                }
            }
        }
    }

    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions, ObjectRenameCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "ALTER TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, OracleTable object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

}
