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
package org.jkiss.dbeaver.registry.task;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPObjectWithDescription;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.jkiss.dbeaver.model.task.DBTTaskType;

import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * TaskImpl
 */
public class TaskImpl implements DBTTask, DBPNamedObject2, DBPObjectWithDescription {

    private final DBPProject project;
    private String id;
    private String label;
    private String description;
    private Date createTime;
    private Date updateTime;
    private DBTTaskType type;
    private Map<String, Object> properties;

    public TaskImpl(DBPProject project, DBTTaskType type, String id, String label, String description, Date createTime, Date updateTime) {
        this.project = project;
        this.id = id;
        this.label = label;
        this.description = description;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public DBPProject getProject() {
        return project;
    }

    @NotNull
    @Override
    public String getName() {
        return label;
    }

    @Override
    public void setName(@NotNull String label) {
        this.label = label;
    }

    @NotNull
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @NotNull
    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @NotNull
    @Override
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @NotNull
    @Override
    public DBTTaskType getType() {
        return type;
    }

    @NotNull
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Nullable
    @Override
    public DBTTaskRun getLastRun() {
        return null;
    }

    @NotNull
    @Override
    public DBTTaskRun[] getRunStatistics() {
        return new DBTTaskRun[0];
    }

    @Override
    public void cleanRunStatistics() {

    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    File getTaskStatsFolder() {
        return new File(project.getTaskManager().getStatisticsFolder(), id);
    }

    @Override
    public String toString() {
        return id + " " + label + " (" + type.getName() + ")";
    }
}
