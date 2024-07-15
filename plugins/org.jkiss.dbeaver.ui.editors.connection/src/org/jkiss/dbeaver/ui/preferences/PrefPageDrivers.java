/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.net.GlobalProxyAuthenticator;
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

    private Text customDriversHome;

    @Override
    public void init(IWorkbench workbench) {
        // nothing to initialize
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        {
            Group settings = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_ui_general_group_settings, 2, GridData.FILL_HORIZONTAL, 300);
            versionUpdateCheck = UIUtils.createCheckbox(
                    settings,
                    UIConnectionMessages.pref_page_ui_general_check_new_driver_versions,
                    store.getBoolean(ModelPreferences.UI_DRIVERS_VERSION_UPDATE));
        }

        {
            Group proxyObjects = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_ui_general_group_http_proxy, 4, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 300);
            proxyHostText = UIUtils.createLabelText(
                proxyObjects,
                UIConnectionMessages.pref_page_ui_general_label_proxy_host,
                store.getString(ModelPreferences.UI_PROXY_HOST));
            proxyPortSpinner = UIUtils.createLabelSpinner(
                proxyObjects,
                UIConnectionMessages.pref_page_ui_general_spinner_proxy_port,
                store.getInt(ModelPreferences.UI_PROXY_PORT),
                0,
                65535);
            proxyUserText = UIUtils.createLabelText(
                proxyObjects,
                UIConnectionMessages.pref_page_ui_general_label_proxy_user,
                null);
            proxyPasswordText = UIUtils.createLabelText(proxyObjects, UIConnectionMessages.pref_page_ui_general_label_proxy_password, null, SWT.PASSWORD | SWT.BORDER); //$NON-NLS-2$
        }

        {
            Group drivers = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_drivers_group_location, 2, GridData.FILL_HORIZONTAL, 300);
            customDriversHome = DialogUtils.createOutputFolderChooser(drivers, UIConnectionMessages.pref_page_drivers_local_folder, null, null, null, false, null);
            customDriversHome.setText(store.getString(ModelPreferences.UI_DRIVERS_HOME));
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
            Control tip = UIUtils.createInfoLabel(repoGroup, UIConnectionMessages.pref_page_drivers_repo_info);
            tip.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 2, 1));
        }

        DBPAuthInfo credentials = null;
        try {
            credentials = GlobalProxyAuthenticator.readCredentials();
        } catch (DBException e) {
            log.error("Error reading proxy credentials", e);
        }
        if (credentials != null) {
            proxyUserText.setText(CommonUtils.notEmpty(credentials.getUserName()));
            proxyPasswordText.setText(CommonUtils.notEmpty(credentials.getUserPassword()));
        }

        sourceList.removeAll();
        for (String source : DriverDescriptor.getDriversSources()) {
            sourceList.add(source);
        }

        return composite;
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        versionUpdateCheck.setSelection(store.getDefaultBoolean(ModelPreferences.UI_DRIVERS_VERSION_UPDATE));

        proxyHostText.setText(store.getDefaultString(ModelPreferences.UI_PROXY_HOST));
        proxyPortSpinner.setSelection(store.getDefaultInt(ModelPreferences.UI_PROXY_PORT));
        proxyUserText.setText(store.getDefaultString(ModelPreferences.UI_PROXY_USER));
        proxyPasswordText.setText("");
        customDriversHome.setText(store.getDefaultString(ModelPreferences.UI_DRIVERS_HOME));

        sourceList.removeAll();
        for (String source : DriverDescriptor.getDriversSources()) {
            sourceList.add(source);
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        try {
            GlobalProxyAuthenticator.saveCredentials(proxyUserText.getText(), proxyPasswordText.getText());
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Unable to save proxy credentials", e.getMessage(), e);
            return false;
        }

        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(ModelPreferences.UI_DRIVERS_VERSION_UPDATE, versionUpdateCheck.getSelection());
        store.setValue(ModelPreferences.UI_PROXY_HOST, proxyHostText.getText());
        store.setValue(ModelPreferences.UI_PROXY_PORT, proxyPortSpinner.getSelection());
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