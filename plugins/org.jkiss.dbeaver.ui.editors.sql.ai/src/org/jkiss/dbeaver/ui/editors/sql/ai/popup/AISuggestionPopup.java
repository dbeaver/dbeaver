/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.ai.popup;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionScope;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionSettings;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.qm.QMTranslationHistoryItem;
import org.jkiss.dbeaver.model.qm.QMTranslationHistoryManager;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.editors.sql.ai.controls.ScopeSelectorControl;
import org.jkiss.dbeaver.ui.editors.sql.ai.preferences.AIPreferencePage;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class AISuggestionPopup extends AbstractPopupPanel {

    private static final Log log = Log.getLog(AISuggestionPopup.class);

    @NotNull
    private final QMTranslationHistoryManager historyManager;

    @NotNull
    private final DBSLogicalDataSource dataSource;
    @NotNull
    private final DBCExecutionContext executionContext;
    private final DAICompletionSettings settings;

    private Text inputField;
    private String inputText;
    private ScopeSelectorControl scopeSelectorControl;

    public AISuggestionPopup(
        @NotNull Shell parentShell,
        @NotNull String title,
        @NotNull QMTranslationHistoryManager historyManager,
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DAICompletionSettings settings) {
        super(parentShell, title);
        this.historyManager = historyManager;
        this.dataSource = dataSource;
        this.executionContext = executionContext;
        this.settings = settings;
        setImage(DBIcon.AI);
        setModeless(true);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite placeholder = super.createDialogArea(parent);

        Composite hintPanel = UIUtils.createComposite(placeholder, 2);
        hintPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Link hintLabel = new Link(hintPanel, SWT.NONE);
        hintLabel.setText("Enter a text in a human language, it will be translated into SQL (<a>instructions</a>)");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        hintLabel.setLayoutData(gd);
        hintLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                UIUtils.openWebBrowser(HelpUtils.getHelpExternalReference("AI-Smart-Assistance"));
            }
        });

        scopeSelectorControl = new ScopeSelectorControl(placeholder, dataSource, executionContext, settings);
        scopeSelectorControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createToolItem(
            scopeSelectorControl.getToolBar(),
            "Settings",
            UIIcon.CONFIGURATION,
            SelectionListener.widgetSelectedAdapter(e -> UIUtils.showPreferencesFor(getShell(), null, AIPreferencePage.PAGE_ID))
        );

        inputField = new Text(placeholder, SWT.BORDER | SWT.MULTI);
        //inputField.setLayoutData(new GridData(GridData.FILL_BOTH));
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = UIUtils.getFontHeight(placeholder.getFont()) * 10;
        gd.widthHint = UIUtils.getFontHeight(placeholder.getFont()) * 40;
        inputField.setLayoutData(gd);
        inputField.setTextLimit(10000);

        inputField.addModifyListener(e -> inputText = inputField.getText());
        inputField.addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.CR && event.stateMask == 0) {
                event.doit = false;
                okPressed();
            }
        });

        Composite miscPanel = UIUtils.createComposite(placeholder, 2);
        miscPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(miscPanel, "History");
        Combo historyCombo = new Combo(miscPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
        historyCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button applyButton = UIUtils.createDialogButton(placeholder, "Translate",
            SelectionListener.widgetSelectedAdapter(selectionEvent -> okPressed()));
        ((GridData)applyButton.getLayoutData()).grabExcessHorizontalSpace = false;
        ((GridData)applyButton.getLayoutData()).horizontalAlignment = GridData.END;

        closeOnFocusLost(
            inputField,
            scopeSelectorControl.getScopeCombo(),
            scopeSelectorControl.getScopeText(),
            historyCombo,
            applyButton
        );

        historyCombo.setEnabled(false);
        AbstractJob completionJob = new AbstractJob("Read completion history") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    List<QMTranslationHistoryItem> queries = historyManager.readTranslationHistory(monitor, dataSource, executionContext, 100);
                    UIUtils.syncExec(() -> {
                        if (!CommonUtils.isEmpty(queries)) {
                            for (QMTranslationHistoryItem query : queries) {
                                historyCombo.add(query.getNaturalText());
                            }
                            historyCombo.select(0);
                            inputField.setText(queries.get(0).getNaturalText());
                            inputField.selectAll();
                            historyCombo.setEnabled(true);
                        } else {
                            historyCombo.setEnabled(false);
                        }
                    });
                } catch (DBException e) {
                    log.error("Error reading completion history", e);
                }
                return Status.OK_STATUS;
            }
        };
        completionJob.schedule();

        inputField.setFocus();

        historyCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String text = historyCombo.getText();
                if (!CommonUtils.isEmpty(text)) {
                    inputField.setText(text);
                }
            }
        });

        return placeholder;
    }

    protected void createButtonsForButtonBar(Composite parent) {
        // No buttons
    }

    @Override
    protected boolean isModeless() {
        return true;
    }

    public String getInputText() {
        return inputText;
    }

    public DAICompletionScope getScope() {
        return scopeSelectorControl.getScope();
    }

    public List<DBSEntity> getCustomEntities(@NotNull DBRProgressMonitor monitor) {
        return scopeSelectorControl.getCustomEntities(monitor);
    }

    @Override
    protected void okPressed() {
        inputText = inputField.getText().trim();

        settings.setScope(scopeSelectorControl.getScope());
        settings.setCustomObjectIds(scopeSelectorControl.getCheckedObjectIds().toArray(new String[0]));
        settings.saveSettings();

        super.okPressed();
    }
}
