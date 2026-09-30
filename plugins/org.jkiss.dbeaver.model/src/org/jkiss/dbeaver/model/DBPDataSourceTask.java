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

package org.jkiss.dbeaver.model;

/**
 * DBPDataSourceTask
 */
public interface DBPDataSourceTask
{
    boolean isActiveTask();

    /**
     * Per default, the current behavior is that saveable DBPDataSourceTasks are included when disconnecting users of
     * data sources. Certain cases (see #41844) where application users can set the application to not "Auto-save upon
     * close" unfortunately interfered with this behavior, i.e. SQL script was recently edited -> application user
     * decides to disconnect -> prompt to save the script is triggered.
     * @return true on default; we save on disconnects in the standard case
     */
    default boolean shouldSaveOnDisconnect() {
        return true;
    }
}
