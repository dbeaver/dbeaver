/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.QMConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.util.*;

/**
 * PrefPageConfirmations
 */
public class PrefPageConfirmations extends PreferencePage implements IWorkbenchPreferencePage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.confirmations";

    private Map<String, Button> confirmChecks = new HashMap<String, Button>();

    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Composite filterSettings = UIUtils.createPlaceholder(composite, 2, 5);

        Group groupObjects = UIUtils.createControlGroup(filterSettings, "Confirm actions", 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 150);
        createConfirmCheckbox(groupObjects, PrefConstants.CONFIRM_EXIT);
        createConfirmCheckbox(groupObjects, PrefConstants.CONFIRM_ORDER_RESULTSET);

        performDefaults();

        return composite;
    }

    private void createConfirmCheckbox(Composite parent, String id)
    {
        ResourceBundle bundle = DBeaverActivator.getInstance().getResourceBundle();
        String labelKey = ConfirmationDialog.getResourceKey(id, ConfirmationDialog.RES_KEY_TITLE);

        Button checkbox = UIUtils.createCheckbox(parent, bundle.getString(labelKey), false);
        confirmChecks.put(id, checkbox);
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        for (Map.Entry<String, Button> entry : confirmChecks.entrySet()) {
            String id = entry.getKey();
            String value = store.getString(ConfirmationDialog.PREF_KEY_PREFIX + id);
            entry.getValue().setSelection(!ConfirmationDialog.ALWAYS.equals(value));
        }

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();


        for (Map.Entry<String, Button> entry : confirmChecks.entrySet()) {
            String id = entry.getKey();
            if (entry.getValue().getSelection()) {
                store.setToDefault(ConfirmationDialog.PREF_KEY_PREFIX + id);
            } else {
                store.setValue(ConfirmationDialog.PREF_KEY_PREFIX + id, ConfirmationDialog.ALWAYS);
            }
        }

        RuntimeUtils.savePreferenceStore(store);

        return super.performOk();
    }

}