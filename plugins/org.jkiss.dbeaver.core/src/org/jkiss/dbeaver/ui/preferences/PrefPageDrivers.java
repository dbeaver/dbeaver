/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

import org.eclipse.core.runtime.IAdaptable;
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.registry.maven.MavenRegistry;
import org.jkiss.dbeaver.registry.maven.MavenRepository;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * PrefPageDrivers
 */
public class PrefPageDrivers extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    private static final Log log = Log.getLog(PrefPageDrivers.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.drivers"; //$NON-NLS-1$

    private Button versionUpdateCheck;

    private Table mavenRepoTable;
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

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group settings = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, 300);
            versionUpdateCheck = UIUtils.createCheckbox(settings, "Check for new driver versions", false);
        }
        {
            Group mavenGroup = UIUtils.createControlGroup(composite, "Maven repositories", 2, GridData.FILL_HORIZONTAL, 300);
            mavenRepoTable = new Table(mavenGroup, SWT.BORDER | SWT.FULL_SELECTION);
            UIUtils.createTableColumn(mavenRepoTable, SWT.LEFT, "Id");
            UIUtils.createTableColumn(mavenRepoTable, SWT.LEFT, "URL");
            mavenRepoTable.setHeaderVisible(true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 60;
            mavenRepoTable.setLayoutData(gd);

            Composite buttonsPH = UIUtils.createPlaceholder(mavenGroup, 1);
            UIUtils.createToolButton(buttonsPH, "Add", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    String urlString = EnterNameDialog.chooseName(getShell(), "Enter Maven repository URL", "http://");
                    if (urlString != null) {
                        try {
                            URL url = new URL(urlString);
                            new TableItem(mavenRepoTable, SWT.NONE).setText(new String[]{url.getHost(), urlString});
                        } catch (MalformedURLException e1) {
                            UIUtils.showErrorDialog(getShell(), "Bad URL", "Bad Maven repository URL", e1);
                        }
                    }
                }
            });
            final Button removeButton = UIUtils.createToolButton(buttonsPH, "Remove", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    mavenRepoTable.remove(mavenRepoTable.getSelectionIndices());
                    mavenRepoTable.notifyListeners(SWT.Selection, new Event());
                }
            });
            removeButton.setEnabled(false);

            mavenRepoTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    boolean enabled = false;
                    TableItem[] selection = mavenRepoTable.getSelection();
                    if (selection.length == 1) {
                        enabled = selection[0].getData() instanceof MavenRepository &&
                            ((MavenRepository) selection[0].getData()).getType() == MavenRepository.RepositoryType.CUSTOM;
                    }
                    removeButton.setEnabled(enabled);
                }
            });
        }

        {
            Group repoGroup = UIUtils.createControlGroup(composite, "File repositories", 2, GridData.FILL_HORIZONTAL, 300);
            sourceList = new List(repoGroup, SWT.BORDER | SWT.SINGLE);
            sourceList.setLayoutData(new GridData(GridData.FILL_BOTH));
            Composite buttonsPH = UIUtils.createPlaceholder(repoGroup, 1);
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

        {
            Group proxyObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_http_proxy, 4, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 300);
            proxyHostText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_host, null); //$NON-NLS-2$
            proxyPortSpinner = UIUtils.createLabelSpinner(proxyObjects, CoreMessages.pref_page_ui_general_spinner_proxy_port, 0, 0, 65535);
            proxyUserText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_user, null); //$NON-NLS-2$
            proxyPasswordText = UIUtils.createLabelText(proxyObjects, CoreMessages.pref_page_ui_general_label_proxy_password, null, SWT.PASSWORD | SWT.BORDER); //$NON-NLS-2$
        }

        {
            Group drivers = UIUtils.createControlGroup(composite, CoreMessages.pref_page_drivers_group_location, 2, GridData.FILL_HORIZONTAL, 300);
            customDriversHome = DialogUtils.createOutputFolderChooser(drivers, "Local folder", null);
        }



        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        versionUpdateCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_DRIVERS_VERSION_UPDATE));

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

        for (MavenRepository repo : MavenRegistry.getInstance().getRepositories()) {
            TableItem item = new TableItem(mavenRepoTable, SWT.NONE);
            item.setText(new String[]{repo.getId(), repo.getUrl()});
            item.setData(repo);
        }
        UIUtils.packColumns(mavenRepoTable, true);
        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.setValue(DBeaverPreferences.UI_DRIVERS_VERSION_UPDATE, versionUpdateCheck.getSelection());

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

        {
            StringBuilder sources = new StringBuilder();
            for (String item : sourceList.getItems()) {
                if (sources.length() > 0) sources.append('|');
                sources.append(item);
            }
            store.setValue(DBeaverPreferences.UI_DRIVERS_SOURCES, sources.toString());
        }

        {
            StringBuilder mavenRepos = new StringBuilder();
            for (TableItem item : mavenRepoTable.getItems()) {
                String repoId = item.getText(0);
                String repoURL = item.getText(1);
                MavenRepository repository = MavenRegistry.getInstance().findRepository(repoId);
                if (repository != null && repository.getType() != MavenRepository.RepositoryType.CUSTOM) {
                    continue;
                }
                if (mavenRepos.length() > 0) mavenRepos.append('|');
                mavenRepos.append(repoId).append(':').append(repoURL);
            }
            store.setValue(DBeaverPreferences.UI_MAVEN_REPOSITORIES, mavenRepos.toString());
            MavenRegistry.getInstance().loadCustomRepositories();
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