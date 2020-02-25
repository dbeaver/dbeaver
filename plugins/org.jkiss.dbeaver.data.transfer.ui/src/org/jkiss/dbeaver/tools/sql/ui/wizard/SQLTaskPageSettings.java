/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.sql.ui.wizard;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

/**
 * SQL task settings page
 */
class SQLTaskPageSettings extends ActiveWizardPage<SQLTaskConfigurationWizard> {

    private static final Log log = Log.getLog(SQLTaskPageSettings.class);

    private SQLTaskConfigurationWizard sqlWizard;
    private Button ignoreErrorsCheck;
    private Button dumpQueryCheck;
    private Button autoCommitCheck;
    private TableViewer scriptsTree;
    private DatabaseNavigatorTree dataSourceTree;

    private List<DBNResource> selectedScripts = new ArrayList<>();
    private List<DBNDataSource> selectedDataSources = new ArrayList<>();

    SQLTaskPageSettings(SQLTaskConfigurationWizard wizard) {
        super(DTMessages.sql_script_task_title);
        setTitle(DTMessages.sql_script_task_page_settings_title);
        setDescription(DTMessages.sql_script_task_page_settings_description);
        this.sqlWizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        SQLScriptExecuteSettings dtSettings = getWizard().getSettings();

        SashForm mainGroup = new SashForm(composite, SWT.NONE);
        mainGroup.setSashWidth(5);
        mainGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(sqlWizard.getProject());

        {
            Composite filesGroup = UIUtils.createControlGroup(mainGroup, DTMessages.sql_script_task_page_settings_group_files, 2, GridData.FILL_BOTH, 0);

            scriptsTree = new TableViewer(filesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            scriptsTree.setContentProvider(new ListContentProvider());
            scriptsTree.getTable().setHeaderVisible(true);
            scriptsTree.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((DBNResource) element).getResource().getProjectRelativePath().toString();
                }
                @Override
                public Image getImage(Object element) {
                    return DBeaverIcons.getImage(((DBNResource)element).getNodeIconDefault());
                }

            });
            SQLTaskScriptSelectorDialog.createScriptColumns(scriptsTree);

