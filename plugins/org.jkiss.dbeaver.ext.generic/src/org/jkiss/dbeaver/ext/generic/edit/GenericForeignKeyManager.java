/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
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
    public boolean canCreateObject(GenericTable parent) {
        return parent.getDataSource().getSQLDialect().supportsAlterTableConstraint();
    }

    @Override
    public boolean canDeleteObject(GenericTableForeignKey object) {
        return object.getDataSource().getSQLDialect().supportsAlterTableConstraint();
    }

    @Override
    protected GenericTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final GenericTable table, Object from)
    {
        return new UITask<GenericTableForeignKey>() {
            @Override
            protected GenericTableForeignKey runTask() {
                EditForeignKeyPage editPage = new EditForeignKeyPage(
                    "Create foreign key",
                    table,
                    new DBSForeignKeyModifyRule[] {
                        DBSForeignKeyModifyRule.NO_ACTION,
                        DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                        DBSForeignKeyModifyRule.SET_NULL,
                        DBSForeignKeyModifyRule.SET_DEFAULT });
                if (!editPage.edit()) {
                    return null;
                }

                final GenericTableForeignKey foreignKey = new GenericTableForeignKey(
                    table,
                    null,
                    null,
                    (GenericPrimaryKey) editPage.getUniqueConstraint(),
                    editPage.getOnDeleteRule(),
                    editPage.getOnUpdateRule(),
                    DBSForeignKeyDeferability.NOT_DEFERRABLE,
                    false);
                foreignKey.setName(getNewConstraintName(monitor, foreignKey));
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new GenericTableForeignKeyColumnTable(
                            foreignKey,
                            (GenericTableColumn) tableColumn.getOwnColumn(),
                            colIndex++,
                            (GenericTableColumn) tableColumn.getRefColumn()));
                }
                return foreignKey;
            }
        }.execute();
    }

    @Override
    protected boolean isLegacyForeignKeySyntax(GenericTable owner) {
        return GenericUtils.isLegacySQLDialect(owner);
    }
}
