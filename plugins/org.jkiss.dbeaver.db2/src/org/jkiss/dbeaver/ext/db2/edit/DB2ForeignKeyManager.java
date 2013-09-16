/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableForeignKey;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCForeignKeyManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.dialogs.struct.EditForeignKeyDialog;

/**
 * DB2 foreign key manager
 */
public class DB2ForeignKeyManager extends JDBCForeignKeyManager<DB2TableForeignKey, DB2Table> {

    private static final String SQL_DROP_FK = "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP FOREIGN KEY " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$

    @Override
    public DBSObjectCache<? extends DBSObject, DB2TableForeignKey> getObjectsCache(DB2TableForeignKey object)
    {
        return object.getParentObject().getSchema().getAssociationCache();
    }

    @Override
    protected DB2TableForeignKey createDatabaseObject(IWorkbenchWindow workbenchWindow,
                                                      DBECommandContext context,
                                                      DB2Table table,
                                                      Object from)
    {
        EditForeignKeyDialog editDialog = new EditForeignKeyDialog(workbenchWindow.getShell(),
            DB2Messages.edit_db2_foreign_key_manager_dialog_title,
            table,
            new DBSForeignKeyModifyRule[]{
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE,
                DBSForeignKeyModifyRule.RESTRICT,
                DBSForeignKeyModifyRule.SET_NULL,
                DBSForeignKeyModifyRule.SET_DEFAULT});
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        // final DB2TableFK foreignKey = new DB2TableFK(table, null, null,
        // (DB2TableConstraint) editDialog.getUniqueConstraint(),
        // editDialog.getOnDeleteRule());
        //      foreignKey.setName(DBObjectNameCaseTransformer.transformName(foreignKey, CommonUtils.escapeIdentifier(table.getName()) + "_" + //$NON-NLS-1$
        //               CommonUtils.escapeIdentifier(editDialog.getUniqueConstraint().getParentObject().getName()) + "_FK")); //$NON-NLS-1$
        // int colIndex = 1;
        // for (EditForeignKeyDialog.FKColumnInfo tableColumn :
        // editDialog.getColumns()) {
        // foreignKey.addColumn(new DB2TableFKColumn(foreignKey, (DB2TableColumn)
        // tableColumn.getOwnColumn(), colIndex++));
        // }
        // return foreignKey;
        return null;
    }

    @Override
    protected String getDropForeignKeyPattern(DB2TableForeignKey foreignKey)
    {
        return SQL_DROP_FK;
    }

}
