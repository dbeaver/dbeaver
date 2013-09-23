/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableForeignKey;
import org.jkiss.dbeaver.ext.db2.model.DB2TableUniqueKey;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableCache;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * DB2 Table Manager
 * 
 * @author Denis Forveille<
 * 
 */
public class DB2TableManager extends JDBCTableManager<DB2Table, DB2Schema> implements DBEObjectRenamer<DB2Table> {

    private static final String SQL_ALTER = "ALTER TABLE ";
    private static final String SQL_RENAME_TABLE = "RENAME TABLE %s TO %s";
    private static final String SQL_COMMENT = "COMMENT ON TABLE %s IS '%s'";

    private static final Class<?>[] CHILD_TYPES = { DB2TableColumn.class, DB2TableUniqueKey.class, DB2TableForeignKey.class,
        DB2Index.class };

    @Override
    public DBSObjectCache<? extends DBSObject, DB2Table> getObjectsCache(DB2Table object)
    {
        return (DB2TableCache) object.getSchema().getTableCache();
    }

    @Override
    protected DB2Table createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, DB2Schema db2Schema,
        Object copyFrom)
    {
        return new DB2Table(db2Schema, "NEW_TABLE");
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        final DB2Table db2Table = command.getObject();

        List<IDatabasePersistAction> listeActions = new ArrayList<IDatabasePersistAction>(2);

        StringBuilder sb = new StringBuilder(128);
        sb.append(SQL_ALTER);
        sb.append(db2Table.getFullQualifiedName()).append(" ");
        appendTableModifiers(command.getObject(), command, sb);

        listeActions.add(new AbstractDatabasePersistAction("Alter Table", sb.toString()));

        String comment = buildComment(command.getObject());
        if (comment != null) {
            listeActions.add(new AbstractDatabasePersistAction("Comment on Table", comment));
        }

        return listeActions.toArray(new IDatabasePersistAction[listeActions.size()]);
    }

    @Override
    protected void appendTableModifiers(DB2Table table, NestedObjectCommand tableProps, StringBuilder ddl)
    {
    }

    @Override
    protected IDatabasePersistAction[] makeObjectRenameActions(ObjectRenameCommand command)
    {
        String sql = String.format(SQL_RENAME_TABLE, command.getObject().getName(), command.getNewName());
        IDatabasePersistAction[] actions = new IDatabasePersistAction[1];
        actions[0] = new AbstractDatabasePersistAction("Rename table", sql); //$NON-NLS-1$
        return actions;
    }

    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, DB2Table object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

    // -------
    // Helpers
    // -------
    private String buildComment(DB2Table db2Table)
    {
        if ((db2Table.getDescription() != null) && (db2Table.getDescription().length() > 0)) {
            return String.format(SQL_COMMENT, db2Table.getFullQualifiedName(), db2Table.getDescription());
        } else {
            return null;
        }
    }
}
