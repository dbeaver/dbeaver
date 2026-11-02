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
package org.jkiss.dbeaver.ext.cdata.registry;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.function.ThrowableSupplier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

record CDataDriverSettings(
    @SerializedName("datatype_id") @NotNull String dataTypeId,
    @SerializedName("icon_driver") @Nullable String iconDriver
) {
    static final String FILE_NAME = "cdata-driver-settings.json";
    static final String URL = "https://dbeaver.com/info/cdata-driver-settings.json";
    private static final int CONNECTION_TIMEOUT = 10_000;
    private static final Log log = Log.getLog(CDataDriverSettings.class);

    @NotNull
    static Map<String, CDataDriverSettings> load(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String settingsUrl,
        @NotNull Path file
    ) throws InterruptedException {
        return load(monitor, file, () -> WebUtils.openURLConnection(
            monitor, settingsUrl, null, null, "GET", 1, CONNECTION_TIMEOUT, null));
    }

    @NotNull
    static Map<String, CDataDriverSettings> load(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Path file,
        @NotNull ThrowableSupplier<URLConnection, IOException> connectionFactory
    ) throws InterruptedException {
        var cached = load(file);
        checkCanceled(monitor);
        try {
            URLConnection connection = connectionFactory.get();
            try {
                Files.createDirectories(file.getParent());
                Path temporary = Files.createTempFile(file.getParent(), FILE_NAME, ".tmp");
                try {
                    try (var input = connection.getInputStream(); var output = Files.newOutputStream(temporary)) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            checkCanceled(monitor);
                            output.write(buffer, 0, count);
                        }
                    }
                    Map<String, CDataDriverSettings> settings;
                    try (Reader reader = Files.newBufferedReader(temporary)) {
                        settings = read(reader);
                    }
                    checkCanceled(monitor);
                    try {
                        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return settings;
                } finally {
                    Files.deleteIfExists(temporary);
                }
            } finally {
                if (connection instanceof HttpURLConnection httpConnection) {
                    httpConnection.disconnect();
                }
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            log.warn("Error updating CData driver settings; using the cached settings", e);
            return cached;
        }
    }

    @NotNull
    static Map<String, CDataDriverSettings> load(@NotNull Path file) {
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                return read(reader);
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                log.warn("Error reading CData driver settings", e);
            }
        }
        return loadBundled();
    }

    @NotNull
    private static Map<String, CDataDriverSettings> loadBundled() {
        try (var input = CDataDriverSettings.class.getResourceAsStream(FILE_NAME)) {
            if (input == null) {
                throw new IOException("Bundled CData driver settings are missing");
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return read(reader);
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            log.error("Error reading bundled CData driver settings", e);
            return Map.of();
        }
    }

    @NotNull
    private static Map<String, CDataDriverSettings> read(@NotNull Reader reader) {
        try {
            Map<String, CDataDriverSettings> settings = JSONUtils.GSON.fromJson(
                reader, new TypeToken<Map<String, CDataDriverSettings>>() { }.getType());
            if (settings == null || settings.isEmpty()) {
                throw new IllegalStateException("CData driver settings are empty");
            }
            for (var entry : settings.entrySet()) {
                var value = entry.getValue();
                if (entry.getKey().isBlank() || value == null || value.dataTypeId() == null || value.dataTypeId().isBlank()
                    || value.iconDriver() != null && !value.iconDriver().matches("[^:]+:[^:]+")) {
                    throw new IllegalStateException("Invalid CData driver settings: " + entry.getKey());
                }
            }
            return Map.copyOf(settings);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Error parsing CData driver settings", e);
        }
    }

    private static void checkCanceled(@NotNull DBRProgressMonitor monitor) throws InterruptedException {
        if (monitor.isCanceled()) {
            throw new InterruptedException("CData driver settings loading canceled");
        }
    }
}
