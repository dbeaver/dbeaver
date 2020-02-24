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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.utils.CommonUtils;

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
    private DatabaseNavigatorTree scriptsTree;
    private DatabaseNavigatorTree dataSourceTree;

    private List<DBNResource> selectedScripts = new ArrayList<>();
    private List<DBNDataSource> selectedDataSources = new ArrayList<>();

    SQLTaskPageSettings(SQLTaskConfigurationWizard wizard) {
        super("SQL Script execute");
        setTitle("SQL Script execute settings");
        setDescription("Select scripts and connections). Each script will be executed in all selected connections.");
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
            Composite filesGroup = UIUtils.createControlGroup(mainGroup, "Files", 1, GridData.FILL_BOTH, 0);

            INavigatorFilter scriptFilter = new INavigatorFilter() {
                @Override
                public boolean filterFolders() {
                    return true;
                }

                @Override
                public boolean isLeafObject(Object object) {
                    return object instanceof DBNResource && ((DBNResource) object).getResource() instanceof IFile;
                }

                @Override
                public boolean select(Object element) {
                    return element instanceof DBNLocalFolder || element instanceof DBNResource;
                }
            };
            scriptsTree = new DatabaseNavigatorTree(
                filesGroup,
                projectNode,
                SWT.SINGLE | SWT.BORDER | SWT.CHECK,
                false,
                scriptFilter);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            scriptsTree.setLayoutData(gd);
            scriptsTree.getViewer().addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof TreeNodeSpecial) {
                        return true;
                    }
                    if (element instanceof DBNResource) {
                        return isResourceApplicable((DBNResource) element);
                    }
                    return false;
                }
            });
            scriptsTree.getViewer().addSelectionChangedListener(event -> {
                updateSelectedScripts();
            });
            scriptsTree.getViewer().expandToLevel(2);
            scriptsTree.getViewer().getTree().setHeaderVisible(true);
            createScriptColumns();

            Composite connectionsGroup = UIUtils.createControlGroup(mainGroup, "Connections", 1, GridData.FILL_BOTH, 0);

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
            dataSourceTree.setLayoutData(new GridData(GridData.FILL_BOTH));
            //dataSourceTree.getViewer().getTree().setHeaderVisible(true);
            dataSourceTree.getViewer().addSelectionChangedListener(event -> {
                updateSelectedDataSources();
                updatePageCompletion();
            });
        }

        {
            Composite settingsGroup = UIUtils.createControlGroup(composite, "Script settings", 3, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            ignoreErrorsCheck = UIUtils.createCheckbox(settingsGroup, "Ignore Errors", "", dtSettings.isIgnoreErrors(), 1);
            dumpQueryCheck = UIUtils.createCheckbox(settingsGroup, "Dump query results to log file", "", dtSettings.isDumpQueryResultsToLog(), 1);
            autoCommitCheck = UIUtils.createCheckbox(settingsGroup, "Auto-commit", "", dtSettings.isAutoCommit(), 1);
        }

        getWizard().createTaskSaveButtons(composite, true, 1);

        loadSettings();

        setControl(composite);
    }

    private void updateSelectedScripts() {
        DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(sqlWizard.getProject());

        List<DBNResource> newCheckedScripts = new ArrayList<>();
        Set<DBPDataSourceContainer> dataSources = new LinkedHashSet<>();
        for (Object element : scriptsTree.getCheckboxViewer().getCheckedElements()) {
            if (element instanceof DBNResource) {
                newCheckedScripts.add((DBNResource) element);
                Collection<DBPDataSourceContainer> resDS = ((DBNResource) element).getAssociatedDataSources();
                if (!CommonUtils.isEmpty(resDS)) {
                    dataSources.addAll(resDS);
                }
            }
        }
        if (newCheckedScripts.equals(selectedScripts)) {
            return;
        }
        selectedScripts.clear();
        selectedScripts.addAll(newCheckedScripts);

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

    private void createScriptColumns() {
        final ILabelProvider mainLabelProvider = (ILabelProvider) scriptsTree.getViewer().getLabelProvider();
        ViewerColumnController columnController = new ViewerColumnController("sqlTaskScriptViewer", scriptsTree.getViewer());
        columnController.setForceAutoSize(true);
        columnController.addColumn("Name", "Script", SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return mainLabelProvider.getText(element);
            }
            @Override
            public Image getImage(Object element) {
                return mainLabelProvider.getImage(element);
            }
            @Override
            public String getToolTipText(Object element) {
                if (mainLabelProvider instanceof IToolTipProvider) {
                    return ((IToolTipProvider) mainLabelProvider).getToolTipText(element);
                }
                return null;
            }
        });

        columnController.addColumn("DataSource", "Script datasource", SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DBNResource) {
                    Collection<DBPDataSourceContainer> containers = ((DBNResource) element).getAssociatedDataSources();
                    if (!CommonUtils.isEmpty(containers)) {
                        StringBuilder text = new StringBuilder();
                        for (DBPDataSourceContainer container : containers) {
                            if (text.length() > 0) {
                                text.append(", ");
                            }
                            text.append(container.getName());
                        }
                        return text.toString();
                    }
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                return null;
            }
        });
        columnController.createColumns(true);
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

    private boolean isResourceApplicable(DBNResource element) {
        IResource resource = element.getResource();
        if (resource instanceof IFolder) {
            // FIXME: this is a hack
            return "script folder".equals(element.getNodeType());
        }
        return resource instanceof IContainer || (resource instanceof IFile && "sql".equals(resource.getFileExtension()));
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
        if (!selectedScripts.isEmpty()) {
            scriptsTree.getCheckboxViewer().setCheckedElements(selectedScripts.toArray());
            scriptsTree.getCheckboxViewer().reveal(selectedScripts.get(0));
        }

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