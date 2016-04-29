/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.dialogs.struct.EditForeignKeyDialog;
import org.jkiss.utils.CommonUtils;

/**
 * Postgre foreign key manager
 */
public class PostgreForeignKeyManager extends SQLForeignKeyManager<PostgreTableForeignKey, PostgreTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTableForeignKey> getObjectsCache(PostgreTableForeignKey object)
    {
        final PostgreTableBase parent = object.getParentObject();
        if (parent instanceof PostgreTable) {
            return ((PostgreTable) parent).getForeignKeyCache();
        }
        return null;
    }

    @Override
    protected PostgreTableForeignKey createDatabaseObject(DBECommandContext context, PostgreTableBase table, Object from)
    {
        EditForeignKeyDialog editDialog = new EditForeignKeyDialog(
            DBeaverUI.getActiveWorkbenchShell(),
            "Edit foreign key",
            table,
            new DBSForeignKeyModifyRule[] {
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                DBSForeignKeyModifyRule.SET_NULL,
                DBSForeignKeyModifyRule.SET_DEFAULT });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final PostgreTableForeignKey foreignKey = new PostgreTableForeignKey(
            table,
            editDialog.getUniqueConstraint(),
            editDialog.getOnDeleteRule(),
            editDialog.getOnUpdateRule());
        foreignKey.setName(DBObjectNameCaseTransformer.transformObjectName(foreignKey,
            CommonUtils.escapeIdentifier(table.getName()) + "_" + //$NON-NLS-1$
                CommonUtils.escapeIdentifier(editDialog.getUniqueConstraint().getParentObject().getName()) + "_FK")); //$NON-NLS-1$
        int colIndex = 1;
        for (EditForeignKeyDialog.FKColumnInfo tableColumn : editDialog.getColumns()) {
            foreignKey.addColumn(
                new PostgreTableForeignKeyColumn(
                    foreignKey,
                    (PostgreTableColumn) tableColumn.getOwnColumn(),
                    colIndex++,
                    (PostgreTableColumn) tableColumn.getRefColumn()));
        }
        return foreignKey;
    }

    @Override
    protected String getDropForeignKeyPattern(PostgreTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
