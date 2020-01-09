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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskConfigPanel;
import org.jkiss.dbeaver.model.task.DBTTaskConfigurator;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.SelectDataSourceCombo;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * SQL task configurator
 */
public class SQLTaskConfigurator implements DBTTaskConfigurator {

    private static final Log log = Log.getLog(SQLTaskConfigurator.class);

    @Override
    public ConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public IWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        return new SQLTaskConfigurationWizard();
    }

    private static class ConfigPanel implements DBTTaskConfigPanel {

        private DBRRunnableContext runnableContext;
        private DBTTaskType taskType;
        private DBPProject currentProject;
        private SQLTaskConfigurationWizard sqlWizard;

        private Runnable propertyChangeListener;
        private DBNResource selectedScript;
        private DBPProject selectedProject;
        private SelectDataSourceCombo dsSelectCombo;

        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            this.runnableContext = runnableContext;
            this.taskType = taskType;
            this.currentProject = NavigatorUtils.getSelectedProject();
        }

        @Override
        public void createControl(Object parent, Object wizard, Runnable propertyChangeListener) {
            sqlWizard = (SQLTaskConfigurationWizard) wizard;
            this.propertyChangeListener = propertyChangeListener;

            Group group = UIUtils.createControlGroup(
                (Composite) parent,
                "SQL Script parameters",
                1,
                GridData.FILL_BOTH,
                0);

            INavigatorFilter filter = null;/*new INavigatorFilter() {
                @Override
                public boolean filterFolders() {
                    return false;
                }

                @Override
                public boolean isLeafObject(Object object) {
                    return object instanceof DBNResource && ((DBNResource) object).getResource() instanceof IFile;
                }

                @Override
                public boolean select(Object element) {
                    if (element instanceof DBNRoot) {
                        return true;
                    }
                    if (!(element instanceof DBNResource)) {
                        return false;
                    }
                    return isResourceApplicable((DBNResource) element);
                }
            }*/;
            DatabaseNavigatorTree dataSourceTree = new DatabaseNavigatorTree(
                group,
                DBWorkbench.getPlatform().getNavigatorModel().getRoot(),
                SWT.SINGLE | SWT.BORDER,
                false,
                filter);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            dataSourceTree.setLayoutData(gd);
            dataSourceTree.getViewer().addFilter(new ViewerFilter() {
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
            dataSourceTree.getViewer().addSelectionChangedListener(event -> {
                DBPProject oldSelectedProject = selectedProject;
                selectedProject = null;
                selectedScript = null;
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                    Object firstElement = ((IStructuredSelection) selection).getFirstElement();
                    if (firstElement instanceof DBNResource) {
                        selectedScript = (DBNResource) firstElement;
                        selectedProject = selectedScript.getOwnerProject();
                    }
                }
                if (selectedProject != oldSelectedProject) {
                    dsSelectCombo.removeAll();
                    if (selectedProject != null) {
                        for (DBPDataSourceContainer ds : selectedProject.getDataSourceRegistry().getDataSources()) {
                            dsSelectCombo.addItem(ds);
                        }
                    }
                }
                updateDataSource();
            });

            dsSelectCombo = new SelectDataSourceCombo(group) {
                @Override
                protected DBPProject getActiveProject() {
                    return selectedProject == null ? NavigatorUtils.getSelectedProject() : selectedProject;
                }
            };
        }

        private boolean isResourceApplicable(DBNResource element) {
            IResource resource = element.getResource();
            if (resource instanceof IFolder) {
                // FIXME: this is a hack
                return "script folder".equals(element.getNodeType());
            }
            return resource instanceof IContainer || (resource instanceof IFile && "sql".equals(resource.getFileExtension()));
        }

        private void updateDataSource() {
            DBPDataSourceContainer ds = null;
            if (selectedScript != null) {
                Collection<DBPDataSourceContainer> associatedDataSources = selectedScript.getAssociatedDataSources();
                if (!CommonUtils.isEmpty(associatedDataSources)) {
                    ds = associatedDataSources.iterator().next();
                }
            }
            dsSelectCombo.select(ds);

            if (propertyChangeListener != null) {
                propertyChangeListener.run();
            }
        }

        private void updateSettings(Runnable propertyChangeListener) {
//            saveSettings();
//            propertyChangeListener.run();
//            UIUtils.asyncExec(() -> UIUtils.packColumns(objectsTable, true));
        }

        @Override
        public void loadSettings() {
        }

        @Override
        public void saveSettings() {
        }

        @Override
        public boolean isComplete() {
            return selectedScript != null && dsSelectCombo.getSelectedItem() != null;
        }
    }

}
