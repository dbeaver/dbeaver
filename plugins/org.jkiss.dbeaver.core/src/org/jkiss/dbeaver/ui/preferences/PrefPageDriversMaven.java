/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.maven.MavenRegistry;
import org.jkiss.dbeaver.registry.maven.MavenRepository;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * PrefPageDriversMaven
 */
public class PrefPageDriversMaven extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    private static final Log log = Log.getLog(PrefPageDriversMaven.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.drivers.maven"; //$NON-NLS-1$

    private Table mavenRepoTable;

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

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

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

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