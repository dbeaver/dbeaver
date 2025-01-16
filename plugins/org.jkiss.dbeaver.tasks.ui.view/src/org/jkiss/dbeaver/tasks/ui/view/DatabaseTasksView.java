/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.IViewDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.TaskFeatures;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIViewMessages;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class DatabaseTasksView extends ViewPart implements DBTTaskListener {
    private static final Log log = Log.getLog(DatabaseTasksView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.tasks";
    private static final String TASKS_VIEW_MENU_ID = VIEW_ID + ".menu";
    private static final String TASK_RUNS_MENU_ID = VIEW_ID + ".runs.menu";

    public static final String CREATE_TASK_CMD_ID = "org.jkiss.dbeaver.task.create";
    public static final String COPY_TASK_CMD_ID = "org.jkiss.dbeaver.task.copy";
    public static final String EDIT_TASK_CMD_ID = "org.jkiss.dbeaver.task.edit";
    public static final String RUN_TASK_CMD_ID = "org.jkiss.dbeaver.task.run";
    private static final String CREATE_FOLDER_TASK_CMD_ID = "org.jkiss.dbeaver.folder.task.create";
    private static final String CREATE_FOLDER_RENAME_CMD_ID = "org.jkiss.dbeaver.folder.rename";
    public static final String GROUP_TASK_CMD_ID = "org.jkiss.dbeaver.task.group";

    private static final ArrayList<Object> EMPTY_TASK_RUN_LIST = new ArrayList<>();

    private DatabaseTasksTree tasksTree;

    private TreeViewer taskRunViewer;
    private ViewerColumnController<?,?> taskRunColumnController;
    private DBPProjectListener projectListener;
    private transient DBTTask currentTask;

    public DatabaseTasksView() {
    }

    @Nullable
    public DatabaseTasksTree getTasksTree() {
        return tasksTree;
    }

    public TreeViewer getTaskRunViewer() {
        return taskRunViewer;
    }

    @Override
    public void createPartControl(Composite parent) {
        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER)) {
            log.debug("The user needs more permissions to see the Database Tasks View.");
            return;
        }
        SashForm sashForm = UIUtils.createPartDivider(this, parent, SWT.HORIZONTAL);

        createTaskTree(sashForm);
        createTaskRunTable(sashForm);
        getSite().setSelectionProvider(tasksTree.getViewer());

        sashForm.setWeights(700, 300);

        loadViewConfig();
        loadTasks();
        updateViewTitle();

        projectListener = new DBPProjectListener() {
            @Override
            public void handleActiveProjectChange(@NotNull DBPProject oldValue, @NotNull DBPProject newValue) {
                refresh();
            }
        };
        DBPPlatformDesktop.getInstance().getWorkspace().addProjectListener(projectListener);

        TaskFeatures.TASKS_VIEW_OPEN.use();
    }

    private void createTaskTree(Composite composite) {
        tasksTree = new DatabaseTasksTree(composite, false);

        MenuManager menuMgr = createTaskContextMenu(tasksTree.getViewer());
        getSite().registerContextMenu(TASKS_VIEW_MENU_ID, menuMgr, tasksTree.getViewer());
        getSite().setSelectionProvider(tasksTree.getViewer());

        tasksTree.getViewer().addDoubleClickListener(event -> {
            if (ActionUtils.isCommandEnabled(EDIT_TASK_CMD_ID, getSite())) {
                ActionUtils.runCommand(EDIT_TASK_CMD_ID, getSite().getSelectionProvider().getSelection(), getSite());
            }
        });
        tasksTree.getViewer().addSelectionChangedListener(event -> UIUtils.asyncExec(() -> loadTaskRuns(false)));
    }

    private void createTaskRunTable(Composite parent) {
        taskRunViewer = DialogUtils.createFilteredTree(parent, SWT.SINGLE | SWT.FULL_SELECTION, new NamedObjectPatternFilter(), TaskUIViewMessages.db_tasks_view_filtered_tree_text_error_message);
        Tree taskrunTree = taskRunViewer.getTree();
        taskrunTree.setHeaderVisible(true);
        taskrunTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        taskRunColumnController = new ViewerColumnController<>("taskruns", taskRunViewer);
        taskRunColumnController.addColumn(TaskUIViewMessages.db_tasks_view_column_controller_add_name_time, TaskUIViewMessages.db_tasks_view_column_controller_add_descr_start_time, SWT.LEFT, true, true, new TaskRunLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTaskRun taskRun) {
                cell.setText(tasksTree.getDateFormat().format(taskRun.getStartTime()));
            }
        });
        taskRunColumnController.addColumn(TaskUIViewMessages.db_tasks_view_column_controller_add_name_duration, TaskUIViewMessages.db_tasks_view_column_controller_add_descr_task_duration, SWT.LEFT, true, false, true, null, new TaskRunLabelProviderEx() {
            @Override
            public String getText(Object element, boolean forUI) {
                DBTTaskRun taskRun = (DBTTaskRun) element;
                return !taskRun.isFinished() ? "N/A" :
                    (forUI ? RuntimeUtils.formatExecutionTime(taskRun.getRunDuration()) : String.valueOf(taskRun.getRunDuration()));
            }

            @Override
            protected void update(ViewerCell cell, DBTTaskRun taskRun) {
                cell.setText(!taskRun.isFinished() ? "N/A" : RuntimeUtils.formatExecutionTime(taskRun.getRunDuration()));
            }
        }, null);
        taskRunColumnController.addColumn(TaskUIViewMessages.db_tasks_view_column_controller_add_name_result, TaskUIViewMessages.db_tasks_view_column_controller_add_descr_task_result, SWT.LEFT, true, false, new TaskRunLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTaskRun taskRun) {
                String resultMessage =
                    taskRun.isFinished() ?
                        (taskRun.isRunSuccess() ? TaskUIViewMessages.db_tasks_view_cell_text_success : CommonUtils.notEmpty(taskRun.getErrorMessage())) :
                        "In progress";

                String extraMessage = taskRun.getExtraMessage();
                if (CommonUtils.isNotEmpty(extraMessage)) {
                    resultMessage += " (" + extraMessage + ")";
                }

                cell.setText(resultMessage);
            }
        });
        taskRunColumnController.setForceAutoSize(true);
        taskRunColumnController.createColumns(true);

        taskRunViewer.setContentProvider(new TreeRunContentProvider());

        MenuManager menuMgr = createTaskRunContextMenu(taskRunViewer);
        getSite().registerContextMenu(DatabaseTasksView.TASK_RUNS_MENU_ID, menuMgr, taskRunViewer);

        taskRunViewer.addDoubleClickListener(event -> new ViewRunLogAction().run());
    }

    private MenuManager createTaskContextMenu(TreeViewer viewer) {
        final MenuManager menuMgr = new MenuManager(null, TASKS_VIEW_MENU_ID);
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            boolean isVisible = true;
            DBTTask selectedTask = tasksTree.getSelectedTask();
            if (selectedTask != null) {
                isVisible = selectedTask.getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
            }
            manager.add(ActionUtils.makeCommandContribution(getSite(), RUN_TASK_CMD_ID));
            if (isVisible) {
                manager.add(ActionUtils.makeCommandContribution(getSite(), EDIT_TASK_CMD_ID));
                manager.add(ActionUtils.makeCommandContribution(getSite(), CREATE_TASK_CMD_ID));
            }
            manager.add(ActionUtils.makeCommandContribution(getSite(), COPY_TASK_CMD_ID));
            if (isVisible) {
                manager.add(
                    ActionUtils.makeCommandContribution(
                        getSite(),
                        IWorkbenchCommandConstants.EDIT_DELETE,
                        TaskUIViewMessages.db_tasks_view_context_menu_command_delete_task,
                        null));
                manager.add(ActionUtils.makeCommandContribution(getSite(), CREATE_FOLDER_TASK_CMD_ID));
                manager.add(ActionUtils.makeCommandContribution(getSite(), CREATE_FOLDER_RENAME_CMD_ID));
            }
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
            manager.add(new Action(WorkbenchMessages.Workbench_copy) {
                @Override
                public void run()
                {
                    ClipboardData clipboardData = new ClipboardData();
                    StringBuilder buf = new StringBuilder();
                    DatabaseTasksTree tasksTree = getTasksTree();
                    if (tasksTree == null) {
                        return;
                    }
                    for (TreeItem item : tasksTree.getViewer().getTree().getSelection()) {
                        if (buf.length() > 0) buf.append(GeneralUtils.getDefaultLineSeparator());
                        buf.append(item.getText(0));
                    }
                    clipboardData.addTransfer(TextTransfer.getInstance(), buf.toString());
                    clipboardData.pushToClipboard(tasksTree.getViewer().getTree().getDisplay());
                }
            });

            manager.add(new Separator());
            tasksTree.getColumnController().fillConfigMenu(manager);
        });

        Control control = viewer.getControl();
        control.setMenu(menuMgr.createContextMenu(control));

        return menuMgr;
    }

    private MenuManager createTaskRunContextMenu(TreeViewer viewer) {
        final MenuManager menuMgr = new MenuManager(null, TASK_RUNS_MENU_ID);
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            manager.add(new GroupMarker("start"));
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
        if (tasksTree == null) {
            return;
        }
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
                    return TaskUIViewMessages.db_tasks_view_adapter_label_database_tasks;
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
        DBPPlatformDesktop.getInstance().getWorkspace().removeProjectListener(projectListener);
        TaskRegistry.getInstance().removeTaskListener(this);
        super.dispose();
    }

    @Override
    public void handleTaskEvent(DBTTaskEvent event) {
        UIUtils.asyncExec(() -> {
            DBTTask task = event.getTask();
            switch (event.getAction()) {
                case TASK_ADD -> {
                    refresh();
                    tasksTree.getViewer().setSelection(new StructuredSelection(task), true);
                }
                case TASK_REMOVE -> refresh();
                case TASK_UPDATE -> {
                    tasksTree.getViewer().refresh(task);
                    if (task == tasksTree.getSelectedTask()) {
                        loadTaskRuns(true);
                    }
                }
                case TASK_EXECUTE -> refresh();
            }
        });
    }

    @Override
    public void handleTaskFolderEvent(DBTTaskFolderEvent event) {
        UIUtils.asyncExec(() -> {
            DBTTaskFolder taskFolder = event.getTaskFolder();
            switch (event.getAction()) {
                case TASK_FOLDER_ADD -> {
                    refresh();
                    tasksTree.getViewer().setSelection(new StructuredSelection(taskFolder), true);
                }
                case TASK_FOLDER_UPDATE -> tasksTree.getViewer().refresh(taskFolder);
                case TASK_FOLDER_REMOVE -> refresh();
            }
        });
    }

    private void loadViewConfig() {
        if (tasksTree == null) {
            return;
        }
        tasksTree.loadViewConfig();
    }

    public void refresh() {
        updateViewTitle();

        if (tasksTree != null) {
            tasksTree.refresh();
        }

        loadTaskRuns(true);
    }

    private void updateViewTitle() {
        IViewDescriptor viewDescriptor = PlatformUI.getWorkbench().getViewRegistry().find(VIEW_ID);
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        setPartName(Objects.requireNonNull(viewDescriptor == null ? null : viewDescriptor.getLabel(), "") +
            " - " + Objects.requireNonNull(activeProject == null ? null : activeProject.getName(), ""));
    }

    private void loadTasks() {
        if (tasksTree == null) {
            return;
        }
        tasksTree.loadTasks();
    }

    private void loadTaskRuns(boolean force) {
        if (tasksTree == null) {
            return;
        }
        DBTTask selectedTask = tasksTree.getSelectedTask();
        if (!force && selectedTask == currentTask) {
            return;
        }
        currentTask = selectedTask;
        if (selectedTask == null) {
            taskRunViewer.setInput(EMPTY_TASK_RUN_LIST);
        } else {
            selectedTask.refreshRunStatistics();
            DBTTaskRun[] runs = selectedTask.getAllRuns();
            if (ArrayUtils.isEmpty(runs)) {
                taskRunViewer.setInput(EMPTY_TASK_RUN_LIST);
            } else {
                Arrays.sort(runs, Comparator.comparing(DBTTaskRun::getStartTime).reversed());
                taskRunViewer.setInput(Arrays.asList(runs));
            }
        }
    }

    private static class TreeRunContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            return ((Collection<?>) inputElement).toArray();
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

    private abstract class TaskRunLabelProviderEx extends TaskRunLabelProvider implements ILabelProviderEx {

    }

    private class ViewRunLogAction extends Action {

        ViewRunLogAction() {
            super(TaskUIViewMessages.db_tasks_view_run_log_view);
        }

        @Override
        public void run() {
            DBTTask task = tasksTree.getSelectedTask();
            DBTTaskRun taskRun = getSelectedTaskRun();
            if (task != null && taskRun != null) {
                Path runLog = task.getRunLog(taskRun);
                if (runLog == null) {
                    try {
                        runLog = receiveLogFile(task, taskRun);
                    } catch (InvocationTargetException e) {
                        DBWorkbench.getPlatformUI().showError("Open log error", "Error while retrieving task run log", e.getCause());
                        return;
                    }
                }
                if (Files.exists(runLog)) {
                    try {
                        IEditorPart editorPart = EditorUtils.openExternalFileEditor(runLog.toFile(), getSite().getWorkbenchWindow());
                        // Set UTF8 encoding
                        if (editorPart instanceof ITextEditor) {
                            IDocumentProvider prov = ((ITextEditor) editorPart).getDocumentProvider();
                            if (prov instanceof TextFileDocumentProvider) {
                                ((TextFileDocumentProvider) prov).setEncoding(editorPart.getEditorInput(), StandardCharsets.UTF_8.name());
                                prov.resetDocument(editorPart.getEditorInput());
                            }
                        }
                    } catch (Exception e) {
                        DBWorkbench.getPlatformUI().showError("Open log error", "Error while opening task execution log", e);
                    }
                } else {
                    UIUtils.showMessageBox(getSite().getShell(), "Log file not found", "Can't find log file '" + runLog.toAbsolutePath() + "'", SWT.ICON_ERROR);
                }
            }
        }

        @NotNull
        private Path receiveLogFile(@NotNull DBTTask task, @NotNull DBTTaskRun run) throws InvocationTargetException {
            final Path[] path = {null};

            UIUtils.runInProgressDialog(monitor -> {
                try {
                    monitor.beginTask("Retrieve task run log contents", 1);

                    try (InputStream is = task.getRunLogInputStream(run)) {
                        final Path folder = DBWorkbench.getPlatform().getTempFolder(monitor, "task-runs");
                        final Path file = folder.resolve(run.getId() + ".txt");
                        Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
                        path[0] = file;
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                } finally {
                    monitor.done();
                }
            });

            return Objects.requireNonNull(path[0]);
        }
    }

    private class DeleteRunLogAction extends Action {

        DeleteRunLogAction() {
            super(TaskUIViewMessages.db_tasks_view_run_log_delete, DBeaverIcons.getImageDescriptor(UIIcon.DELETE));
        }

        @Override
        public void run() {
            DBTTask task = tasksTree.getSelectedTask();
            DBTTaskRun taskRun = getSelectedTaskRun();
            if (task != null && taskRun != null &&
                UIUtils.confirmAction(
                	TaskUIViewMessages.db_tasks_view_run_log_confirm_remove,
                    NLS.bind(TaskUIViewMessages.db_tasks_view_run_log_confirm_delete_task, task.getName(), tasksTree.getDateFormat().format(taskRun.getStartTime()))))
            {
                task.removeRun(taskRun);
            }
        }
    }

    private class ClearRunLogAction extends Action {

        ClearRunLogAction() {
            super(TaskUIViewMessages.db_tasks_view_clear_run_log_clear, DBeaverIcons.getImageDescriptor(UIIcon.ERASE));
        }

        @Override
        public void run() {
            DBTTask task = tasksTree.getSelectedTask();
            if (task == null || !UIUtils.confirmAction(TaskUIViewMessages.db_tasks_view_clear_run_log_confirm_clear, NLS.bind(TaskUIViewMessages.db_tasks_view_clear_run_log_confirm_delete_log, task.getName()))) {
                return;
            }
            task.cleanRunStatistics();
        }
    }

    private class OpenRunLogFolderAction extends Action {

        OpenRunLogFolderAction() {
            super(TaskUIViewMessages.db_tasks_view_open_run_log_folder_open);
        }

        @Override
        public void run() {
            DBTTask task = tasksTree.getSelectedTask();
            if (task != null) {
                final Path path = task.getProject().getTaskManager().getStatisticsFolder(task);
                DBWorkbench.getPlatformUI().executeShellProgram(path.toAbsolutePath().toString());
            }
        }
    }

}
