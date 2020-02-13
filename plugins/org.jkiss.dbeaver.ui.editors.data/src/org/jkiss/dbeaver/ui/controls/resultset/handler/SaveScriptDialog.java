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
package org.jkiss.dbeaver.ui.controls.resultset.handler;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetSaveReport;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetSaveSettings;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SaveScriptDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.RSV.SaveScriptDialog";//$NON-NLS-1$

    private ResultSetViewer viewer;
    private Object sqlPanel;
    private ResultSetSaveSettings saveSettings;
    private ResultSetSaveReport saveReport;
    private String scriptText;

    SaveScriptDialog(ResultSetViewer viewer, ResultSetSaveReport saveReport) {
        super(viewer.getControl().getShell(), "Preview changes", UIIcon.SQL_SCRIPT);

        this.viewer = viewer;
        this.saveSettings = new ResultSetSaveSettings();
        this.saveReport = saveReport;
    }

    public ResultSetSaveSettings getSaveSettings() {
        return saveSettings;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return null;//UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite messageGroup = super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumWidth = 400;
        messageGroup.setLayoutData(gd);

        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        if (serviceSQL != null) {
            Composite sqlContainer = new Composite(messageGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            sqlContainer.setLayout(new FillLayout());
            gd.widthHint = 500;
            gd.heightHint = 400;
            sqlContainer.setLayoutData(gd);
            try {
                sqlPanel = serviceSQL.createSQLPanel(
                    viewer.getSite(),
                    sqlContainer,
                    viewer,
                    UINavigatorMessages.editors_entity_dialog_preview_title,
                    true,
                    "");
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Can't create SQL panel", "Error creating SQL panel", e);
            }
        }
        populateSQL();

        boolean useDeleteCascade = saveReport.isHasReferences() && saveReport.getDeletes() > 0;
        createDeleteCascadeControls(messageGroup, saveSettings, useDeleteCascade, this::populateSQL);

        return messageGroup;
    }

    public static void createDeleteCascadeControls(
        Composite messageGroup,
        ResultSetSaveSettings settings,
        boolean enableControls,
        Runnable settingsRefreshHandler)
    {
        GridData gd;
        Composite settingsComposite = UIUtils.createComposite(messageGroup, 3);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        settingsComposite.setLayoutData(gd);

        Button useFQNamesCheck = UIUtils.createCheckbox(settingsComposite, "Use fully qualified names", "", settings.isUseFullyQualifiedNames(), 1);
        Button deleteCascadeCheck = UIUtils.createCheckbox(settingsComposite, "Delete cascade",
            "Delete rows from all tables referencing this table by foreign keys", settings.isDeleteCascade(), 1);
        Button deleteDeepCascadeCheck = UIUtils.createCheckbox(settingsComposite, "Deep cascade",
            "Delete cascade recursively (deep references)", settings.isDeepCascade(), 1);

        if (!enableControls) {
            useFQNamesCheck.setEnabled(false);
            deleteCascadeCheck.setEnabled(false);
            deleteDeepCascadeCheck.setEnabled(false);
        } else {
            useFQNamesCheck.setEnabled(true);
            useFQNamesCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setUseFullyQualifiedNames(useFQNamesCheck.getSelection());
                    settingsRefreshHandler.run();
                }
            });
            deleteCascadeCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (deleteCascadeCheck.getSelection()) {
                        settings.setDeleteCascade(true);
                    } else {
                        settings.setDeleteCascade(false);
                        settings.setDeepCascade(false);
                        deleteDeepCascadeCheck.setSelection(false);
                    }
                    settingsRefreshHandler.run();
                }
            });
            deleteDeepCascadeCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (deleteDeepCascadeCheck.getSelection()) {
                        settings.setDeleteCascade(true);
                        settings.setDeepCascade(true);
                        deleteCascadeCheck.setSelection(true);
                    } else {
                        settings.setDeepCascade(false);
                    }
                    settingsRefreshHandler.run();
                }
            });
            // TODO: implement deep cascade
            deleteDeepCascadeCheck.setEnabled(false);
        }
    }

    private static String appendReportLine(String report, int count, String info) {
        if (!report.isEmpty()) report += ", ";
        return report + count + " " + info;
    }

    protected String getDetailsLabel(boolean show) {
        return show ? "SQL >>" : "SQL <<";
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button persistButton = createButton(parent, IDialogConstants.OK_ID, "Persist", false);
        ((GridData) persistButton.getLayoutData()).horizontalAlignment = GridData.BEGINNING;

        Label spacer = new Label(parent, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.minimumWidth = 50;
        spacer.setLayoutData(gd);

        ((GridLayout) parent.getLayout()).numColumns++;
        ((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;

        createButton(parent, IDialogConstants.DETAILS_ID, "Copy", false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    private void populateSQL() {
        try {
            final List<DBEPersistAction> sqlScript = new ArrayList<>();
            UIUtils.runInProgressService(monitor -> {
                List<DBEPersistAction> script = viewer.generateChangesScript(monitor, saveSettings);
                if (script != null) {
                    sqlScript.addAll(script);
                }
            });

            scriptText = "";
            if (!sqlScript.isEmpty()) {
                scriptText = SQLUtils.generateScript(
                    viewer.getDataSource(),
                    sqlScript.toArray(new DBEPersistAction[0]),
                    false);
                scriptText =
                    SQLUtils.generateCommentLine(
                        viewer.getDataSource(),
                        "Auto-generated SQL script #" + new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN).format(new Date())) +
                        scriptText;
                UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                if (serviceSQL != null) {
                    serviceSQL.setSQLPanelText(sqlPanel, scriptText);
                }
            }
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Can't generate SQL script", "Error generating SQL script from data changes", e);
        }
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            ResultSetUtils.copyToClipboard(scriptText);
            super.buttonPressed(IDialogConstants.CANCEL_ID);
        }
        super.buttonPressed(buttonId);
    }
}
