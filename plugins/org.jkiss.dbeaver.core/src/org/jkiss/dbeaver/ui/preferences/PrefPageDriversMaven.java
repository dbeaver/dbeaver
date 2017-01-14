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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.maven.MavenRegistry;
import org.jkiss.dbeaver.registry.maven.MavenRepository;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * PrefPageDriversMaven
 */
public class PrefPageDriversMaven extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    private static final Log log = Log.getLog(PrefPageDriversMaven.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.drivers.maven"; //$NON-NLS-1$

    private Table mavenRepoTable;
    private Text idText;
    private Text nameText;
    private Text urlText;
    private Text scopeText;
    private final Set<MavenRepository> disabledRepositories = new HashSet<>();
    private Button disableButton;
    private Button removeButton;
    private Color enabledColor, disabledColor;

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    protected Control createContents(Composite parent)
    {
        enabledColor = parent.getForeground();
        disabledColor = parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group mavenGroup = UIUtils.createControlGroup(composite, "Repositories", 2, GridData.FILL_BOTH, 300);
            mavenRepoTable = new Table(mavenGroup, SWT.BORDER | SWT.FULL_SELECTION);
            UIUtils.createTableColumn(mavenRepoTable, SWT.LEFT, "Id");
            UIUtils.createTableColumn(mavenRepoTable, SWT.LEFT, "URL");
            mavenRepoTable.setHeaderVisible(true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            mavenRepoTable.setLayoutData(gd);

            Composite buttonsPH = UIUtils.createPlaceholder(mavenGroup, 1);
            buttonsPH.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            UIUtils.createToolButton(buttonsPH, "Add", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    String urlString = EnterNameDialog.chooseName(getShell(), "Enter Maven repository URL", "http://");
                    if (urlString != null) {
                        try {
                            URL url = new URL(urlString);
                            MavenRepository repository = new MavenRepository(url.getHost(), url.getHost(), url.toString(), MavenRepository.RepositoryType.CUSTOM);
                            final TableItem item = new TableItem(mavenRepoTable, SWT.NONE);
                            item.setText(new String[]{url.getHost(), urlString});
                            item.setData(repository);
                        } catch (MalformedURLException e1) {
                            UIUtils.showErrorDialog(getShell(), "Bad URL", "Bad Maven repository URL", e1);
                        }
                    }
                }
            });
            removeButton = UIUtils.createToolButton(buttonsPH, "Remove", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    mavenRepoTable.remove(mavenRepoTable.getSelectionIndices());
                    mavenRepoTable.notifyListeners(SWT.Selection, new Event());
                }
            });
            removeButton.setEnabled(false);

            disableButton = UIUtils.createToolButton(buttonsPH, "Disable", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    for (TableItem item : mavenRepoTable.getSelection()) {
                        MavenRepository repo = (MavenRepository) item.getData();
                        if (!disabledRepositories.remove(repo)) {
                            disabledRepositories.add(repo);
                            item.setForeground(disabledColor);
                        } else {
                            item.setForeground(enabledColor);
                        }
                    }
                    mavenRepoTable.notifyListeners(SWT.Selection, new Event());
                }
            });
            removeButton.setEnabled(false);

            mavenRepoTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateSelection();
                }
            });
        }

        {
            Group propsGroup = UIUtils.createControlGroup(composite, "Properties", 2, GridData.FILL_HORIZONTAL, 0);
            idText = UIUtils.createLabelText(propsGroup, "ID", "", SWT.BORDER | SWT.READ_ONLY);
            nameText = UIUtils.createLabelText(propsGroup, "Name", "", SWT.BORDER);
            urlText = UIUtils.createLabelText(propsGroup, "URL", "", SWT.BORDER);
            scopeText = UIUtils.createLabelText(propsGroup, "Scope", "", SWT.BORDER);
        }

        performDefaults();

        return composite;
    }

    private void updateSelection() {
        TableItem[] selection = mavenRepoTable.getSelection();
        if (selection.length == 1) {
            final MavenRepository repo = (MavenRepository) selection[0].getData();
            if (disabledRepositories.contains(repo)) {
                disableButton.setText("Enable");
            } else {
                disableButton.setText("Disable");
            }
            disableButton.setEnabled(true);
            final boolean isEditable = repo.getType() == MavenRepository.RepositoryType.CUSTOM;
            removeButton.setEnabled(isEditable);

            idText.setEnabled(true);
            nameText.setEnabled(true);
            urlText.setEnabled(true);
            scopeText.setEnabled(true);

            idText.setText(repo.getId());
            nameText.setEditable(isEditable);
            nameText.setText(repo.getName());
            urlText.setEditable(isEditable);
            urlText.setText(repo.getUrl());
            scopeText.setEditable(isEditable);
            scopeText.setText(CommonUtils.makeString(repo.getScopes(), ','));
        } else {
            disableButton.setEnabled(false);
            removeButton.setEnabled(false);
            idText.setEnabled(false);
            nameText.setEnabled(false);
            urlText.setEnabled(false);
            scopeText.setEnabled(false);
        }
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        for (MavenRepository repo : MavenRegistry.getInstance().getRepositories()) {
            TableItem item = new TableItem(mavenRepoTable, SWT.NONE);
            item.setText(new String[]{repo.getId(), repo.getUrl()});
            item.setData(repo);
            if (!repo.isEnabled()) {
                disabledRepositories.add(repo);
                item.setForeground(disabledColor);
            }
        }
        UIUtils.packColumns(mavenRepoTable, true);
        updateSelection();
        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        MavenRegistry.getInstance().saveConfiguration();
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