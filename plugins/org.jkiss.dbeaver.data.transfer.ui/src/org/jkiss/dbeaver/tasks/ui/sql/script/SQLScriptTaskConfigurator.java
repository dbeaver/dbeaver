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

package org.jkiss.dbeaver.tasks.ui.sql.script;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanel;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;

/**
 * SQL task configurator
 */
public class SQLScriptTaskConfigurator implements DBTTaskConfigurator {

    @Override
    public DBTTaskConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return null;
    }

    @Override
    public TaskConfigurationWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        return new SQLScriptTaskConfigurationWizard(taskConfiguration);
    }

}
