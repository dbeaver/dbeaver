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
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
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
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DatabaseTasksView extends ViewPart implements DBTTaskListener {
    private static final Log log = Log.getLog(DatabaseTasksView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.tasks";
    public static final String TASKS_VIEW_MENU_ID = VIEW_ID + ".menu";

    public static final String CREATE_TASK_CMD_ID = "org.jkiss.dbeaver.task.create";
    public static final String EDIT_TASK_CMD_ID = "org.jkiss.dbeaver.task.edit";
    public static final String RUN_TASK_CMD_ID = "org.jkiss.dbeaver.task.run";

    private static final ArrayList<Object> EMPTY_TASK_RUN_LIST = new ArrayList<>();

    private TreeViewer taskViewer;
    private ViewerColumnController taskColumnController;

    private TreeViewer taskRunViewer;
    private ViewerColumnController taskRunColumnController;

    private final List<DBTTask> allTasks = new ArrayList<>();

    //private final SimpleDateFormat dateFormat = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH);
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$
    private Color colorError;

    private static class TreeListContentProvider implements ITreeContentProvider {

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

    @Override
    public void createPartControl(Composite parent) {
        ColorRegistry colorRegistry = getSite().getWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

        colorError = colorRegistry.get("org.jkiss.dbeaver.txn.color.reverted.background");

        SashForm sashForm = UIUtils.createPartDivider(this, parent, SWT.HORIZONTAL);

        createTaskTree(sashForm);
        createTaskRunTable(sashForm);

        sashForm.setWeights(new int[]{700, 300});

        loadTasks();
    }

    private void createTaskTree(Composite composite) {
        FilteredTree filteredTree = new FilteredTree(composite, SWT.MULTI | SWT.FULL_SELECTION, new NamedObjectPatternFilter(), true);
        filteredTree.setInitialText("Tasks: type a part of task name here");
        taskViewer = filteredTree.getViewer();
        Tree taskTree = taskViewer.getTree();
        taskTree.setHeaderVisible(true);
        taskTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        taskColumnController = new ViewerColumnController("tasks", filteredTree.getViewer());
        taskColumnController.addColumn("Name", "Task name", SWT.LEFT, true, true, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                return task.getName();
            }

            @Override
            protected DBPImage getCellImage(DBTTask task) {
                DBPImage icon = task.getType().getIcon();
                return icon != null ? icon : DBIcon.TREE_TASK;
            }

            @Override
            public String getToolTipText(Object element) {
                String description = ((DBTTask) element).getDescription();
                if (CommonUtils.isEmpty(description)) {
                    description = ((DBTTask) element).getName();
                }
                return description;
            }
        });
        taskColumnController.addColumn("Created", "Task create time", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                return dateFormat.format(task.getCreateTime());
            }
        });
        taskColumnController.addColumn("Last Run", "Task last start time", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                DBTTaskRun lastRun = task.getLastRun();
                if (lastRun == null) {
                    return "N/A";
                } else {
                    return dateFormat.format(lastRun.getStartTime());
                }
            }
        });
        taskColumnController.addColumn("Last Duration", "Task last run duration", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                DBTTaskRun lastRun = task.getLastRun();
                if (lastRun == null) {
                    return "N/A";
                } else {
                    return RuntimeUtils.formatExecutionTime(lastRun.getRunDuration());
                }
            }
        });
        taskColumnController.addColumn("Last Result", "Task last result", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                DBTTaskRun lastRun = task.getLastRun();
                if (lastRun == null) {
                    return "N/A";
                } else {
                    if (lastRun.isRunSuccess()) {
                        return "Success";
                    } else {
                        return CommonUtils.notEmpty(lastRun.getErrorMessage());
                    }
                }
            }
        });
        DBTScheduler scheduler = TaskRegistry.getInstance().getActiveSchedulerInstance();
        if (scheduler != null) {
            taskColumnController.addColumn("Next Run", "Task next scheduled run", SWT.LEFT, true, false, new TaskLabelProvider() {
                @Override
                protected String getCellText(DBTTask task) {
                    DBTTaskScheduleInfo scheduledTask = scheduler.getScheduledTaskInfo(task);
                    if (scheduledTask == null) {
                        return "";
                    } else {
                        return scheduledTask.getNextRunInfo();
                    }
                }
            });
        }
        taskColumnController.addColumn("Description", "Task description", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                return CommonUtils.notEmpty(task.getDescription());
            }
        });
        taskColumnController.addColumn("Type", "Task type", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                return task.getType().getName();
            }
        });
        taskColumnController.addColumn("Category", "Task category", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                return task.getType().getCategory().getName();
            }
        });
        taskColumnController.addColumn("Project", "Task container project", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(DBTTask task) {
                return task.getProject().getName();
            }
        });
        taskColumnController.createColumns(true);

        taskViewer.setContentProvider(new TreeListContentProvider());

        MenuManager menuMgr = createTaskContextMenu(taskViewer);
        getSite().registerContextMenu(TASKS_VIEW_MENU_ID, menuMgr, taskViewer);
        getSite().setSelectionProvider(filteredTree.getViewer());

        taskViewer.addDoubleClickListener(event -> ActionUtils.runCommand(EDIT_TASK_CMD_ID, getSite().getSelectionProvider().getSelection(), getSite()));
        taskViewer.addSelectionChangedListener(event -> loadTaskRuns());
        //viewer.addOpenListener(event -> openCurrentTask());
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
                cell.setText(dateFormat.format(taskRun.getStartTime()));
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

        taskRunViewer.setContentProvider(new TreeListContentProvider());

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
            manager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.EDIT_DELETE, "Delete task", null));
            manager.add(new Separator());
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            taskColumnController.fillConfigMenu(manager);
        });

        Control control = viewer.getControl();
        control.setMenu(menuMgr.createContextMenu(control));

        return menuMgr;
    }

    private MenuManager createTaskRunContextMenu(TreeViewer viewer) {
        final MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            DBTTask task = getSelectedTask();
            DBTTaskRun taskRun = getSelectedTaskRun();
            if (task != null && taskRun != null) {
                manager.add(new ViewRunLogAction());
            }
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            manager.add(new Separator());
            taskRunColumnController.fillConfigMenu(manager);
        });

        Control control = viewer.getControl();
        control.setMenu(menuMgr.createContextMenu(control));

        return menuMgr;
    }

    @Nullable
    private DBTTask getSelectedTask() {
        ISelection selection = taskViewer.getSelection();
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            return null;
        }
        Object element = ((IStructuredSelection) selection).getFirstElement();
        return element instanceof DBTTask ? (DBTTask) element : null;
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
    public void setFocus() {
        taskViewer.getControl().setFocus();
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
                    allTasks.add(task);
                    taskViewer.add(taskViewer.getInput(), task);
                    break;
                case TASK_REMOVE:
                    allTasks.remove(task);
                    taskViewer.remove(task);
                    break;
                case TASK_UPDATE:
                    taskViewer.refresh(task);
                    if (task == getSelectedTask()) {
                        loadTaskRuns();
                    }
                    break;
                case TASK_EXECUTE:
                    refresh();
                    break;
            }
        });
    }

    public void refresh() {
        refreshTasks();
        refreshScheduledTasks();
        taskViewer.refresh(true);

        loadTaskRuns();
    }

    private void loadTasks() {
        refreshTasks();
        refreshScheduledTasks();

        taskViewer.setInput(allTasks);
        taskColumnController.repackColumns();
    }

    private void refreshTasks() {
        allTasks.clear();

        List<DBPProject> projectsWithTasks = new ArrayList<>();
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            DBTTaskManager taskManager = project.getTaskManager();
            DBTTask[] tasks = taskManager.getAllTasks();
            if (tasks.length == 0) {
                continue;
            }
            projectsWithTasks.add(project);
            Collections.addAll(allTasks, tasks);
        }
        allTasks.sort(Comparator.comparing(DBTTask::getCreateTime));
    }

    private void refreshScheduledTasks() {
        DBTScheduler scheduler = TaskRegistry.getInstance().getActiveSchedulerInstance();
        if (scheduler != null) {
            try {
                UIUtils.runInProgressService(monitor -> {
                    try {
                        scheduler.refreshScheduledTasks(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError("Scheduled tasks", "Error reading scheduled tasks", e);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void loadTaskRuns() {
        DBTTask selectedTask = getSelectedTask();
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

    private abstract class TaskLabelProvider extends ColumnLabelProvider {
        @Override
        public final void update(ViewerCell cell) {
            DBTTask task = (DBTTask) cell.getElement();
            DBTTaskRun lastRun = task.getLastRun();
            if (lastRun != null && !lastRun.isRunSuccess()) {
                cell.setBackground(colorError);
            } else {
                cell.setBackground(null);
            }
            cell.setText(getCellText(task));
            DBPImage cellImage = getCellImage(task);
            if (cellImage != null) {
                cell.setImage(DBeaverIcons.getImage(cellImage));
            }
        }

        protected DBPImage getCellImage(DBTTask task) {
            return null;
        }

        protected abstract String getCellText(DBTTask task);

        @Override
        public String getText(Object element) {
            return getCellText((DBTTask) element);
        }
    }

    private abstract class TaskRunLabelProvider extends ColumnLabelProvider {
        @Override
        public final void update(ViewerCell cell) {
            DBTTaskRun taskRun = (DBTTaskRun) cell.getElement();
            if (taskRun != null && !taskRun.isRunSuccess()) {
                cell.setBackground(colorError);
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
            DBTTask task = getSelectedTask();
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
}
