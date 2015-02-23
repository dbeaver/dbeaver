/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.eclipse.ui.texteditor.rulers.RulerColumnPreferenceAdapter;
import org.eclipse.ui.texteditor.rulers.RulerColumnRegistry;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.util.HashMap;
import java.util.Map;

/**
 * PrefPageSQLEditor
 */
public class PrefPageSQLEditor extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqleditor"; //$NON-NLS-1$

    private Button csAutoActivationCheck;
    private Spinner csAutoActivationDelaySpinner;
    private Button csAutoInsertCheck;
    private Combo csInsertCase;
    private Button csHideDuplicates;
    private Map<RulerColumnDescriptor, Button> rulerChecks = new HashMap<RulerColumnDescriptor, Button>();
    private Button acSingleQuotesCheck;
    private Button acDoubleQuotesCheck;
    private Button acBracketsCheck;
    private Button autoFoldersCheck;
    private Text scriptTitlePattern;

    public PrefPageSQLEditor()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(DBeaverPreferences.SCRIPT_AUTO_FOLDERS) ||
            store.contains(DBeaverPreferences.SCRIPT_TITLE_PATTERN) ||
            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES) ||
            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES) ||
            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS)
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

        Composite composite2 = UIUtils.createPlaceholder(composite, 2);
        ((GridLayout)composite2.getLayout()).horizontalSpacing = 5;
        composite2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // Content assistant
        {
            Composite assistGroup = UIUtils.createControlGroup(composite2, "Content assistant", 2, GridData.FILL_HORIZONTAL, 0);
            assistGroup.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));
            ((GridData)assistGroup.getLayoutData()).horizontalSpan = 2;

            csAutoActivationCheck = UIUtils.createLabelCheckbox(assistGroup, "Enable auto activation", "Enables the content assistant's auto activation", false);
            UIUtils.createControlLabel(assistGroup, "Auto activation delay");
            csAutoActivationDelaySpinner = new Spinner(assistGroup, SWT.BORDER);
            csAutoActivationDelaySpinner.setSelection(0);
            csAutoActivationDelaySpinner.setDigits(0);
            csAutoActivationDelaySpinner.setIncrement(50);
            csAutoActivationDelaySpinner.setMinimum(0);
            csAutoActivationDelaySpinner.setMaximum(1000000);
            csAutoInsertCheck = UIUtils.createLabelCheckbox(
                assistGroup,
                "Auto-insert proposal",
                "Enables the content assistant's auto insertion mode.\nIf enabled, the content assistant inserts a proposal automatically if it is the only proposal.\nIn the case of ambiguities, the user must make the choice.",
                false);
            UIUtils.createControlLabel(assistGroup, "Insert case");
            csInsertCase = new Combo(assistGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            csInsertCase.add("Default");
            csInsertCase.add("Upper case");
            csInsertCase.add("Lower case");

            csHideDuplicates = UIUtils.createLabelCheckbox(assistGroup, "Hide duplicate names from\nnon-active schemas", false);
        }

        // Rulers
        {
            Composite rulersGroup = UIUtils.createControlGroup(composite2, "Rulers", 2, GridData.FILL_HORIZONTAL, 0);
            rulersGroup.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));

            for (Object obj : RulerColumnRegistry.getDefault().getColumnDescriptors()) {
                final RulerColumnDescriptor descriptor = (RulerColumnDescriptor)obj;
                if (!descriptor.isGlobal())
                    continue;
                Button checkbox = UIUtils.createCheckbox(rulersGroup, descriptor.getName(), false);
                rulerChecks.put(descriptor, checkbox);
            }
        }

        // Autoclose
        {
            Composite acGroup = UIUtils.createControlGroup(composite2, "Auto close", 2, GridData.FILL_BOTH, 0);

            acSingleQuotesCheck = UIUtils.createLabelCheckbox(acGroup, "Single quotes", false);
            acDoubleQuotesCheck = UIUtils.createLabelCheckbox(acGroup, "Double quotes", false);
            acBracketsCheck = UIUtils.createLabelCheckbox(acGroup, "Brackets", false);
        }

        // Scripts
        {
            Composite scriptsGroup = UIUtils.createControlGroup(composite2, CoreMessages.pref_page_sql_editor_group_resources, 2, GridData.FILL_BOTH, 0);
            ((GridData)scriptsGroup.getLayoutData()).horizontalSpan = 2;

            autoFoldersCheck = UIUtils.createLabelCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_put_new_scripts, false);
            scriptTitlePattern = UIUtils.createLabelText(scriptsGroup, CoreMessages.pref_page_sql_editor_title_pattern, "");

            String[] vars = new String[] {SQLEditorInput.VAR_CONNECTION_NAME, SQLEditorInput.VAR_DRIVER_NAME, SQLEditorInput.VAR_FILE_NAME, SQLEditorInput.VAR_FILE_EXT};
            String[] explain = new String[] {"Connection name", "Database driver name", "File name", "File extension"};
            StringBuilder legend = new StringBuilder("Supported variables: ");
            for (int i = 0; i <vars.length; i++) {
                legend.append("\n\t- ${").append(vars[i]).append("}:  ").append(explain[i]);
            }

            Label legendLabel = new Label(scriptsGroup, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            legendLabel.setLayoutData(gd);
            legendLabel.setText(legend.toString());
        }
        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            csAutoActivationCheck.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
            csAutoActivationDelaySpinner.setSelection(store.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
            csAutoInsertCheck.setSelection(store.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
            csInsertCase.select(store.getInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE));
            csHideDuplicates.setSelection(store.getBoolean(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS));
            acSingleQuotesCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES));
            acDoubleQuotesCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES));
            acBracketsCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS));

            final RulerColumnPreferenceAdapter adapter = new RulerColumnPreferenceAdapter(
                store,
                AbstractTextEditor.PREFERENCE_RULER_CONTRIBUTIONS);
            for (Map.Entry<RulerColumnDescriptor, Button> entry : rulerChecks.entrySet()) {
                entry.getValue().setSelection(adapter.isEnabled(entry.getKey()));
            }

            autoFoldersCheck.setSelection(store.getBoolean(DBeaverPreferences.SCRIPT_AUTO_FOLDERS));
            scriptTitlePattern.setText(store.getString(DBeaverPreferences.SCRIPT_TITLE_PATTERN));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, csAutoActivationCheck.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, csAutoActivationDelaySpinner.getSelection());
            store.setValue(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, csAutoInsertCheck.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_INSERT_CASE, csInsertCase.getSelectionIndex());
            store.setValue(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, csHideDuplicates.getSelection());

            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, acSingleQuotesCheck.getSelection());
            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, acDoubleQuotesCheck.getSelection());
            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, acBracketsCheck.getSelection());

            final RulerColumnPreferenceAdapter adapter = new RulerColumnPreferenceAdapter(
                    store,
                    AbstractTextEditor.PREFERENCE_RULER_CONTRIBUTIONS);
            for (Map.Entry<RulerColumnDescriptor, Button> entry : rulerChecks.entrySet()) {
                adapter.setEnabled(entry.getKey(), entry.getValue().getSelection());
            }

            store.setValue(DBeaverPreferences.SCRIPT_AUTO_FOLDERS, autoFoldersCheck.getSelection());
            store.setValue(DBeaverPreferences.SCRIPT_TITLE_PATTERN, scriptTitlePattern.getText());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION);
        store.setToDefault(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY);
        store.setToDefault(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_INSERT_CASE);
        store.setToDefault(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS);

        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS);

        store.setToDefault(DBeaverPreferences.SCRIPT_AUTO_FOLDERS);
        store.setToDefault(DBeaverPreferences.SCRIPT_TITLE_PATTERN);
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}