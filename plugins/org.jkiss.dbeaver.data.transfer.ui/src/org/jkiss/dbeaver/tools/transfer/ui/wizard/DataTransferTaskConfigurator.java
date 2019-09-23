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

package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskConfigurator;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;

/**
 * Data transfer task configurator
 */
public class DataTransferTaskConfigurator implements DBTTaskConfigurator {

    @Override
    public Object createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public boolean openTaskConfigDialog(@NotNull DBTTask taskConfiguration) {
        return DataTransferWizardDialog.openWizard(
            UIUtils.getActiveWorkbenchWindow(),
            taskConfiguration) == IDialogConstants.OK_ID;
    }

    private static class ConfigPanel implements IObjectPropertyConfigurator<DBTTask> {

        private DBRRunnableContext runnableContext;
        private DBTTaskType taskType;
        private DatabaseObjectsSelectorPanel selectorPanel;

        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            this.runnableContext = runnableContext;
            this.taskType = taskType;
        }

        @Override
        public void createControl(Composite parent) {
            selectorPanel = new DatabaseObjectsSelectorPanel(parent, runnableContext);
        }

        @Override
        public void loadSettings(DBTTask configuration) {

        }

        @Override
        public void saveSettings(DBTTask configuration) {

        }

        @Override
        public void resetSettings(DBTTask configuration) {

        }

        @Override
        public boolean isComplete() {
            return !selectorPanel.getCheckedNodes().isEmpty();
        }
    }

}
