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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

/**
 * MySQL foreign key manager
 */
public class MySQLForeignKeyManager extends SQLForeignKeyManager<MySQLTableForeignKey, MySQLTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MySQLTableForeignKey> getObjectsCache(MySQLTableForeignKey object)
    {
        return object.getParentObject().getForeignKeyCache();
    }

    @Override
    protected MySQLTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final MySQLTable table, Object from)
    {
        return new UITask<MySQLTableForeignKey>() {
            @Override
            protected MySQLTableForeignKey runTask() {
                EditForeignKeyPage editPage = new EditForeignKeyPage(
                    MySQLMessages.edit_foreign_key_manager_title,
                    table,
                    new DBSForeignKeyModifyRule[] {
                        DBSForeignKeyModifyRule.NO_ACTION,
                        DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                        DBSForeignKeyModifyRule.SET_NULL,
                        DBSForeignKeyModifyRule.SET_DEFAULT });
                if (!editPage.edit()) {
                    return null;
                }

                final MySQLTableForeignKey foreignKey = new MySQLTableForeignKey(
                    table,
                    null,
                    null,
                    (MySQLTableConstraint) editPage.getUniqueConstraint(),
                    editPage.getOnDeleteRule(),
                    editPage.getOnUpdateRule(),
                    false);
                foreignKey.setName(DBObjectNameCaseTransformer.transformObjectName(foreignKey,
                    CommonUtils.escapeIdentifier(table.getName()) + "_" + //$NON-NLS-1$
                        CommonUtils.escapeIdentifier(editPage.getUniqueConstraint().getParentObject().getName()) + "_FK")); //$NON-NLS-1$
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new MySQLTableForeignKeyColumn(
                            foreignKey,
                            (MySQLTableColumn) tableColumn.getOwnColumn(),
                            colIndex++,
                            (MySQLTableColumn) tableColumn.getRefColumn()));
                }
                return foreignKey;
            }
        }.execute();
    }

    @Override
    protected String getDropForeignKeyPattern(MySQLTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP FOREIGN KEY " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
