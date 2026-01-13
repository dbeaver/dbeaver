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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.task.DBTTaskSettings;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;

@SuppressWarnings("rawtypes")
public abstract class TaskConfigurationWIzardActionConfigurator<T extends DBTTaskSettings>
    implements IObjectPropertyConfigurator<TaskConfigurationWizard<T>, T> {

    protected abstract void updateActions();

    protected abstract void enableActions(boolean enable);

    @Override
    public final void loadSettings(@NotNull T t) {
        // do nothing
    }

    @Override
    public final void saveSettings(@NotNull T t) {
        // do nothing
    }

    @Override
    public final void resetSettings(@NotNull T t) {
        // do nothing
    }

    @Override
    public final boolean isComplete() {
        return true;
    }
}
