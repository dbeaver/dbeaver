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
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DB2 Table Column Manager
 * 
 * @author Denis Forveille
 */
public class DB2TableColumnManager extends JDBCTableColumnManager<DB2TableColumn, DB2TableBase> {

    private static final String SQL_ALTER = "ALTER TABLE %s ALTER COLUMN %s ";
    private static final String SQL_COMMENT = "COMMENT ON COLUMN %s.%s IS '%s'";

    private static final String CMD_ALTER = "Alter Column";
    private static final String CMD_COMMENT = "Comment on Column";

    // -----------------
    // Business Contract
    // -----------------
    @Override
    public DBSObjectCache<? extends DBSObject, DB2TableColumn> getObjectsCache(DB2TableColumn object)
    {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache((DB2Table) object.getParentObject());
    }

    @Override
    public StringBuilder getNestedDeclaration(DB2TableBase owner, DBECommandComposite<DB2TableColumn, PropertyHandler> command)
    {
        StringBuilder decl = super.getNestedDeclaration(owner, command);

        return decl;
    }

    // ------
    // Create
    // ------

    @Override
    protected DB2TableColumn createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, DB2TableBase parent,
        Object copyFrom)
    {
        //        DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar2"); //$NON-NLS-1$

        final DB2TableColumn column = new DB2TableColumn(parent, "abcd");
        column.setName(DBObjectNameCaseTransformer.transformName(column, getNewColumnName(context, parent)));
        // column.setType((DB2DataType) columnType);
        //        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName()); //$NON-NLS-1$
        // column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
        // column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
        // column.setOrdinalPosition(-1);
        return column;
    }

    // -----
    // Alter
    // -----
    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        DB2TableColumn column = command.getObject();
        String comment = (String) command.getProperty("comment");

        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>(2);

        String sqlAlterColumn = String.format(SQL_ALTER, column.getTable().getFullQualifiedName(), computeDeltaSQL(command));

        IDatabasePersistAction action = new AbstractDatabasePersistAction(CMD_ALTER, sqlAlterColumn);
        actions.add(action);

        if (comment != null) {
            String sqlCommentColumn =
                String.format(SQL_COMMENT, column.getTable().getFullQualifiedName(), column.getName(), comment);
            action = new AbstractDatabasePersistAction(CMD_COMMENT, sqlCommentColumn);
            actions.add(action);
        }

        return actions.toArray(new IDatabasePersistAction[actions.size()]);
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
        DB2TableBase db2Table = column.getTable();

        StringBuilder sb = new StringBuilder(128);
        sb.append(column.getName());

        Boolean required = (Boolean) command.getProperty("required");
        if (required != null) {
            if (required) {
                sb.append(" SET NOT NULL");
            } else {
                sb.append(" DROP NOT NULL");
            }
        }
        String type = (String) command.getProperty("type");
        sb.append(" SET DATA TYPE ");
        sb.append(type);

        sb.append(";\t");
        sb.append("CALL SYSPROC.ADMIN_CMD('REORG TABLE " + db2Table.getFullQualifiedName() + "')");

        return sb.toString();
    }
}
