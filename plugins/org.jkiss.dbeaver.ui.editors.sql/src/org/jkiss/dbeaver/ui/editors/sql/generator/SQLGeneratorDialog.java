/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.generator.SQLGenerator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.ViewSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.utils.CommonUtils;

class SQLGeneratorDialog extends ViewSQLDialog {
    private static final Log log = Log.getLog(SQLGeneratorDialog.class);

    private static final String PROP_USE_FQ_NAMES = "GenerateSQL.useFQNames";
    private static final String PROP_USE_COMPACT_SQL = "GenerateSQL.compactSQL";
    private static final String PROP_EXCLUDE_AUTO_GENERATED_COLUMNS = "GenerateSQL.excludeAutoGeneratedColumn";
    private static final String PROP_USE_CUSTOM_DATA_FORMAT = "GenerateSQL.useCustomDataFormat";
    private static final String PROP_USE_SEPARATE_FK_STATEMENTS = "GenerateSQL.useSeparateFKStatements";

    private final SQLGenerator<?> sqlGenerator;
    private Job generateDDLJob;

    SQLGeneratorDialog(IWorkbenchPartSite parentSite, DBCExecutionContext context, SQLGenerator<?> sqlGenerator) {
        super(parentSite, () -> context,
            NLS.bind(SQLEditorMessages.sql_generator_dialog_title, context.getDataSource().getContainer().getName()),
            null, "");
        this.sqlGenerator = sqlGenerator;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        sqlGenerator.setFullyQualifiedNames(getDialogBoundsSettings().get(PROP_USE_FQ_NAMES) == null ||
                getDialogBoundsSettings().getBoolean(PROP_USE_FQ_NAMES));
        sqlGenerator.setCompactSQL(getDialogBoundsSettings().get(PROP_USE_COMPACT_SQL) != null &&
                getDialogBoundsSettings().getBoolean(PROP_USE_COMPACT_SQL));
        sqlGenerator.setExcludeAutoGeneratedColumn(getDialogBoundsSettings().get(PROP_EXCLUDE_AUTO_GENERATED_COLUMNS) != null &&
                getDialogBoundsSettings().getBoolean(PROP_EXCLUDE_AUTO_GENERATED_COLUMNS));
        sqlGenerator.setUseSeparateForeignKeys(getDialogBoundsSettings().get(PROP_USE_SEPARATE_FK_STATEMENTS) == null ||
                getDialogBoundsSettings().getBoolean(PROP_USE_SEPARATE_FK_STATEMENTS));

        boolean supportPermissions = false;
        boolean supportComments = false;
        boolean supportFullDDL = false;
        boolean supportSeparateFKStatements = false;
        boolean supportsPartitionsDDL = false;
        for (Object object : sqlGenerator.getObjects()) {
            if (object instanceof DBPScriptObjectExt2) {
                DBPScriptObjectExt2 sourceObject = (DBPScriptObjectExt2) object;
                if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
                    supportPermissions = true;
                }
                if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
                    supportComments = true;
                }
                if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS)) {
                    supportFullDDL = true;
                }
                if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS) && 
                    sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS)) { 
                    supportSeparateFKStatements = true;
                }
                if (supportPermissions && supportComments && supportFullDDL) {
                    break; //it supports everything
                }
                if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_PARTITIONS)) {
                    supportsPartitionsDDL = true;
                }
            }
        }

        sqlGenerator.setShowPermissions(getDialogBoundsSettings().get(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS) != null &&
                getDialogBoundsSettings().getBoolean(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS));
        sqlGenerator.setShowComments(getDialogBoundsSettings().get(DBPScriptObject.OPTION_INCLUDE_COMMENTS) != null &&
                getDialogBoundsSettings().getBoolean(DBPScriptObject.OPTION_INCLUDE_COMMENTS));
        sqlGenerator.setShowFullDdl(getDialogBoundsSettings().get(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS) != null &&
                getDialogBoundsSettings().getBoolean(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS));

        generateDDLJob = new AbstractJob("Generating DDL") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    DBExecUtils.tryExecuteRecover(monitor, getExecutionContext().getDataSource(), param -> {
                        sqlGenerator.run(monitor);
                    });
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    Object sql = sqlGenerator.getResult();
                    UIUtils.syncExec(() -> {
                        if (SQLGeneratorDialog.this.getShell() != null && !SQLGeneratorDialog.this.getShell()
                            .isDisposed()) {
                            if (sql != null) {
                                setSQLText(CommonUtils.toString(sql));
                                updateSQL();
                            }
                            Button button = getButton(IDialogConstants.DETAILS_ID);
                            if (button != null) {
                                button.setEnabled(true);
                            }
                        }
                    });

                    return Status.OK_STATUS;
                } catch (DBException e) {
                    UIUtils.syncExec(() -> {
                        if (SQLGeneratorDialog.this.getShell() != null && !SQLGeneratorDialog.this.getShell().isDisposed()) {
                            setSQLText("Error running DDL generation");
                        }
                    });
                    log.error(e);
                    return Status.error("Error running DDL generation", e);
                }
            }
        };

        Composite composite = super.createDialogArea(parent);
        startGenerateJob();

        if (!sqlGenerator.hasOptions()) {
            return composite;
        }
        
        Group settings = UIUtils.createControlGroup(composite, "Settings", 5, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        settings.setLayout(new RowLayout());
        Button useFQNames = UIUtils.createCheckbox(settings, SQLEditorMessages.sql_generator_dialog_button_use_fully_names, sqlGenerator.isFullyQualifiedNames());
        useFQNames.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sqlGenerator.setFullyQualifiedNames(useFQNames.getSelection());
                getDialogBoundsSettings().put(PROP_USE_FQ_NAMES, useFQNames.getSelection());
                startGenerateJob();
            }
        });
        Button useCompactSQL = UIUtils.createCheckbox(settings, SQLEditorMessages.sql_generator_dialog_button_compact_sql, sqlGenerator.isCompactSQL());
        useCompactSQL.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sqlGenerator.setCompactSQL(useCompactSQL.getSelection());
                getDialogBoundsSettings().put(PROP_USE_COMPACT_SQL, useCompactSQL.getSelection());
                startGenerateJob();
            }
        });
        if (sqlGenerator.isInsertOption()) {
            Button excludeAutoGeneratedColumn = UIUtils.createCheckbox(settings, SQLEditorMessages.sql_generator_dialog_button_exclude_columns, sqlGenerator.isExcludeAutoGeneratedColumn());
            excludeAutoGeneratedColumn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setExcludeAutoGeneratedColumn(excludeAutoGeneratedColumn.getSelection());
                    getDialogBoundsSettings().put(PROP_EXCLUDE_AUTO_GENERATED_COLUMNS, excludeAutoGeneratedColumn.getSelection());
                    startGenerateJob();
                }
            });
        }
        if (sqlGenerator.isDMLOption()) {
            Button useCustomDateFormat = UIUtils.createCheckbox(settings, SQLEditorMessages.sql_generator_dialog_button_use_custom_data_format, sqlGenerator.isUseCustomDataFormat());
            useCustomDateFormat.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setUseCustomDataFormat(useCustomDateFormat.getSelection());
                    getDialogBoundsSettings().put(PROP_USE_CUSTOM_DATA_FORMAT, useCustomDateFormat.getSelection());
                    startGenerateJob();
                }
            });
        }
        if (sqlGenerator.isDDLOption()) {
            if (supportComments) {
                Button useShowComments = UIUtils.createCheckbox(settings, SQLEditorMessages.sql_generator_dialog_button_show_comments, sqlGenerator.isShowComments());
                useShowComments.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        sqlGenerator.setShowComments(useShowComments.getSelection());
                        getDialogBoundsSettings().put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, useShowComments.getSelection());
                        startGenerateJob();
                    }
                });
            }
            if (supportPermissions) {
                Button useShowPermissions = UIUtils.createCheckbox(settings, SQLEditorMessages.sql_generator_dialog_button_show_permissions, sqlGenerator.isIncludePermissions());
                useShowPermissions.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        sqlGenerator.setShowPermissions(useShowPermissions.getSelection());
                        getDialogBoundsSettings().put(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, useShowPermissions.getSelection());
                        startGenerateJob();
                    }
                });
            }
        }
        if (supportFullDDL) {
            Button useShowFullDdl = UIUtils.createCheckbox(settings, SQLEditorMessages.sql_generator_dialog_button_show_full_DDL, sqlGenerator.isShowFullDdl());
            useShowFullDdl.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setShowFullDdl(useShowFullDdl.getSelection());
                    getDialogBoundsSettings().put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, useShowFullDdl.getSelection());
                    startGenerateJob();
                }
            });
        }
        if (supportSeparateFKStatements) {
            Button useSeparateFkStatements = UIUtils.createCheckbox(settings, SQLEditorMessages.sql_generator_dialog_button_separate_fk_constraints_definition, sqlGenerator.isUseSeparateForeignKeys());
            useSeparateFkStatements.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setUseSeparateForeignKeys(useSeparateFkStatements.getSelection());
                    getDialogBoundsSettings().put(DBPScriptObject.OPTION_DDL_SEPARATE_FOREIGN_KEYS_STATEMENTS, useSeparateFkStatements.getSelection());
                    startGenerateJob();
                }
            });
        }
        if (supportsPartitionsDDL) {
            Button supportsPartitionsDDLButton = UIUtils.createCheckbox(
                settings,
                SQLEditorMessages.sql_generator_dialog_button_show_partitions_DDL,
                sqlGenerator.isShowPartitionsDDL());
            supportsPartitionsDDLButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setShowPartitionsDDL(supportsPartitionsDDLButton.getSelection());
                    getDialogBoundsSettings().put(DBPScriptObject.OPTION_INCLUDE_PARTITIONS, supportsPartitionsDDLButton.getSelection());
                    startGenerateJob();
                }
            });
        }
        if (sqlGenerator.supportCastParams()) {
            Button supportsCastParamsButton = UIUtils.createCheckbox(
                    settings,
                    SQLEditorMessages.sql_generator_dialog_button_show_cast_params,
                    sqlGenerator.isShowCastParams());
            supportsCastParamsButton.setToolTipText(SQLEditorMessages.sql_generator_dialog_button_show_cast_params_tip);
            supportsCastParamsButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setShowCastParams(supportsCastParamsButton.getSelection());
                    getDialogBoundsSettings().put(DBPScriptObject.OPTION_CAST_PARAMS, supportsCastParamsButton.getSelection());
                    startGenerateJob();
                }
            });
        }
        return composite;
    }

    private void startGenerateJob() {
        setSQLText("Loading DDL...");
        updateSQL();
        Button button = getButton(IDialogConstants.DETAILS_ID);
        if (button != null) {
            button.setEnabled(false);
        }
        generateDDLJob.cancel();
        generateDDLJob.schedule();
    }

    @Override
    public boolean close() {
        generateDDLJob.cancel();
        return super.close();
    }
}
