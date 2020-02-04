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
package org.jkiss.dbeaver.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.internal.localstore.Bucket;
import org.eclipse.core.internal.localstore.BucketTree;
import org.eclipse.core.internal.properties.PropertyBucket;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.registry.task.TaskManagerImpl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ProjectMetadata implements DBPProject {

    private static final Log log = Log.getLog(ProjectMetadata.class);

    public static final String SETTINGS_STORAGE_FILE = "project-settings.json";
    public static final String METADATA_STORAGE_FILE = "project-metadata.json";
    public static final String PROP_PROJECT_ID = "id";

    public enum ProjectFormat {
        UNKNOWN,    // Project is not open or corrupted
        LEGACY,     // Old format (before 6.1 version
        MODERN,     // 6.1+ version
    }

    private static Gson METADATA_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create();

    private final AbstractJob metadataSyncJob;

    private final DBPWorkspace workspace;
    private final IProject project;

    private volatile ProjectFormat format = ProjectFormat.UNKNOWN;
    private volatile DataSourceRegistry dataSourceRegistry;
    private volatile TaskManagerImpl taskManager;
    private volatile Map<String, Object> properties;
    private volatile Map<String, Map<String, Object>> resourceProperties;
    private DBASecureStorage secureStorage;
    private UUID projectID;
    private final Object metadataSync = new Object();

    public ProjectMetadata(DBPWorkspace workspace, IProject project) {
        this.workspace = workspace;
        this.project = project;
        this.metadataSyncJob = new ProjectSyncJob();
    }

    @NotNull
    @Override
    public DBPWorkspace getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public String getName() {
        return project.getName();
    }

    @Override
    public UUID getProjectID() {
        if (projectID == null) {
            String idStr = CommonUtils.toString(this.getProjectProperty(PROP_PROJECT_ID), null);
            if (CommonUtils.isEmpty(idStr)) {
                projectID = UUID.randomUUID();
                this.setProjectProperty(PROP_PROJECT_ID, projectID.toString());
            } else {
                projectID = UUID.fromString(idStr);
            }
        }
        return projectID;
    }

    @NotNull
    @Override
    public File getAbsolutePath() {
        return project.getLocation().toFile();
    }

    @NotNull
    @Override
    public IProject getEclipseProject() {
        return project;
    }

    @NotNull
    @Override
    public IFolder getMetadataFolder(boolean create) {
        IFolder metadataFolder = project.getFolder(METADATA_FOLDER);
        if (create && !metadataFolder.exists()) {
            try {
                metadataFolder.create(IResource.FORCE | IResource.HIDDEN, true, new NullProgressMonitor());
            } catch (CoreException e) {
                log.error("Error creating project metadata folder", e);
            }
        }

        return metadataFolder;
    }

    @NotNull
    private File getMetadataPath() {
        return new File(getAbsolutePath(), METADATA_FOLDER);
    }

    @Override
    public boolean isOpen() {
        return project.isOpen();
    }

    @Override
    public void ensureOpen() throws IllegalStateException {
        if (format != ProjectFormat.UNKNOWN) {
            return;
        }
        if (!project.isOpen()) {
            try {
                NullProgressMonitor monitor = new NullProgressMonitor();
                project.open(monitor);
                project.refreshLocal(IFile.DEPTH_ONE, monitor);
            } catch (CoreException e) {
                log.error("Error opening project", e);
                return;
            }
        }

        IFolder mdFolder = getMetadataFolder(false);

        File dsConfig = new File(getAbsolutePath(), DataSourceRegistry.LEGACY_CONFIG_FILE_NAME);
        if (!mdFolder.exists() && dsConfig.exists()) {
            format = ProjectFormat.LEGACY;
        } else {
            format = ProjectFormat.MODERN;
        }

        // Check project structure and migrate
        checkAndUpdateProjectStructure();
    }

    @Override
    public boolean isRegistryLoaded() {
        return dataSourceRegistry != null;
    }

    @NotNull
    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        ensureOpen();
        synchronized (metadataSync) {
            if (dataSourceRegistry == null) {
                dataSourceRegistry = new DataSourceRegistry(workspace.getPlatform(), this);
            }
        }
        return dataSourceRegistry;
    }

    @NotNull
    @Override
    public DBTTaskManager getTaskManager() {
        ensureOpen();
        if (taskManager == null) {
            synchronized (metadataSync) {
                if (taskManager == null) {
                    taskManager = new TaskManagerImpl(this);
                }
            }
        }
        return taskManager;
    }

    ////////////////////////////////////////////////////////
    // Secure storage

    @NotNull
    @Override
    public DBASecureStorage getSecureStorage() {
        synchronized (metadataSync) {
            if (this.secureStorage == null) {
                this.secureStorage = workspace.getPlatform().getApplication().getProjectSecureStorage(this);
            }
        }
        return secureStorage;
    }

    ////////////////////////////////////////////////////////
    // Properties

    @Override
    public Object getProjectProperty(String propName) {
        synchronized (this) {
            loadProperties();
            return properties.get(propName);
        }
    }

    @Override
    public void setProjectProperty(String propName, Object propValue) {
        synchronized (this) {
            loadProperties();
            if (propValue == null) {
                properties.remove(propName);
            } else {
                properties.put(propName, propValue);
            }
            saveProperties();
        }
    }

    private void loadProperties() {
        if (properties != null) {
            return;
        }

        synchronized (metadataSync) {
            File settingsFile = new File(getMetadataPath(), SETTINGS_STORAGE_FILE);
            if (settingsFile.exists() && settingsFile.length() > 0) {
                // Parse metadata
                try (Reader settingsReader = new InputStreamReader(new FileInputStream(settingsFile), StandardCharsets.UTF_8)) {
                    properties = METADATA_GSON.fromJson(settingsReader, Map.class);
                } catch (Throwable e) {
                    log.error("Error reading project '" + getName() + "' setting from "  + settingsFile.getAbsolutePath(), e);
                }
            }
            if (properties == null) {
                properties = new LinkedHashMap<>();
            }
        }
    }

    private void saveProperties() {
        File settingsFile = new File(getMetadataPath(), SETTINGS_STORAGE_FILE);
        String settingsString = METADATA_GSON.toJson(properties);
        try (Writer settingsWriter = new OutputStreamWriter(new FileOutputStream(settingsFile), StandardCharsets.UTF_8)) {
            settingsWriter.write(settingsString);
        } catch (Throwable e) {
            log.error("Error writing project '" + getName() + "' setting to "  + settingsFile.getAbsolutePath(), e);
        }
    }

    @Override
    public Object getResourceProperty(IResource resource, String propName) {
        loadMetadata();
        synchronized (metadataSync) {
            Map<String, Object> resProps = resourceProperties.get(resource.getProjectRelativePath().toString());
            if (resProps != null) {
                return resProps.get(propName);
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getResourceProperties(IResource resource) {
        loadMetadata();
        synchronized (metadataSync) {
            return resourceProperties.get(resource.getProjectRelativePath().toString());
        }
    }

    @Override
    public Map<String, Map<String, Object>> getResourceProperties() {
        loadMetadata();
        synchronized (metadataSync) {
            return new LinkedHashMap<>(resourceProperties);
        }
    }

    @Override
    public void setResourceProperty(IResource resource, String propName, Object propValue) {
        loadMetadata();
        synchronized (metadataSync) {
            String filePath = resource.getProjectRelativePath().toString();
            Map<String, Object> resProps = resourceProperties.get(filePath);
            if (resProps == null) {
                if (propValue == null) {
                    // No props + no new value - ignore
                    return;
                }
                resProps = new LinkedHashMap<>();
                resourceProperties.put(filePath, resProps);
            }
            if (propValue == null) {
                if (resProps.remove(propName) == null) {
                    if (resProps.isEmpty()) {
                        resourceProperties.remove(filePath);
                    } else {
                        // No changes
                        return;
                    }
                }
            } else {
                Object oldValue = resProps.put(propName, propValue);
                if (Objects.equals(oldValue, propValue)) {
                    // No changes
                    return;
                }
            }
        }
        flushMetadata();
    }

    public void dispose() {
        if (dataSourceRegistry != null) {
            dataSourceRegistry.dispose();
        }
    }

    public ProjectFormat getFormat() {
        return format;
    }

    private void loadMetadata() {
        ensureOpen();
        synchronized (metadataSync) {
            if (resourceProperties != null) {
                return;
            }

            File mdFile = new File(getMetadataPath(), METADATA_STORAGE_FILE);
            if (mdFile.exists() && mdFile.length() > 0) {
                // Parse metadata
                Map<String, Map<String, Object>> mdCache = new TreeMap<>();
                try (Reader mdReader = new InputStreamReader(new FileInputStream(mdFile), StandardCharsets.UTF_8)) {
                    try (JsonReader jsonReader = METADATA_GSON.newJsonReader(mdReader)) {
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()) {
                            String topName = jsonReader.nextName();
                            if ("resources".equals(topName)) {
                                jsonReader.beginObject();

                                while (jsonReader.hasNext()) {
                                    String resourceName = jsonReader.nextName();
                                    Map<String, Object> resProperties = new HashMap<>();
                                    jsonReader.beginObject();
                                    while (jsonReader.hasNext()) {
                                        String propName = jsonReader.nextName();
                                        Object propValue;
                                        switch (jsonReader.peek()) {
                                            case NUMBER:
                                                propValue = jsonReader.nextDouble();
                                                break;
                                            case BOOLEAN:
                                                propValue = jsonReader.nextBoolean();
                                                break;
                                            case NULL:
                                                propValue = null;
                                                break;
                                            default:
                                                propValue = jsonReader.nextString();
                                                break;
                                        }
                                        resProperties.put(propName, propValue);
                                    }
                                    jsonReader.endObject();
                                    if (!resProperties.isEmpty()) {
                                        mdCache.put(resourceName, resProperties);
                                    }
                                }
                                jsonReader.endObject();
                            }
                        }

                        jsonReader.endObject();

                        resourceProperties = mdCache;
                    }
                } catch (Throwable e) {
                    log.error("Error reading project '" + getName() + "' metadata from "  + mdFile.getAbsolutePath(), e);
                }
            }
            if (resourceProperties == null) {
                resourceProperties = new TreeMap<>();
            }
        }
    }

    /**
     * Validates project files structure.
     * If project was created in older DBeaver version then converts it to newer format
     */
    private void checkAndUpdateProjectStructure() {
        if (format == ProjectFormat.UNKNOWN || format == ProjectFormat.MODERN) {
            return;
        }

        File mdConfig = new File(getMetadataPath(), METADATA_STORAGE_FILE);
        if (!mdConfig.exists()) {
            // Migrate
            log.debug("Migrate Eclipse resource properties to the project metadata (" + mdConfig.getAbsolutePath() + ")");
            Map<String, Map<String, Object>> projectResourceProperties = extractProjectResourceProperties();
            synchronized (metadataSync) {
                this.resourceProperties = projectResourceProperties;
            }
            flushMetadata();
        }

        // Now project is in modern format
        format = ProjectFormat.MODERN;
    }

    private Map<String, Map<String, Object>> extractProjectResourceProperties() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        try {
            BucketTree bucketTree = new BucketTree((Workspace) workspace.getEclipseWorkspace(), new PropertyBucket());
            try {
                final IPath projectPath = project.getFullPath();
                bucketTree.accept(new Bucket.Visitor() {
                    @Override
                    public int visit(Bucket.Entry entry) {
                        Object value = entry.getValue();
                        if (value instanceof String[][]) {
                            String[][] bucketProps = (String[][]) value;
                            for (String[] resProps : bucketProps) {
                                if (resProps.length == 3) {
                                    if ("org.jkiss.dbeaver".equals(resProps[0])) {
                                        if ("sql-editor-project-id".equals(resProps[1])) {
                                            continue;
                                        }
                                        Map<String, Object> propsMap = result.computeIfAbsent(
                                            entry.getPath().makeRelativeTo(projectPath).toString(), s -> new LinkedHashMap<>());
                                        propsMap.put(resProps[1], resProps[2]);
                                    }
                                }
                            }
                        }
                        return CONTINUE;
                    }
                },
                    projectPath,
                    BucketTree.DEPTH_INFINITE);
            } catch (CoreException e) {
                log.error(e);
            }
        } catch (Throwable e) {
            log.error("Error extracting project metadata", e);
        }

        return result;
    }

    private void flushMetadata() {
        synchronized (metadataSync) {
            metadataSyncJob.schedule(100);
        }
    }

    void removeResourceFromCache(IPath path) {
        synchronized (metadataSync) {
            if (resourceProperties != null) {
                resourceProperties.remove(path.toString());
            }
        }
        flushMetadata();
    }

    void updateResourceCache(IPath oldPath, IPath newPath) {
        synchronized (metadataSync) {
            if (resourceProperties != null) {
                Map<String, Object> props = resourceProperties.remove(oldPath.toString());
                if (props != null) {
                    resourceProperties.put(newPath.toString(), props);
                }
            }
        }
        flushMetadata();
    }

    @Override
    public String toString() {
        return getName();
    }

    private class ProjectSyncJob extends AbstractJob {
        ProjectSyncJob() {
            super("Project metadata sync");
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            setName("Project '" + ProjectMetadata.this.getName() + "' sync job");

            synchronized (metadataSync) {
                File mdFile = new File(getMetadataPath(), METADATA_STORAGE_FILE);
                if (CommonUtils.isEmpty(resourceProperties) && !mdFile.exists()) {
                    // Nothing to save and metadata file doesn't exist
                    return Status.OK_STATUS;
                }
                try {
                    ContentUtils.makeFileBackup(getMetadataFolder(true).getFile(new Path(METADATA_STORAGE_FILE)));

                    if (!CommonUtils.isEmpty(resourceProperties)) {
                        try (Writer mdWriter = new OutputStreamWriter(new FileOutputStream(mdFile), StandardCharsets.UTF_8)) {
                            try (JsonWriter jsonWriter = METADATA_GSON.newJsonWriter(mdWriter)) {
                                jsonWriter.beginObject();

                                jsonWriter.name("resources");
                                jsonWriter.beginObject();
                                for (Map.Entry<String, Map<String, Object>> resEntry : resourceProperties.entrySet()) {
                                    jsonWriter.name(resEntry.getKey());
                                    jsonWriter.beginObject();
                                    Map<String, Object> resProps = resEntry.getValue();
                                    for (Map.Entry<String, Object> propEntry : resProps.entrySet()) {
                                        jsonWriter.name(propEntry.getKey());
                                        Object value = propEntry.getValue();
                                        if (value == null) {
                                            jsonWriter.nullValue();
                                        } else if (value instanceof Number) {
                                            jsonWriter.value((Number) value);
                                        } else if (value instanceof Boolean) {
                                            jsonWriter.value((Boolean) value);
                                        } else {
                                            jsonWriter.value(CommonUtils.toString(value));
                                        }
                                    }
                                    jsonWriter.endObject();
                                }
                                jsonWriter.endObject();

                                jsonWriter.endObject();
                                jsonWriter.flush();
                            }
                        }
                    }

                } catch (IOException e) {
                    log.error("Error flushing project metadata", e);
                }
            }

            return Status.OK_STATUS;
        }
    }
}
