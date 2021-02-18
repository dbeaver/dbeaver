/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetSaveReport;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetSaveSettings;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.dialogs.DetailsViewDialog;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

import java.util.ArrayList;
import java.util.List;

public class SavePreviewDialog extends DetailsViewDialog {

    private static final String DIALOG_ID = "DBeaver.RSV.SavePreviewDialog";//$NON-NLS-1$

    private ResultSetViewer viewer;
    private boolean showCascadeSettings;
    private Object sqlPanel;
    private ResultSetSaveSettings saveSettings;
    private ResultSetSaveReport saveReport;

    public SavePreviewDialog(@NotNull ResultSetViewer viewer, boolean showCascadeSettings, @NotNull ResultSetSaveReport saveReport) {
        super(viewer.getControl().getShell(), "Preview changes", UIIcon.SQL_SCRIPT);

        this.viewer = viewer;
        this.showCascadeSettings = showCascadeSettings;

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
    protected void createMessageArea(Composite composite) {

        Composite messageGroup = UIUtils.createComposite(composite, 1);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumWidth = 400;
        messageGroup.setLayoutData(gd);

        String changesReport = "";

        if (saveReport.getInserts() > 0)
            changesReport = appendReportLine(changesReport, saveReport.getInserts(), "rows(s) added");
        if (saveReport.getUpdates() > 0)
            changesReport = appendReportLine(changesReport, saveReport.getUpdates(), "rows(s) changed");
        if (saveReport.getDeletes() > 0)
            changesReport = appendReportLine(changesReport, saveReport.getDeletes(), "rows(s) deleted");

        {
            Composite msgComposite = UIUtils.createComposite(messageGroup, 2);
            Label imgLabel = new Label(msgComposite, SWT.NONE);
            imgLabel.setImage(DBeaverIcons.getImage(UIIcon.SQL_SCRIPT));
            Label msgText = new Label(msgComposite, SWT.NONE);
            msgText.setText("You are about to save your changes into the database (" + viewer.getDataSource().getContainer().getName() + ").\n" +
                changesReport + ".\nAre you sure you want to proceed?");
        }

        //UIUtils.createHorizontalLine(messageGroup);
        if (showCascadeSettings) {
            boolean useDeleteCascade = saveReport.isHasReferences() && saveReport.getDeletes() > 0;
            SaveScriptDialog.createDeleteCascadeControls(messageGroup, saveSettings, useDeleteCascade, this::populateSQL);
        }


            /*
        if (saveReport.getDeletes() > 0) {
            Composite settingsComposite = UIUtils.createComposite(messageGroup, showCascadeSettings ? 2 : 1);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            settingsComposite.setLayoutData(gd);

            Button deleteCascadeCheck = UIUtils.createCheckbox(settingsComposite, "Delete cascade",
                "Delete rows from all tables referencing this table by foreign keys", false, 1);
            if (showCascadeSettings) UIUtils.createEmptyLabel(settingsComposite, 1, 1);
            Button deleteDeepCascadeCheck = UIUtils.createCheckbox(settingsComposite, "Deep cascade",
                "Delete cascade recursively (deep references)", false, 1);

            if (showCascadeSettings) {
                Button hideDialogButton = UIUtils.createCheckbox(settingsComposite, "Do not show again",
                    "Do not show this dialog next time (you can re-enable this option in preferences/confirmations)", false, 1);
                gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
                gd.grabExcessHorizontalSpace = true;
                hideDialogButton.setLayoutData(gd);
            }
        }*/
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

        createDetailsButton(parent);
        ((GridData) detailsButton.getLayoutData()).horizontalAlignment = GridData.BEGINNING;

        Label spacer = new Label(parent, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.minimumWidth = 50;
        spacer.setLayoutData(gd);

        ((GridLayout) parent.getLayout()).numColumns++;
        ((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;

        createButton(parent, IDialogConstants.OK_ID, UINavigatorMessages.dialog_filter_save_button, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Control createDetailsContents(Composite composite) {
        Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(1, true));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite previewFrame = new Composite(group, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 250;
        previewFrame.setLayoutData(gd);
        previewFrame.setLayout(new FillLayout());

        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        if (serviceSQL != null) {
            try {
                sqlPanel = serviceSQL.createSQLPanel(
                    viewer.getSite(),
                    previewFrame,
                    viewer,
                    UINavigatorMessages.editors_entity_dialog_preview_title,
                    true,
                    "");
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Can't create SQL panel", "Error creating SQL panel", e);
            }
        }
        populateSQL();

        return previewFrame;
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

            String scriptText = "";
            if (!sqlScript.isEmpty()) {
                scriptText = SQLUtils.generateScript(
                    viewer.getDataSource(),
                    sqlScript.toArray(new DBEPersistAction[0]),
                    false);
                scriptText =
                    SQLUtils.generateCommentLine(
                        viewer.getDataSource(),
                        "Auto-generated SQL script. Actual values for binary/complex data types may differ - what you see is the default string representation of values.") +
                        scriptText;
                UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                if (serviceSQL != null) {
                    serviceSQL.setSQLPanelText(sqlPanel, scriptText);
                }
            }
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Can't generalte SQL script", "Error generating SQL script from changes", e);
        }
    }

}
