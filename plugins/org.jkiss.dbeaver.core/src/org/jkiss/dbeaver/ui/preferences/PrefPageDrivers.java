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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageDrivers
 */
public class PrefPageDrivers extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    static final Log log = Log.getLog(PrefPageDrivers.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.drivers"; //$NON-NLS-1$

    private Text proxyHostText;
    private Spinner proxyPortSpinner;
    private Text proxyUserText;
    private Text proxyPasswordText;
    private SecuredPasswordEncrypter encrypter;

    private Text customDriversHome;
    private List sourceList;

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

        Group proxyObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_http_proxy, 2, GridData.VERTICAL_ALIGN_BEGINNING, 300);
        proxyHostText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_host, null); //$NON-NLS-2$
        proxyPortSpinner = UIUtils.createLabelSpinner(proxyObjects, CoreMessages.pref_page_ui_general_spinner_proxy_port, 0, 0, 65535);
        proxyUserText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_user, null); //$NON-NLS-2$
        proxyPasswordText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_password, null, SWT.PASSWORD | SWT.BORDER); //$NON-NLS-2$

        {
            Group drivers = UIUtils.createControlGroup(composite, CoreMessages.pref_page_drivers_group_location, 2, GridData.FILL_HORIZONTAL, 300);
            customDriversHome = UIUtils.createOutputFolderChooser(drivers, "Local folder", null);
            Label sourcesLabel = UIUtils.createControlLabel(drivers, "Drivers' sources");
            sourcesLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            Composite sourcePH = UIUtils.createPlaceholder(drivers, 2, 5);
            sourcePH.setLayoutData(new GridData(GridData.FILL_BOTH));
            sourceList = new List(sourcePH, SWT.BORDER | SWT.SINGLE);
            sourceList.setLayoutData(new GridData(GridData.FILL_BOTH));
            Composite buttonsPH = UIUtils.createPlaceholder(sourcePH, 1);
            UIUtils.createToolButton(buttonsPH, "Add", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    String url = EnterNameDialog.chooseName(getShell(), "Enter drivers location URL", "http://");
                    if (url != null) {
                        sourceList.add(url);
                    }
                }
            });
            final Button removeButton = UIUtils.createToolButton(buttonsPH, "Remove", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    sourceList.remove(sourceList.getSelectionIndices());
                    sourceList.notifyListeners(SWT.Selection, new Event());
                }
            });
            removeButton.setEnabled(false);
            final Button upButton = UIUtils.createToolButton(buttonsPH, "Up", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    int index = sourceList.getSelectionIndex();
                    String prevValue = sourceList.getItem(index - 1);
                    sourceList.setItem(index - 1, sourceList.getItem(index));
                    sourceList.setItem(index, prevValue);
                    sourceList.setSelection(index - 1);
                    sourceList.notifyListeners(SWT.Selection, new Event());
                }
            });
            upButton.setEnabled(false);
            final Button downButton = UIUtils.createToolButton(buttonsPH, "Down", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    int index = sourceList.getSelectionIndex();
                    String nextValue = sourceList.getItem(index + 1);
                    sourceList.setItem(index + 1, sourceList.getItem(index));
                    sourceList.setItem(index, nextValue);
                    sourceList.setSelection(index + 1);
                    sourceList.notifyListeners(SWT.Selection, new Event());
                }
            });
            downButton.setEnabled(false);

            sourceList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    if (sourceList.getSelectionIndex() >= 0) {
                        removeButton.setEnabled(sourceList.getItemCount() > 1);
                        upButton.setEnabled(sourceList.getSelectionIndex() > 0);
                        downButton.setEnabled(sourceList.getSelectionIndex() < sourceList.getItemCount() - 1);
                    } else {
                        removeButton.setEnabled(false);
                        upButton.setEnabled(false);
                        downButton.setEnabled(false);
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
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        proxyHostText.setText(store.getString(DBeaverPreferences.UI_PROXY_HOST));
        proxyPortSpinner.setSelection(store.getInt(DBeaverPreferences.UI_PROXY_PORT));
        proxyUserText.setText(store.getString(DBeaverPreferences.UI_PROXY_USER));
        // Load and decrypt password
        String passwordString = store.getString(DBeaverPreferences.UI_PROXY_PASSWORD);
        if (!CommonUtils.isEmpty(passwordString) && encrypter != null) {
            try {
                passwordString = encrypter.decrypt(passwordString);
            } catch (EncryptionException e) {
                log.warn(e);
            }
        }
        proxyPasswordText.setText(passwordString);
        customDriversHome.setText(DriverDescriptor.getCustomDriversHome().getAbsolutePath());

        for (String source : DriverDescriptor.getDriversSources()) {
            sourceList.add(source);
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.setValue(DBeaverPreferences.UI_PROXY_HOST, proxyHostText.getText());
        store.setValue(DBeaverPreferences.UI_PROXY_PORT, proxyPortSpinner.getSelection());
        store.setValue(DBeaverPreferences.UI_PROXY_USER, proxyUserText.getText());
        String password = proxyPasswordText.getText();
        if (!CommonUtils.isEmpty(password) && encrypter != null) {
            // Encrypt password
            try {
                password = encrypter.encrypt(password);
            } catch (EncryptionException e) {
                log.warn(e);
            }
        }
        store.setValue(DBeaverPreferences.UI_PROXY_PASSWORD, password);
        store.setValue(DBeaverPreferences.UI_DRIVERS_HOME, customDriversHome.getText());

        StringBuilder sources = new StringBuilder();
        for (String item : sourceList.getItems()) {
            if (sources.length() > 0) sources.append('|');
            sources.append(item);
        }
        store.setValue(DBeaverPreferences.UI_DRIVERS_SOURCES, sources.toString());
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