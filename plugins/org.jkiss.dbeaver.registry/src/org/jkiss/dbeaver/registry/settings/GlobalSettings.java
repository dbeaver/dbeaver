/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.registry.settings;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.BasePlatformImpl;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * GlobalSettings
 */
public class GlobalSettings {

    private static final String DBEAVER_CONFIG_FOLDER = "settings";
    private static final String DBEAVER_CONFIG_FILE = "global-settings.ini";
    private static final String DBEAVER_PROP_LANGUAGE = "nl";

    private static final Log log = Log.getLog(GlobalSettings.class);

    private static GlobalSettings instance;

    private Properties properties;

    public static synchronized GlobalSettings getInstance() {
        if (instance == null) {
            instance = new GlobalSettings();
        }
        return instance;
    }

    public synchronized String getGlobalProperty(@NotNull String key) {
        loadProperties();
        return properties.getProperty(key);
    }

    public synchronized void setGlobalProperty(@NotNull String key, @Nullable String value) {
        loadProperties();

        if (value != null) {
            properties.setProperty(key, value);
        } else {
            properties.remove(key);
        }

        saveProperties();
    }

    private void loadProperties() {
        if (properties != null) {
            return;
        }
        properties = new Properties();

        final Path file = getPropertiesFile();

        try {
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    properties.load(reader);
                }
            }
        } catch (IOException e) {
            log.error("Error loading global properties", e);
        }
    }

    @NotNull
    private static Path getPropertiesFile() {
        final Path root = Path.of(RuntimeUtils.getWorkingDirectory(BasePlatformImpl.DBEAVER_DATA_DIR));
        return root.resolve(DBEAVER_CONFIG_FOLDER).resolve(DBEAVER_CONFIG_FILE);
    }

    private void saveProperties() {
        if (properties == null) {
            properties = new Properties();
        }
        try {
            final Path file = getPropertiesFile();
            // Ensure the config directory exists
            Files.createDirectories(file.getParent());

            try (Writer writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "DBeaver configuration");
            }
        } catch (IOException e) {
            log.error("Error saving global properties", e);
        }
    }


}
