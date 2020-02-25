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
package org.jkiss.dbeaver.tools.sql.ui.wizard;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.dbeaver.tools.sql.SQLTaskConstants;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

class SQLTaskConfigurationWizard extends TaskConfigurationWizard {
    private SQLScriptExecuteSettings settings = new SQLScriptExecuteSettings();
    private SQLTaskPageSettings pageSettings;

    public SQLTaskConfigurationWizard() {
    }

    public SQLTaskConfigurationWizard(@NotNull DBTTask task) {
        super(task);
        settings.loadConfiguration(UIUtils.getDefaultRunnableContext(), task.getProperties());
    }

    @Override
    protected String getDefaultWindowTitle() {
        return "Script Execute";
    }

    @Override
    public String getTaskTypeId() {
        return SQLTaskConstants.TASK_SCRIPT_EXECUTE;
    }

    @Override
    public void addPages() {
        super.addPages();
        pageSettings = new SQLTaskPageSettings(this);
        addPage(pageSettings);
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {
        pageSettings.saveSettings();

        settings.saveConfiguration(state);
    }

    public SQLScriptExecuteSettings getSettings() {
        return settings;
    }
}
