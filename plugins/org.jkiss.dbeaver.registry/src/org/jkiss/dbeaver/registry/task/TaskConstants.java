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
package org.jkiss.dbeaver.registry.task;

public class TaskConstants {

    static final String TEMPORARY_ID = "#temp";
    public static final String CONFIG_FILE = "tasks.json";
    public static final String TASK_STATS_FOLDER = "task-stats";
    static final String TASKS_FOLDERS_TAG = "##tasksFolders";
    
    public static final String TOOL_TASK_PROP = "isToolTask";

    static final String TAG_TASK = "task";
    static final String TAG_LABEL = "label";
    static final String TAG_DESCRIPTION = "description";
    static final String TAG_TASK_FOLDER = "taskFolder";
    static final String TAG_CREATE_TIME = "createTime";
    static final String TAG_UPDATE_TIME = "updateTime";
    static final String TAG_STATE = "state";

    static final String TAG_PARENT = "parent";
    static final String TAG_MAX_EXEC_TIME = "maxExecutionTime";

    public static final int DEFAULT_MAX_EXECUTION_TIME = 300;

}
