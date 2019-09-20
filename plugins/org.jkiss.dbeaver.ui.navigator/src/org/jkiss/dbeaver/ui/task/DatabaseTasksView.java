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
import org.eclipse.jface.action.Separator;
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
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DatabaseTasksView extends ViewPart implements DBTTaskListener {
    public static final String VIEW_ID = "org.jkiss.dbeaver.tasks";
    private Tree taskTree;
    private FilteredTree filteredTree;
    private ViewerColumnController columnController;

    //private final SimpleDateFormat dateFormat = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH);
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$

    public class NamedObjectPatternFilter extends PatternFilter {
        public NamedObjectPatternFilter() {
            setIncludeLeadingWildcard(true);
        }

        protected boolean isLeafMatch(Viewer viewer, Object element) {
            if (element instanceof DBTTask) {
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
        taskTree.setHeaderVisible(true);
        taskTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        columnController = new ViewerColumnController("tasks", filteredTree.getViewer());
        columnController.addColumn("Name", "Task name", SWT.LEFT, true, true, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                cell.setImage(DBeaverIcons.getImage(DBIcon.TREE_PACKAGE));
                cell.setText(((DBTTask) cell.getElement()).getLabel());
            }
        });
        columnController.addColumn("Created", "Task create time", SWT.LEFT, true, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                cell.setText(dateFormat.format(((DBTTask) cell.getElement()).getCreateTime()));
            }
        });
        columnController.addColumn("Last Run", "Task last start time", SWT.LEFT, true, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DBTTaskRun lastRun = ((DBTTask) cell.getElement()).getLastRun();
                if (lastRun == null) {
                    cell.setText("N/A");
                } else {
                    cell.setText(dateFormat.format(lastRun.getStartTime()));
                }
            }
        });
        columnController.addColumn("Last Duration", "Task last run duration", SWT.LEFT, false, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DBTTaskRun lastRun = ((DBTTask) cell.getElement()).getLastRun();
                if (lastRun == null) {
                    cell.setText("N/A");
                } else {
                    cell.setText(RuntimeUtils.formatExecutionTime(lastRun.getRunDuration()));
                }
            }
        });
        columnController.addColumn("Description", "Task description", SWT.LEFT, false, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                cell.setText(CommonUtils.notEmpty(((DBTTask) cell.getElement()).getDescription()));
            }
        });
        columnController.addColumn("Type", "Task type", SWT.LEFT, true, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                cell.setText(((DBTTask) cell.getElement()).getType().getName());
            }
        });
        columnController.addColumn("Category", "Task category", SWT.LEFT, false, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                cell.setText(((DBTTask) cell.getElement()).getType().getCategory().getName());
            }
        });
        columnController.addColumn("Project", "Task container project", SWT.LEFT, true, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                cell.setText(((DBTTask) cell.getElement()).getProject().getName());
            }
        });
        columnController.createColumns(true);

        viewer.setContentProvider(new ITreeContentProvider() {

            @Override
            public Object[] getElements(Object inputElement) {
                return ((Collection)inputElement).toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                return new Object[0];
            }

            @Override
            public Object getParent(Object element) {
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                return false;
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
        menuMgr.add(new Separator());
        columnController.fillConfigMenu(menuMgr);

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
        TaskRegistry.getInstance().addTaskListener(this);
    }

    @Override
    public void dispose() {
        TaskRegistry.getInstance().removeTaskListener(this);
        super.dispose();
    }

    @Override
    public void handleTaskEvent(DBTTaskEvent event) {
        switch (event.getAction()) {
            case TASK_ADD:
                filteredTree.getViewer().add(null, event.getTask());
                break;
            case TASK_REMOVE:
                filteredTree.getViewer().remove(event.getTask());
                break;
            case TASK_UPDATE:
                filteredTree.getViewer().refresh(event.getTask());
                break;
        }
    }

    private void loadTasks() {

        List<DBPProject> projectsWithTasks = new ArrayList<>();
        List<DBTTask> allTasks = new ArrayList<>();
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            DBTTaskManager taskManager = project.getTaskManager();
            DBTTask[] tasks = taskManager.getTaskConfigurations();
            if (tasks.length == 0) {
                continue;
            }
            projectsWithTasks.add(project);
            Collections.addAll(allTasks, tasks);
        }
        allTasks.sort(Comparator.comparing(DBTTask::getCreateTime));

        filteredTree.getViewer().setInput(allTasks);
        columnController.repackColumns();
    }

}
