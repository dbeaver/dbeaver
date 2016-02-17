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
package org.jkiss.dbeaver.ext.generic.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDefferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.dialogs.struct.EditForeignKeyDialog;
import org.jkiss.utils.CommonUtils;

/**
 * Generic foreign manager
 */
public class GenericForeignKeyManager extends SQLForeignKeyManager<GenericTableForeignKey, GenericTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTableForeignKey> getObjectsCache(GenericTableForeignKey object)
    {
        return object.getParentObject().getContainer().getForeignKeysCache();
    }

    @Override
    protected GenericTableForeignKey createDatabaseObject(DBECommandContext context, GenericTable table, Object from)
    {
        EditForeignKeyDialog editDialog = new EditForeignKeyDialog(
            DBeaverUI.getActiveWorkbenchShell(),
            "Create foreign key",
            table,
            new DBSForeignKeyModifyRule[] {
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                DBSForeignKeyModifyRule.SET_NULL,
                DBSForeignKeyModifyRule.SET_DEFAULT });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final GenericTableForeignKey foreignKey = new GenericTableForeignKey(
            table,
            null,
            null,
            (GenericPrimaryKey) editDialog.getUniqueConstraint(),
            editDialog.getOnDeleteRule(),
            editDialog.getOnUpdateRule(),
            DBSForeignKeyDefferability.NOT_DEFERRABLE,
            false);
        foreignKey.setName(DBObjectNameCaseTransformer.transformObjectName(foreignKey,
            CommonUtils.escapeIdentifier(table.getName()) + "_" +
                CommonUtils.escapeIdentifier(editDialog.getUniqueConstraint().getParentObject().getName()) + "_FK"));
        int colIndex = 1;
        for (EditForeignKeyDialog.FKColumnInfo tableColumn : editDialog.getColumns()) {
            foreignKey.addColumn(
                new GenericTableForeignKeyColumnTable(
                    foreignKey,
                    (GenericTableColumn) tableColumn.getOwnColumn(),
                    colIndex++,
                    (GenericTableColumn) tableColumn.getRefColumn()));
        }
        return foreignKey;
    }

    @Override
    protected boolean isLegacyForeignKeySyntax(GenericTable owner) {
        return ((GenericSQLDialect)owner.getDataSource().getSQLDialect()).isLegacySQLDialect();
    }
}
