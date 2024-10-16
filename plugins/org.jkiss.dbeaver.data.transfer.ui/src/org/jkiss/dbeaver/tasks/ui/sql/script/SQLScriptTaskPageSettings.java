/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.sql.script;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystems;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.rm.RMControllerProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * SQL task settings page
 */
class SQLScriptTaskPageSettings extends ActiveWizardPage<SQLScriptTaskConfigurationWizard> {

    private static final Log log = Log.getLog(SQLScriptTaskPageSettings.class);

    private final SQLScriptTaskConfigurationWizard sqlWizard;
    private Button ignoreErrorsCheck;
    private Button dumpQueryCheck;
    private Button autoCommitCheck;
    private TableViewer scriptsViewer;
    private TableViewer dataSourceViewer;

    private final List<DBNNode> selectedScripts = new ArrayList<>();
    private final List<DBNDataSource> selectedDataSources = new ArrayList<>();

    SQLScriptTaskPageSettings(SQLScriptTaskConfigurationWizard wizard) {
        super(DTMessages.sql_script_task_title);
        setTitle(DTMessages.sql_script_task_page_settings_title);
        setDescription(DTMessages.sql_script_task_page_settings_description);
        this.sqlWizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        SQLScriptExecuteSettings dtSettings = getWizard().getSettings();

        SashForm mainGroup = new SashForm(composite, SWT.NONE);
        mainGroup.setSashWidth(5);
        mainGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        DBPProject project = sqlWizard.getProject();
        DBNProject projectNode = project.getNavigatorModel().getRoot().getProjectNode(project);

        {
            Composite filesGroup = UIUtils.createControlGroup(mainGroup, DTMessages.sql_script_task_page_settings_group_files, 2, GridData.FILL_BOTH, 0);

            scriptsViewer = new TableViewer(filesGroup, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
            scriptsViewer.setContentProvider(new ListContentProvider());
            scriptsViewer.getTable().setHeaderVisible(true);
            scriptsViewer.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    if (element instanceof DBNPathBase path) {
                        return path.getPath().toString();
                    }
                    DBNNode node = (DBNNode) element;
                    DBPProject ownerProject = node.getOwnerProject();
                    if (ownerProject instanceof RCPProject rcpProject) {
                        IResource resource = node.getAdapter(IResource.class);
                        if (resource != null) {
                            return rcpProject.getResourcePath(resource);
                        }
                    }
                    return "";
                }
                @Override
                public Image getImage(Object element) {
                    DBNNode node = (DBNNode) element;
                    DBPImage icon;
                    if (node instanceof DBNPathBase) {
                        icon = DBIcon.TREE_SCRIPT;
                    } else {
                        icon = node.getNodeIconDefault();
                    }
                    return DBeaverIcons.getImage(icon);
                }
            });
            scriptsViewer.addDoubleClickListener(event -> {
                StructuredSelection selection = (StructuredSelection) event.getSelection();
                IResource resource = ((DBNNode) selection.getFirstElement()).getAdapter(IResource.class);
                if (resource != null) {
                    DBPResourceHandler handler = DBPPlatformDesktop.getInstance().getWorkspace().getResourceHandler(resource);
                    if (handler != null) {
                        try {
                            handler.openResource(resource);
                        } catch (Exception e) {
                            log.error("Failed to open resource " + resource, e);
                        }
                    }
                }
            });
//            GridData gd = new GridData(GridData.FILL_BOTH);
//            gd.heightHint = 300;
//            gd.widthHint = 400;
//            scriptsViewer.getTable().setLayoutData(gd);
            SQLScriptTaskScriptSelectorDialog.createScriptColumns(scriptsViewer);

