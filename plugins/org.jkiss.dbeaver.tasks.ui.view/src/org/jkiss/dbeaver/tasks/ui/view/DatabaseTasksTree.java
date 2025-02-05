/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskImpl;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.registry.timezone.TimezoneRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIViewMessages;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.DefaultViewerToolTipSupport;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseTasksTree {
    private static final Log log = Log.getLog(DatabaseTasksTree.class);

    private TreeViewer taskViewer;
    private ViewerColumnController<?, ?> taskColumnController;

    private final List<DBTTask> allTasks = new ArrayList<>();
    private final List<DBTTaskFolder> allTasksFolders = new ArrayList<>();

    private boolean groupByType = false;
    private boolean groupByCategory = false;

    private final DateFormat dateFormat;
    private final Color colorError, colorErrorForeground;

    public DatabaseTasksTree(Composite composite, boolean selector) {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$
        dateFormat.setTimeZone(TimeZone.getTimeZone(TimezoneRegistry.getUserDefaultTimezone()));
        colorError = BaseThemeSettings.instance.colorError;
        colorErrorForeground = UIUtils.getContrastColor(colorError);
        
        taskViewer = DialogUtils.createFilteredTree(composite,
            SWT.MULTI | SWT.FULL_SELECTION | (selector ? SWT.BORDER | SWT.CHECK : SWT.NONE),
            new NamedObjectPatternFilter(), TaskUIViewMessages.db_tasks_tree_text_tasks_type);
        Tree taskTree = taskViewer.getTree();
        taskTree.setHeaderVisible(true);
        taskTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        taskColumnController = new ViewerColumnController<>(TaskUIViewMessages.db_tasks_tree_column_controller_tasks, taskViewer);
        taskColumnController.setComparator(new ViewerColumnController.DefaultComparator(Collator.getInstance()) {
            @Override
            public int category(Object element) {
                return element instanceof DBTTaskFolder ? 0 : 1;
            }
        });
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_name, SWT.LEFT, true, true, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBPProject) {
                    return ((DBPProject) element).getName();
                } else if (element instanceof DBTTask) {
                    return ((DBTTask) element).getName();
                } else if (element instanceof DBTTaskFolder) {
                    return ((DBTTaskFolder) element).getName();
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
                } else if (element instanceof DBTTaskFolder) {
                    return DBIcon.TREE_FOLDER;
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
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_created, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_create_time, SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return dateFormat.format(((DBTTask) element).getCreateTime());
                } else {
                    return null;
                }
            }
        });
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_last_run, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_start_time, SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    DBTTaskRun lastRun = ((DBTTask) element).getLastRun();
                    if (lastRun == null || lastRun.getStartTime() == null) {
                        return "N/A";
                    } else {
                        return dateFormat.format(lastRun.getStartTime());
                    }
                }
                return null;
            }
        });
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_last_duration, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_run_duration, SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    DBTTaskRun lastRun = ((DBTTask) element).getLastRun();
                    if (lastRun == null || !lastRun.isFinished()) {
                        return "N/A";
                    } else {
                        return RuntimeUtils.formatExecutionTime(lastRun.getRunDuration());
                    }
                }
                return null;
            }
        });
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_last_result, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_last_result, SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    DBTTaskRun lastRun = ((DBTTask) element).getLastRun();
                    if (lastRun == null) {
                        return "N/A";
                    } else {
                        if (lastRun.isRunSuccess()) {
                            return TaskUIViewMessages.db_tasks_tree_column_cell_text_success;
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
            taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_next_run, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_next_run, SWT.LEFT, true, false, new TaskLabelProvider() {
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
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_description, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_task_description, SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return CommonUtils.notEmpty(((DBTTask) element).getDescription());
                }
                return null;
            }
        });
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_type, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_task_type, SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return ((DBTTask) element).getType().getName();
                }
                return null;
            }
        });
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_category, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_category, SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return ((DBTTask) element).getType().getCategory().getName();
                }
                return null;
            }
        });
        taskColumnController.addColumn(TaskUIViewMessages.db_tasks_tree_column_controller_add_name_project, TaskUIViewMessages.db_tasks_tree_column_controller_add_descr_project, SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return ((DBTTask) element).getProject().getName();
                }
                return null;
            }
        });
        taskColumnController.createColumns(true);
        new DefaultViewerToolTipSupport(taskViewer);

        taskViewer.setContentProvider(new TreeListContentProvider());
    }

    public TreeViewer getViewer() {
        return taskViewer;
    }

    DateFormat getDateFormat() {
        return dateFormat;
    }

    Color getColorError() {
        return colorError;
    }

    ViewerColumnController getColumnController() {
        return taskColumnController;
    }

    @Nullable
    public DBTTask getSelectedTask() {
        ISelection selection = taskViewer.getSelection();
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            return null;
        }
        Object element = ((IStructuredSelection) selection).getFirstElement();
        return element instanceof DBTTask ? (DBTTask) element : null;
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

    public void loadViewConfig() {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        groupByCategory = preferenceStore.getBoolean("dbeaver.tasks.view.groupByCategory");
        groupByType = preferenceStore.getBoolean("dbeaver.tasks.view.groupByType");

    }

    public void saveViewConfig() {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        preferenceStore.setValue("dbeaver.tasks.view.groupByCategory", groupByCategory);
        preferenceStore.setValue("dbeaver.tasks.view.groupByType", groupByType);
        try {
            preferenceStore.save();
        } catch (IOException e) {
            log.debug(e);
        }
    }

    void refresh() {
        refreshTasks();
        regroupTasks(ExpansionOptions.RETAIN);
        refreshScheduledTasks();
    }

    void loadTasks() {
        refreshTasks();
        refreshScheduledTasks();
        regroupTasks(ExpansionOptions.EXPAND_ALL);
    }

    void regroupTasks(ExpansionOptions options) {
        if (taskViewer.isBusy()) {
            return;
        }
        taskViewer.getTree().setRedraw(false);
        try {
            List<Object> rootObjects = new ArrayList<>();
            // Add task folders as parent elements, task from these folders will be added in children list
            if (!CommonUtils.isEmpty(allTasksFolders)) {
                List<DBTTaskFolder> sortedFoldersWithoutParents = allTasksFolders.stream()
                    .filter(e -> e.getParentFolder() == null)
                    .sorted(DBUtils.nameComparatorIgnoreCase())
                    .collect(Collectors.toList());
                rootObjects.addAll(sortedFoldersWithoutParents);
            }

            // Collect all tasks without folders
            List<DBTTask> allTasksWithoutFolders = allTasks.stream()
                .filter(task -> task.getTaskFolder() == null)
                .sorted(DBUtils.nameComparatorIgnoreCase())
                .collect(Collectors.toList());

            // Now we need to distribute all tasks without folders
            if (!CommonUtils.isEmpty(allTasksWithoutFolders)) {
                if (groupByCategory) {
                    for (DBTTaskCategory category : getTaskCategories(null, null, allTasksWithoutFolders)) {
                        rootObjects.add(new TaskCategoryNode(null, null, category, null));
                    }
                } else if (groupByType) {
                    for (DBTTaskType type : getTaskTypes(null, null, allTasksWithoutFolders)) {
                        rootObjects.add(new TaskTypeNode(null, null, type, null));
                    }
                } else {
                    rootObjects.addAll(allTasksWithoutFolders);
                }
            }
            switch (options) {
                case EXPAND_ALL:
                    taskViewer.setInput(rootObjects);
                    taskViewer.expandAll();
                    break;
                case RETAIN:
                    Object[] expandedElements = taskViewer.getExpandedElements();
                    taskViewer.setInput(rootObjects);
                    taskViewer.setExpandedElements(expandedElements);
                    break;
                default:
                    log.warn("Unknown expansion option reached!");
                    break;
            }
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
                for (DBTTaskCategory category = task.getType().getCategory(); category != null; category = category.getParent()) {
                    if (parentCategory == category.getParent()) {
                        categories.add(category);
                    }
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
        List<DBTTaskType> sortedTypes = new ArrayList<>(types);
        sortedTypes.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        return sortedTypes;
    }

    private static List<DBTTaskType> getNotEmptyTaskTypes(DBPProject project, DBTTaskCategory category, List<DBTTask> tasks, DBTTaskFolder taskFolder) {
        List<DBTTaskType> taskTypes = getTaskTypes(project, category, tasks);
        List<DBTTaskType> resultTaskTypes = new ArrayList<>();
        // We need to find all types with tasks for current folder or if no folder at all. We do not need empty types without tasks.
        for (DBTTaskType taskType : taskTypes) {
            if (tasks.stream().anyMatch(task -> task.getType() == taskType && task.getTaskFolder() == taskFolder)) {
                resultTaskTypes.add(taskType);
            }
        }
        return resultTaskTypes;
    }

    private void refreshTasks() {
        allTasks.clear();
        allTasksFolders.clear();

        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        DBTTaskManager taskManager = project.getTaskManager();
        DBTTask[] tasks = taskManager.getAllTasks();
        if (tasks.length != 0) {
            Collections.addAll(allTasks, tasks);
        }
        DBTTaskFolder[] tasksFolders = taskManager.getTasksFolders();
        if (!ArrayUtils.isEmpty(tasksFolders)) {
            Collections.addAll(allTasksFolders, tasksFolders);
        }

        allTasksFolders.sort(Comparator.comparing(DBTTaskFolder::getName));
        allTasks.sort(Comparator.comparing(DBTTask::getName));
    }

    private int compareTasksTime(DBTTask o1, DBTTask o2) {
        DBTTaskRun lr1 = o1.getLastRun();
        DBTTaskRun lr2 = o2.getLastRun();
        if (lr1 == null) {
            if (lr2 == null) return o1.getName().compareToIgnoreCase(o2.getName());
            return 1;
        } else if (lr2 == null) {
            return -1;
        } else {
            return lr2.getStartTime().compareTo(lr1.getStartTime());
        }
    }

    private boolean refreshScheduledTasks() {
        DBTScheduler scheduler = TaskRegistry.getInstance().getActiveSchedulerInstance();
        if (scheduler != null) {
            new AbstractJob("Refresh scheduled tasks") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {

                    try {
                        scheduler.refreshScheduledTasks(monitor);
                    } catch (Exception e) {
                        log.debug(e);
                        return GeneralUtils.makeExceptionStatus("Error reading scheduled tasks", e);
                    }

                    UIUtils.asyncExec(() -> {
                        if (!taskViewer.isBusy()) {
                            taskViewer.refresh(true);
                        }
                    });
                    return Status.OK_STATUS;
                }
            }.schedule();
            return true;
        }
        return false;
    }

    public List<DBTTask> getCheckedTasks() {
        List<DBTTask> tasks = new ArrayList<>();
        for (TreeItem item : taskViewer.getTree().getItems()) {
            addCheckedItem(item, tasks);
        }
        return tasks;
    }

    private void addCheckedItem(TreeItem item, List<DBTTask> tasks) {
        if (item.getChecked()) {
            if (item.getData() instanceof DBTTask) {
                tasks.add((DBTTask) item.getData());
            }
        }
        for (TreeItem child : item.getItems()) {
            addCheckedItem(child, tasks);
        }
    }

    enum ExpansionOptions {
        EXPAND_ALL,
        RETAIN,
    }

    private class TreeListContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            return ((Collection<?>) inputElement).toArray();
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            List<Object> children = new ArrayList<>();
            if (parentElement instanceof DBPProject) {
                DBPProject project = (DBPProject) parentElement;

                // First add all tasks folders belonging to this project without parent folders
                children.addAll(allTasksFolders.stream()
                    .filter(taskFolder -> taskFolder.getProject() == parentElement && taskFolder.getParentFolder() == null)
                    .sorted(DBUtils.nameComparatorIgnoreCase())
                    .collect(Collectors.toList()));

                // Then check all tasks belonging to this project without folder
                List<DBTTask> thisProjectTasksWithoutFolder = allTasks.stream()
                    .filter(task -> task.getTaskFolder() == null && task.getProject() == parentElement)
                    .sorted(DBUtils.nameComparatorIgnoreCase())
                    .collect(Collectors.toList());

                if (!CommonUtils.isEmpty(thisProjectTasksWithoutFolder)) {
                    if (groupByCategory) {
                        for (DBTTaskCategory category : getTaskCategories(project, null, thisProjectTasksWithoutFolder)) {
                            children.add(new TaskCategoryNode(project, null, category, null));
                        }
                    } else if (groupByType) {
                        for (DBTTaskType type : getTaskTypes(project, null, thisProjectTasksWithoutFolder)) {
                            children.add(new TaskTypeNode(project, null, type, null));
                        }
                    } else {
                        children.addAll(thisProjectTasksWithoutFolder);
                    }
                }
            } else if (parentElement instanceof DBTTaskFolder) {
                DBTTaskFolder taskFolder = (DBTTaskFolder) parentElement;
                DBPProject folderProject = null;

                // First add nested task folders to elements list
                List<DBTTaskFolder> nestedTaskFolders = taskFolder.getNestedTaskFolders();
                if (!CommonUtils.isEmpty(nestedTaskFolders)) {
                    children.addAll(new ArrayList<>(nestedTaskFolders));
                }

                List<DBTTask> thisFolderTasks = allTasks.stream()
                    .filter(task -> task.getTaskFolder() == taskFolder)
                    .sorted(DBUtils.nameComparatorIgnoreCase())
                    .collect(Collectors.toList());
                if (groupByCategory) {
                    for (DBTTaskCategory category : getTaskCategories(folderProject, null, thisFolderTasks)) {
                        children.add(new TaskCategoryNode(folderProject, null, category, taskFolder));
                    }
                } else if (groupByType) {
                    for (DBTTaskType type : getTaskTypes(folderProject, null, thisFolderTasks)) {
                        children.add(new TaskTypeNode(folderProject, null, type, taskFolder));
                    }
                } else {
                    children.addAll(thisFolderTasks);
                }
            } else if (parentElement instanceof TaskCategoryNode) {
                // Child categories
                TaskCategoryNode parentCat = (TaskCategoryNode) parentElement;
                for (DBTTaskCategory childCat : getTaskCategories(parentCat.project, parentCat.category, allTasks)) {
                    children.add(new TaskCategoryNode(parentCat.project, parentCat, childCat, parentCat.taskFolder));
                }

                if (groupByType) {
                    // Task types
                    for (DBTTaskType type : getNotEmptyTaskTypes(parentCat.project, parentCat.category, allTasks, parentCat.taskFolder)) {
                        children.add(new TaskTypeNode(parentCat.project, parentCat, type, parentCat.taskFolder));
                    }
                } else {
                    // Tasks
                    List<DBTTask> allTasksThisCategory = getSortedByCategoryTasks(parentCat);
                    allTasksThisCategory.sort(DBUtils.nameComparatorIgnoreCase());
                    if (parentCat.taskFolder != null) {
                        children.addAll(allTasksThisCategory.stream().filter(task -> task.getTaskFolder() == parentCat.taskFolder).collect(Collectors.toList()));
                    } else {
                        children.addAll(allTasksThisCategory.stream().filter(task -> task.getTaskFolder() == null).collect(Collectors.toList()));
                    }
                }
            } else if (parentElement instanceof TaskTypeNode) {
                // Child tasks
                TaskTypeNode parentType = (TaskTypeNode) parentElement;
                for (DBTTask task : allTasks) {
                    DBTTaskFolder taskFolder = task.getTaskFolder();
                    if (isSuitableType(parentType, task) &&
                            ((parentType.taskFolder == null && taskFolder == null) ||
                                    (parentType.taskFolder != null && parentType.taskFolder.equals(taskFolder)))) {
                        children.add(task);
                    }
                }
            }

            return children.toArray();
        }

        private boolean isSuitableType(TaskTypeNode parentType, DBTTask task) {
           return task.getType() == parentType.type && (parentType.project == null || task.getProject() == parentType.project);
        }

        // Sort all tasks into list by task category
        private List<DBTTask> getSortedByCategoryTasks(TaskCategoryNode taskCategory) {
            List<DBTTask> sortedByDescriptorList = new ArrayList<>();
            for (DBTTask task : allTasks) {
                if ((taskCategory.project == null || task.getProject() == taskCategory.project) && (task.getType().getCategory() == taskCategory.category)) {
                    sortedByDescriptorList.add(task);
                }
            }
            return sortedByDescriptorList;
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

    public static class NamedObjectPatternFilter extends PatternFilter {
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
        @Nullable final DBTTaskFolder taskFolder;

        TaskCategoryNode(DBPProject project, TaskCategoryNode parent, DBTTaskCategory category, @Nullable DBTTaskFolder taskFolder) {
            this.project = project;
            this.parent = parent;
            this.category = category;
            this.taskFolder = taskFolder;
        }

        @Override
        public String toString() {
            return category.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TaskCategoryNode)) return false;
            TaskCategoryNode that = (TaskCategoryNode) o;
            return Objects.equals(project, that.project) &&
                Objects.equals(parent, that.parent) &&
                Objects.equals(category, that.category) &&
                Objects.equals(taskFolder, that.taskFolder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(project, parent, category, taskFolder);
        }
    }

    private static class TaskTypeNode {
        final DBPProject project;
        final TaskCategoryNode parent;
        final DBTTaskType type;
        @Nullable final DBTTaskFolder taskFolder;

        TaskTypeNode(DBPProject project, TaskCategoryNode parent, DBTTaskType type, @Nullable DBTTaskFolder taskFolder) {
            this.project = project;
            this.parent = parent;
            this.type = type;
            this.taskFolder = taskFolder;
        }

        @Override
        public String toString() {
            return type.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TaskTypeNode)) return false;
            TaskTypeNode that = (TaskTypeNode) o;
            return Objects.equals(type, that.type) &&
                Objects.equals(project, that.project) &&
                Objects.equals(parent, that.parent) &&
                Objects.equals(taskFolder, that.taskFolder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(project, parent, type, taskFolder);
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
                    cell.setForeground(colorErrorForeground);
                } else {
                    cell.setBackground(null);
                    cell.setForeground(null);
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

    public static void addDragSourceSupport(Viewer viewer, IFilter draggableChecker) {
        Transfer[] types = new Transfer[] {TextTransfer.getInstance(), DatabaseTaskTransfer.getInstance()};
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        final DragSource source = new DragSource(viewer.getControl(), operations);
        source.setTransfer(types);
        source.addDragListener (new DragSourceAdapter() {
            private IStructuredSelection selection;

            @Override
            public void dragStart(DragSourceEvent event) {
                selection = (IStructuredSelection) viewer.getSelection();
            }

            @Override
            public void dragSetData (DragSourceEvent event) {
                if (!selection.isEmpty()) {
                    List<DBTTask> tasks = new ArrayList<>();
                    StringBuilder buf = new StringBuilder();
                    for (Object nextSelected : selection.toArray()) {
                        if (draggableChecker != null && !draggableChecker.select(nextSelected)) {
                            continue;
                        }
                        DBTTask task = null;
                        if (nextSelected instanceof DBTTask) {
                            task  = (DBTTask) nextSelected;
                        } else if (nextSelected instanceof DBTTaskReference) {
                            task = ((DBTTaskReference) nextSelected).getTask();
                        }
                        if (task == null) {
                            continue;
                        }
                        tasks.add(task);
                        String taskName = task.getName();
                        if (buf.length() > 0) {
                            buf.append(", ");
                        }
                        buf.append(taskName);
                    }
                    if (DatabaseTaskTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = new DatabaseTaskTransfer.Data(viewer.getControl(), tasks);
                    } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = buf.toString();
                    }
                } else {
                    if (DatabaseTaskTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = new DatabaseTaskTransfer.Data(viewer.getControl(), Collections.emptyList());
                    } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = "";
                    }
                }
            }
        });

    }

    static void addDragAndDropSourceSupport(Viewer viewer) {
        addDragSourceSupport(viewer, null);

        DropTarget dropTarget = new DropTarget(viewer.getControl(), DND.DROP_MOVE);
        dropTarget.setTransfer(DatabaseTaskTransfer.getInstance());
        dropTarget.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void drop(DropTargetEvent event) {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE) {
                    moveNodes(event);
                }
            }

            @Override
            public void dropAccept(DropTargetEvent event) {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event) {
                event.detail = isDropSupported(event) ? DND.DROP_MOVE : DND.DROP_NONE;
                event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
            }

            private boolean isDropSupported(DropTargetEvent event) {
                if (DatabaseTaskTransfer.getInstance().isSupportedType(event.currentDataType)) {
                    Object curObject;
                    if (event.item instanceof Item) {
                        curObject = event.item.getData();
                    } else {
                        curObject = null;
                    }

                    if (curObject instanceof DBTTask || curObject instanceof DBTTaskFolder) {
                        return true;
                    } else if (curObject instanceof TaskCategoryNode) {
                        return ((TaskCategoryNode) curObject).taskFolder != null;
                    } else if (curObject instanceof TaskTypeNode) {
                        return ((TaskTypeNode) curObject).taskFolder != null;
                    }
                }
                return false;
            }

            private void moveNodes(DropTargetEvent event) {
                Object curObject;
                if (event.item instanceof Item) {
                    curObject = event.item.getData();
                } else {
                    // Do not drop tasks to empty spaces
                    return;
                }

                if (curObject instanceof DBTTask || curObject instanceof DBTTaskFolder || curObject instanceof TaskCategoryNode || curObject instanceof TaskTypeNode) {
                    DBTTaskFolder taskFolder;
                    if (curObject instanceof DBTTask) {
                        taskFolder = ((DBTTask) curObject).getTaskFolder();
                    } else if (curObject instanceof TaskCategoryNode) {
                        taskFolder = ((TaskCategoryNode) curObject).taskFolder;
                    } else if (curObject instanceof TaskTypeNode) {
                        taskFolder = ((TaskTypeNode) curObject).taskFolder;
                    } else {
                        taskFolder = (DBTTaskFolder) curObject;
                    }

                    if (taskFolder == null) {
                        // We do not want unFolder this task/tasks
                        return;
                    }

                    if (event.data instanceof DatabaseTaskTransfer.Data) {
                        List<DBTTask> tasksToDrop = ((DatabaseTaskTransfer.Data) event.data).getTasks();
                        if (!CommonUtils.isEmpty(tasksToDrop)) {
                            for (DBTTask task : tasksToDrop) {
                                if (task instanceof TaskImpl && task.getProject() == taskFolder.getProject()) { // Do not move tasks into another project
                                    ((TaskImpl)task).setTaskFolder(taskFolder);
                                    taskFolder.addTaskToFolder(task);
                                    try {
                                        task.getProject().getTaskManager().updateTaskConfiguration(task);
                                    } catch (DBException e) {
                                        DBWorkbench.getPlatformUI().showError("Task save error", "Error saving task configuration", e);
                                    }
                                }
                            }
                            TaskRegistry.getInstance().notifyTaskFoldersListeners(new DBTTaskFolderEvent(taskFolder, DBTTaskFolderEvent.Action.TASK_FOLDER_REMOVE)); // Refresh all
                        }
                    }
                }
            }

        });
    }
}
