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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ProjectMetadata implements DBPProject {
    private static final Log log = Log.getLog(ProjectMetadata.class);

    public static final String METADATA_FOLDER = ".dbeaver";
    public static final String METADATA_STORAGE_FILE = "project-metadata.json";

    public enum ProjectFormat {
        UNKNOWN,    // Project is not open or corrupted
        LEGACY,     // Old format (before 6.1 version
        MODERN,     // 6.1+ version
    }

    private static Gson METADATA_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create();

    private final DBPWorkspace workspace;
    private final IProject project;

    private ProjectFormat format = ProjectFormat.UNKNOWN;
    private volatile DataSourceRegistry dataSourceRegistry;
    private Map<String, Map<String, Object>> resourceProperties;
    private final Object metadataSync = new Object();

    public ProjectMetadata(DBPWorkspace workspace, IProject project) {
        this.workspace = workspace;
        this.project = project;
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
                throw new IllegalStateException("Error opening project", e);
            }
        }

        File dsConfig = new File(getAbsolutePath(), DataSourceRegistry.CONFIG_FILE_NAME);
        if (dsConfig.exists()) {
            format = ProjectFormat.LEGACY;
        } else {
            format = ProjectFormat.MODERN;
        }

        // Check project structure and migrate
        checkAndUpdateProjectStructure();
    }

    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        ensureOpen();
        if (dataSourceRegistry == null) {
            dataSourceRegistry = new DataSourceRegistry(workspace.getPlatform(), this);
        }
        return dataSourceRegistry;
    }

    @Override
    public Object getResourceProperty(IResource resource, String propName) {
        loadMetadata();
        Map<String, Object> resProps = resourceProperties.get(resource.getLocation().toString());
        if (resProps != null) {
            return resProps.get(propName);
        }
        return null;
    }

    @Override
    public Map<String, Object> getResourceProperties(IResource resource) {
        loadMetadata();
        return resourceProperties.get(resource.getLocation().toString());
    }

    @Override
    public void setResourceProperties(IResource resource, Map<String, Object> properties) {
        flushMetadata();
    }

    public void dispose() {
        dataSourceRegistry.dispose();
    }

    @NotNull
    public File getMetadataPath() {
        return new File(getAbsolutePath(), METADATA_FOLDER);
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
            if (mdFile.exists()) {
                // Parse metadata
                Map<String, Map<String, Object>> mdCache = new TreeMap<>();
                try (Reader mdReader = new InputStreamReader(new FileInputStream(mdFile), StandardCharsets.UTF_8)) {
                    try (JsonReader jsonReader = METADATA_GSON.newJsonReader(mdReader)) {
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
                            }
                            jsonReader.endObject();
                            mdCache.put(resourceName, resProperties);
                        }

                        jsonReader.endObject();

                        resourceProperties = mdCache;
                    }
                } catch (IOException e) {
                    log.error("Error reading project '" + getName() + "' metadata", e);
                }

            } else {
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

        File dsConfig = new File(getAbsolutePath(), DataSourceRegistry.CONFIG_FILE_NAME);
        if (dsConfig.exists()) {

        }
    }

    private void flushMetadata() {
        synchronized (metadataSync) {
            File mdFile = new File(getMetadataPath(), METADATA_STORAGE_FILE);
            if (CommonUtils.isEmpty(resourceProperties) && !mdFile.exists()) {
                // Nothing to save and metadata file doesn't exist
                return;
            }
            try {
                IOUtils.makeFileBackup(mdFile);

                if (CommonUtils.isEmpty(resourceProperties)) {
                    if (mdFile.exists() && !mdFile.delete()) {
                        log.error("Error deleting project metadata file " + mdFile.getAbsolutePath());
                    }
                    return;
                }
                try (Writer mdWriter = new OutputStreamWriter(new FileOutputStream(mdFile), StandardCharsets.UTF_8)) {
                    try (JsonWriter jsonWriter = METADATA_GSON.newJsonWriter(mdWriter)) {
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
                                    jsonWriter.value((Number)value);
                                } else if (value instanceof Boolean) {
                                    jsonWriter.value((Boolean)value);
                                } else {
                                    jsonWriter.value(CommonUtils.toString(value));
                                }
                            }
                            jsonWriter.endObject();
                        }
                        jsonWriter.endObject();
                        jsonWriter.flush();
                    }
                }

            } catch (IOException e) {
                log.error("Error flushing project metadata", e);
            }

        }
    }

}
