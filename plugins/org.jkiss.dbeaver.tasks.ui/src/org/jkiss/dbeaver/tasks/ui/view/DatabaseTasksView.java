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
package org.jkiss.dbeaver.tasks.ui.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.*;

public class DatabaseTasksView extends ViewPart implements DBTTaskListener {
    private static final Log log = Log.getLog(DatabaseTasksView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.tasks";
    private static final String TASKS_VIEW_MENU_ID = VIEW_ID + ".menu";

    public static final String CREATE_TASK_CMD_ID = "org.jkiss.dbeaver.task.create";
    public static final String COPY_TASK_CMD_ID = "org.jkiss.dbeaver.task.copy";
    public static final String EDIT_TASK_CMD_ID = "org.jkiss.dbeaver.task.edit";
    public static final String RUN_TASK_CMD_ID = "org.jkiss.dbeaver.task.run";
    public static final String GROUP_TASK_CMD_ID = "org.jkiss.dbeaver.task.group";

    private static final ArrayList<Object> EMPTY_TASK_RUN_LIST = new ArrayList<>();

    private DatabaseTasksTree tasksTree;

    private TreeViewer taskRunViewer;
    private ViewerColumnController taskRunColumnController;

    public DatabaseTasksView() {
    }

    public DatabaseTasksTree getTasksTree() {
        return tasksTree;
    }

    public TreeViewer getTaskRunViewer() {
        return taskRunViewer;
    }

    @Override
    public void createPartControl(Composite parent) {
        SashForm sashForm = UIUtils.createPartDivider(this, parent, SWT.HORIZONTAL);

        createTaskTree(sashForm);
        createTaskRunTable(sashForm);

        sashForm.setWeights(new int[]{700, 300});

        loadViewConfig();
        loadTasks();
    }

    private void createTaskTree(Composite composite) {
        tasksTree = new DatabaseTasksTree(composite, false);

        MenuManager menuMgr = createTaskContextMenu(tasksTree.getViewer());
        getSite().registerContextMenu(TASKS_VIEW_MENU_ID, menuMgr, tasksTree.getViewer());
        getSite().setSelectionProvider(tasksTree.getViewer());

        tasksTree.getViewer().addDoubleClickListener(event -> ActionUtils.runCommand(EDIT_TASK_CMD_ID, getSite().getSelectionProvider().getSelection(), getSite()));
        tasksTree.getViewer().addSelectionChangedListener(event -> loadTaskRuns());

        DatabaseTasksTree.addDragSourceSupport(tasksTree.getViewer(), null);
    }

    private void createTaskRunTable(Composite parent) {
        FilteredTree filteredTree = new FilteredTree(parent, SWT.SINGLE | SWT.FULL_SELECTION, new NamedObjectPatternFilter(), true);
        filteredTree.setInitialText("Task executions: type a part of error message");
        taskRunViewer = filteredTree.getViewer();
        Tree taskrunTree = taskRunViewer.getTree();
        taskrunTree.setHeaderVisible(true);
        taskrunTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        taskRunColumnController = new ViewerColumnController("taskruns", taskRunViewer);
        taskRunColumnController.addColumn("Time", "Task start time", SWT.LEFT, true, true, new TaskRunLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTaskRun taskRun) {
                cell.setText(tasksTree.getDateFormat().format(taskRun.getStartTime()));
            }
        });
        taskRunColumnController.addColumn("Duration", "Task last run duration", SWT.LEFT, true, false, new TaskRunLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTaskRun taskRun) {
                cell.setText(RuntimeUtils.formatExecutionTime(taskRun.getRunDuration()));
            }
        });
        taskRunColumnController.addColumn("Result", "Task result", SWT.LEFT, true, false, new TaskRunLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTaskRun taskRun) {
                if (taskRun.isRunSuccess()) {
                    cell.setText("Success");
                } else {
                    cell.setText(CommonUtils.notEmpty(taskRun.getErrorMessage()));
                }
            }
        });
        taskRunColumnController.setForceAutoSize(true);
        taskRunColumnController.createColumns(true);

        taskRunViewer.setContentProvider(new TreeRunContentProvider());

        MenuManager menuMgr = createTaskRunContextMenu(taskRunViewer);
        getSite().registerContextMenu(menuMgr, taskRunViewer);

        taskRunViewer.addDoubleClickListener(event -> new ViewRunLogAction().run());
    }

    private MenuManager createTaskContextMenu(TreeViewer viewer) {
        final MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            manager.add(ActionUtils.makeCommandContribution(getSite(), RUN_TASK_CMD_ID));
            manager.add(ActionUtils.makeCommandContribution(getSite(), EDIT_TASK_CMD_ID));
            //manager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.FILE_PROPERTIES, "Task properties", null));
            manager.add(ActionUtils.makeCommandContribution(getSite(), CREATE_TASK_CMD_ID));
            manager.add(ActionUtils.makeCommandContribution(getSite(), COPY_TASK_CMD_ID));
            manager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.EDIT_DELETE, "Delete task", null));
            manager.add(new Separator());
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            manager.add(new Separator());
            for (TaskHandlerGroupBy.GroupBy gb : TaskHandlerGroupBy.GroupBy.values()) {
                manager.add(ActionUtils.makeCommandContribution(
                    getSite(),
                    GROUP_TASK_CMD_ID,
                    CommandContributionItem.STYLE_CHECK,
                    null, null, null, true, Collections.singletonMap("group", gb.name())));
            }
            manager.add(new Separator());
            tasksTree.getColumnController().fillConfigMenu(manager);
        });

        Control control = viewer.getControl();
        control.setMenu(menuMgr.createContextMenu(control));

        return menuMgr;
    }

    private MenuManager createTaskRunContextMenu(TreeViewer viewer) {
        final MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            DBTTask task = tasksTree.getSelectedTask();
            DBTTaskRun taskRun = getSelectedTaskRun();
            if (task != null && taskRun != null) {
                manager.add(new ViewRunLogAction());
                manager.add(new DeleteRunLogAction());
            }
            if (task != null && task.getLastRun() != null) {
                manager.add(new ClearRunLogAction());
                manager.add(new OpenRunLogFolderAction());
            }
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            manager.add(new Separator());
            taskRunColumnController.fillConfigMenu(manager);
        });

        Control control = viewer.getControl();
        control.setMenu(menuMgr.createContextMenu(control));

        return menuMgr;
    }

    @Override
    public void setFocus() {
        tasksTree.getViewer().getControl().setFocus();
    }

    @Nullable
    private DBTTaskRun getSelectedTaskRun() {
        ISelection selection = taskRunViewer.getSelection();
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            return null;
        }
        Object element = ((IStructuredSelection) selection).getFirstElement();
        return element instanceof DBTTaskRun ? (DBTTaskRun) element : null;
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
        UIUtils.asyncExec(() -> {
            DBTTask task = event.getTask();
            switch (event.getAction()) {
                case TASK_ADD:
                    refresh();
                    tasksTree.getViewer().setSelection(new StructuredSelection(task), true);
                    break;
                case TASK_REMOVE:
                    refresh();
                    break;
                case TASK_UPDATE:
                    tasksTree.getViewer().refresh(task);
                    if (task == tasksTree.getSelectedTask()) {
                        loadTaskRuns();
                    }
                    break;
                case TASK_EXECUTE:
                    refresh();
                    break;
            }
        });
    }

    private void loadViewConfig() {
        tasksTree.loadViewConfig();

    }

    public void refresh() {
        tasksTree.refresh();

        loadTaskRuns();
    }

    private void loadTasks() {
        tasksTree.loadTasks();
    }

    private void loadTaskRuns() {
        DBTTask selectedTask = tasksTree.getSelectedTask();
        if (selectedTask == null) {
            taskRunViewer.setInput(EMPTY_TASK_RUN_LIST);
        } else {
            DBTTaskRun[] runs = selectedTask.getRunStatistics();
            if (ArrayUtils.isEmpty(runs)) {
                taskRunViewer.setInput(EMPTY_TASK_RUN_LIST);
            } else {
                Arrays.sort(runs, Comparator.comparing(DBTTaskRun::getStartTime).reversed());
                taskRunViewer.setInput(Arrays.asList(runs));
            }
        }
    }

    private class TreeRunContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            return ((Collection) inputElement).toArray();
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
    }

    public class NamedObjectPatternFilter extends PatternFilter {
        NamedObjectPatternFilter() {
            setIncludeLeadingWildcard(true);
        }

        protected boolean isLeafMatch(Viewer viewer, Object element) {
            if (element instanceof DBTTask) {
                return wordMatches(((DBTTask) element).getName());
            } else if (element instanceof DBTTaskRun) {
                return wordMatches(element.toString());
            }
            return true;
        }
    }

    private static class TaskCategoryNode {
        final DBPProject project;
        final TaskCategoryNode parent;
        final DBTTaskCategory category;

        TaskCategoryNode(DBPProject project, TaskCategoryNode parent, DBTTaskCategory category) {
            this.project = project;
            this.parent = parent;
            this.category = category;
        }

        @Override
        public String toString() {
            return category.getName();
        }

        @Override
        public int hashCode() {
            return (project == null ? 0 : project.hashCode()) +
                (parent == null ? 0 : parent.hashCode()) +
                (category == null ? 0 : category.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TaskCategoryNode)) {
                return false;
            }
            TaskCategoryNode cmp = (TaskCategoryNode)obj;
            return project == cmp.project &&
                CommonUtils.equalObjects(parent, cmp.parent) &&
                category == cmp.category;
        }
    }

    private abstract class TaskRunLabelProvider extends ColumnLabelProvider {
        @Override
        public final void update(ViewerCell cell) {
            DBTTaskRun taskRun = (DBTTaskRun) cell.getElement();
            if (taskRun != null && !taskRun.isRunSuccess()) {
                cell.setBackground(tasksTree.getColorError());
            } else {
                cell.setBackground(null);
            }
            update(cell, taskRun);
        }

        protected abstract void update(ViewerCell cell, DBTTaskRun task);
    }

    private class ViewRunLogAction extends Action {

        ViewRunLogAction() {
            super("View log");
        }

        @Override
        public void run() {
            DBTTask task = tasksTree.getSelectedTask();
            DBTTaskRun taskRun = getSelectedTaskRun();
            if (task != null && taskRun != null) {
                File runLog = task.getRunLog(taskRun);
                if (runLog.exists()) {
                    try {
                        EditorUtils.openExternalFileEditor(runLog, getSite().getWorkbenchWindow());
                    } catch (Exception e) {
                        DBWorkbench.getPlatformUI().showError("Open log error", "Error while opening task execution log", e);
                    }
                } else {
                    UIUtils.showMessageBox(getSite().getShell(), "Lof file not found", "Can't find log file '" + runLog.getAbsolutePath() + "'", SWT.ICON_ERROR);
                }
            }
        }
    }

    private class DeleteRunLogAction extends Action {

        DeleteRunLogAction() {
            super("Delete run log", DBeaverIcons.getImageDescriptor(UIIcon.DELETE));
        }

        @Override
        public void run() {
            DBTTask task = tasksTree.getSelectedTask();
            DBTTaskRun taskRun = getSelectedTaskRun();
            if (task != null && taskRun != null &&
                UIUtils.confirmAction(
                    "Remove task run",
                    "Are you sure you want to delete task '" + task.getName() + "' run at '" + tasksTree.getDateFormat().format(taskRun.getStartTime()) + "'?"))
            {
                task.removeRunLog(taskRun);
            }
        }
    }

    private class ClearRunLogAction extends Action {

        ClearRunLogAction() {
            super("Clear logs", DBeaverIcons.getImageDescriptor(UIIcon.ERASE));
        }

        @Override
        public void run() {
            DBTTask task = tasksTree.getSelectedTask();
            if (task == null || !UIUtils.confirmAction("Clear task runs", "Are you sure you want to delete all log of task '" + task.getName() + "'?")) {
                return;
            }
            task.cleanRunStatistics();
        }
    }

    private class OpenRunLogFolderAction extends Action {

        OpenRunLogFolderAction() {
            super("Open logs folder");
        }

        @Override
        public void run() {
            DBTTask task = tasksTree.getSelectedTask();
            if (task != null) {
                DBWorkbench.getPlatformUI().executeShellProgram(task.getRunLogFolder().getAbsolutePath());
            }
        }
    }

}
