/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ResourceBundle;

/**
 * PrefPageConfirmations
 */
public class PrefPageConfirmations extends AbstractPrefPage implements IWorkbenchPreferencePage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.confirmations"; //$NON-NLS-1$

    private Table confirmTable;

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        ResourceBundle coreBundle = DBeaverActivator.getCoreResourceBundle();
        ResourceBundle rsvBundle = ResourceBundle.getBundle(ResultSetMessages.BUNDLE_NAME);
        ResourceBundle navigatorBundle = ResourceBundle.getBundle(UINavigatorMessages.BUNDLE_NAME);

        Composite composite = UIUtils.createPlaceholder(parent, 1);

        confirmTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        confirmTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        confirmTable.setHeaderVisible(true);
        confirmTable.setLinesVisible(true);
        UIUtils.createTableColumn(confirmTable, SWT.LEFT, "Confirmation");
        UIUtils.createTableColumn(confirmTable, SWT.LEFT, "Group");
        UIUtils.createTableColumn(confirmTable, SWT.RIGHT, "Value");

        final CustomTableEditor tableEditor = new CustomTableEditor(confirmTable) {
            {
                firstTraverseIndex = 2;
                lastTraverseIndex = 2;
                editOnEnter = false;
            }
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                if (index != 2) {
                    return null;
                }
                CCombo editor = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
                editor.setItems(new String[] { CoreMessages.pref_page_confirmations_combo_always, CoreMessages.pref_page_confirmations_combo_never, CoreMessages.pref_page_confirmations_combo_prompt} );
                editor.setText(item.getText(2));
                return editor;
            }
            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                item.setText(2, ((CCombo) control).getText());

            }
        };

        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_general_actions, coreBundle, DBeaverPreferences.CONFIRM_EXIT);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_general_actions, rsvBundle, ResultSetPreferences.CONFIRM_ORDER_RESULTSET);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_general_actions, rsvBundle, ResultSetPreferences.CONFIRM_RS_EDIT_CLOSE);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_general_actions, rsvBundle, ResultSetPreferences.CONFIRM_RS_FETCH_ALL);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_general_actions, coreBundle, DBeaverPreferences.CONFIRM_TXN_DISCONNECT);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_general_actions, coreBundle, DBeaverPreferences.CONFIRM_DRIVER_DOWNLOAD);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_general_actions, coreBundle, DBeaverPreferences.CONFIRM_VERSION_CHECK);

        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, navigatorBundle, NavigatorPreferences.CONFIRM_ENTITY_EDIT_CLOSE);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, navigatorBundle, NavigatorPreferences.CONFIRM_ENTITY_DELETE);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, navigatorBundle, NavigatorPreferences.CONFIRM_ENTITY_REJECT);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, navigatorBundle, NavigatorPreferences.CONFIRM_ENTITY_REVERT);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, rsvBundle, ResultSetPreferences.CONFIRM_KEEP_STATEMENT_OPEN);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, coreBundle, DBeaverPreferences.CONFIRM_DANGER_SQL);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, coreBundle, DBeaverPreferences.CONFIRM_MASS_PARALLEL_SQL);

        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, navigatorBundle, NavigatorPreferences.CONFIRM_EDITOR_CLOSE);
        createConfirmCheckbox(CoreMessages.pref_page_confirmations_group_object_editor, coreBundle, DBeaverPreferences.CONFIRM_RUNNING_QUERY_CLOSE);

        UIUtils.asyncExec(() -> UIUtils.packColumns(confirmTable, true));

        //performDefaults();

        return composite;
    }

    private void createConfirmCheckbox(String group, ResourceBundle bundle, String id)
    {
        String labelKey = ConfirmationDialog.getResourceKey(id, ConfirmationDialog.RES_KEY_TITLE);
        String title = bundle.getString(labelKey);

        TableItem item = new TableItem(confirmTable, SWT.NONE);
        item.setData("id", id);
        item.setData("bundle", bundle);

        item.setText(0, title);
        item.setText(1, group);
        item.setText(2, getCurrentConfirmValue(id));
    }

    private String getCurrentConfirmValue(String id) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        String value = store.getString(ConfirmationDialog.PREF_KEY_PREFIX + id);
        if (CommonUtils.isEmpty(value)) {
            value = ConfirmationDialog.PROMPT;
        }

        switch (value) {
            case ConfirmationDialog.ALWAYS: return CoreMessages.pref_page_confirmations_combo_always;
            case ConfirmationDialog.NEVER: return CoreMessages.pref_page_confirmations_combo_never;
            default: return CoreMessages.pref_page_confirmations_combo_prompt;
        }
    }

    @Override
    protected void performDefaults()
    {
        for (TableItem item : confirmTable.getItems()) {
            String id = (String) item.getData("id");
            item.setText(2, getCurrentConfirmValue(id));
        }

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        for (TableItem item : confirmTable.getItems()) {
            String id = (String) item.getData("id");
            String title = item.getText(2);
            String value;
            if (title.equals(CoreMessages.pref_page_confirmations_combo_always)) {
                value = ConfirmationDialog.ALWAYS;
            } else if (title.equals(CoreMessages.pref_page_confirmations_combo_never)) {
                value = ConfirmationDialog.NEVER;
            } else {
                value = ConfirmationDialog.PROMPT;
            }
            store.setValue(ConfirmationDialog.PREF_KEY_PREFIX + id, value);
        }

        PrefUtils.savePreferenceStore(store);

        return super.performOk();
    }

}