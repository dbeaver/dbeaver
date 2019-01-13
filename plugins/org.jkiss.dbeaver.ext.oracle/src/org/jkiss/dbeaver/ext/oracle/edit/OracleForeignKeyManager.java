/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.*;
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
 * Oracle foreign key manager
 */
public class OracleForeignKeyManager extends SQLForeignKeyManager<OracleTableForeignKey, OracleTableBase> {


    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTableForeignKey> getObjectsCache(OracleTableForeignKey object)
    {
        return object.getParentObject().getSchema().foreignKeyCache;
    }

    @Override
    protected OracleTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final OracleTableBase table, Object from)
    {
        return new UITask<OracleTableForeignKey>() {
            @Override
            protected OracleTableForeignKey runTask() {
                EditForeignKeyPage editPage = new EditForeignKeyPage(
                    OracleMessages.edit_oracle_foreign_key_manager_dialog_title,
                    table,
                    new DBSForeignKeyModifyRule[] {
                        DBSForeignKeyModifyRule.NO_ACTION,
                        DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                        DBSForeignKeyModifyRule.SET_NULL,
                        DBSForeignKeyModifyRule.SET_DEFAULT });
                if (!editPage.edit()) {
                    return null;
                }

                final OracleTableForeignKey foreignKey = new OracleTableForeignKey(
                    table,
                    null,
                    null,
                    (OracleTableConstraint) editPage.getUniqueConstraint(),
                    editPage.getOnDeleteRule());
                foreignKey.setName(getNewConstraintName(monitor, foreignKey));
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new OracleTableForeignKeyColumn(
                            foreignKey,
                            (OracleTableColumn) tableColumn.getOwnColumn(),
                            colIndex++));
                }
                return foreignKey;
            }
        }.execute();
    }

/*
    // FIX: Oracle uses standard syntax
    @Override
    protected String getDropForeignKeyPattern(OracleTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP FOREIGN KEY " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }
*/

}
