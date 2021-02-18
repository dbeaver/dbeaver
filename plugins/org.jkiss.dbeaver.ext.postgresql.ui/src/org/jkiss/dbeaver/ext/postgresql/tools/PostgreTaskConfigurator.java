/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.postgresql.tools;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreSQLTasks;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.nativetool.NativeToolConfigPanel;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;

/**
 * PostgreSQL task configurator
 */
public class PostgreTaskConfigurator implements DBTTaskConfigurator {

    private static final Log log = Log.getLog(PostgreTaskConfigurator.class);

    @Override
    public ConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public TaskConfigurationWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        switch (taskConfiguration.getType().getId()) {
            case PostgreSQLTasks.TASK_DATABASE_BACKUP:
                return new PostgreBackupWizard(taskConfiguration);
            case PostgreSQLTasks.TASK_DATABASE_RESTORE:
                return new PostgreRestoreWizard(taskConfiguration);
            case PostgreSQLTasks.TASK_SCRIPT_EXECUTE:
                return new PostgreScriptExecuteWizard(taskConfiguration);
        }
        return null;
    }

    private static class ConfigPanel extends NativeToolConfigPanel<PostgreDatabase> {
        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            super(runnableContext, taskType, PostgreDatabase.class, PostgreDataSourceProvider.class);
        }
    }

}
