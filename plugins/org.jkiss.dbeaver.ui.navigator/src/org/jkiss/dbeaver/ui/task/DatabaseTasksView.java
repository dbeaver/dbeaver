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
package org.jkiss.dbeaver.ui.task;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTaskConfiguration;
import org.jkiss.dbeaver.model.task.DBTTaskDescriptor;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;

import java.util.ArrayList;
import java.util.List;

public class DatabaseTasksView extends ViewPart {
    public static final String VIEW_ID = "org.jkiss.dbeaver.tasks";
    private Tree taskTree;
    private FilteredTree filteredTree;

    public class NamedObjectPatternFilter extends PatternFilter {
        public NamedObjectPatternFilter() {
            setIncludeLeadingWildcard(true);
        }

        protected boolean isLeafMatch(Viewer viewer, Object element) {
            if (element instanceof DBPProject) {
                return wordMatches(((DBPProject) element).getName());
            } else if (element instanceof DBTTaskDescriptor) {
                return wordMatches(((DBTTaskDescriptor) element).getName());
            } else if (element instanceof DBTTaskConfiguration) {
                return wordMatches(((DBTTaskConfiguration) element).getLabel());
            }
            return true;
        }
    }

    private static class TaskTypeInfo {
        DBPProject project;
        DBTTaskDescriptor task;

        public TaskTypeInfo(DBPProject project, DBTTaskDescriptor task) {
            this.project = project;
            this.task = task;
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        Composite group = UIUtils.createComposite(parent, 1);

        filteredTree = new FilteredTree(group, SWT.FULL_SELECTION, new NamedObjectPatternFilter(), true);
        taskTree = filteredTree.getViewer().getTree();
        taskTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        filteredTree.getViewer().setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();
                if (element instanceof DBPProject) {
                    cell.setImage(DBeaverIcons.getImage(DBIcon.PROJECT));
                    cell.setText(((DBPProject) element).getName());
                } else if (element instanceof TaskTypeInfo) {
                    cell.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATA_TYPE));
                    cell.setText(((TaskTypeInfo) element).task.getName());
                } else if (element instanceof DBTTaskConfiguration) {
                    cell.setImage(DBeaverIcons.getImage(DBIcon.TREE_PACKAGE));
                    cell.setText(((DBTTaskConfiguration) element).getLabel());
                }
            }
        });
        filteredTree.getViewer().setContentProvider(new TreeContentProvider() {
            @Override
            public Object[] getChildren(Object parentElement) {
                if (parentElement instanceof DBPProject) {
                    DBPProject project = (DBPProject) parentElement;
                    DBTTaskManager taskManager = project.getTaskManager();
                    DBTTaskDescriptor[] existingTaskTypes = taskManager.getExistingTaskTypes();
                    if (existingTaskTypes.length == 0) {
                        return null;
                    }
                    TaskTypeInfo[] children = new TaskTypeInfo[existingTaskTypes.length];
                    for (int i = 0; i < existingTaskTypes.length; i++) children[i] = new TaskTypeInfo(project, existingTaskTypes[i]);
                    return children;
                } else if (parentElement instanceof TaskTypeInfo) {
                    return ((TaskTypeInfo) parentElement).project.getTaskManager().getTaskConfigurations(((TaskTypeInfo) parentElement).task);
                }
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                return element instanceof DBPProject || element instanceof TaskTypeInfo;
            }
        });

        loadTasks();
    }

    @Override
    public void setFocus() {
        taskTree.setFocus();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IWorkbenchAdapter.class) {
            return adapter.cast(new WorkbenchAdapter() {
                @Override
                public String getLabel(Object o) {
                    return "Database Tasks";
                }
            });
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
    }

    private void loadTasks() {

        List<DBPProject> projectsWithTasks = new ArrayList<>();
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            DBTTaskManager taskManager = project.getTaskManager();
            DBTTaskDescriptor[] taskTypes = taskManager.getExistingTaskTypes();
            if (taskTypes.length == 0) {
                continue;
            }
            projectsWithTasks.add(project);
        }
        filteredTree.getViewer().setInput(projectsWithTasks);
        filteredTree.getViewer().expandAll();

/*
        taskTree.removeAll();

        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            DBTTaskManager taskManager = project.getTaskManager();
            DBTTaskDescriptor[] taskTypes = taskManager.getExistingTaskTypes();
            if (taskTypes.length == 0) {
                continue;
            }
            TreeItem projectItem = new TreeItem(taskTree, SWT.NONE);
            projectItem.setImage(DBeaverIcons.getImage(DBIcon.PROJECT));
            projectItem.setText(0, project.getName());
            projectItem.setData(project);
            for (DBTTaskDescriptor task : taskTypes) {
                TreeItem taskTypeItem = new TreeItem(projectItem, SWT.NONE);
                taskTypeItem.setText(0, task.getName());
                taskTypeItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATA_TYPE));
                taskTypeItem.setData(task);

                for (DBTTaskConfiguration taskConfig : taskManager.getTaskConfigurations(task)) {
                    TreeItem taskItem = new TreeItem(taskTypeItem, SWT.NONE);
                    taskItem.setText(0, taskConfig.getLabel());
                    taskItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_PACKAGE));
                    taskItem.setData(taskConfig);
                }
                taskTypeItem.setExpanded(true);
            }
            projectItem.setExpanded(true);
        }
*/
    }
}
