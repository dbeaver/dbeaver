/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.search;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.ArrayList;
import java.util.List;

public class DatabaseSearchDialog extends HelpEnabledDialog implements IObjectSearchContainer {

    static final Log log = Log.getLog(DatabaseSearchDialog.class);

    private static final int SEARCH_ID = 1000;
    private static final String PROVIDER_PREF_NAME = "search.dialog.cur-provider";
    private static final String NEW_TAB_PREF_NAME = "search.dialog.results.newTab";
    private static final String DIALOG_ID = "DBeaver.SearchDialog";//$NON-NLS-1$

    private volatile static DatabaseSearchDialog instance;

    private boolean searchEnabled = true;
    private Button searchButton;
    private TabFolder providersFolder;
    private Button openNewTabCheck;
    private List<ObjectSearchProvider> providers;


    private DatabaseSearchDialog(Shell shell, DBSDataSourceContainer currentDataSource)
    {
        super(shell, IHelpContextIds.CTX_SQL_EDITOR);
        setShellStyle(SWT.DIALOG_TRIM | SWT.MAX | SWT.RESIZE | getDefaultOrientation());
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite group = (Composite) super.createDialogArea(parent);
        Shell shell = getShell();

        shell.setText(CoreMessages.dialog_search_objects_title);
        shell.setImage(DBeaverIcons.getImage(UIIcon.FIND));

        shell.addShellListener(new ShellAdapter() {
            @Override
            public void shellActivated(ShellEvent e)
            {
                if (searchButton != null && !searchButton.isDisposed()) {
                    getShell().setDefaultButton(searchButton);
                }
            }
        });
        //shell.setDefaultButton(searchButton);
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        providersFolder = new TabFolder(group, SWT.TOP);
        providersFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        providers = new ArrayList<ObjectSearchProvider>(ObjectSearchRegistry.getInstance().getProviders());
        for (ObjectSearchProvider provider : providers) {
            IObjectSearchPage searchPage;
            try {
                searchPage = provider.createSearchPage();
            } catch (DBException e) {
                log.error("Can't create search page '" + provider.getId() + "'", e);
                continue;
            }
            searchPage.setSearchContainer(this);
            searchPage.loadState(store);
            searchPage.createControl(providersFolder);

            TabItem item = new TabItem(providersFolder, SWT.NONE);
            item.setData("provider", provider);
            item.setData("page", searchPage);
            item.setText(provider.getLabel());
            DBPImage icon = provider.getIcon();
            if (icon != null) {
                item.setImage(DBeaverIcons.getImage(icon));
            }
            item.setControl(searchPage.getControl());
        }
        int provIndex = 0;
        String curProvider = store.getString(PROVIDER_PREF_NAME);
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).getId().equals(curProvider)) {
                provIndex = i;
            }
        }
        providersFolder.setSelection(provIndex);

        return providersFolder;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        // New tab check
        ((GridLayout) parent.getLayout()).numColumns++;
        openNewTabCheck = UIUtils.createCheckbox(parent, "Open results in new tab", DBeaverCore.getGlobalPreferenceStore().getBoolean(NEW_TAB_PREF_NAME));

        // Buttons
        searchButton = createButton(parent, SEARCH_ID, "Search", true);
        searchButton.setEnabled(searchEnabled);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
    }

    @Override
    public boolean close()
    {
        saveState();
        return super.close();
    }


    @Override
    public void setSearchEnabled(boolean enabled)
    {
        if (searchButton != null) {
            searchButton.setEnabled(enabled);
        }
        searchEnabled = enabled;
    }

    public void saveState()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.setValue(NEW_TAB_PREF_NAME, openNewTabCheck.getSelection());
        for (TabItem item : providersFolder.getItems()) {
            IObjectSearchPage page = (IObjectSearchPage) item.getData("page");
            page.saveState(store);
        }
        store.setValue(PROVIDER_PREF_NAME, providers.get(providersFolder.getSelectionIndex()).getId());
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == SEARCH_ID) {
            performSearch();
        }
        super.buttonPressed(buttonId);
    }

    private void performSearch()
    {
        TabItem selectedItem = providersFolder.getItem(providersFolder.getSelectionIndex());
        ObjectSearchProvider provider = (ObjectSearchProvider) selectedItem.getData("provider");
        IObjectSearchPage page = (IObjectSearchPage) selectedItem.getData("page");

        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        DatabaseSearchView resultsView;
        try {
            resultsView = (DatabaseSearchView)activePage.showView(DatabaseSearchView.VIEW_ID);
            activePage.bringToTop(resultsView);
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(getShell(), "Search", "Can't open search view", e);
            return;
        }
        IObjectSearchQuery query;
        try {
            query = page.createQuery();
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), "Search", "Can't create search query", e);
            return;
        }
        IObjectSearchResultPage resultsPage;
        try {
            resultsPage = resultsView.openResultPage(provider, query, openNewTabCheck.getSelection());
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), "Search", "Can't open search results page", e);
            return;
        }

        saveState();

        // Run search job
        setSearchEnabled(false);
        final ControlEnableState disableState = ControlEnableState.disable(providersFolder);
        DatabaseSearchJob job = new DatabaseSearchJob(query, resultsPage);

        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event)
            {
                UIUtils.runInUI(getShell(), new Runnable() {
                    @Override
                    public void run()
                    {
                        if (!providersFolder.isDisposed()) {
                            setSearchEnabled(true);
                            disableState.restore();
                        }
                    }
                });
            }
        });

        job.schedule();
    }

    public static void open(Shell shell, DBSDataSourceContainer currentDataSource)
    {
        if (ObjectSearchRegistry.getInstance().getProviders().isEmpty()) {
            UIUtils.showMessageBox(shell, "Search error", "No search providers found", SWT.ICON_ERROR);
            return;
        }
        if (instance != null) {
            instance.getShell().setActive();
            return;
        }
        DatabaseSearchDialog dialog = new DatabaseSearchDialog(shell, currentDataSource);
        instance = dialog;
        try {
            dialog.open();
        } finally {
            instance = null;
        }
    }

}
