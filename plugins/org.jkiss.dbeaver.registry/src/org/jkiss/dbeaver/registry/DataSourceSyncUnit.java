/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.model.sync.DBPSyncUnit;
import org.jkiss.dbeaver.registry.internal.RegistryMessages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Project connections: data source list, their credentials and project settings.
 */
public class DataSourceSyncUnit implements DBPSyncUnit {

    private static final Log log = Log.getLog(DataSourceSyncUnit.class);

    private static final List<String> CONFIG_PREFIXES = List.of(
        DBPDataSourceRegistry.MODERN_CONFIG_FILE_PREFIX,
        DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_PREFIX);

    @NotNull
    @Override
    public String getId() {
        return "connections";
    }

    @NotNull
    @Override
    public String getName() {
        return RegistryMessages.data_source_sync_unit_name;
    }

    @NotNull
    @Override
    public DBPSyncScope getScope() {
        return DBPSyncScope.PROJECT;
    }

    @NotNull
    @Override
    public Map<String, byte[]> read(@NotNull DBPSyncTarget target) throws DBException {
        Path folder = target.root().resolve(DBPProject.METADATA_FOLDER);
        if (!Files.isDirectory(folder)) {
            return Map.of();
        }
        Map<String, byte[]> resources = new LinkedHashMap<>();
        try (Stream<Path> list = Files.list(folder)) {
            for (Path file : list.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (isSyncedFile(name)) {
                    resources.put(name, Files.readAllBytes(file));
                }
            }
        } catch (IOException e) {
            throw new DBException("Error reading " + folder, e);
        }
        return resources;
    }

    @Override
    public void write(@NotNull DBPSyncTarget target, @NotNull Map<String, byte[]> resources) throws DBException {
        Path folder = target.root().resolve(DBPProject.METADATA_FOLDER);
        for (Map.Entry<String, byte[]> resource : resources.entrySet()) {
            String name = resource.getKey();
            if (!isSyncedFile(name)) {
                log.debug("Skip unexpected connections resource " + name);
                continue;
            }
            if (BaseProjectImpl.SETTINGS_STORAGE_FILE.equals(name)) {
                applySettings(target, resource.getValue());
                continue;
            }
            Path file = resolve(target, name);
            try {
                Files.createDirectories(file.getParent());
                Files.write(file, resource.getValue());
            } catch (IOException e) {
                throw new DBException("Error writing " + file, e);
            }
        }
        if (Files.isDirectory(folder)) {
            try (Stream<Path> list = Files.list(folder)) {
                for (Path file : list.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString();
                    if (isManagedFile(name) && !resources.containsKey(name)) {
                        Files.delete(file);
                    }
                }
            } catch (IOException e) {
                throw new DBException("Error cleaning up " + folder, e);
            }
        }
    }

    private static void applySettings(@NotNull DBPSyncTarget target, @NotNull byte[] content) {
        DBPProject project = target.project();
        if (project == null) {
            return;
        }
        Map<String, Object> settings = JSONUtils.GSON.fromJson(new String(content, StandardCharsets.UTF_8), Map.class);
        if (settings != null) {
            project.setProjectProperties(settings);
        }
    }

    private static boolean isManagedFile(@NotNull String name) {
        return isSyncedFile(name) && !BaseProjectImpl.SETTINGS_STORAGE_FILE.equals(name);
    }

    private static boolean isSyncedFile(@NotNull String name) {
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            return false;
        }
        if (BaseProjectImpl.SETTINGS_STORAGE_FILE.equals(name)) {
            return true;
        }
        if (!name.endsWith(DBPDataSourceRegistry.MODERN_CONFIG_FILE_EXT)) {
            return false;
        }
        for (String prefix : CONFIG_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static Path resolve(@NotNull DBPSyncTarget target, @NotNull String name) {
        return target.root().resolve(DBPProject.METADATA_FOLDER).resolve(name);
    }
}
