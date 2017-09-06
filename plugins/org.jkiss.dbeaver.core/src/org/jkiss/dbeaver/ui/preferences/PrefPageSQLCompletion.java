/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageSQLEditor
 */
public class PrefPageSQLCompletion extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.completion"; //$NON-NLS-1$

    private Button csAutoActivationCheck;
    private Spinner csAutoActivationDelaySpinner;
    private Button csAutoActivateOnKeystroke;
    private Button csAutoInsertCheck;
    private Combo csInsertCase;
    private Button csHideDuplicates;
    private Button csShortName;
    private Button csInsertSpace;
    private Button csUseGlobalSearch;

    private Button csFoldingEnabled;

    public PrefPageSQLCompletion()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION) ||
            store.contains(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY) ||
            store.contains(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION) ||
            store.contains(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_INSERT_CASE) ||
            store.contains(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_SHORT_NAME) ||
            store.contains(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS) ||
            store.contains(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT) ||

            store.contains(SQLPreferenceConstants.FOLDING_ENABLED)
        ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @Override
    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        // Content assistant
        {
            Composite assistGroup = UIUtils.createControlGroup(composite, "SQL assistant/completion", 2, GridData.FILL_HORIZONTAL, 0);

            csAutoActivationCheck = UIUtils.createCheckbox(assistGroup, "Enable auto activation", "Enables content assistant auto activation (on text typing)", false, 2);

            UIUtils.createControlLabel(assistGroup, "Auto activation delay");
            csAutoActivationDelaySpinner = new Spinner(assistGroup, SWT.BORDER);
            csAutoActivationDelaySpinner.setSelection(0);
            csAutoActivationDelaySpinner.setDigits(0);
            csAutoActivationDelaySpinner.setIncrement(50);
            csAutoActivationDelaySpinner.setMinimum(0);
            csAutoActivationDelaySpinner.setMaximum(1000000);
            csAutoActivationDelaySpinner.setToolTipText("Delay before content assistant will run after typing trigger key");

            csAutoActivateOnKeystroke = UIUtils.createCheckbox(
                assistGroup,
                "Activate on typing",
                "Activate completion proposals on any letter typing.",
                false, 2);
            csAutoInsertCheck = UIUtils.createCheckbox(
                assistGroup,
                "Auto-insert proposal",
                "Enables the content assistant's auto insertion mode.\nIf enabled, the content assistant inserts a proposal automatically if it is the only proposal.\nIn the case of ambiguities, the user must make the choice.",
                false, 2);

            UIUtils.createControlLabel(assistGroup, "Insert case");
            csInsertCase = new Combo(assistGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            csInsertCase.add("Default");
            csInsertCase.add("Upper case");
            csInsertCase.add("Lower case");

            csHideDuplicates = UIUtils.createCheckbox(assistGroup, "Hide duplicate names from non-active schemas", null, false, 2);
            csShortName = UIUtils.createCheckbox(assistGroup, "Use short object names (omit schema/catalog)", null, false, 2);
            csInsertSpace = UIUtils.createCheckbox(assistGroup, "Insert space after table/column names", null, false, 2);
            csUseGlobalSearch = UIUtils.createCheckbox(assistGroup, "Use global search (in all schemas)", "Search for objects in all schemas. Otherwise search only in current/system schemas.", false, 2);
        }

        // Content assistant
        {
            Composite foldingGroup = UIUtils.createControlGroup(composite, "Folding", 2, GridData.FILL_HORIZONTAL, 0);

            csUseGlobalSearch = UIUtils.createCheckbox(foldingGroup, "Folding enabled", "Use folding in SQL scripts.", false, 2);
        }
        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            csAutoActivationCheck.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
            csAutoActivationDelaySpinner.setSelection(store.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
            csAutoActivateOnKeystroke.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION));
            csAutoInsertCheck.setSelection(store.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
            csInsertCase.select(store.getInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE));

            csHideDuplicates.setSelection(store.getBoolean(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS));
            csShortName.setSelection(store.getBoolean(SQLPreferenceConstants.PROPOSAL_SHORT_NAME));
            csInsertSpace.setSelection(store.getBoolean(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS));

            csUseGlobalSearch.setSelection(store.getBoolean(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT));

            csUseGlobalSearch.setSelection(store.getBoolean(SQLPreferenceConstants.FOLDING_ENABLED));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, csAutoActivationCheck.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, csAutoActivationDelaySpinner.getSelection());
            store.setValue(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION, csAutoActivateOnKeystroke.getSelection());
            store.setValue(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, csAutoInsertCheck.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_INSERT_CASE, csInsertCase.getSelectionIndex());
            store.setValue(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, csHideDuplicates.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_SHORT_NAME, csShortName.getSelection());
            store.setValue(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS, csInsertSpace.getSelection());
            store.setValue(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT, csUseGlobalSearch.getSelection());

            store.setValue(SQLPreferenceConstants.FOLDING_ENABLED, csUseGlobalSearch.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION);
        store.setToDefault(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY);
        store.setToDefault(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION);
        store.setToDefault(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_INSERT_CASE);
        store.setToDefault(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_SHORT_NAME);
        store.setToDefault(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS);
        store.setToDefault(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT);

        store.setToDefault(SQLPreferenceConstants.FOLDING_ENABLED);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}