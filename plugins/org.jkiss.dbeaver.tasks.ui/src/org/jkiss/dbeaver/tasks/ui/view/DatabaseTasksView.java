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
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

    private TreeViewer taskViewer;
    private ViewerColumnController taskColumnController;

    private TreeViewer taskRunViewer;
    private ViewerColumnController taskRunColumnController;

    private final List<DBTTask> allTasks = new ArrayList<>();

    private boolean groupByProject = false;
    private boolean groupByType = false;
    private boolean groupByCategory = false;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$
    private Color colorError;

    public DatabaseTasksView() {
    }

    public TreeViewer getTaskViewer() {
        return taskViewer;
    }

    public TreeViewer getTaskRunViewer() {
        return taskRunViewer;
    }

    @Override
    public void createPartControl(Composite parent) {
        ColorRegistry colorRegistry = getSite().getWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

        colorError = colorRegistry.get("org.jkiss.dbeaver.txn.color.reverted.background");

        SashForm sashForm = UIUtils.createPartDivider(this, parent, SWT.HORIZONTAL);

        createTaskTree(sashForm);
        createTaskRunTable(sashForm);

        sashForm.setWeights(new int[]{700, 300});

        loadViewConfig();
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
            protected String getCellText(Object element) {
                if (element instanceof DBPProject) {
                    return ((DBPProject) element).getName();
                } else if (element instanceof DBTTask) {
                    return ((DBTTask) element).getName();
                } else {
                    return element.toString();
                }
            }

            @Override
            protected DBPImage getCellImage(Object element) {
                if (element instanceof DBPProject) {
                    return DBIcon.PROJECT;
                } else if (element instanceof DBTTask) {
                    return ((DBTTask) element).getType().getIcon();
                } else if (element instanceof TaskCategoryNode) {
                    return ((TaskCategoryNode) element).category.getIcon();
                } else if (element instanceof TaskTypeNode) {
                    return ((TaskTypeNode) element).type.getIcon();
                }
                return null;
            }

            @Override
            public String getToolTipText(Object element) {
                if (element instanceof DBTTask) {
                    String description = ((DBTTask) element).getDescription();
                    if (CommonUtils.isEmpty(description)) {
                        description = ((DBTTask) element).getName();
                    }
                    return description;
                }
                return null;
            }
        });
        taskColumnController.addColumn("Created", "Task create time", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return dateFormat.format(((DBTTask) element).getCreateTime());
                } else {
                    return null;
                }
            }
        });
        taskColumnController.addColumn("Last Run", "Task last start time", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    DBTTaskRun lastRun = ((DBTTask) element).getLastRun();
                    if (lastRun == null) {
                        return "N/A";
                    } else {
                        return dateFormat.format(lastRun.getStartTime());
                    }
                }
                return null;
            }
        });
        taskColumnController.addColumn("Last Duration", "Task last run duration", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    DBTTaskRun lastRun = ((DBTTask) element).getLastRun();
                    if (lastRun == null) {
                        return "N/A";
                    } else {
                        return RuntimeUtils.formatExecutionTime(lastRun.getRunDuration());
                    }
                }
                return null;
            }
        });
        taskColumnController.addColumn("Last Result", "Task last result", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    DBTTaskRun lastRun = ((DBTTask) element).getLastRun();
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
                return null;
            }
        });
        DBTScheduler scheduler = TaskRegistry.getInstance().getActiveSchedulerInstance();
        if (scheduler != null) {
            taskColumnController.addColumn("Next Run", "Task next scheduled run", SWT.LEFT, true, false, new TaskLabelProvider() {
                @Override
                protected String getCellText(Object element) {
                    if (element instanceof DBTTask) {
                        DBTTaskScheduleInfo scheduledTask = scheduler.getScheduledTaskInfo((DBTTask) element);
                        if (scheduledTask == null) {
                            return "";
                        } else {
                            return scheduledTask.getNextRunInfo();
                        }
                    }
                    return null;
                }
            });
        }
        taskColumnController.addColumn("Description", "Task description", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return CommonUtils.notEmpty(((DBTTask) element).getDescription());
                }
                return null;
            }
        });
        taskColumnController.addColumn("Type", "Task type", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return ((DBTTask) element).getType().getName();
                }
                return null;
            }
        });
        taskColumnController.addColumn("Category", "Task category", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return ((DBTTask) element).getType().getCategory().getName();
                }
                return null;
            }
        });
        taskColumnController.addColumn("Project", "Task container project", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return ((DBTTask) element).getProject().getName();
                }
                return null;
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
                    refresh();
                    taskViewer.setSelection(new StructuredSelection(task), true);
                    break;
                case TASK_REMOVE:
                    refresh();
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

    public boolean isGroupByProject() {
        return groupByProject;
    }

    public void setGroupByProject(boolean groupByProject) {
        this.groupByProject = groupByProject;
        saveViewConfig();
    }

    public boolean isGroupByType() {
        return groupByType;
    }

    public void setGroupByType(boolean groupByType) {
        this.groupByType = groupByType;
        saveViewConfig();
    }

    public boolean isGroupByCategory() {
        return groupByCategory;
    }

    public void setGroupByCategory(boolean groupByCategory) {
        this.groupByCategory = groupByCategory;
        saveViewConfig();
    }

    private void loadViewConfig() {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        groupByProject = preferenceStore.getBoolean("dbeaver.tasks.view.groupByProject");
        groupByCategory = preferenceStore.getBoolean("dbeaver.tasks.view.groupByCategory");
        groupByType = preferenceStore.getBoolean("dbeaver.tasks.view.groupByType");

    }

    private void saveViewConfig() {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        preferenceStore.setValue("dbeaver.tasks.view.groupByProject", groupByProject);
        preferenceStore.setValue("dbeaver.tasks.view.groupByCategory", groupByCategory);
        preferenceStore.setValue("dbeaver.tasks.view.groupByType", groupByType);
        try {
            preferenceStore.save();
        } catch (IOException e) {
            log.debug(e);
        }
    }

    public void refresh() {
        refreshTasks();
        regroupTasks();
        taskViewer.refresh(true);
        if (refreshScheduledTasks()) {
            taskViewer.refresh(true);
        }

        loadTaskRuns();
    }

    private void loadTasks() {
        refreshTasks();
        refreshScheduledTasks();

        regroupTasks();
    }

    public void regroupTasks() {
        taskViewer.getTree().setRedraw(false);
        try {
            List<Object> rootObjects = new ArrayList<>();
            if (groupByProject) {
                rootObjects.addAll(getTaskProjects(allTasks));
            } else if (groupByCategory) {
                for (DBTTaskCategory category : getTaskCategories(null, null, allTasks)) {
                    rootObjects.add(new TaskCategoryNode(null, null, category));
                }
            } else if (groupByType) {
                for (DBTTaskType type : getTaskTypes(null, null, allTasks)) {
                    rootObjects.add(new TaskTypeNode(null, null, type));
                }
            } else {
                rootObjects.addAll(allTasks);
            }

            taskViewer.setInput(rootObjects);
            taskViewer.expandAll();
            taskColumnController.repackColumns();
        } finally {
            taskViewer.getTree().setRedraw(true);
        }
    }

    private List<DBPProject> getTaskProjects(List<DBTTask> tasks) {
        Set<DBPProject> projects = new LinkedHashSet<>();
        tasks.forEach(task -> projects.add(task.getProject()));
        return new ArrayList<>(projects);
    }

    private static List<DBTTaskCategory> getTaskCategories(DBPProject project, DBTTaskCategory parentCategory, List<DBTTask> tasks) {
        Set<DBTTaskCategory> categories = new LinkedHashSet<>();
        tasks.forEach(task -> {
            if (project == null || project == task.getProject()) {
                DBTTaskCategory category = task.getType().getCategory();
                if (parentCategory == category.getParent()) {
                    categories.add(category);
                }
            }
        });

        return new ArrayList<>(categories);
    }

    private static List<DBTTaskType> getTaskTypes(DBPProject project, DBTTaskCategory category, List<DBTTask> tasks) {
        Set<DBTTaskType> types = new LinkedHashSet<>();
        tasks.forEach(task -> {
            if (project == null || project == task.getProject()) {
                if (category == null || category == task.getType().getCategory()) {
                    types.add(task.getType());
                }
            }
        });
        return new ArrayList<>(types);
    }

    private void refreshTasks() {
        allTasks.clear();

        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            DBTTaskManager taskManager = project.getTaskManager();
            DBTTask[] tasks = taskManager.getAllTasks();
            if (tasks.length == 0) {
                continue;
            }
            Collections.addAll(allTasks, tasks);
        }
        allTasks.sort((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()));
    }

    private boolean refreshScheduledTasks() {
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
                return false;
            } catch (InterruptedException e) {
                // ignore
            }
            return true;
        }
        return false;
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

    private class TreeListContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            return ((Collection) inputElement).toArray();
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            List<Object> children = new ArrayList<>();
            if (parentElement instanceof DBPProject) {
                DBPProject project = (DBPProject) parentElement;
                if (groupByCategory) {
                    for (DBTTaskCategory category : getTaskCategories(project, null, allTasks)) {
                        children.add(new TaskCategoryNode(project, null, category));
                    }
                } else if (groupByType) {
                    for (DBTTaskType type : getTaskTypes(project, null, allTasks)) {
                        children.add(new TaskTypeNode(project, null, type));
                    }
                } else {
                    for (DBTTask task : allTasks) {
                        if (task.getProject() == parentElement) {
                            children.add(task);
                        }
                    }
                }
            } else if (parentElement instanceof TaskCategoryNode) {
                // Child categories
                TaskCategoryNode parentCat = (TaskCategoryNode) parentElement;
                for (DBTTaskCategory childCat : getTaskCategories(parentCat.project, parentCat.category, allTasks)) {
                    children.add(new TaskCategoryNode(parentCat.project, parentCat, childCat));
                }

                if (groupByType) {
                    // Task types
                    for (DBTTaskType type : getTaskTypes(parentCat.project, parentCat.category, allTasks)) {
                        children.add(new TaskTypeNode(parentCat.project, parentCat, type));
                    }
                } else {
                    // Tasks
                    for (DBTTask task : allTasks) {
                        if ((parentCat.project == null || task.getProject() == parentCat.project) && task.getType().getCategory() == parentCat.category) {
                            children.add(task);
                        }
                    }
                }
            } else if (parentElement instanceof TaskTypeNode) {
                // Child tasks
                TaskTypeNode parentType = (TaskTypeNode) parentElement;
                for (DBTTask task : allTasks) {
                    if ((parentType.project == null || task.getProject() == parentType.project) && task.getType() == parentType.type) {
                        children.add(task);
                    }
                }
            } else {

            }

            return children.toArray();
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof TaskTypeNode) {
                if (((TaskTypeNode) element).parent != null) {
                    return ((TaskTypeNode) element).parent;
                } else {
                    return ((TaskTypeNode) element).project;
                }
            } else if (element instanceof TaskCategoryNode) {
                if (((TaskCategoryNode) element).parent != null) {
                    return ((TaskCategoryNode) element).parent;
                } else {
                    return ((TaskCategoryNode) element).project;
                }
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object parentElement) {
            return !(parentElement instanceof DBTTask);
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

    private static class TaskTypeNode {
        final DBPProject project;
        final TaskCategoryNode parent;
        final DBTTaskType type;

        TaskTypeNode(DBPProject project, TaskCategoryNode parent, DBTTaskType type) {
            this.project = project;
            this.parent = parent;
            this.type = type;
        }

        @Override
        public String toString() {
            return type.getName();
        }

        @Override
        public int hashCode() {
            return (project == null ? 0 : project.hashCode()) +
                (parent == null ? 0 : parent.hashCode()) +
                (type == null ? 0 : type.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TaskTypeNode)) {
                return false;
            }
            TaskTypeNode cmp = (TaskTypeNode)obj;
            return project == cmp.project &&
                CommonUtils.equalObjects(parent, cmp.parent) &&
                type == cmp.type;
        }
    }

    private abstract class TaskLabelProvider extends ColumnLabelProvider {
        @Override
        public final void update(ViewerCell cell) {
            Object element = cell.getElement();
            if (element instanceof DBTTask) {
                DBTTaskRun lastRun = ((DBTTask) element).getLastRun();
                if (lastRun != null && !lastRun.isRunSuccess()) {
                    cell.setBackground(colorError);
                } else {
                    cell.setBackground(null);
                }
            }
            cell.setText(CommonUtils.notEmpty(getCellText(element)));
            DBPImage cellImage = getCellImage(element);
            if (cellImage != null) {
                cell.setImage(DBeaverIcons.getImage(cellImage));
            }

        }

        protected DBPImage getCellImage(Object element) {
            return null;
        }

        protected abstract String getCellText(Object element);

        @Override
        public String getText(Object element) {
            return getCellText(element);
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

    private class DeleteRunLogAction extends Action {

        DeleteRunLogAction() {
            super("Delete run log", DBeaverIcons.getImageDescriptor(UIIcon.DELETE));
        }

        @Override
        public void run() {
            DBTTask task = getSelectedTask();
            DBTTaskRun taskRun = getSelectedTaskRun();
            if (task != null && taskRun != null &&
                UIUtils.confirmAction(
                    "Remove task run",
                    "Are you sure you want to delete task '" + task.getName() + "' run at '" + dateFormat.format(taskRun.getStartTime()) + "'?"))
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
            DBTTask task = getSelectedTask();
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
            DBTTask task = getSelectedTask();
            if (task != null) {
                DBWorkbench.getPlatformUI().executeShellProgram(task.getRunLogFolder().getAbsolutePath());
            }
        }
    }

}
