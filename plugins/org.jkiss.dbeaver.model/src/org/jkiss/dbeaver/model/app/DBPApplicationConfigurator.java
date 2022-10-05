/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.app;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBConfigurationController;
import org.jkiss.dbeaver.model.DBFileController;
import org.jkiss.dbeaver.model.task.DBTTaskController;

/**
 * DB desktop application.
 */
public interface DBPApplicationConfigurator {

    DBConfigurationController createConfigurationController();

    /**
     * Creates platform plug-in configuration controller by plug-in id.
     * Keeps plug-in configuration which can be shared with other users.
     */
    @NotNull
    DBConfigurationController createPluginConfigurationController(@NotNull String pluginId);

    DBFileController createFileController();

    /**
     * Returns task controller. Task controller helps to work with task: load and save configuration file for different type of projects
     */
    DBTTaskController createTaskController();

}
