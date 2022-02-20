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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBASecureStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.auth.DBASessionContext;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.registry.task.TaskManagerImpl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ProjectMetadata implements DBPProject {

    private static final Log log = Log.getLog(ProjectMetadata.class);

    public static final String SETTINGS_STORAGE_FILE = "project-settings.json";
    public static final String METADATA_STORAGE_FILE = "project-metadata.json";
    public static final String PROP_PROJECT_ID = "id";
    private static final String EMPTY_PROJECT_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<projectDescription>\n" +
        "<name>${project-name}</name>\n" +
        "<comment></comment>\n" +
        "<projects>\n" +
        "</projects>\n" +
        "<buildSpec>\n" +
        "</buildSpec>\n" +
        "<natures>\n" +
        "</natures>\n" +
        "</projectDescription>";

    public enum ProjectFormat {
        UNKNOWN,    // Project is not open or corrupted
        LEGACY,     // Old format (before 6.1 version
        MODERN,     // 6.1+ version
    }

    private static final Gson METADATA_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create();

    private final AbstractJob metadataSyncJob;

    private final DBPWorkspace workspace;
    private final IProject project;
    private final DBASessionContext sessionContext;

    private String projectName;
    private Path projectPath;

    private volatile ProjectFormat format = ProjectFormat.UNKNOWN;
    private volatile DataSourceRegistry dataSourceRegistry;
    private volatile TaskManagerImpl taskManager;
    private volatile Map<String, Object> properties;
    private volatile Map<String, Map<String, Object>> resourceProperties;
    private DBASecureStorage secureStorage;
    private UUID projectID;
    private final Object metadataSync = new Object();
    private boolean inMemory;

    public ProjectMetadata(DBPWorkspace workspace, IProject project, DBASessionContext sessionContext) {
        this.workspace = workspace;
        this.project = project;
        this.metadataSyncJob = new ProjectSyncJob();
        this.sessionContext = sessionContext == null ? workspace.getAuthContext() : sessionContext;
    }

    public ProjectMetadata(DBPWorkspace workspace, String name, Path path, DBASessionContext sessionContext) {
        this(workspace, workspace.getActiveProject() == null ? null : workspace.getActiveProject().getEclipseProject(), sessionContext);
        this.projectName = name;
        this.projectPath = path;
    }

    public void setInMemory(boolean inMemory) {
        this.inMemory = inMemory;
    }

    public boolean isInMemory() {
        return inMemory;
    }

    @NotNull
    @Override
    public DBPWorkspace getWorkspace() {
        return workspace;
    }

    @Override
    public boolean isVirtual() {
        return projectName != null;
    }

    @NotNull
    @Override
    public String getName() {
        return projectName != null ? projectName : project.getName();
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
    public Path getAbsolutePath() {
        return projectPath != null ? projectPath : project.getLocation().toFile().toPath();
    }

    @NotNull
    @Override
    public IProject getEclipseProject() {
        return project;
    }

    @NotNull
    @Override
    public Path getMetadataFolder(boolean create) {
        Path metadataFolder = getMetadataPath();
        if (create && !Files.exists(metadataFolder)) {
            try {
                Files.createDirectories(metadataFolder);
            } catch (IOException e) {
                log.error("Error creating metadata folder" + metadataFolder, e);
            }
        }

        return metadataFolder;
    }

    @NotNull
    private Path getMetadataPath() {
        return getAbsolutePath().resolve(METADATA_FOLDER);
    }

    @Override
    public boolean isOpen() {
        return project == null || project.isOpen();
    }

    @Override
    public void ensureOpen() throws IllegalStateException {
        if (format != ProjectFormat.UNKNOWN) {
            return;
        }
        if (project != null && !project.isOpen()) {
            NullProgressMonitor monitor = new NullProgressMonitor();
            try {
                project.open(monitor);
                project.refreshLocal(IFile.DEPTH_ONE, monitor);
            } catch (CoreException e) {
                if (workspace.getPlatform().getApplication().isStandalone() &&
                    e.getMessage().contains(IProjectDescription.DESCRIPTION_FILE_NAME)) {
                    try {
                        recoverProjectDescription();
                        project.open(monitor);
                        project.refreshLocal(IFile.DEPTH_ONE, monitor);
                    } catch (Exception e2) {
                        log.error("Error opening project", e2);
                        return;
                    }
                }
            }
        }
        if (inMemory) {
            format = ProjectFormat.MODERN;
            return;
        }

        Path mdFolder = getMetadataFolder(false);

        Path dsConfig = getAbsolutePath().resolve(DataSourceRegistry.LEGACY_CONFIG_FILE_NAME);
        if (!Files.exists(mdFolder) && Files.exists(dsConfig)) {
            format = ProjectFormat.LEGACY;
        } else {
            format = ProjectFormat.MODERN;
        }

        // Check project structure and migrate
        checkAndUpdateProjectStructure();
    }

    public void recoverProjectDescription() throws IOException {
        // .project file missing. Let's try to create an empty project config
        Path mdFile = getAbsolutePath().resolve(IProjectDescription.DESCRIPTION_FILE_NAME);
        log.debug("Recovering project '" + project.getName() + "' metadata " + mdFile.toAbsolutePath());

        IOUtils.writeFileFromString(
            mdFile.toFile(),
            EMPTY_PROJECT_TEMPLATE.replace("${project-name}", project.getName()));
    }

    @Override
    public boolean isRegistryLoaded() {
        return dataSourceRegistry != null;
    }

    @Override
    public boolean isModernProject() {
        return getFormat() == ProjectFormat.MODERN;
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

    @NotNull
    @Override
    public DBASessionContext getSessionContext() {
        return sessionContext;
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
            Path settingsFile = getMetadataPath().resolve(SETTINGS_STORAGE_FILE);
            if (Files.exists(settingsFile) && settingsFile.toFile().length() > 0) {
                // Parse metadata
                try (Reader settingsReader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
                    properties = JSONUtils.parseMap(METADATA_GSON, settingsReader);
                } catch (Throwable e) {
                    log.error("Error reading project '" + getName() + "' setting from "  + settingsFile.toAbsolutePath(), e);
                }
            }
            if (properties == null) {
                properties = new LinkedHashMap<>();
            }
        }
    }

    private void saveProperties() {
        Path settingsFile = getMetadataPath().resolve(SETTINGS_STORAGE_FILE);
        String settingsString = METADATA_GSON.toJson(properties);
        try (Writer settingsWriter = new OutputStreamWriter(Files.newOutputStream(settingsFile), StandardCharsets.UTF_8)) {
            settingsWriter.write(settingsString);
        } catch (Throwable e) {
            log.error("Error writing project '" + getName() + "' setting to "  + settingsFile.toAbsolutePath(), e);
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

    @Override
    public void setResourceProperties(IResource resource, Map<String, Object> props) {
        loadMetadata();
        synchronized (metadataSync) {
            String filePath = resource.getProjectRelativePath().toString();
            Map<String, Object> resProps = resourceProperties.get(filePath);
            if (resProps == null) {
                if (props.isEmpty()) {
                    // No props + no new value - ignore
                    return;
                }
                resProps = new LinkedHashMap<>();
                resourceProperties.put(filePath, resProps);
            }
            boolean hasChanges = false;
            for (Map.Entry<String, Object> pe : props.entrySet()) {
                if (pe.getValue() == null) {
                    if (resProps.remove(pe.getKey()) != null) {
                        hasChanges = true;
                    }
                } else {
                    Object oldValue = resProps.get(pe.getKey());
                    if (!CommonUtils.equalObjects(oldValue, pe.getValue())) {
                        resProps.put(pe.getKey(), pe.getValue());
                        hasChanges = true;
                    }
                }
            }
            if (!hasChanges) {
                return;
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
        if (isInMemory()) {
            return;
        }
        ensureOpen();
        synchronized (metadataSync) {
            if (resourceProperties != null) {
                return;
            }

            Path mdFile = getMetadataPath().resolve(METADATA_STORAGE_FILE);
            if (Files.exists(mdFile) && mdFile.toFile().length() > 0) {
                // Parse metadata
                Map<String, Map<String, Object>> mdCache = new TreeMap<>();
                try (Reader mdReader = Files.newBufferedReader(mdFile, StandardCharsets.UTF_8)) {
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
                    log.error("Error reading project '" + getName() + "' metadata from "  + mdFile.toAbsolutePath(), e);
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

        Path mdConfig = getMetadataPath().resolve(METADATA_STORAGE_FILE);
        if (!Files.exists(mdConfig)) {
            // Migrate
            log.debug("Migrate Eclipse resource properties to the project metadata (" + mdConfig.toAbsolutePath() + ")");
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
        if (inMemory) {
            return;
        }
        synchronized (metadataSync) {
            metadataSyncJob.schedule(100);
        }
    }

    void removeResourceFromCache(IPath path) {
        boolean cacheChanged = false;
        synchronized (metadataSync) {
            if (resourceProperties != null) {
                cacheChanged = (resourceProperties.remove(path.toString()) != null);
            }
        }
        if (cacheChanged) {
            flushMetadata();
        }
    }

    void updateResourceCache(IPath oldPath, IPath newPath) {
        boolean cacheChanged = false;
        synchronized (metadataSync) {
            if (resourceProperties != null) {
                Map<String, Object> props = resourceProperties.remove(oldPath.toString());
                if (props != null) {
                    resourceProperties.put(newPath.toString(), props);
                    cacheChanged = true;
                }
            }
        }
        if (cacheChanged) {
            flushMetadata();
        }
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

            ContentUtils.makeFileBackup(getMetadataFolder(false).resolve(METADATA_STORAGE_FILE));

            synchronized (metadataSync) {
                Path mdFile = getMetadataPath().resolve(METADATA_STORAGE_FILE);
                if (CommonUtils.isEmpty(resourceProperties) && !Files.exists(mdFile)) {
                    // Nothing to save and metadata file doesn't exist
                    return Status.OK_STATUS;
                }
                try {
                    if (!CommonUtils.isEmpty(resourceProperties)) {
                        try (Writer mdWriter = Files.newBufferedWriter(mdFile, StandardCharsets.UTF_8)) {
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
