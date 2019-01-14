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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;
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
                EditPGForeignKeyPage editPage = new EditPGForeignKeyPage(
                    "Edit foreign key",
                    table);
                if (!editPage.edit()) {
                    return null;
                }

                final PostgreTableForeignKey foreignKey = new PostgreTableForeignKey(
                    table,
                    editPage.getUniqueConstraint(),
                    editPage.getOnDeleteRule(),
                    editPage.getOnUpdateRule());
                foreignKey.setName(getNewConstraintName(monitor, foreignKey));
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new PostgreTableForeignKeyColumn(
                            foreignKey,
                            (PostgreTableColumn) tableColumn.getOwnColumn(),
                            colIndex++,
                            (PostgreTableColumn) tableColumn.getRefColumn()));
                }
                foreignKey.setDeferrable(editPage.isDeferrable);
                foreignKey.setDeferred(editPage.isDeferred);
                return foreignKey;
            }
        }.execute();
    }

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, PostgreTableBase owner, DBECommandAbstract<PostgreTableForeignKey> command, Map<String, Object> options) {
        PostgreTableForeignKey fk = command.getObject();
        if (fk.isPersisted()) {
            try {
                String constrDDL = fk.getObjectDefinitionText(
                    monitor,
                    Collections.singletonMap(DBPScriptObject.OPTION_EMBEDDED_SOURCE, true));
                if (!CommonUtils.isEmpty(constrDDL)) {
                    return new StringBuilder(constrDDL);
                }
            } catch (DBException e) {
                log.warn("Can't extract FK DDL", e);
            }
        }
        StringBuilder sql = super.getNestedDeclaration(monitor, owner, command, options);

        if (fk.isDeferrable()) {
            sql.append(" DEFERRABLE");
        }
        if (fk.isDeferred()) {
            sql.append(" INITIALLY DEFERRED");
        }

        return sql;
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            PostgreConstraintManager.addConstraintCommentAction(actionList, command.getObject());
        }
    }

    @Override
    protected String getDropForeignKeyPattern(PostgreTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static class EditPGForeignKeyPage extends EditForeignKeyPage {

        private boolean isDeferrable;
        private boolean isDeferred;

        public EditPGForeignKeyPage(String title, DBSTable table) {
            super(title, table, new DBSForeignKeyModifyRule[] {
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                DBSForeignKeyModifyRule.SET_NULL,
                DBSForeignKeyModifyRule.SET_DEFAULT });
        }

        @Override
        protected Composite createPageContents(Composite parent) {
            Composite panel = super.createPageContents(parent);

            final Composite defGroup = UIUtils.createComposite(panel, 2);
            {
                // Cascades
                defGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                final Button deferrableCheck = UIUtils.createCheckbox(defGroup, "Deferrable", false);
                deferrableCheck.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        isDeferrable = deferrableCheck.getSelection();
                    }
                });
                final Button deferredCheck = UIUtils.createCheckbox(defGroup, "Deferred", false);
                deferredCheck.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        isDeferred = deferredCheck.getSelection();
                    }
                });
            }

            return panel;
        }
    }

}
