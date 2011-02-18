/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * PrefPageConfirmations
 */
public class PrefPageConfirmations extends PreferencePage implements IWorkbenchPreferencePage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.confirmations";

    private Map<String, Combo> confirmChecks = new HashMap<String, Combo>();

    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Composite filterSettings = UIUtils.createPlaceholder(composite, 1, 5);

        Group groupObjects = UIUtils.createControlGroup(filterSettings, "Confirm actions", 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        createConfirmCheckbox(groupObjects, PrefConstants.CONFIRM_EXIT);
        createConfirmCheckbox(groupObjects, PrefConstants.CONFIRM_ORDER_RESULTSET);
        createConfirmCheckbox(groupObjects, PrefConstants.CONFIRM_RS_EDIT_CLOSE);
        createConfirmCheckbox(groupObjects, PrefConstants.CONFIRM_TXN_DISCONNECT);
        createConfirmCheckbox(groupObjects, PrefConstants.CONFIRM_ENTITY_EDIT_CLOSE);

        performDefaults();

        return composite;
    }

    private void createConfirmCheckbox(Composite parent, String id)
    {
        ResourceBundle bundle = DBeaverActivator.getInstance().getResourceBundle();
        String labelKey = ConfirmationDialog.getResourceKey(id, ConfirmationDialog.RES_KEY_TITLE);

        UIUtils.createControlLabel(parent, bundle.getString(labelKey));
        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
            //UIUtils.createCheckbox(parent, bundle.getString(labelKey), false);
        combo.setItems(new String[] {"Always", "Never", "Prompt"} );
        confirmChecks.put(id, combo);
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

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
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        for (Map.Entry<String, Combo> entry : confirmChecks.entrySet()) {
            String id = entry.getKey();
            int selectionIndex = entry.getValue().getSelectionIndex();
            if (selectionIndex == 2) {
                store.setToDefault(ConfirmationDialog.PREF_KEY_PREFIX + id);
            } else {
                store.setValue(ConfirmationDialog.PREF_KEY_PREFIX + id, selectionIndex == 0 ? ConfirmationDialog.ALWAYS : ConfirmationDialog.NEVER);
            }
        }

        RuntimeUtils.savePreferenceStore(store);

        return super.performOk();
    }

}