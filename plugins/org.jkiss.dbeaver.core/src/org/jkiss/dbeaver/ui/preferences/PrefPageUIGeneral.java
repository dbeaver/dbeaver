/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * PrefPageUIGeneral
 */
public class PrefPageUIGeneral extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ui.general";
    private Button automaticUpdateCheck;

    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group groupObjects = UIUtils.createControlGroup(composite, "General", 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 150);
        automaticUpdateCheck = UIUtils.createCheckbox(groupObjects, "Automatic Updates Check", false);

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        automaticUpdateCheck.setSelection(store.getBoolean(PrefConstants.UI_AUTO_UPDATE_CHECK));

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();
        store.setValue(PrefConstants.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
        RuntimeUtils.savePreferenceStore(store);

        return super.performOk();
    }

    public IAdaptable getElement()
    {
        return null;
    }

    public void setElement(IAdaptable element)
    {

    }
}