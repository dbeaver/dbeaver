/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.encode.EncryptionException;
import org.jkiss.dbeaver.runtime.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.StringJoiner;

/**
 * PrefPageDrivers
 */
public class PrefPageDrivers extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    private static final Log log = Log.getLog(PrefPageDrivers.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.drivers"; //$NON-NLS-1$

    private Button versionUpdateCheck;

    private List sourceList;

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

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group settings = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_ui_general_group_settings, 2, GridData.FILL_HORIZONTAL, 300);
            versionUpdateCheck = UIUtils.createCheckbox(settings, UIConnectionMessages.pref_page_ui_general_check_new_driver_versions, false);
        }

        {
            Group proxyObjects = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_ui_general_group_http_proxy, 4, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 300);
            proxyHostText = UIUtils.createLabelText(proxyObjects, UIConnectionMessages.pref_page_ui_general_label_proxy_host, null); //$NON-NLS-2$
            proxyPortSpinner = UIUtils.createLabelSpinner(proxyObjects, UIConnectionMessages.pref_page_ui_general_spinner_proxy_port, 0, 0, 65535);
            proxyUserText = UIUtils.createLabelText(proxyObjects, UIConnectionMessages.pref_page_ui_general_label_proxy_user, null); //$NON-NLS-2$
            proxyPasswordText = UIUtils.createLabelText(proxyObjects, UIConnectionMessages.pref_page_ui_general_label_proxy_password, null, SWT.PASSWORD | SWT.BORDER); //$NON-NLS-2$
        }

        {
            Group drivers = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_drivers_group_location, 2, GridData.FILL_HORIZONTAL, 300);
            customDriversHome = DialogUtils.createOutputFolderChooser(drivers, UIConnectionMessages.pref_page_drivers_local_folder, null);
        }

        {
            Group repoGroup = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_drivers_group_file_repositories, 2, GridData.FILL_HORIZONTAL, 300);
            sourceList = new List(repoGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
            sourceList.setLayoutData(new GridData(GridData.FILL_BOTH));

            final ToolBar toolbar = new ToolBar(repoGroup, SWT.VERTICAL);
            toolbar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            UIUtils.createToolItem(toolbar, UIConnectionMessages.pref_page_drivers_button_add, UIIcon.ADD, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    String url = EnterNameDialog.chooseName(getShell(), UIConnectionMessages.pref_page_drivers_label_enter_drivers_location_url, "http://");
                    if (url != null) {
                        sourceList.add(url);
                    }
                }
            });
            final ToolItem removeButton = UIUtils.createToolItem(toolbar, UIConnectionMessages.pref_page_drivers_button_remove, UIIcon.DELETE, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    final int index = sourceList.getSelectionIndex();
                    sourceList.remove(index);
                    sourceList.select(CommonUtils.clamp(index, 0, sourceList.getItemCount() - 1));
                    sourceList.notifyListeners(SWT.Selection, new Event());
                }
            });
            removeButton.setEnabled(false);

            sourceList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    if (sourceList.getSelectionIndex() >= 0) {
                        removeButton.setEnabled(sourceList.getItemCount() > 1);
                    } else {
                        removeButton.setEnabled(false);
                    }
                }
            });
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        versionUpdateCheck.setSelection(store.getBoolean(ModelPreferences.UI_DRIVERS_VERSION_UPDATE));

        proxyHostText.setText(store.getString(ModelPreferences.UI_PROXY_HOST));
        proxyPortSpinner.setSelection(store.getInt(ModelPreferences.UI_PROXY_PORT));
        proxyUserText.setText(store.getString(ModelPreferences.UI_PROXY_USER));
        // Load and decrypt password
        String passwordString = store.getString(ModelPreferences.UI_PROXY_PASSWORD);
        if (!CommonUtils.isEmpty(passwordString) && encrypter != null) {
            try {
                passwordString = encrypter.decrypt(passwordString);
            } catch (EncryptionException e) {
                log.warn(e);
            }
        }
        proxyPasswordText.setText(passwordString);
        customDriversHome.setText(DriverDescriptor.getCustomDriversHome().toAbsolutePath().toString());

        sourceList.removeAll();
        for (String source : DriverDescriptor.getDriversSources()) {
            sourceList.add(source);
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(ModelPreferences.UI_DRIVERS_VERSION_UPDATE, versionUpdateCheck.getSelection());

        store.setValue(ModelPreferences.UI_PROXY_HOST, proxyHostText.getText());
        store.setValue(ModelPreferences.UI_PROXY_PORT, proxyPortSpinner.getSelection());
        store.setValue(ModelPreferences.UI_PROXY_USER, proxyUserText.getText());
        String password = proxyPasswordText.getText();
        if (!CommonUtils.isEmpty(password) && encrypter != null) {
            // Encrypt password
            try {
                password = encrypter.encrypt(password);
            } catch (EncryptionException e) {
                log.warn(e);
            }
        }
        store.setValue(ModelPreferences.UI_PROXY_PASSWORD, password);
        store.setValue(ModelPreferences.UI_DRIVERS_HOME, customDriversHome.getText());

        {
            final StringJoiner sources = new StringJoiner("|");
            for (String item : sourceList.getItems()) {
                sources.add(item);
            }
            store.setValue(ModelPreferences.UI_DRIVERS_SOURCES, sources.toString());
        }

        PrefUtils.savePreferenceStore(store);

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