/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ui.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;

import java.util.List;

public class SearchDatabaseObjectsDialog extends HelpEnabledDialog implements IObjectSearchContainer {

    static final Log log = LogFactory.getLog(SearchDatabaseObjectsDialog.class);

    private static final int SEARCH_ID = 1000;

    private volatile static SearchDatabaseObjectsDialog instance;

    private boolean searchEnabled = true;
    private Button searchButton;
    private TabFolder providersFolder;


    private SearchDatabaseObjectsDialog(Shell shell, DBSDataSourceContainer currentDataSource)
    {
        super(shell, IHelpContextIds.CTX_SQL_EDITOR);
        setShellStyle(SWT.DIALOG_TRIM | SWT.MAX | SWT.RESIZE | getDefaultOrientation());
    }


    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite group = (Composite) super.createDialogArea(parent);
        Shell shell = getShell();

        shell.setText(CoreMessages.dialog_search_objects_title);
        shell.setImage(DBIcon.FIND.getImage());

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

        providersFolder = new TabFolder(group, SWT.TOP);
        providersFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        List<ObjectSearchProvider> providers = ObjectSearchRegistry.getInstance().getProviders();
        for (ObjectSearchProvider provider : providers) {
            IObjectSearchPage searchPage;
            try {
                searchPage = provider.createSearchPage();
            } catch (DBException e) {
                log.error("Can't create search page '" + provider.getId() + "'", e);
                continue;
            }
            searchPage.setSearchContainer(this);
            searchPage.createControl(providersFolder);

            TabItem item = new TabItem(providersFolder, SWT.NONE);
            item.setData("provider", provider);
            item.setData("page", searchPage);
            item.setText(provider.getLabel());
            Image icon = provider.getIcon();
            if (icon != null) {
                item.setImage(icon);
            }
            item.setControl(searchPage.getControl());
        }
        providersFolder.setSelection(0);

        return providersFolder;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        searchButton = createButton(parent, SEARCH_ID, "Search", true);
        searchButton.setEnabled(searchEnabled);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
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
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

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

        try {
            IObjectSearchQuery query = page.createQuery();
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), "Search", "Search error", e);
        }
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
        SearchDatabaseObjectsDialog dialog = new SearchDatabaseObjectsDialog(shell, currentDataSource);
        instance = dialog;
        try {
            dialog.open();
        } finally {
            instance = null;
        }
    }

}
