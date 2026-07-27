/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.sql.script;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskWizardExecutor;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.dbeaver.tools.sql.SQLTaskConstants;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;

import java.util.Map;

class SQLScriptTaskConfigurationWizard extends TaskConfigurationWizard<SQLScriptExecuteSettings> {
    private static final Log log = Log.getLog(SQLScriptTaskConfigurationWizard.class);

    private SQLScriptExecuteSettings settings = new SQLScriptExecuteSettings();
    private SQLScriptTaskPageSettings pageSettings;

    public SQLScriptTaskConfigurationWizard() {
    }

    public SQLScriptTaskConfigurationWizard(@NotNull DBTTask task) {
        super(task);
        try {
            settings.loadConfiguration(task);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Configuration error", "Unable to load task configuration", e);
        }
    }

    @Override
    protected String getDefaultWindowTitle() {
        return DTUIMessages.sql_script_task_configuration_wizard_default_window_title;
    }

    @Override
    public String getTaskTypeId() {
        return SQLTaskConstants.TASK_SCRIPT_EXECUTE;
    }

    @Override
    public void addPages() {
        super.addPages();
        pageSettings = new SQLScriptTaskPageSettings(this);
        addPage(pageSettings);
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {
        pageSettings.saveSettings();

        settings.saveConfiguration(state);
    }

    @Override
    public SQLScriptExecuteSettings getSettings() {
        return settings;
    }

    @Override
    public boolean performFinish() {
        DBTTask task = getCurrentTask();
        if (task != null && task.isTemporary()) {
            if (!saveConfigurationToTask(task)) {
                return false;
            }
            TaskConfigurationWizardDialog container = getContainer();
            container.disableButtonsOnProgress();
            try {
                TaskWizardExecutor executor = new TaskWizardExecutor(getRunnableContext(), task, log, System.out);
                executor.executeTask();
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Task run error", e.getMessage(), e);
                return false;
            } finally {
                container.enableButtonsAfterProgress();
            }
            return true;
        }
        return super.performFinish();
    }
}
