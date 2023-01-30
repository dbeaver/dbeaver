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
package org.jkiss.dbeaver.ui.editors.sql.ai.popup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.translator.DAIHistoryManager;
import org.jkiss.dbeaver.model.ai.translator.SimpleFilterManager;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.editors.sql.ai.gpt3.GPTPreferencePage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AISuggestionPopup extends AbstractPopupPanel {

    private static final Map<String, List<String>> queryHistory = new HashMap<>();

    @NotNull
    private static DAIHistoryManager historyManager;

    static {
        historyManager = GeneralUtils.adapt(AISuggestionPopup.class, DAIHistoryManager.class);
        if (historyManager == null) {
            historyManager = new SimpleFilterManager();
        }
    }

    private final DBPDataSourceContainer dataSourceContainer;
    private Text inputField;
    private String inputText;

    public AISuggestionPopup(@NotNull Shell parentShell, @NotNull String title, @NotNull DBPDataSourceContainer dataSourceContainer) {
        super(parentShell, title);
        this.dataSourceContainer = dataSourceContainer;
        setImage(DBIcon.AI);
        setModeless(true);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite placeholder = super.createDialogArea(parent);

        Composite hintPanel = UIUtils.createComposite(placeholder, 2);
        hintPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label hintLabel = new Label(hintPanel, SWT.NONE);
        hintLabel.setText("Enter a text in a human language, it will be translated into SQL.");
        hintLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            ToolBar tb = new ToolBar(hintPanel, SWT.FLAT);
            UIUtils.createToolItem(tb, "Configure", UIIcon.CONFIGURATION,
                SelectionListener.widgetSelectedAdapter(
                    selectionEvent -> UIUtils.showPreferencesFor(getShell(), null, GPTPreferencePage.PAGE_ID)));
        }

        inputField = new Text(placeholder, SWT.BORDER | SWT.MULTI);
        //inputField.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = UIUtils.getFontHeight(placeholder.getFont()) * 10;
        gd.widthHint = UIUtils.getFontHeight(placeholder.getFont()) * 40;
        inputField.setLayoutData(gd);

        inputField.addModifyListener(e -> inputText = inputField.getText());
        inputField.addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.CR && event.stateMask == 0) {
                event.doit = false;
                okPressed();
            }
        });

        Composite historyPanel = UIUtils.createComposite(placeholder, 2);
        historyPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Combo historyCombo = new Combo(historyPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
        historyCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button applyButton = UIUtils.createDialogButton(historyPanel, "Translate",
            SelectionListener.widgetSelectedAdapter(selectionEvent -> okPressed()));

        closeOnFocusLost(inputField, historyCombo, applyButton);

        List<String> queries = queryHistory.get(dataSourceContainer.getId());
        if (!CommonUtils.isEmpty(queries)) {
            for (String query : queries) {
                historyCombo.add(query);
            }
            historyCombo.select(0);
            inputField.setText(queries.get(0));
            inputField.selectAll();
        } else {
            historyCombo.setEnabled(false);
        }

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

    @Override
    protected void okPressed() {
        inputText = inputField.getText().trim();
        if (!CommonUtils.isEmpty(inputText)) {
            List<String> queries = queryHistory.computeIfAbsent(dataSourceContainer.getId(), k -> new ArrayList<>());
            queries.remove(inputText);
            queries.add(0, inputText);
        }

        super.okPressed();
    }

}
