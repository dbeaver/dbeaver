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

package org.jkiss.dbeaver.tasks.ui.sql;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanel;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;

/**
 * SQL tool task configurator
 */
public class SQLToolTaskConfigurator implements DBTTaskConfigurator {

    private static final Log log = Log.getLog(SQLToolTaskConfigurator.class);

    @Override
    public ConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public TaskConfigurationWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        return new SQLToolTaskConfigurationWizard(taskConfiguration);
    }

    private static class ConfigPanel implements DBTTaskConfigPanel {

        private DBRRunnableContext runnableContext;
        private DBTTaskType taskType;
        private Table objectsTable;
        private DBPProject currentProject;
        private SQLToolTaskConfigurationWizard dtWizard;

        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            this.runnableContext = runnableContext;
            this.taskType = taskType;
            //this.currentProject = NavigatorUtils.getSelectedProject();
        }

        @Override
        public void createControl(Composite parent, TaskConfigurationWizard wizard, Runnable propertyChangeListener) {
            dtWizard = (SQLToolTaskConfigurationWizard) wizard;
        }

        @Override
        public void loadSettings() {
        }

        @Override
        public void saveSettings() {
        }

        @Override
        public boolean isComplete() {
            return objectsTable.getItemCount() > 0;
        }

        @Override
        public String getErrorMessage() {
            if (objectsTable.getItemCount() == 0) {
                return "No objects selected";
            }
            return null;
        }
    }

}
