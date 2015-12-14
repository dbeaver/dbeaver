/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * PrefPageConfirmations
 */
public class PrefPageConfirmations extends PreferencePage implements IWorkbenchPreferencePage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.confirmations"; //$NON-NLS-1$

    private Map<String, Combo> confirmChecks = new HashMap<>();

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Composite filterSettings = UIUtils.createPlaceholder(composite, 1, 5);

        {
            Group groupObjects = UIUtils.createControlGroup(filterSettings, CoreMessages.pref_page_confirmations_group_general_actions, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_EXIT);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_ORDER_RESULTSET);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_RS_EDIT_CLOSE);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_RS_FETCH_ALL);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_TXN_DISCONNECT);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_DRIVER_DOWNLOAD);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_VERSION_CHECK);
        }

        {
            Group groupObjects = UIUtils.createControlGroup(filterSettings, CoreMessages.pref_page_confirmations_group_object_editor, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_ENTITY_EDIT_CLOSE);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_ENTITY_DELETE);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_ENTITY_REJECT);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_ENTITY_REVERT);
            createConfirmCheckbox(groupObjects, DBeaverPreferences.CONFIRM_KEEP_STATEMENT_OPEN);
        }

        performDefaults();

        return composite;
    }

    private void createConfirmCheckbox(Composite parent, String id)
    {
        ResourceBundle bundle = DBeaverActivator.getCoreResourceBundle();
        String labelKey = ConfirmationDialog.getResourceKey(id, ConfirmationDialog.RES_KEY_TITLE);

        UIUtils.createControlLabel(parent, bundle.getString(labelKey));
        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
            //UIUtils.createCheckbox(parent, bundle.getString(labelKey), false);
        combo.setItems(new String[] {CoreMessages.pref_page_confirmations_combo_always, CoreMessages.pref_page_confirmations_combo_never, CoreMessages.pref_page_confirmations_combo_prompt} );
        confirmChecks.put(id, combo);
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        for (Map.Entry<String, Combo> entry : confirmChecks.entrySet()) {
            String id = entry.getKey();
            String value = store.getString(ConfirmationDialog.PREF_KEY_PREFIX + id);
            if (ConfirmationDialog.ALWAYS.equals(value)) {
                entry.getValue().select(0);
            } else if (ConfirmationDialog.NEVER.equals(value)) {
                entry.getValue().select(1);
            } else {
                entry.getValue().select(2);
            }
        }

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        for (Map.Entry<String, Combo> entry : confirmChecks.entrySet()) {
            String id = entry.getKey();
            int selectionIndex = entry.getValue().getSelectionIndex();
            if (selectionIndex == 2) {
                store.setToDefault(ConfirmationDialog.PREF_KEY_PREFIX + id);
            } else {
                store.setValue(ConfirmationDialog.PREF_KEY_PREFIX + id, selectionIndex == 0 ? ConfirmationDialog.ALWAYS : ConfirmationDialog.NEVER);
            }
        }

        PrefUtils.savePreferenceStore(store);

        return super.performOk();
    }

}