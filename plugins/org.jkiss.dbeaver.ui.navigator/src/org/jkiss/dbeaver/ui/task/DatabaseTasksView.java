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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskCategory;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.model.task.DBTTaskType;
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
            } else if (element instanceof DBTTaskType) {
                return wordMatches(((DBTTaskType) element).getName());
            } else if (element instanceof DBTTask) {
                return wordMatches(((DBTTask) element).getLabel());
            }
            return true;
        }
    }

    private static class TaskTypeInfo {
        DBPProject project;
        DBTTaskType task;

        public TaskTypeInfo(DBPProject project, DBTTaskType task) {
            this.project = project;
            this.task = task;
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        Composite group = UIUtils.createComposite(parent, 1);

        filteredTree = new FilteredTree(group, SWT.FULL_SELECTION, new NamedObjectPatternFilter(), true);
        TreeViewer viewer = filteredTree.getViewer();
        taskTree = viewer.getTree();
        taskTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();
                if (element instanceof DBPProject) {
                    cell.setImage(DBeaverIcons.getImage(DBIcon.PROJECT));
                    cell.setText(((DBPProject) element).getName());
                } else if (element instanceof TaskTypeInfo) {
                    cell.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATA_TYPE));
                    cell.setText(((TaskTypeInfo) element).task.getName());
                } else if (element instanceof DBTTask) {
                    cell.setImage(DBeaverIcons.getImage(DBIcon.TREE_PACKAGE));
                    cell.setText(((DBTTask) element).getLabel());
                }
            }
        });
        viewer.setContentProvider(new TreeContentProvider() {
            @Override
            public Object[] getChildren(Object parentElement) {
                if (parentElement instanceof DBPProject) {
                    DBPProject project = (DBPProject) parentElement;
                    DBTTaskManager taskManager = project.getTaskManager();
                    DBTTaskType[] existingTaskTypes = taskManager.getExistingTaskTypes();
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

        MenuManager menuMgr = createContextMenu(viewer);
        getSite().registerContextMenu(menuMgr, viewer);

        viewer.addDoubleClickListener(event -> openCurrentTask());

        loadTasks();
    }

    private MenuManager createContextMenu(TreeViewer viewer) {
        final MenuManager menuMgr = new MenuManager();
        menuMgr.add(new Action("Open task") {
            @Override
            public void run() {
                openCurrentTask();
            }
        });

        Control control = viewer.getControl();
        control.setMenu(menuMgr.createContextMenu(control));
        control.addDisposeListener(e -> menuMgr.dispose());
        return menuMgr;
    }

    private void openCurrentTask() {
        ISelection selection = filteredTree.getViewer().getSelection();
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            return;
        }
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if (!(element instanceof DBTTask)) {
            return;
        }

        DBTTask task = (DBTTask)element;
        DBTTaskCategory taskTypeDescriptor = task.getType().getCategory();
        if (!taskTypeDescriptor.supportsConfigurator()) {
            return;
        }

        try {
            taskTypeDescriptor.createConfigurator().configureTask(DBWorkbench.getPlatform(), task);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Task configuration", "Error opening task configuration editor", e);
        }
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
            DBTTaskType[] taskTypes = taskManager.getExistingTaskTypes();
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
            DBTTaskType[] taskTypes = taskManager.getExistingTaskTypes();
            if (taskTypes.length == 0) {
                continue;
            }
            TreeItem projectItem = new TreeItem(taskTree, SWT.NONE);
            projectItem.setImage(DBeaverIcons.getImage(DBIcon.PROJECT));
            projectItem.setText(0, project.getName());
            projectItem.setData(project);
            for (DBTTaskType task : taskTypes) {
                TreeItem taskTypeItem = new TreeItem(projectItem, SWT.NONE);
                taskTypeItem.setText(0, task.getName());
                taskTypeItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATA_TYPE));
                taskTypeItem.setData(task);

                for (DBTTask taskConfig : taskManager.getTaskConfigurations(task)) {
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
