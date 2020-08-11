/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.generator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.generator.SQLGenerator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.ViewSQLDialog;
import org.jkiss.utils.CommonUtils;

class SQLGeneratorDialog extends ViewSQLDialog {

    private static final String PROP_USE_FQ_NAMES = "GenerateSQL.useFQNames";
    private static final String PROP_USE_COMPACT_SQL = "GenerateSQL.compactSQL";

    private final SQLGenerator<?> sqlGenerator;

    SQLGeneratorDialog(IWorkbenchPartSite parentSite, DBCExecutionContext context, SQLGenerator<?> sqlGenerator) {
        super(parentSite, () -> context,
            "Generated SQL (" + context.getDataSource().getContainer().getName() + ")",
            null, "");
        this.sqlGenerator = sqlGenerator;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        sqlGenerator.setFullyQualifiedNames(
            getDialogBoundsSettings().get(PROP_USE_FQ_NAMES) == null ||
                getDialogBoundsSettings().getBoolean(PROP_USE_FQ_NAMES));
        sqlGenerator.setCompactSQL(
            getDialogBoundsSettings().get(PROP_USE_COMPACT_SQL) != null &&
                getDialogBoundsSettings().getBoolean(PROP_USE_COMPACT_SQL));
        sqlGenerator.setShowPermissions(
                getDialogBoundsSettings().get(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS) != null &&
                        getDialogBoundsSettings().getBoolean(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS));
        sqlGenerator.setShowComments(
                getDialogBoundsSettings().get(DBPScriptObject.OPTION_INCLUDE_COMMENTS) != null &&
                        getDialogBoundsSettings().getBoolean(DBPScriptObject.OPTION_INCLUDE_COMMENTS));
        sqlGenerator.setShowFullDdl(
                getDialogBoundsSettings().get(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS) != null &&
                        getDialogBoundsSettings().getBoolean(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS));
        UIUtils.runInUI(sqlGenerator);
        Object sql = sqlGenerator.getResult();
        if (sql != null) {
            setSQLText(CommonUtils.toString(sql));
        }

        Composite composite = super.createDialogArea(parent);
        Group settings = UIUtils.createControlGroup(composite, "Settings", 5, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        Button useFQNames = UIUtils.createCheckbox(settings, "Use fully qualified names", sqlGenerator.isFullyQualifiedNames());
        useFQNames.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sqlGenerator.setFullyQualifiedNames(useFQNames.getSelection());
                getDialogBoundsSettings().put(PROP_USE_FQ_NAMES, useFQNames.getSelection());

                UIUtils.runInUI(sqlGenerator);
                Object sql = sqlGenerator.getResult();
                if (sql != null) {
                    setSQLText(CommonUtils.toString(sql));
                    updateSQL();
                }
            }
        });
        Button useCompactSQL = UIUtils.createCheckbox(settings, "Compact SQL", sqlGenerator.isCompactSQL());
        useCompactSQL.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sqlGenerator.setCompactSQL(useCompactSQL.getSelection());
                getDialogBoundsSettings().put(PROP_USE_COMPACT_SQL, useCompactSQL.getSelection());

                UIUtils.runInUI(sqlGenerator);
                Object sql = sqlGenerator.getResult();
                if (sql != null) {
                    setSQLText(CommonUtils.toString(sql));
                    updateSQL();
                }
            }
        });
        Button useShowComments = UIUtils.createCheckbox(settings, "Show comments", sqlGenerator.isShowComments());
        useShowComments.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sqlGenerator.setShowComments(useShowComments.getSelection());
                getDialogBoundsSettings().put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, useShowComments.getSelection());

                UIUtils.runInUI(sqlGenerator);
                Object sql = sqlGenerator.getResult();
                if (sql != null) {
                    setSQLText(CommonUtils.toString(sql));
                    updateSQL();
                }
            }
        });
        Button useShowPermissions = UIUtils.createCheckbox(settings, "Show permissions", sqlGenerator.isIncludePermissions());
        useShowPermissions.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sqlGenerator.setShowPermissions(useShowPermissions.getSelection());
                getDialogBoundsSettings().put(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, useShowPermissions.getSelection());

                UIUtils.runInUI(sqlGenerator);
                Object sql = sqlGenerator.getResult();
                if (sql != null) {
                    setSQLText(CommonUtils.toString(sql));
                    updateSQL();
                }
            }
        });
        Button useShowFullDdl = UIUtils.createCheckbox(settings, "Show full DDL", sqlGenerator.isShowFullDdl());
        useShowFullDdl.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sqlGenerator.setShowFullDdl(useShowFullDdl.getSelection());
                getDialogBoundsSettings().put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, useShowFullDdl.getSelection());

                UIUtils.runInUI(sqlGenerator);
                Object sql = sqlGenerator.getResult();
                if (sql != null) {
                    setSQLText(CommonUtils.toString(sql));
                    updateSQL();
                }
            }
        });
        return composite;
    }
}
