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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Configuration files manager on FS
 */
public class DataSourceConfigurationManagerNIO implements DataSourceConfigurationManager {

    private static final Log log = Log.getLog(DataSourceConfigurationManagerNIO.class);

    @NotNull
    private final DBPProject project;

    public DataSourceConfigurationManagerNIO(@NotNull DBPProject project) {
        this.project = project;
    }

    private Path getConfigurationPath(boolean create) {
        return project.getMetadataFolder(create);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public List<DBPDataSourceConfigurationStorage> getConfigurationStorages() {
        List<DBPDataSourceConfigurationStorage> storages = new ArrayList<>();
        Path metadataFolder = project.getMetadataFolder(false);
        boolean modernFormat = false;
        if (Files.exists(metadataFolder)) {
            try (Stream<Path> list = Files.list(metadataFolder)) {
                List<Path> mdFiles = list
                    .filter(path -> !Files.isDirectory(path) && Files.exists(path))
                    .toList();
                for (Path res : mdFiles) {
                    String fileName = res.getFileName().toString();
                    if (fileName.startsWith(DataSourceRegistry.MODERN_CONFIG_FILE_PREFIX) &&
                        fileName.endsWith(DataSourceRegistry.MODERN_CONFIG_FILE_EXT))
                    {
                        storages.add(new DataSourceFileStorage(res, false, fileName.equals(DataSourceRegistry.MODERN_CONFIG_FILE_NAME)));
                        modernFormat = true;
                    }
                }
            } catch (IOException e) {
                log.error("Error during project files read", e);
            }
        }
        if (!modernFormat) {
            if (Files.exists(project.getAbsolutePath())) {
                try (Stream<Path> list = Files.list(project.getAbsolutePath())) {
                    // Legacy way (search config.xml in project folder)
                    List<Path> mdFiles = list
                        .filter(path -> !Files.isDirectory(path) && Files.exists(path))
                        .toList();
                    for (Path res : mdFiles) {
                        String fileName = res.getFileName().toString();
                        if (fileName.startsWith(DBPDataSourceRegistry.LEGACY_CONFIG_FILE_PREFIX) && fileName.endsWith(DBPDataSourceRegistry.LEGACY_CONFIG_FILE_EXT)) {
                            storages.add(new DataSourceFileStorage(res, true, fileName.equals(DataSourceRegistry.LEGACY_CONFIG_FILE_NAME)));
                        }
                    }
                } catch (IOException e) {
                    log.error("Error during legacy project files read", e);
                }
            }
        }
        if (storages.isEmpty()) {
            storages.add(
                new DataSourceFileStorage(
                    metadataFolder.resolve(DataSourceRegistry.MODERN_CONFIG_FILE_NAME), false, true));
        }
        return storages;
    }

    @Override
    public InputStream readConfiguration(@NotNull String name, Collection<String> dataSourceIds) throws IOException {
        Path path = getConfigurationPath(false).resolve(name);
        if (Files.notExists(path)) {
            // maybe it's .dbeaver-data-sources*.xml in the project folder (DBeaver < 6.1.3 (Legacy))
            path = project.getAbsolutePath().resolve(name);
        }
        if (Files.notExists(path)) {
            return null;
        }
        return Files.newInputStream(path);
    }

    @Override
    public void writeConfiguration(@NotNull String name, @Nullable byte[] data) throws IOException {
        Path configFile = getConfigurationPath(true).resolve(name);
        ContentUtils.makeFileBackup(configFile);

        if (data == null || data.length == 0) {
            if (Files.exists(configFile)) {
                try {
                    Files.delete(configFile);
                } catch (IOException e) {
                    log.debug("Error deleting file " + configFile.toAbsolutePath(), e);
                }
            }
        } else {
            Files.write(configFile, data);
        }
    }
}
