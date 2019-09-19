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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskConfiguration;
import org.jkiss.dbeaver.model.task.DBTTaskDescriptor;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.model.task.DBTTaskRegistry;
import org.jkiss.dbeaver.registry.ProjectMetadata;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TaskManagerImpl
 */
public class TaskManagerImpl implements DBTTaskManager {

    private static final Log log = Log.getLog(TaskManagerImpl.class);

    public static final String CONFIG_FILE = "tasks.json";
    private static Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    private final ProjectMetadata projectMetadata;
    private final List<TaskConfigurationImpl> tasks = new ArrayList<>();

    public TaskManagerImpl(ProjectMetadata projectMetadata) {
        this.projectMetadata = projectMetadata;
        loadConfiguration();
    }

    @NotNull
    @Override
    public DBTTaskRegistry getRegistry() {
        return TaskRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBPProject getProject() {
        return projectMetadata;
    }

    @NotNull
    @Override
    public DBTTaskConfiguration[] getTaskConfigurations() {
        return tasks.toArray(new DBTTaskConfiguration[0]);
    }

    @NotNull
    @Override
    public DBTTaskConfiguration[] getTaskConfigurations(DBTTaskDescriptor task) {
        List<DBTTaskConfiguration> result = new ArrayList<>();
        for (DBTTaskConfiguration tc : tasks) {
            if (tc.getDescriptor() == task) {
                result.add(tc);
            }
        }
        return result.toArray(new DBTTaskConfiguration[0]);
    }

    @NotNull
    @Override
    public DBTTaskConfiguration createTaskConfiguration(
        DBTTaskDescriptor taskDescriptor,
        String label,
        String description,
        Map<String, Object> properties) throws DBException
    {
/*
        DBTTaskDescriptor taskDescriptor = getRegistry().getTask(taskId);
        if (taskDescriptor == null) {
            throw new DBException("Task " + taskId + " not found");
        }
*/
        Date createTime = new Date();
        String id = UUID.randomUUID().toString();
        TaskConfigurationImpl task = new TaskConfigurationImpl(id, label, description, createTime, createTime, taskDescriptor);
        task.setProperties(properties);
        synchronized (tasks) {
            tasks.add(task);
        }

        saveConfiguration();

        return task;
    }

    @Override
    public void updateTaskConfiguration(DBTTaskConfiguration task) {
        saveConfiguration();
    }

    @Override
    public void deleteTaskConfiguration(DBTTaskConfiguration task) {
        throw new RuntimeException("Not Implemented");
    }

    private void loadConfiguration() {
        IFile configFile = getConfigFile(false);
        if (!configFile.exists()) {
            return;
        }
    }

    private void saveConfiguration() {
        IProgressMonitor monitor = new NullProgressMonitor();

        IFile configFile = getConfigFile(true);
        try {
            if (configFile.exists()) {
                IOUtils.makeFileBackup(configFile.getLocation().toFile());
            }
            if (tasks.isEmpty()) {
                configFile.delete(true, false, monitor);
                return;
            }
        } catch (Exception e) {
            log.error("Error processing config file", e);
        }

        ByteArrayOutputStream dsConfigBuffer = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(dsConfigBuffer, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                synchronized (tasks) {
                    serializeTasks(new DefaultProgressMonitor(monitor), jsonWriter);
                }
            }
        } catch (IOException e) {
            log.error(e);
            return;
        }
        InputStream ifs = new ByteArrayInputStream(dsConfigBuffer.toByteArray());

        try {
            if (!configFile.exists()) {
                configFile.create(ifs, true, monitor);
                configFile.setHidden(true);
            } else {
                configFile.setContents(ifs, true, false, monitor);
            }
        } catch (Exception e) {
            log.error("Error saving configuration to a file " + configFile.getFullPath(), e);
        }
    }

    private void serializeTasks(DBRProgressMonitor monitor, JsonWriter jsonWriter) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH);
        jsonWriter.setIndent("\t");
        jsonWriter.beginObject();
        for (TaskConfigurationImpl task : tasks) {
            jsonWriter.name(task.getId());
            jsonWriter.beginObject();
            JSONUtils.field(jsonWriter, "task", task.getDescriptor().getId());
            JSONUtils.field(jsonWriter, "label", task.getLabel());
            JSONUtils.field(jsonWriter, "description", task.getDescription());
            JSONUtils.field(jsonWriter, "createTime", dateFormat.format(task.getCreateTime()));
            JSONUtils.field(jsonWriter, "updateTime", dateFormat.format(task.getUpdateTime()));
            JSONUtils.serializeProperties(jsonWriter, "state", task.getProperties());
            jsonWriter.endObject();
        }
        jsonWriter.endObject();
    }

    private IFile getConfigFile(boolean create) {
        return projectMetadata.getMetadataFolder(create).getFile(CONFIG_FILE);
    }

}
