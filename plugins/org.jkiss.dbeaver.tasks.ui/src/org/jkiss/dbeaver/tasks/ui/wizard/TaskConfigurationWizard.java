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
package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.views.IViewDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskConstants;
import org.jkiss.dbeaver.registry.task.TaskImpl;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.tasks.ui.registry.TaskUIRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseWizard;
import org.jkiss.dbeaver.ui.dialogs.DurationPickerDialog;
import org.jkiss.dbeaver.ui.dialogs.IWizardPageActive;
import org.jkiss.dbeaver.ui.dialogs.IWizardPageNavigable;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class TaskConfigurationWizard<SETTINGS extends DBTTaskSettings> extends BaseWizard implements IWorkbenchWizard {

    private static final Log log = Log.getLog(TaskConfigurationWizard.class);

    private static final String TASKS_VIEW_ID = "org.jkiss.dbeaver.tasks";

    private DBTTask currentTask;
    private IStructuredSelection currentSelection;
    private Button saveAsTaskButton;

    private Map<String, Object> variables;
    private boolean promptVariables;
    private DBTTaskContext taskContext;
    @Nullable private DBTTaskFolder currentSelectedTaskFolder;

    protected TaskConfigurationWizard() {
    }

    protected TaskConfigurationWizard(@Nullable DBTTask task) {
        this.currentTask = task;
    }

    protected void initializeWizard(Composite parent) {

    }

    protected abstract SETTINGS getSettings();

    protected abstract String getDefaultWindowTitle();

    public boolean isTaskEditor() {
        return currentTask != null;
    }

    public abstract String getTaskTypeId();

    public abstract void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) throws DBException;

    public boolean isRunTaskOnFinish() {
        return getCurrentTask() != null && !getCurrentTask().isTemporary() && !getContainer().isSelectorMode();
    }
    
    protected boolean isToolTask() {
        return getCurrentTask() != null &&
            getCurrentTask().getProperties().getOrDefault(TaskConstants.TOOL_TASK_PROP, false).equals(true);
    }

    public IStructuredSelection getCurrentSelection() {
        return currentSelection;
    }

    @Nullable
    public DBTTask getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(DBTTask currentTask) {
        this.currentTask = currentTask;
        updateWizardTitle();
        getContainer().updateButtons();
    }

    @Nullable
    public DBTTaskFolder getCurrentSelectedTaskFolder() {
        return currentSelectedTaskFolder;
    }

    public void setCurrentSelectedTaskFolder(@Nullable DBTTaskFolder taskFolder) {
        this.currentSelectedTaskFolder = taskFolder;
    }

    public DBPProject getProject() {
        return currentTask != null ? currentTask.getProject() : NavigatorUtils.getSelectedProject();
    }

    public DBTTaskType getTaskType() {
        return TaskRegistry.getInstance().getTaskType(getTaskTypeId());
    }

    protected void updateWizardTitle() {
        String wizTitle = getDefaultWindowTitle();
        if (isTaskEditor()) {
            TaskConfigurationWizardPageTask taskPage = getContainer() == null ? null : getContainer().getTaskPage();
            wizTitle += " - [" + (taskPage == null ? currentTask.getName() : taskPage.getTaskName()) + "]";
        }
        setWindowTitle(wizTitle);
    }

    @Override
    public TaskConfigurationWizardDialog getContainer() {
        return (TaskConfigurationWizardDialog) super.getContainer();
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        updateWizardTitle();
        setNeedsProgressMonitor(true);
        this.currentSelection = currentSelection;
        getContainer().addPageChangedListener(event -> updateWizardTitle());
    }

    @Override
    public void addPages() {
        super.addPages();
        addTaskConfigPages();
    }

    protected boolean isTaskConfigPage(IWizardPage page) {
        return page instanceof TaskConfigurationWizardPageTask || page instanceof TaskConfigurationWizardPageSettings;
    }

    protected void addTaskConfigPages() {
        // If we are in task edit mode then add special first page.
        // Do not add it if this is an ew task wizard (because this page is added separately)
        if (isTaskEditor() && !currentTask.isTemporary()) {
            // Task editor. Add first page
            addPage(new TaskConfigurationWizardPageTask(getCurrentTask()));
            addPage(new TaskConfigurationWizardPageSettings(getCurrentTask()));
        }
    }

    public boolean isNewTaskEditor() {
        return currentTask != null && getProject().getTaskManager().getTaskById(currentTask.getId()) == null;
    }

    public boolean isCurrentTaskSaved() {
        return getCurrentTask() != null && getCurrentTask().getProject().getTaskManager().getTaskById(getCurrentTask().getId()) != null;
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        IWizardPage nextPage = super.getNextPage(page);
        if (nextPage instanceof TaskConfigurationWizardPageSettings &&
            page instanceof TaskConfigurationWizardPageTask &&
            !TaskUIRegistry.getInstance().supportsConfiguratorPage(getContainer().getTaskPage().getSelectedTaskType()))
        {
            // Skip settings page (not supported by task type)
            return getNextPage(nextPage);
        }
        return nextPage;
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        IWizardPage prevPage = super.getPreviousPage(page);
        if (prevPage instanceof TaskConfigurationWizardPageSettings &&
            !TaskUIRegistry.getInstance().supportsConfiguratorPage(getContainer().getTaskPage().getSelectedTaskType()))
        {
            // Skip settings page (not supported by task type)
            return getPreviousPage(prevPage);
        }
        return prevPage;
    }

    @Override
    public boolean canFinish() {
        if (isCurrentTaskSaved()) {
            return true;
        }
        for (IWizardPage page : getPages()) {
            if (isPageNeedsCompletion(page) && isPageValid(page) && !page.isPageComplete()) {
                return false;
            }
        }
        TaskConfigurationWizardPageTask taskPage = getContainer().getTaskPage();
        if (taskPage != null && !taskPage.isPageComplete()) {
            return false;
        }

        return true;
    }

    protected boolean isPageNeedsCompletion(IWizardPage page) {
        if (page instanceof TaskConfigurationWizardPageTask) {
            return false;
        }
        if (page instanceof IWizardPageNavigable && !((IWizardPageNavigable) page).isPageApplicable()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean performFinish() {
        if (currentTask != null && !currentTask.isTemporary()) {
            if (!saveTask()) {
                return false;
            }
        }

        if (isRunTaskOnFinish()) {
            return runTask();
        }

        return true;
    }

    protected boolean runTask() {
        try {
            DBTTask task = getCurrentTask();
            if (task == null) {
                task = getProject().getTaskManager().createTemporaryTask(getTaskType(), getWindowTitle());
                saveConfigurationToTask(task);
            }
            // Run task thru task manager
            // Pass executor to visualize task progress in UI
            TaskWizardExecutor executor = new TaskWizardExecutor(getRunnableContext(), task, log, System.out);
            if (getCurrentTask() == null) {
                // Execute directly in wizard
                executor.executeTask();
            } else {
                task.getProject().getTaskManager().scheduleTask(task, executor);
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Task run error", e.getMessage(), e);
            return false;
        }
        return true;
    }

    protected boolean isPageValid(IWizardPage page) {
        return true;
    }

    private boolean saveTask() {
        IWizardPage currentPage = getContainer().getCurrentPage();
        // Save current page settings
        if (currentPage instanceof IWizardPageActive) {
            ((IWizardPageActive) currentPage).deactivatePage();
            ((IWizardPageActive) currentPage).activatePage();
        }
        // Save task
        DBTTask currentTask = getCurrentTask();
        if (currentTask == null || currentTask.isTemporary()) {
            // Create new task
            DBTTaskType taskType = getTaskType();
            if (taskType == null) {
                DBWorkbench.getPlatformUI().showError("No task type", "Can't find task type " + getTaskTypeId());
                return false;
            }
            EditTaskConfigurationDialog dialog = new EditTaskConfigurationDialog(getContainer().getShell(), getProject(), taskType);
            if (dialog.open() == IDialogConstants.OK_ID) {
                setCurrentTask(currentTask = dialog.getTask());
            } else {
                return false;
            }
        } else {
            TaskConfigurationWizardPageTask taskPage = getContainer().getTaskPage();
            if (taskPage != null) {
                taskPage.saveSettings();
            }
        }
        DBTTask theTask = currentTask;
        return saveConfigurationToTask(theTask);
    }

    protected boolean saveConfigurationToTask(DBTTask theTask) {
        try {
            Map<String, Object> state = new LinkedHashMap<>();
            saveTaskState(getRunnableContext(), theTask, state);

            DBTTaskContext context = getTaskContext();
            if (context != null) {
                DBTaskUtils.saveTaskContext(state, context);
            }
            if (theTask.getType().supportsVariables()) {
                DBTaskUtils.setVariables(state, getTaskVariables());
                if (promptVariables) {
                    state.put(DBTaskUtils.TASK_PROMPT_VARIABLES, true);
                }
            }
            theTask.setProperties(state);

            theTask.getProject().getTaskManager().updateTaskConfiguration(theTask);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Task save error", "Error saving task configuration", e);
            return false;
        }
        return true;
    }

    public void createTaskSaveButtons(Composite parent, int hSpan) {
        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER)) {
            return;
        }

        IViewDescriptor tasksViewDescriptor = PlatformUI.getWorkbench().getViewRegistry().find(TASKS_VIEW_ID);
        if (tasksViewDescriptor == null || getContainer().isSelectorMode()) {
            // Do not create save buttons
            UIUtils.createEmptyLabel(parent, hSpan, 1);
        } else {
            Composite panel = new Composite(parent, SWT.NONE);
            panel.setBackground(parent.getBackground());
            if (parent.getLayout() instanceof GridLayout) {
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = hSpan;
                panel.setLayoutData(gd);
            }
            boolean supportsVariables = false;//getTaskType().supportsVariables();
            GridLayout layout = new GridLayout(1, false);
            panel.setLayout(layout);

            if (supportsVariables) {
                layout.numColumns++;
                UIUtils.createDialogButton(panel, TaskUIMessages.task_config_wizard_button_variables + " ...", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        configureVariables();
                    }
                });
            }

            saveAsTaskButton = UIUtils.createDialogButton(panel, TaskUIMessages.task_config_wizard_button_save_task, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    saveTask();
                }
            });

            IViewDescriptor taskViewDescriptor = PlatformUI.getWorkbench().getViewRegistry().find(TASKS_VIEW_ID);
            if (taskViewDescriptor != null) {
                layout.numColumns++;

                Button taskViewButton = UIUtils.createDialogButton(
                    panel,
                    null,
                    null,
                    TaskUIMessages.task_config_wizard_link_open_tasks_view,
                    SelectionListener.widgetSelectedAdapter(e -> {
                        try {
                            UIUtils.getActiveWorkbenchWindow().getActivePage().showView(tasksViewDescriptor.getId());
                        } catch (PartInitException e1) {
                            DBWorkbench.getPlatformUI().showError("Show view", "Error opening database tasks view", e1);
                        }
                    })
                );
                Image viewImage = taskViewDescriptor.getImageDescriptor().createImage();
                taskViewButton.setImage(viewImage);
                taskViewButton.addDisposeListener(e -> viewImage.dispose());
                taskViewButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            }

            layout.numColumns++;
            createTimeoutConfigurationButton(panel);
        }
    }

    public void createVariablesEditButton(Composite parent) {
        final Group group = UIUtils.createControlGroup(
            parent,
            TaskUIMessages.task_config_wizard_button_variables,
            1,
            GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING,
            0
        );
        UIUtils.createDialogButton(group, TaskUIMessages.task_config_wizard_button_variables_configure, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                configureVariables();
            }
        });
        final Button promptTaskVariablesCheckbox = UIUtils.createCheckbox(
            group,
            TaskUIMessages.task_config_wizard_button_variables_prompt,
            TaskUIMessages.task_config_wizard_button_variables_prompt_tip,
            currentTask != null && CommonUtils.toBoolean(currentTask.getProperties().get(DBTaskUtils.TASK_PROMPT_VARIABLES)),
            1
        );
        promptTaskVariablesCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                promptVariables = promptTaskVariablesCheckbox.getSelection();
            }
        });
        promptTaskVariablesCheckbox.notifyListeners(SWT.Selection, new Event());
    }

    private void createTimeoutConfigurationButton(@NotNull Composite parent) {
        class CustomTimeoutAction extends Action {
            public CustomTimeoutAction() {
                super("Customize ...", AS_PUSH_BUTTON);
            }

            @Override
            public void run() {
                if (!(getCurrentTask() instanceof TaskImpl task)) {
                    return;
                }
                DurationPickerDialog dialog = new DurationPickerDialog(
                    getShell(),
                    "Specify custom duration",
                    task.getMaxExecutionTime()
                );
                if (dialog.open() == IDialogConstants.OK_ID) {
                    task.setMaxExecutionTime(dialog.getDuration());
                }
            }
        }

        class PresetTimeoutAction extends Action {
            private final Duration duration;

            public PresetTimeoutAction(int index, @NotNull Duration duration) {
                super(ActionUtils.getLabelWithIndexMnemonic(RuntimeUtils.formatDuration(duration), index), AS_RADIO_BUTTON);
                this.duration = duration;
            }

            @Override
            public void run() {
                if (!(getCurrentTask() instanceof TaskImpl task)) {
                    return;
                }
                task.setMaxExecutionTime(duration);
            }

            @Override
            public boolean isChecked() {
                return getCurrentTask() instanceof TaskImpl task && task.getMaxExecutionTime().equals(duration);
            }
        }

        class DisableTimeoutAction extends Action {
            public DisableTimeoutAction() {
                super("Disable timeout");
            }

            @Override
            public void run() {
                if (!(getCurrentTask() instanceof TaskImpl task)) {
                    return;
                }
                task.setMaxExecutionTime(Duration.ZERO);
            }
        }

        MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(m -> {
            m.add(new CustomTimeoutAction());
            m.add(new Separator());

            List<Duration> presets = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofHours(1));
            for (int i = 0; i < presets.size(); i++) {
                m.add(new PresetTimeoutAction(i, presets.get(i)));
            }

            if (getCurrentTask() instanceof TaskImpl task && task.getMaxExecutionTime().isPositive()) {
                Duration duration = task.getMaxExecutionTime();
                if (!presets.contains(duration)) {
                    m.add(new PresetTimeoutAction(presets.size(), duration));
                }

                m.add(new Separator());
                m.add(new DisableTimeoutAction());
            }
        });

        Button executionTimeoutButton = UIUtils.createDialogButton(
            parent,
            null,
            "Execution timeout",
            UIIcon.CLOCK_START,
            SelectionListener.widgetSelectedAdapter(e -> {
                Button button = (Button) e.widget;
                Point location = button.toDisplay(0, 0);
                location.y += button.getSize().y;

                Menu menu = manager.createContextMenu(button);
                menu.setLocation(location);
                menu.setVisible(true);
            })
        );
        executionTimeoutButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        executionTimeoutButton.addDisposeListener(e -> manager.dispose());
    }

    private void configureVariables() {
        Map<String, Object> variables = getTaskVariables();
        EditTaskVariablesDialog dialog = new EditTaskVariablesDialog(getContainer().getShell(), Map.of(currentTask, variables));
        if (dialog.open() == IDialogConstants.OK_ID) {
            this.variables = dialog.getVariables(currentTask);
        }
    }

    protected Map<String, Object> getTaskVariables() {
        if (variables == null) {
            if (currentTask != null) {
                variables = DBTaskUtils.getVariables(currentTask);
            } else {
                variables = new LinkedHashMap<>();
            }
        }
        return variables;
    }

    public DBTTaskContext getTaskContext() {
        if (taskContext == null) {
            if (currentTask != null) {
                taskContext = DBTaskUtils.loadTaskContext(currentTask.getProperties());
            }
        }
        return taskContext;
    }

    protected void saveTaskContext(DBCExecutionContext executionContext) {
        taskContext = DBTaskUtils.extractContext(executionContext);
    }

    void updateSaveTaskButton(boolean enable) {
        if (saveAsTaskButton != null) {
            saveAsTaskButton.setEnabled(enable);
        }
        if (enable) {
            updateSaveTaskButtons();
        }
    }

    public void updateSaveTaskButtons() {
        if (saveAsTaskButton != null) {
            saveAsTaskButton.setEnabled(canFinish() && getTaskType() != null);
        }
    }

    @Override
    public IWizardPage getStartingPage() {
        IWizardPage startingPage = super.getStartingPage();
        if (getContainer().isEditMode()) {
            // Start from second page for task editor
            return getNextPage(startingPage);
        }
        return startingPage;
    }

    public void onWizardActivation() {

    }

    @NotNull
    public TaskConfigurationWizardDialog createWizardDialog(@NotNull IWorkbenchWindow window, @Nullable IStructuredSelection selection) {
        return new TaskConfigurationWizardDialog(window, this, selection);
    }
}