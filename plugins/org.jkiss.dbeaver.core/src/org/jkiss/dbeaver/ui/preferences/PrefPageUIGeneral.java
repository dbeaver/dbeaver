/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * PrefPageUIGeneral
 */
public class PrefPageUIGeneral extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ui.general"; //$NON-NLS-1$
    private Button automaticUpdateCheck;
    private Text proxyHostText;
    private Spinner proxyPortSpinner;

    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group groupObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 300);
        automaticUpdateCheck = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_ui_general_checkbox_automatic_updates, false);

        Group proxyObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_http_proxy, 2, GridData.VERTICAL_ALIGN_BEGINNING, 300);
        proxyHostText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_host, ""); //$NON-NLS-2$
        proxyPortSpinner = UIUtils.createLabelSpinner(proxyObjects, CoreMessages.pref_page_ui_general_spinner_proxy_port, 0, 0, 65535);

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        automaticUpdateCheck.setSelection(store.getBoolean(PrefConstants.UI_AUTO_UPDATE_CHECK));
        proxyHostText.setText(store.getString(PrefConstants.UI_PROXY_HOST));
        proxyPortSpinner.setSelection(store.getInt(PrefConstants.UI_PROXY_PORT));

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();
        store.setValue(PrefConstants.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
        store.setValue(PrefConstants.UI_PROXY_HOST, proxyHostText.getText());
        store.setValue(PrefConstants.UI_PROXY_PORT, proxyPortSpinner.getSelection());
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