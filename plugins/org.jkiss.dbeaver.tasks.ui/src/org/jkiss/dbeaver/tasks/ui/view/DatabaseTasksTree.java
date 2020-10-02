/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.PatternFilter;
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
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DatabaseTasksTree {
    private static final Log log = Log.getLog(DatabaseTasksTree.class);

    private TreeViewer taskViewer;
    private ViewerColumnController taskColumnController;

    private final List<DBTTask> allTasks = new ArrayList<>();

    private boolean groupByProject = false;
    private boolean groupByType = false;
    private boolean groupByCategory = false;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$
    private final Color colorError, colorErrorForeground;

    public DatabaseTasksTree(Composite composite, boolean selector) {
        ColorRegistry colorRegistry = UIUtils.getActiveWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
        colorError = colorRegistry.get("org.jkiss.dbeaver.txn.color.reverted.background");
        colorErrorForeground = UIUtils.getContrastColor(colorError);
        composite.addDisposeListener(e -> colorErrorForeground.dispose());

        taskViewer = DialogUtils.createFilteredTree(composite,
            SWT.MULTI | SWT.FULL_SELECTION | (selector ? SWT.BORDER | SWT.CHECK : SWT.NONE),
            new NamedObjectPatternFilter(), TaskUIMessages.db_tasks_tree_text_tasks_type);
        Tree taskTree = taskViewer.getTree();
        taskTree.setHeaderVisible(true);
        taskTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        taskColumnController = new ViewerColumnController(TaskUIMessages.db_tasks_tree_column_controller_tasks, taskViewer);
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name, TaskUIMessages.db_tasks_tree_column_controller_add_descr_name, SWT.LEFT, true, true, new TaskLabelProvider() {
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
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_created, TaskUIMessages.db_tasks_tree_column_controller_add_descr_create_time, SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return dateFormat.format(((DBTTask) element).getCreateTime());
                } else {
                    return null;
                }
            }
        });
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_last_run, TaskUIMessages.db_tasks_tree_column_controller_add_descr_start_time, SWT.LEFT, true, false, new TaskLabelProvider() {
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
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_last_duration, TaskUIMessages.db_tasks_tree_column_controller_add_descr_run_duration, SWT.LEFT, false, false, new TaskLabelProvider() {
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
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_last_result, TaskUIMessages.db_tasks_tree_column_controller_add_descr_last_result, SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    DBTTaskRun lastRun = ((DBTTask) element).getLastRun();
                    if (lastRun == null) {
                        return "N/A";
                    } else {
                        if (lastRun.isRunSuccess()) {
                            return TaskUIMessages.db_tasks_tree_column_cell_text_success;
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
            taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_next_run, TaskUIMessages.db_tasks_tree_column_controller_add_descr_next_run, SWT.LEFT, true, false, new TaskLabelProvider() {
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
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_description, TaskUIMessages.db_tasks_tree_column_controller_add_descr_task_description, SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return CommonUtils.notEmpty(((DBTTask) element).getDescription());
                }
                return null;
            }
        });
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_type, TaskUIMessages.db_tasks_tree_column_controller_add_descr_task_type, SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return ((DBTTask) element).getType().getName();
                }
                return null;
            }
        });
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_category, TaskUIMessages.db_tasks_tree_column_controller_add_descr_category, SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected String getCellText(Object element) {
                if (element instanceof DBTTask) {
                    return ((DBTTask) element).getType().getCategory().getName();
                }
                return null;
            }
        });
        taskColumnController.addColumn(TaskUIMessages.db_tasks_tree_column_controller_add_name_project, TaskUIMessages.db_tasks_tree_column_controller_add_descr_project, SWT.LEFT, true, false, new TaskLabelProvider() {
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

    public void loadViewConfig() {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        groupByProject = preferenceStore.getBoolean("dbeaver.tasks.view.groupByProject");
        groupByCategory = preferenceStore.getBoolean("dbeaver.tasks.view.groupByCategory");
        groupByType = preferenceStore.getBoolean("dbeaver.tasks.view.groupByType");

    }

    public void saveViewConfig() {
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
        regroupTasks(false);
        //taskViewer.refresh(true);
        if (refreshScheduledTasks()) {
            taskViewer.refresh(true);
        }
    }

    public void loadTasks() {
        refreshTasks();
        refreshScheduledTasks();

        regroupTasks(true);
    }

    public void regroupTasks(boolean expandAll) {
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
            if (expandAll) {
                taskViewer.expandAll();
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
        allTasks.sort(this::comparTasksTime);
    }

    private int comparTasksTime(DBTTask o1, DBTTask o2) {
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

    public static void addDragSourceSupport(Viewer viewer, IFilter draggableChecker)
    {
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

}
