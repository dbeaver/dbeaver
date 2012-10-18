/*
 * Copyright (C) 2010-2012 Serge Rieder serge@jkiss.org
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageUIGeneral
 */
public class PrefPageUIGeneral extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    static final Log log = LogFactory.getLog(PrefPageUIGeneral.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ui.general"; //$NON-NLS-1$
    private Button automaticUpdateCheck;
    private Text proxyHostText;
    private Spinner proxyPortSpinner;
    private Text proxyUserText;
    private Text proxyPasswordText;
    private SecuredPasswordEncrypter encrypter;

    private Text customDriversHome;

    @Override
    public void init(IWorkbench workbench)
    {
        try {
            encrypter = new SecuredPasswordEncrypter();
        } catch (EncryptionException e) {
            // ignore
            log.warn(e);
        }
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        Group groupObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 300);
        automaticUpdateCheck = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_ui_general_checkbox_automatic_updates, false);

        Group proxyObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_http_proxy, 2, GridData.VERTICAL_ALIGN_BEGINNING, 300);
        proxyHostText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_host, ""); //$NON-NLS-2$
        proxyPortSpinner = UIUtils.createLabelSpinner(proxyObjects, CoreMessages.pref_page_ui_general_spinner_proxy_port, 0, 0, 65535);
        proxyUserText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_user, ""); //$NON-NLS-2$
        proxyPasswordText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_password, "", SWT.PASSWORD | SWT.BORDER); //$NON-NLS-2$

        Group drivers = UIUtils.createControlGroup(composite, "Drivers location", 3, GridData.FILL_HORIZONTAL, 300);
        customDriversHome = UIUtils.createOutputFolderChooser(drivers, null);

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
        proxyUserText.setText(store.getString(PrefConstants.UI_PROXY_USER));
        // Load and decrypt password
        String passwordString = store.getString(PrefConstants.UI_PROXY_PASSWORD);
        if (!CommonUtils.isEmpty(passwordString) && encrypter != null) {
            try {
                passwordString = encrypter.decrypt(passwordString);
            } catch (EncryptionException e) {
                log.warn(e);
            }
        }
        proxyPasswordText.setText(passwordString);
        customDriversHome.setText(DriverDescriptor.getCustomDriversHome().getAbsolutePath());

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();
        store.setValue(PrefConstants.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
        store.setValue(PrefConstants.UI_PROXY_HOST, proxyHostText.getText());
        store.setValue(PrefConstants.UI_PROXY_PORT, proxyPortSpinner.getSelection());
        store.setValue(PrefConstants.UI_PROXY_USER, proxyUserText.getText());
        String password = proxyPasswordText.getText();
        if (!CommonUtils.isEmpty(password) && encrypter != null) {
            // Encrypt password
            try {
                password = encrypter.encrypt(password);
            } catch (EncryptionException e) {
                log.warn(e);
            }
        }
        store.setValue(PrefConstants.UI_PROXY_PASSWORD, password);
        store.setValue(PrefConstants.UI_DRIVERS_HOME, customDriversHome.getText());
        RuntimeUtils.savePreferenceStore(store);

        return super.performOk();
    }

    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {

    }

}