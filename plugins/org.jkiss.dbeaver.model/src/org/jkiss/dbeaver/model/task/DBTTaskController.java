/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.task;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObjectController;

/**
 * Helps to work properly with tasks: read and save configuration file in different type of DBeaver applications.
 */
public interface DBTTaskController extends DBPObjectController {

    /**
     * Method to load (read) tasks configuration file (usually tasks.json). Task config can be in the local workspace + project
     * Or it can be on the remote workspace + project
     *
     * @param projectId to find proper project
     * @param filePath  file name + extension
     * @return data from task configuration file in the String format
     * @throws DBException returns in case of file loading
     */
    @Nullable
    String loadTaskConfigurationFile(@NotNull String projectId, @NotNull String filePath) throws DBException;

    /**
     * Method to load (read) task configuration from file (usually tasks.json). Task config can be in the local
     * workspace + project
     * Or it can be on the remote workspace + project
     *
     * @param projectId to find proper project
     * @param taskId    to find proper task
     * @param filePath  file name + extension
     * @return data from task configuration file in the String format or null if task not exist
     * @throws DBException returns in case of file loading
     */
    @Nullable
    String loadTaskConfiguration(
        @NotNull String projectId,
        @NotNull String taskId,
        @NotNull String filePath
    ) throws DBException;

    /**
     * Method saves task configuration file. After task creation/editing as example
     *
     * @param projectId to find proper project task config
     * @param filePath  file name + extension
     * @param data      for update. Can be null in case of purpose of delete file
     * @throws DBException returns in case of file loading/saving
     */
    void saveTaskConfigurationFile(
        @NotNull String projectId,
        @NotNull String filePath,
        @Nullable String data) throws DBException;

}
