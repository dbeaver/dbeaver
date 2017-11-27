/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

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
    protected PostgreTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final PostgreTableBase table, Object from)
    {
        return new UITask<PostgreTableForeignKey>() {
            @Override
            protected PostgreTableForeignKey runTask() {
                EditForeignKeyPage editPage = new EditForeignKeyPage(
                    "Edit foreign key",
                    table,
                    new DBSForeignKeyModifyRule[] {
                        DBSForeignKeyModifyRule.NO_ACTION,
                        DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                        DBSForeignKeyModifyRule.SET_NULL,
                        DBSForeignKeyModifyRule.SET_DEFAULT });
                if (!editPage.edit()) {
                    return null;
                }

                final PostgreTableForeignKey foreignKey = new PostgreTableForeignKey(
                    table,
                    editPage.getUniqueConstraint(),
                    editPage.getOnDeleteRule(),
                    editPage.getOnUpdateRule());
                foreignKey.setName(DBObjectNameCaseTransformer.transformObjectName(foreignKey,
                    CommonUtils.escapeIdentifier(table.getName()) + "_" + //$NON-NLS-1$
                        CommonUtils.escapeIdentifier(editPage.getUniqueConstraint().getParentObject().getName()) + "_FK")); //$NON-NLS-1$
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new PostgreTableForeignKeyColumn(
                            foreignKey,
                            (PostgreTableColumn) tableColumn.getOwnColumn(),
                            colIndex++,
                            (PostgreTableColumn) tableColumn.getRefColumn()));
                }
                return foreignKey;
            }
        }.execute();
    }

    @Override
    public StringBuilder getNestedDeclaration(PostgreTableBase owner, DBECommandAbstract<PostgreTableForeignKey> command, Map<String, Object> options) {
        PostgreTableForeignKey fk = command.getObject();
        if (fk.isPersisted()) {
            try {
                String constrDDL = fk.getObjectDefinitionText(new VoidProgressMonitor(), DBPScriptObject.EMPTY_OPTIONS);
                if (!CommonUtils.isEmpty(constrDDL)) {
                    return new StringBuilder(constrDDL);
                }
            } catch (DBException e) {
                log.warn("Can't extract FK DDL", e);
            }
        }
        return super.getNestedDeclaration(owner, command, options);
    }

    @Override
    protected String getDropForeignKeyPattern(PostgreTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
