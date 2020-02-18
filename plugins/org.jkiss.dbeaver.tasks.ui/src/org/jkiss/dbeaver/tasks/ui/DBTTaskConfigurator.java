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
package org.jkiss.dbeaver.tasks.ui;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;

/**
 * Task configurator.
 * Usually some UI dialog/wizard for task configuration.
 */
public interface DBTTaskConfigurator {

    /**
     * Creates a panel for task input objects configure.
     *
     * @param runnableContext runnable context
     * @param taskType task type
     * @return null if config panel not supported/disabled. Otherwise IObjectPropertyConfigurator implementation (see UI modules).
     */
    DBTTaskConfigPanel createInputConfigurator(
        DBRRunnableContext runnableContext,
        @NotNull DBTTaskType taskType);

    TaskConfigurationWizard createTaskConfigWizard(
        @NotNull DBTTask taskConfiguration);

}
