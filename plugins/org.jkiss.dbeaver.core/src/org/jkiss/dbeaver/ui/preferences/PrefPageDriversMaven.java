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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.registry.maven.MavenRegistry;
import org.jkiss.dbeaver.registry.maven.MavenRepository;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * PrefPageDriversMaven
 */
public class PrefPageDriversMaven extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
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
            idText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    if (getSelectedRepository() != null) {
                        getSelectedRepository().setId(idText.getText());
                        mavenRepoTable.getSelection()[0].setText(0, idText.getText());
                    }
                }
            });
            nameText = UIUtils.createLabelText(propsGroup, "Name", "", SWT.BORDER);
            nameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    if (getSelectedRepository() != null) {
                        getSelectedRepository().setName(nameText.getText());
                    }
                }
            });
            urlText = UIUtils.createLabelText(propsGroup, "URL", "", SWT.BORDER);
            urlText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    if (getSelectedRepository() != null) {
                        getSelectedRepository().setUrl(urlText.getText());
                        mavenRepoTable.getSelection()[0].setText(1, urlText.getText());
                    }
                }
            });
            scopeText = UIUtils.createLabelText(propsGroup, "Scope", "", SWT.BORDER);
            scopeText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    if (getSelectedRepository() != null) {
                        getSelectedRepository().setScopes(CommonUtils.splitString(scopeText.getText(), ','));
                    }
                }
            });
        }

        performDefaults();

        return composite;
    }

    private MavenRepository getSelectedRepository() {
        TableItem[] selection = mavenRepoTable.getSelection();
        if (selection.length == 1) {
            return (MavenRepository) selection[0].getData();
        }
        return null;
    }

    private void updateSelection() {
        final MavenRepository repo = getSelectedRepository();
        if (repo != null) {
            if (disabledRepositories.contains(repo)) {
                disableButton.setText("Enable");
            } else {
                disableButton.setText("Disable");
            }
            disableButton.setEnabled(true);
            final boolean isEditable = repo.getType() == MavenRepository.RepositoryType.CUSTOM;
            removeButton.setEnabled(isEditable);

            idText.setEnabled(true);
            idText.setEditable(isEditable);
            idText.setText(repo.getId());

            nameText.setEnabled(true);
            nameText.setEditable(isEditable);
            nameText.setText(repo.getName());

            urlText.setEnabled(true);
            urlText.setEditable(isEditable);
            urlText.setText(repo.getUrl());

            scopeText.setEnabled(true);
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
        for (MavenRepository repo : MavenRegistry.getInstance().getRepositories()) {
            MavenRepository repoCopy = repo.getType() == MavenRepository.RepositoryType.CUSTOM ? new MavenRepository(repo) : repo;
            TableItem item = new TableItem(mavenRepoTable, SWT.NONE);
            item.setText(new String[]{repoCopy.getId(), repoCopy.getUrl()});
            item.setData(repoCopy);
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
        java.util.List<MavenRepository> customRepos = new ArrayList<>();
        TableItem[] items = mavenRepoTable.getItems();
        for (int i = 0; i < items.length; i++) {
            TableItem item = items[i];
            MavenRepository repo = (MavenRepository) item.getData();
            repo.setEnabled(!disabledRepositories.contains(repo));
            repo.setOrder(i);
            if (repo.getType() == MavenRepository.RepositoryType.CUSTOM) {
                customRepos.add(repo);
            }
        }
        final MavenRegistry registry = MavenRegistry.getInstance();
        registry.setCustomRepositories(customRepos);
        registry.saveConfiguration();
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