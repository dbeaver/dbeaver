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
package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanel;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.tasks.ui.registry.TaskUIRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

/**
 * Page for task initial settings
 */
class TaskConfigurationWizardPageSettings extends ActiveWizardPage<TaskConfigurationWizard> {
    private static final Log log = Log.getLog(TaskConfigurationWizardPageSettings.class);

    private Composite taskSettingsPlaceholder;
    private DBTTaskType curTaskType;
    private DBTTaskConfigPanel taskConfigPanel;

    TaskConfigurationWizardPageSettings(DBTTask task) {
        super(task == null ? TaskUIMessages.task_config_wizard_page_settings_create_task : TaskUIMessages.task_config_wizard_page_settings_edit_task);

        setPageComplete(true);
    }

    @Override
    public void createControl(Composite parent) {
        taskSettingsPlaceholder = new Composite(parent, SWT.NONE);
        taskSettingsPlaceholder.setLayout(new FillLayout());

        setControl(taskSettingsPlaceholder);
    }

    @Override
    protected boolean determinePageCompletion() {
        if (taskConfigPanel != null && !taskConfigPanel.isComplete()) {
            String errorMessage = taskConfigPanel.getErrorMessage();
            if (errorMessage != null) {
                setErrorMessage(errorMessage);
            } else {
                setErrorMessage(NLS.bind(TaskUIMessages.task_configuration_wizard_page_settings_fill_parameters, curTaskType.getName()));
            }
            return false;
        }
        setErrorMessage(null);
        return true;
    }

    @Override
    public void activatePage() {
        DBTTask currentTask = getWizard().getCurrentTask();
        DBTTaskType selectedTaskType = currentTask != null ?
            currentTask.getType() :
            getTaskPage().getSelectedTaskType();
        if (curTaskType == selectedTaskType) {
            return;
        }
        curTaskType = selectedTaskType;

        createTaskSettingsUI();

        if (curTaskType == null) {
            setTitle(TaskUIMessages.task_config_wizard_page_settings_title_task_prop);
            setDescription(TaskUIMessages.task_config_wizard_page_settings_descr_set_task);
        } else {
            String title = curTaskType.getName();
            if (getWizard().getCurrentTask() != null) {
                title += " (" + getWizard().getCurrentTask().getName() + ")";
            }
            setTitle(title);
            setDescription(NLS.bind(TaskUIMessages.task_config_wizard_page_settings_config,curTaskType.getName()));
        }
        setPageComplete(determinePageCompletion());
    }

    @Override
    public void deactivatePage() {
        if (taskConfigPanel != null && taskConfigPanel.isComplete()) {
            taskConfigPanel.saveSettings();
        }
    }

    private void createTaskSettingsUI() {
        UIUtils.disposeChildControls(taskSettingsPlaceholder);

        if (curTaskType != null && TaskUIRegistry.getInstance().supportsConfigurator(curTaskType)) {
            try {
                DBTTaskConfigurator configurator = TaskUIRegistry.getInstance().createConfigurator(curTaskType);
                DBTTaskConfigPanel configPage = configurator.createInputConfigurator(UIUtils.getDefaultRunnableContext(), curTaskType);
                if (configPage != null) {
                    taskConfigPanel = configPage;
                    TaskConfigurationWizard taskWizard = getTaskPage().getTaskWizard();
                    taskConfigPanel.createControl(taskSettingsPlaceholder, taskWizard, this::updatePageCompletion);
                    if (getWizard().getCurrentTask() != null) {
                        taskConfigPanel.loadSettings();
                    }
                    taskSettingsPlaceholder.layout(true, true);
                }
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Task configurator error", "Error creating task configuration UI", e);
            }
        }
    }

    private TaskConfigurationWizardPageTask getTaskPage() {
        return ((TaskConfigurationWizardDialog) getContainer()).getTaskPage();
    }

}
