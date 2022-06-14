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
package org.jkiss.dbeaver.registry.task;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskFolder;

import java.util.ArrayList;
import java.util.List;

public class TaskFolderImpl implements DBTTaskFolder {

    private String folderName;
    private DBPProject folderProject;
    private List<DBTTask> folderTasks;
    private DBTTaskFolder parentFolder;
    private List<DBTTaskFolder> nestedFolders = new ArrayList<>();

    TaskFolderImpl(@NotNull String folderName,
                   @Nullable DBTTaskFolder parentFolder,
                   @NotNull DBPProject folderProject,
                   @Nullable List<DBTTask> folderTasks) {
        this.folderName = folderName;
        this.parentFolder = parentFolder;
        this.folderProject = folderProject;
        this.folderTasks = folderTasks;
    }

    @NotNull
    @Override
    public DBPProject getProject() {
        return folderProject;
    }

    @Nullable
    public DBTTaskFolder getParentFolder() {
        return parentFolder;
    }

    @Nullable
    @Override
    public List<DBTTask> getTasks() {
        return folderTasks;
    }

    @Override
    public void addTaskToFolder(@NotNull DBTTask task) {
        folderTasks.add(task);
    }

    @Override
    public void removeTaskFromFolder(DBTTask task) {
        folderTasks.remove(task);
    }

    @Nullable
    @Override
    public List<DBTTaskFolder> getNestedTaskFolders() {
        return nestedFolders;
    }

    @Override
    public void addFolderToFoldersList(@NotNull DBTTaskFolder taskFolder) {
        nestedFolders.add(taskFolder);
    }

    @Override
    public boolean removeFolderFromFoldersList(@NotNull DBTTaskFolder taskFolder) {
        return nestedFolders.remove(taskFolder);
    }

    @Override
    public void setName(String newName) {
        this.folderName = newName;
    }

    @NotNull
    @Override
    public String getName() {
        return folderName;
    }
}