            final Table scriptTable = scriptsTree.getTable();
            scriptTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            ToolBar buttonsToolbar = new ToolBar(filesGroup, SWT.VERTICAL);
            buttonsToolbar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            ToolItem addItem = UIUtils.createToolItem(buttonsToolbar, "Add script", UIIcon.ROW_ADD, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    SQLTaskScriptSelectorDialog dialog = new SQLTaskScriptSelectorDialog(getShell(), projectNode);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        selectedScripts.addAll(dialog.getSelectedScripts());
                        refreshScripts();
                    }
                }
            });
            ToolItem deleteItem = UIUtils.createToolItem(buttonsToolbar, "Remove script", UIIcon.ROW_DELETE, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ISelection selection = scriptsTree.getSelection();
                    if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
                        for (Object element : ((IStructuredSelection) selection).toArray()) {
                            if (element instanceof DBNResource) {
                                selectedScripts.remove(element);
                            }
                        }
                        refreshScripts();
                    }
                }
            });
            UIUtils.createToolBarSeparator(buttonsToolbar, SWT.HORIZONTAL);
            ToolItem moveUpItem = UIUtils.createToolItem(buttonsToolbar, "Move script up", UIIcon.ARROW_UP, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = scriptTable.getSelectionIndex();
                    if (selectionIndex > 0) {
                        DBNResource prevScript = selectedScripts.get(selectionIndex - 1);
                        selectedScripts.set(selectionIndex - 1, selectedScripts.get(selectionIndex));
                        selectedScripts.set(selectionIndex, prevScript);
                        refreshScripts();
                    }
                }
            });
            ToolItem moveDownItem = UIUtils.createToolItem(buttonsToolbar, "Move script down", UIIcon.ARROW_DOWN, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = scriptTable.getSelectionIndex();
                    if (selectionIndex < scriptTable.getItemCount() - 1) {
                        DBNResource nextScript = selectedScripts.get(selectionIndex + 1);
                        selectedScripts.set(selectionIndex + 1, selectedScripts.get(selectionIndex));
                        selectedScripts.set(selectionIndex, nextScript);
                        refreshScripts();
                    }
                }
            });
            scriptsTree.addSelectionChangedListener(event -> {
                int selectionIndex = scriptTable.getSelectionIndex();
                deleteItem.setEnabled(selectionIndex >= 0);
                moveUpItem.setEnabled(selectionIndex > 0);
                moveDownItem.setEnabled(selectionIndex < scriptTable.getItemCount() - 1);
            });
            deleteItem.setEnabled(false);
        }

        {
            Composite connectionsGroup = UIUtils.createControlGroup(mainGroup, DTMessages.sql_script_task_page_settings_group_connections, 1, GridData.FILL_BOTH, 0);

            INavigatorFilter dsFilter = new INavigatorFilter() {
                @Override
                public boolean filterFolders() {
                    return true;
                }
                @Override
                public boolean isLeafObject(Object object) {
                    return object instanceof DBNDataSource;
                }
                @Override
                public boolean select(Object element) {
                    return element instanceof DBNProject || element instanceof DBNProjectDatabases || element instanceof DBNLocalFolder || element instanceof DBNDataSource;
                }
            };

            dataSourceTree = new DatabaseNavigatorTree(connectionsGroup, projectNode.getDatabases(), SWT.SINGLE | SWT.BORDER | SWT.CHECK, false, dsFilter);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            gd.widthHint = 400;
            dataSourceTree.setLayoutData(gd);
            //dataSourceTree.getViewer().getTree().setHeaderVisible(true);
            dataSourceTree.getViewer().addSelectionChangedListener(event -> {
                updateSelectedDataSources();
                updatePageCompletion();
            });
        }

        {
            Composite settingsGroup = UIUtils.createControlGroup(composite, DTMessages.sql_script_task_page_settings_group_script, 3, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            ignoreErrorsCheck = UIUtils.createCheckbox(settingsGroup, DTMessages.sql_script_task_page_settings_option_ignore_errors, "", dtSettings.isIgnoreErrors(), 1);
            dumpQueryCheck = UIUtils.createCheckbox(settingsGroup, DTMessages.sql_script_task_page_settings_option_dump_results, "", dtSettings.isDumpQueryResultsToLog(), 1);
            autoCommitCheck = UIUtils.createCheckbox(settingsGroup, DTMessages.sql_script_task_page_settings_option_auto_commit, "", dtSettings.isAutoCommit(), 1);
        }

        getWizard().createTaskSaveButtons(composite, true, 1);

        loadSettings();

        setControl(composite);
    }

    private void refreshScripts() {
        scriptsTree.refresh(true, true);
        updateSelectedScripts();
    }

    private void updateSelectedScripts() {
        DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(sqlWizard.getProject());

        Set<DBPDataSourceContainer> dataSources = new LinkedHashSet<>();
        for (DBNResource element : selectedScripts) {
            Collection<DBPDataSourceContainer> resDS = element.getAssociatedDataSources();
            if (!CommonUtils.isEmpty(resDS)) {
                dataSources.addAll(resDS);
            }
        }

        if (!dataSources.isEmpty()) {
            List<DBNDataSource> checkedDataSources = new ArrayList<>();
            for (DBPDataSourceContainer ds : dataSources) {
                DBNDataSource dsNode = projectNode.getDatabases().getDataSource(ds);
                if (dsNode != null) {
                    checkedDataSources.add(dsNode);
                }
            }
            if (!checkedDataSources.isEmpty()) {
                dataSourceTree.getCheckboxViewer().setCheckedElements(checkedDataSources.toArray());
                for (DBNDataSource dsNode : checkedDataSources) {
                    dataSourceTree.getCheckboxViewer().reveal(dsNode);
                }
            } else {
                dataSourceTree.getCheckboxViewer().setCheckedElements(new Object[0]);
            }
        }
        updateSelectedDataSources();
        updatePageCompletion();
    }

    private void updateSelectedDataSources() {
        selectedDataSources.clear();
        for (Object cd : dataSourceTree.getCheckboxViewer().getCheckedElements()) {
            if (cd instanceof DBNDataSource) {
                selectedDataSources.add((DBNDataSource) cd);
            }
        }
    }

    @Override
    public void activatePage() {
        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
    }

    @Override
    protected boolean determinePageCompletion() {
        if (selectedScripts.isEmpty()) {
            setErrorMessage("You must select script(s) to execute");
            return false;
        }
        if (selectedDataSources.isEmpty()) {
            setErrorMessage("You must select connection(s)");
            return false;
        }
        setErrorMessage(null);
        return true;
    }

    public void loadSettings() {
        SQLScriptExecuteSettings settings = sqlWizard.getSettings();

        List<String> scriptFiles = settings.getScriptFiles();
        for (String filePath : scriptFiles) {
            IFile file = SQLScriptExecuteSettings.getWorkspaceFile(filePath);
            if (file == null) {
                log.debug("Script file '" + filePath + "' not found");
                continue;
            }
            DBPProject currentProject = DBWorkbench.getPlatform().getWorkspace().getProject(file.getProject());
            if (currentProject == null) {
                log.debug("Project '" + file.getProject().getName() + "' not found");
                continue;
            }
            DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(currentProject);
            if (projectNode != null) {
                DBNResource resource = projectNode.findResource(file);
                if (resource != null) {
                    selectedScripts.add(resource);
                }
            }
        }
        scriptsTree.setInput(selectedScripts);

        for (DBPDataSourceContainer dataSource : settings.getDataSources()) {
            DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(dataSource.getProject());
            DBNDataSource dsNode = projectNode.getDatabases().getDataSource(dataSource);
            if (dsNode != null) {
                selectedDataSources.add(dsNode);
            }
        }
        if (!selectedDataSources.isEmpty()) {
            dataSourceTree.getCheckboxViewer().setCheckedElements(selectedDataSources.toArray());
            dataSourceTree.getCheckboxViewer().reveal(selectedDataSources.get(0));
        }
    }

    public void saveSettings() {
        if (sqlWizard == null) {
            return;
        }
        SQLScriptExecuteSettings settings = sqlWizard.getSettings();

        List<String> scriptPaths = new ArrayList<>();
        for (DBNResource resource : selectedScripts) {
            IResource res = resource.getResource();
            if (res instanceof IFile) {
                scriptPaths.add(res.getFullPath().toString());
            }
        }
        settings.setScriptFiles(scriptPaths);
        List<DBPDataSourceContainer> dsList = new ArrayList<>();
        for (DBNDataSource dsNode : selectedDataSources) {
            dsList.add(dsNode.getDataSourceContainer());
        }
        settings.setDataSources(dsList);

        settings.setIgnoreErrors(ignoreErrorsCheck.getSelection());
        settings.setDumpQueryResultsToLog(dumpQueryCheck.getSelection());
        settings.setAutoCommit(autoCommitCheck.getSelection());
    }

}