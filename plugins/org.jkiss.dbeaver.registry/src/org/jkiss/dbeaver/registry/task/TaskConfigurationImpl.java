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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTaskConfiguration;
import org.jkiss.dbeaver.model.task.DBTTaskDescriptor;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * TaskConfigurationImpl
 */
public class TaskConfigurationImpl implements DBTTaskConfiguration {

    private String label;
    private String description;
    private Date createTime;
    private Date updateTime;
    private DBTTaskDescriptor task;
    private Map<String, Object> properties;

    public TaskConfigurationImpl(String label, String description, Date createTime, Date updateTime, DBTTaskDescriptor task, Map<String, Object> properties) {
        this.label = label;
        this.description = description;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.task = task;
        this.properties = properties;
    }

    @NotNull
    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(@NotNull String label) {
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
    public DBTTaskDescriptor getDescriptor() {
        return task;
    }

    @NotNull
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @NotNull
    @Override
    public List<DBSObject> getSourceObjects(DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }
}