            final Table scriptTable = scriptsViewer.getTable();
            scriptTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            ToolBar buttonsToolbar = new ToolBar(filesGroup, SWT.VERTICAL);
            buttonsToolbar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            UIUtils.createToolItem(buttonsToolbar, DTUIMessages.sql_script_task_page_settings_tool_item_text_add_script, UIIcon.ROW_ADD, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    SQLScriptTaskScriptSelectorDialog dialog = new SQLScriptTaskScriptSelectorDialog(getShell(), projectNode);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        for (DBNNode script : dialog.getSelectedScripts()) {
                            if (!selectedScripts.contains(script)) {
                                selectedScripts.add(script);
                            }
                        }
                        refreshScripts();
                    }
                }
            });
            if (DBFUtils.supportsMultiFileSystems(project)) {
                UIUtils.createToolItem(buttonsToolbar, UIMessages.text_with_open_dialog_browse_remote, UIIcon.OPEN_EXTERNAL, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBNPathBase selected = DBWorkbench.getPlatformUI().openFileSystemSelector(
                            UIMessages.text_with_open_dialog_browse_remote,
                            false,
                            SWT.OPEN,
                            false,
                            new String[]{ "*.sql", "*"},
                            null);
                        if (selected != null) {
                            if (!selectedScripts.contains(selected)) {
                                selectedScripts.add(selected);
                            }
                            refreshScripts();
                        }
                    }
                });
            }
            ToolItem deleteItem = UIUtils.createToolItem(buttonsToolbar, DTUIMessages.sql_script_task_page_settings_tool_item_text_remove_script, UIIcon.ROW_DELETE, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ISelection selection = scriptsViewer.getSelection();
                    if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
                        for (Object element : ((IStructuredSelection) selection).toArray()) {
                            if (element instanceof DBNNode node && node.getAdapter(IResource.class) != null) {
                                selectedScripts.remove(element);
                            }
                        }
                        refreshScripts();
                    }
                }
            });
            UIUtils.createToolBarSeparator(buttonsToolbar, SWT.HORIZONTAL);
            ToolItem moveUpItem = UIUtils.createToolItem(buttonsToolbar, DTUIMessages.sql_script_task_page_settings_tool_item_text_move_script_up, UIIcon.ARROW_UP, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = scriptTable.getSelectionIndex();
                    if (selectionIndex > 0) {
                        DBNNode prevScript = selectedScripts.get(selectionIndex - 1);
                        selectedScripts.set(selectionIndex - 1, selectedScripts.get(selectionIndex));
                        selectedScripts.set(selectionIndex, prevScript);
                        refreshScripts();
                    }
                }
            });
            ToolItem moveDownItem = UIUtils.createToolItem(buttonsToolbar, DTUIMessages.sql_script_task_page_settings_tool_item_text_move_script_down, UIIcon.ARROW_DOWN, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = scriptTable.getSelectionIndex();
                    if (selectionIndex < scriptTable.getItemCount() - 1) {
                        DBNNode nextScript = selectedScripts.get(selectionIndex + 1);
                        selectedScripts.set(selectionIndex + 1, selectedScripts.get(selectionIndex));
                        selectedScripts.set(selectionIndex, nextScript);
                        refreshScripts();
                    }
                }
            });
            scriptsViewer.addSelectionChangedListener(event -> {
                int selectionIndex = scriptTable.getSelectionIndex();
                deleteItem.setEnabled(selectionIndex >= 0);
                moveUpItem.setEnabled(selectionIndex > 0);
                moveDownItem.setEnabled(selectionIndex < scriptTable.getItemCount() - 1);
            });
            deleteItem.setEnabled(false);
        }

        {
            Composite connectionsGroup = UIUtils.createControlGroup(mainGroup, DTMessages.sql_script_task_page_settings_group_connections, 2, GridData.FILL_BOTH, 0);

            dataSourceViewer = new TableViewer(connectionsGroup, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
            dataSourceViewer.setContentProvider(new ListContentProvider());
            //dataSourceViewer.getTable().setHeaderVisible(true);
            dataSourceViewer.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((DBNDataSource) element).getNodeDisplayName();
                }
                @Override
                public Image getImage(Object element) {
                    return DBeaverIcons.getImage(((DBNDataSource)element).getNodeIcon());
                }
            });
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            gd.widthHint = 400;
            dataSourceViewer.getTable().setLayoutData(gd);

            final Table dsTable = dataSourceViewer.getTable();
            dsTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            ToolBar buttonsToolbar = new ToolBar(connectionsGroup, SWT.VERTICAL);
            buttonsToolbar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            UIUtils.createToolItem(buttonsToolbar, DTUIMessages.sql_script_task_page_settings_tool_item_text_add_data_source, UIIcon.ROW_ADD, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    SQLScriptTaskDataSourceSelectorDialog dialog = new SQLScriptTaskDataSourceSelectorDialog(getShell(), projectNode);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        for (DBNDataSource ds : dialog.getSelectedDataSources()) {
                            if (!selectedDataSources.contains(ds)) {
                                selectedDataSources.add(ds);
                            }
                        }
                        refreshDataSources();
                        updatePageCompletion();
                    }
                }
            });
            ToolItem deleteItem = UIUtils.createToolItem(buttonsToolbar, DTUIMessages.sql_script_task_page_settings_tool_item_text_remove_data_source, UIIcon.ROW_DELETE, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ISelection selection = dataSourceViewer.getSelection();
                    if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
                        for (Object element : ((IStructuredSelection) selection).toArray()) {
                            if (element instanceof DBNDataSource) {
                                selectedDataSources.remove(element);
                            }
                        }
                        refreshDataSources();
                        updatePageCompletion();
                    }
                }
            });
            UIUtils.createToolBarSeparator(buttonsToolbar, SWT.HORIZONTAL);
            ToolItem moveUpItem = UIUtils.createToolItem(buttonsToolbar, DTUIMessages.sql_script_task_page_settings_tool_item_text_move_data_source_up, UIIcon.ARROW_UP, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = dsTable.getSelectionIndex();
                    if (selectionIndex > 0) {
                        DBNDataSource prevScript = selectedDataSources.get(selectionIndex - 1);
                        selectedDataSources.set(selectionIndex - 1, selectedDataSources.get(selectionIndex));
                        selectedDataSources.set(selectionIndex, prevScript);
                        refreshDataSources();
                    }
                }
            });
            ToolItem moveDownItem = UIUtils.createToolItem(buttonsToolbar, DTUIMessages.sql_script_task_page_settings_tool_item_text_move_data_source_down, UIIcon.ARROW_DOWN, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = dsTable.getSelectionIndex();
                    if (selectionIndex < dsTable.getItemCount() - 1) {
                        DBNDataSource nextScript = selectedDataSources.get(selectionIndex + 1);
                        selectedDataSources.set(selectionIndex + 1, selectedDataSources.get(selectionIndex));
                        selectedDataSources.set(selectionIndex, nextScript);
                        refreshScripts();
                    }
                }
            });
            dataSourceViewer.addSelectionChangedListener(event -> {
                int selectionIndex = dsTable.getSelectionIndex();
                deleteItem.setEnabled(selectionIndex >= 0);
                moveUpItem.setEnabled(selectionIndex > 0);
                moveDownItem.setEnabled(selectionIndex < dsTable.getItemCount() - 1);
            });
            deleteItem.setEnabled(false);
        }
        mainGroup.setWeights(700, 300);

        {
            Composite settingsGroup = UIUtils.createControlGroup(
                composite,
                DTMessages.sql_script_task_page_settings_group_script,
                3,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING,
                0
            );

            ignoreErrorsCheck = UIUtils.createCheckbox(settingsGroup, DTMessages.sql_script_task_page_settings_option_ignore_errors, "", dtSettings.isIgnoreErrors(), 1);
            dumpQueryCheck = UIUtils.createCheckbox(settingsGroup, DTMessages.sql_script_task_page_settings_option_dump_results, "", dtSettings.isDumpQueryResultsToLog(), 1);
            autoCommitCheck = UIUtils.createCheckbox(settingsGroup, DTMessages.sql_script_task_page_settings_option_auto_commit, "", dtSettings.isAutoCommit(), 1);
        }

        getWizard().createVariablesEditButton(composite);

        try {
            getWizard().getContainer().run(true, true, monitor -> {
                try {
                    loadSettings(new DefaultProgressMonitor(monitor));
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            setErrorMessage("Error loading settings: " + e.getTargetException().getMessage());
        } catch (InterruptedException e) {
            // ignore
        }

        setControl(composite);
    }

    private void refreshScripts() {
        scriptsViewer.refresh(true, true);
        updateSelectedScripts();
    }

    private void refreshDataSources() {
        dataSourceViewer.refresh(true, true);
    }

    private void updateSelectedScripts() {
        DBPProject project = sqlWizard.getProject();
        DBNProject projectNode = project.getNavigatorModel().getRoot().getProjectNode(project);

        Set<DBPDataSourceContainer> dataSources = new LinkedHashSet<>();
        for (DBNNode element : selectedScripts) {
            if (element instanceof DBNResource res) {
                Collection<DBPDataSourceContainer> resDS = res.getAssociatedDataSources();
                if (!CommonUtils.isEmpty(resDS)) {
                    dataSources.addAll(resDS);
                }
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
                refreshDataSources();
                for (DBNDataSource dsNode : checkedDataSources) {
                    if (!selectedDataSources.contains(dsNode)) {
                        selectedDataSources.add(dsNode);
                    }
                }
            }
        }
        refreshDataSources();
        updatePageCompletion();
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
            setErrorMessage(DTUIMessages.sql_script_task_page_settings_error_message_you_must_select_script_execute);
            return false;
        }
        if (selectedDataSources.isEmpty()) {
            setErrorMessage(DTUIMessages.sql_script_task_page_settings_error_message_you_must_select_connection);
            return false;
        }
        setErrorMessage(null);
        return true;
    }

    public void loadSettings(DBRProgressMonitor monitor) throws DBException {
        SQLScriptExecuteSettings settings = sqlWizard.getSettings();

        DBPProject project = getWizard().getProject();
        DBNProject projectNode = project.getNavigatorModel().getRoot().getProjectNode(project);
        if (projectNode != null) {
            List<String> scriptFiles = settings.getScriptFiles();
            for (String filePath : scriptFiles) {
                if (IOUtils.isLocalFile(filePath)) {
                    Path workspaceFile;
                    RMControllerProvider rmControllerProvider = DBUtils.getAdapter(RMControllerProvider.class, project);
                    if (rmControllerProvider != null) {
                        workspaceFile = project.getAbsolutePath().resolve(filePath);
                    } else {
                        workspaceFile = DTUtils.findProjectFile(project, filePath);
                    }
                    if (workspaceFile == null) {
                        log.debug("Script file '" + filePath + "' not found");
                        continue;
                    }
                    DBNNode resource = projectNode.findResource(monitor, workspaceFile);
                    if (resource != null) {
                        selectedScripts.add(resource);
                    }
                } else {
                    DBNFileSystems fsNode = projectNode.getExtraNode(DBNFileSystems.class);
                    if (fsNode != null) {
                        DBNPathBase pathNode = fsNode.findNodeByPath(monitor, filePath);
                        if (pathNode != null) {
                            selectedScripts.add(pathNode);
                        }
                    }
                }
            }
        }

        for (DBPDataSourceContainer dataSource : settings.getDataSources()) {
            DBNDataSource dsNode = projectNode.getDatabases().getDataSource(dataSource);
            if (dsNode != null) {
                selectedDataSources.add(dsNode);
            }
        }

        UIUtils.syncExec(() -> {
            scriptsViewer.setInput(selectedScripts);
            dataSourceViewer.setInput(selectedDataSources);
        });
    }

    public void saveSettings() {
        if (sqlWizard == null) {
            return;
        }
        SQLScriptExecuteSettings settings = sqlWizard.getSettings();

        List<String> scriptPaths = new ArrayList<>();
        for (DBNNode resource : selectedScripts) {
            if (resource instanceof DBNPathBase) {
                scriptPaths.add(((DBNPathBase) resource).getPath().toString());
            } else {
                IResource res = resource.getAdapter(IResource.class);
                if (res instanceof IFile && getWizard().getProject() instanceof RCPProject rcpProject) {
                    scriptPaths.add(rcpProject.getResourcePath(res));
                }
            }
        }
        if (!CommonUtils.isEmpty(scriptPaths)) {
            settings.setScriptFiles(scriptPaths);
        }
        List<DBPDataSourceContainer> dsList = new ArrayList<>();
        for (DBNDataSource dsNode : selectedDataSources) {
            dsList.add(dsNode.getDataSourceContainer());
        }
        if (!CommonUtils.isEmpty(dsList)) {
            settings.setDataSources(dsList);
        }

        if (ignoreErrorsCheck != null) {
            settings.setIgnoreErrors(ignoreErrorsCheck.getSelection());
        }
        if (dumpQueryCheck != null) {
            settings.setDumpQueryResultsToLog(dumpQueryCheck.getSelection());
        }
        if (autoCommitCheck != null) {
            settings.setAutoCommit(autoCommitCheck.getSelection());
        }
    }

}