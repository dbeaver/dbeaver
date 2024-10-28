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
package org.jkiss.dbeaver.model.impl.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.fs.DBFFileSystemManager;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretSubject;
import org.jkiss.dbeaver.model.secret.DBSValueEncryptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseProjectImpl implements DBPProject, DBSSecretSubject {

    private static final Log log = Log.getLog(BaseProjectImpl.class);

    public static final String SETTINGS_STORAGE_FILE = "project-settings.json";
    public static final String METADATA_STORAGE_FILE = "project-metadata.json";
    public static final String PROP_PROJECT_ID = "id";

    public enum ProjectFormat {
        UNKNOWN,    // Project is not open or corrupted
        LEGACY,     // Old format (before 6.1 version
        MODERN,     // 6.1+ version
    }

    public static final Gson METADATA_GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .create();

    private static final byte[] LOCAL_KEY_CACHE = new byte[] { -70, -69, 74, -97, 119, 74, -72, 83, -55, 108, 45, 101, 61, -2, 84, 74 };

    @NotNull
    private final DBPWorkspace workspace;
    @NotNull
    private final SMSessionContext sessionContext;

    private volatile ProjectFormat format = ProjectFormat.UNKNOWN;
    private volatile DBPDataSourceRegistry dataSourceRegistry;
    private volatile DBFFileSystemManager fileSystemManager;
    private volatile Map<String, String> runtimeProperties = new ConcurrentHashMap<>();
    private volatile Map<String, Object> properties;
    protected volatile Map<String, Map<String, Object>> resourceProperties;
    private UUID projectID;

    protected final Object metadataSync = new Object();
    protected final Object resourcesSync = new Object();
    private ProjectSyncJob metadataSyncJob;

    private boolean inMemory;

    public BaseProjectImpl(@NotNull DBPWorkspace workspace, @Nullable SMSessionContext sessionContext) {
        this.workspace = workspace;
        this.sessionContext = sessionContext == null ? workspace.getAuthContext() : sessionContext;
    }

    public void setInMemory(boolean inMemory) {
        this.inMemory = inMemory;
    }

    public boolean isInMemory() {
        return inMemory;
    }

    @NotNull
    @Override
    public String getId() {
        return getName();
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return getName();
    }

    @NotNull
    @Override
    public DBPWorkspace getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public UUID getProjectID() {
        if (projectID == null) {
            if (isInMemory()) {
                // anonymous project does not have properties file, so we need to generate random uuid
                projectID = UUID.randomUUID();
                return projectID;
            }
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
    protected Path getMetadataPath() {
        return getAbsolutePath().resolve(METADATA_FOLDER);
    }
    
    @Override
    public boolean isRegistryLoaded() {
        return dataSourceRegistry != null;
    }

    @Override
    public boolean isEncryptedProject() {
        return false;
    }

    @Override
    public boolean isPrivateProject() {
        return true;
    }

    @NotNull
    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        if (dataSourceRegistry == null) {
            Runnable registryOpener = () -> {
                if (dataSourceRegistry == null) {
                    synchronized (metadataSync) {
                        ensureOpen();
                        if (dataSourceRegistry == null) {
                            dataSourceRegistry = createDataSourceRegistry();
                        }
                    }
                }
            };
            if (DBWorkbench.isDistributed() || DBWorkbench.getPlatform().getApplication().isMultiuser()) {
                // Run it directly in distributed UI (because it may trigger other conflicting
                // UI interactions which may freeze app)
                registryOpener.run();
            } else {
                RuntimeUtils.runTask(monitor -> registryOpener.run(), "Load registry", 0);
            }
        }
        return dataSourceRegistry;
    }

    @NotNull
    protected abstract DBPDataSourceRegistry createDataSourceRegistry();

    ////////////////////////////////////////////////////////
    // Secure storage

    @NotNull
    @Override
    public DBSValueEncryptor getValueEncryptor() throws DBException {
        SecretKey key = getLocalSecretKey();
        if (key == null) {
            throw new IllegalStateException("Can't obtain secret key");
        }
        return new DefaultValueEncryptor(key);
    }

    @Nullable
    protected SecretKey getLocalSecretKey() {
        return new SecretKeySpec(LOCAL_KEY_CACHE, DefaultValueEncryptor.KEY_ALGORITHM);
    }

    @NotNull
    @Override
    public SMSessionContext getSessionContext() {
        return sessionContext;
    }

    @NotNull
    @Override
    public DBFFileSystemManager getFileSystemManager() {
        if (fileSystemManager == null) {
            synchronized (this) {
                if (fileSystemManager == null) {
                    fileSystemManager = new DBFFileSystemManager(this);
                }
            }
        }

        return fileSystemManager;
    }

    ////////////////////////////////////////////////////////
    // Properties

    @Nullable
    @Override
    public Object getProjectProperty(String propName) {
        synchronized (this) {
            loadProperties();
            return properties.get(propName);
        }
    }

    @Override
    public void setProjectProperty(@NotNull String propName, @Nullable Object propValue) {
        synchronized (metadataSync) {
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
        if (isInMemory() || DBWorkbench.isDistributed()) {
            properties = new LinkedHashMap<>();
            return;
        }

        synchronized (metadataSync) {
            Path settingsFile = getMetadataPath().resolve(SETTINGS_STORAGE_FILE);

            if (fileExistsAndNonEmpty(settingsFile)) {
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
        if (isInMemory() || DBWorkbench.isDistributed()) {
            return;
        }

        Path settingsFile = getMetadataFolder(true).resolve(SETTINGS_STORAGE_FILE);
        String settingsString = METADATA_GSON.toJson(properties);

        try {
            Path configFolder = settingsFile.getParent();
            if (!Files.exists(configFolder)) {
                Files.createDirectories(configFolder);
            }

            Files.writeString(settingsFile, settingsString);
        } catch (Exception e) {
            log.error("Error writing project '" + getName() + "' setting to "  + settingsFile.toAbsolutePath(), e);
        }
    }

    ////////////////////////////////////////////////////////
    // Resources

    @NotNull
    @Override
    public String[] findResources(@NotNull Map<String, ?> properties) throws DBException {
        loadMetadata();

        synchronized (resourcesSync) {
            final List<String> resources = new ArrayList<>();

            for (var resource : resourceProperties.entrySet()) {
                boolean containsRequiredProperties = true;
                final Map<String, Object> props = resource.getValue();
                for (var property : properties.entrySet()) {
                    final String propName = property.getKey();
                    final Object propValue = property.getValue();

                    if (!props.containsKey(propName) || !Objects.equals(props.get(propName), propValue)) {
                        containsRequiredProperties = false;
                        break;
                    }
                }
                if (containsRequiredProperties) {
                    resources.add(resource.getKey());
                }
            }

            return resources.toArray(String[]::new);
        }
    }

    @Nullable
    @Override
    public Object getResourceProperty(@NotNull String resourcePath, @NotNull String propName) {
        loadMetadata();
        resourcePath = CommonUtils.normalizeResourcePath(resourcePath);
        synchronized (resourcesSync) {
            Map<String, Object> resProps = resourceProperties.get(resourcePath);
            if (resProps != null) {
                return resProps.get(propName);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Map<String, Object> getResourceProperties(@NotNull String resourcePath) {
        loadMetadata();
        resourcePath = CommonUtils.normalizeResourcePath(resourcePath);
        synchronized (resourcesSync) {
            return resourceProperties.get(resourcePath);
        }
    }

    @Override
    public void setResourceProperties(@NotNull String resourcePath, @NotNull Map<String, Object> newProps) {
        loadMetadata();
        resourcePath = CommonUtils.normalizeResourcePath(resourcePath);
        synchronized (resourcesSync) {
            this.resourceProperties.put(resourcePath, new LinkedHashMap<>(newProps));
        }
        flushMetadata();
    }

    @Override
    public void setResourceProperty(@NotNull String resourcePath, @NotNull String propName, @Nullable Object propValue) {
        loadMetadata();
        resourcePath = CommonUtils.normalizeResourcePath(resourcePath);
        synchronized (resourcesSync) {
            Map<String, Object> resProps = resourceProperties.get(resourcePath);
            if (resProps == null) {
                if (propValue == null) {
                    // No props + no new value - ignore
                    return;
                }
                resProps = new LinkedHashMap<>();
                resourceProperties.put(resourcePath, resProps);
            }
            if (propValue == null) {
                if (resProps.remove(propName) == null) {
                    if (resProps.isEmpty()) {
                        resourceProperties.remove(resourcePath);
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
    public void moveResourceProperties(@NotNull String oldResourcePath, @NotNull String newResourcePath) {
        loadMetadata();
        oldResourcePath = CommonUtils.normalizeResourcePath(oldResourcePath);
        newResourcePath = CommonUtils.normalizeResourcePath(newResourcePath);
        synchronized (resourcesSync) {
            Map<String, Object> resProps = resourceProperties.remove(oldResourcePath);
            if (resProps != null) {
                resourceProperties.put(newResourcePath, resProps);
            }
        }
        flushMetadata();
    }

    @Override
    public void refreshProject(DBRProgressMonitor monitor) {

    }

    public boolean resetResourceProperties(@NotNull String resourcePath) {
        loadMetadata();
        resourcePath = CommonUtils.normalizeResourcePath(resourcePath);
        boolean hadProperties;
        synchronized (resourcesSync) {
            hadProperties = resourceProperties.remove(resourcePath) != null;
        }
        if (hadProperties) {
            flushMetadata();
        }
        return hadProperties;
    }

    protected void setResourceProperties(Map<String, Map<String, Object>> resourceProperties) {
        synchronized (resourcesSync) {
            this.resourceProperties = resourceProperties;
        }
    }

    ////////////////////////////////////////////////////////
    // Realm

    @Override
    public boolean hasRealmPermission(@NotNull String permission) {
        return true;
    }

    @Override
    public boolean supportsRealmFeature(@NotNull String feature) {
        return true;
    }

    ////////////////////////////////////////////////////////
    // Misc

    public void dispose() {
        if (dataSourceRegistry != null) {
            dataSourceRegistry.dispose();
            dataSourceRegistry = null;
        }
        if (fileSystemManager != null) {
            fileSystemManager.close();
            fileSystemManager = null;
        }
    }

    public ProjectFormat getFormat() {
        return format;
    }

    protected void setFormat(ProjectFormat format) {
        this.format = format;
    }

    protected void loadMetadata() {
        if (isInMemory()) {
            return;
        }
        ensureOpen();
        synchronized (resourcesSync) {
            if (resourceProperties != null) {
                return;
            }

            Path mdFile = getMetadataPath().resolve(METADATA_STORAGE_FILE);
            if (fileExistsAndNonEmpty(mdFile)) {
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
                                        Object propValue = switch (jsonReader.peek()) {
                                            case NUMBER -> jsonReader.nextDouble();
                                            case BOOLEAN -> jsonReader.nextBoolean();
                                            case NULL -> null;
                                            default -> jsonReader.nextString();
                                        };
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

    protected void flushMetadata() {
        if (inMemory) {
            return;
        }
        synchronized (metadataSync) {
            if (metadataSyncJob == null) {
                metadataSyncJob = new ProjectSyncJob();
            }
            // if this is a web app, we want to wait the sync job
            if (DBWorkbench.getPlatform().getApplication().isMultiuser()) {
                metadataSyncJob.run(new VoidProgressMonitor());
            } else {
                metadataSyncJob.schedule(100);
            }
        }
    }

    @Override
    public String getRuntimeProperty(@NotNull String key) {
        return runtimeProperties.get(key);
    }

    @Override
    public void setRuntimeProperty(@NotNull String key, String value) {
        runtimeProperties.put(key, value);
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
            setName("Project '" + BaseProjectImpl.this.getName() + "' sync job");

            ContentUtils.makeFileBackup(getMetadataFolder(false).resolve(METADATA_STORAGE_FILE));

            synchronized (metadataSync) {
                Path mdFile = getMetadataFolder(true).resolve(METADATA_STORAGE_FILE);
                if (CommonUtils.isEmpty(resourceProperties) && !Files.exists(mdFile)) {
                    // Nothing to save and metadata file doesn't exist
                    return Status.OK_STATUS;
                }
                try (Writer mdWriter = Files.newBufferedWriter(mdFile, StandardCharsets.UTF_8);
                     JsonWriter jsonWriter = METADATA_GSON.newJsonWriter(mdWriter)
                ) {
                    jsonWriter.beginObject();

                    jsonWriter.name("resources");
                    jsonWriter.beginObject();
                    for (var resEntry : resourceProperties.entrySet()) {
                        jsonWriter.name(resEntry.getKey());
                        jsonWriter.beginObject();
                        Map<String, Object> resProps = resEntry.getValue();
                        for (var propEntry : resProps.entrySet()) {
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
                } catch (IOException e) {
                    log.error("Error flushing project metadata", e);
                }
            }

            return Status.OK_STATUS;
        }
    }

    @Nullable
    @Override
    public DBNModel getNavigatorModel() {
        return null;
    }

    @Override
    public String getSecretSubjectId() {
        return "project/" + getId();
    }

    private boolean fileExistsAndNonEmpty(@NotNull Path path) {
        boolean fileNotEmpty = false;
        if (Files.exists(path)) {
            try {
                return Files.size(path) > 0;
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return fileNotEmpty;
    }

}
