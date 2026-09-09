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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.function.ThrowableFunction;

import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CDataDriverCatalog {
    private static final Log log = Log.getLog(CDataDriverCatalog.class);
    public static final int SCHEMA_VERSION = 2;
    private static final String CATALOG_FILE_NAME = "cdata-drivers.json";
    private static final String CATALOG_URL = "https://dbeaver.io/product/cdata-drivers.json";
    private static final String CATALOG_URL_PROPERTY = "cdataDriversURL";
    private static final int CONNECTION_TIMEOUT = 10_000;
    private static volatile List<CDataDriverInfo> drivers = List.of();
    private static final AbstractJob UPDATE_JOB = new AbstractJob("Load CData driver catalog") {
        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            var product = Platform.getProduct();
            String catalogUrl = product == null ? null : product.getProperty(CATALOG_URL_PROPERTY);
            if (catalogUrl == null || catalogUrl.isBlank()) {
                catalogUrl = CATALOG_URL;
            }
            try {
                drivers = load(monitor, catalogUrl, CDataDriverLoaderDescriptor.getStoragePath().resolve(CATALOG_FILE_NAME));
                return Status.OK_STATUS;
            } catch (InterruptedException e) {
                return Status.CANCEL_STATUS;
            } catch (RuntimeException e) {
                log.error("Error initializing CData driver catalog", e);
                return Status.CANCEL_STATUS;
            }
        }
    };

    private CDataDriverCatalog() {
    }

    public static void start() {
        UPDATE_JOB.setSystem(true);
        UPDATE_JOB.schedule();
    }

    public static void stop() {
        UPDATE_JOB.cancel();
    }

    @NotNull
    public static List<CDataDriverInfo> load() {
        try {
            UPDATE_JOB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading CData driver catalog", e);
        }
        return drivers;
    }

    @NotNull
    static List<CDataDriverInfo> load(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String catalogUrl,
        @NotNull Path cacheFile
    ) throws InterruptedException {
        return load(monitor, cacheFile, method -> WebUtils.openURLConnection(
            monitor, catalogUrl, null, null, method, 1, CONNECTION_TIMEOUT, null));
    }

    @NotNull
    static List<CDataDriverInfo> load(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Path cacheFile,
        @NotNull ThrowableFunction<String, URLConnection, IOException> connectionFactory
    ) throws InterruptedException {
        List<CDataDriverInfo> cachedDrivers = List.of();
        if (Files.isRegularFile(cacheFile)) {
            try {
                cachedDrivers = read(cacheFile);
            } catch (IOException | IllegalStateException | IllegalArgumentException e) {
                log.warn("Error reading cached CData driver catalog", e);
            }
        }
        try {
            checkCanceled(monitor);
            if (!cachedDrivers.isEmpty()) {
                URLConnection connection = connectionFactory.apply("HEAD");
                try {
                    long lastModified = connection.getLastModified();
                    if (lastModified > 0 && lastModified == Files.getLastModifiedTime(cacheFile).toMillis()) {
                        return cachedDrivers;
                    }
                } finally {
                    disconnect(connection);
                }
            }
            checkCanceled(monitor);
            return download(monitor, cacheFile, connectionFactory);
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            log.warn("Error updating CData driver catalog; using the cached catalog", e);
            return cachedDrivers;
        }
    }

    @NotNull
    private static List<CDataDriverInfo> download(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Path cacheFile,
        @NotNull ThrowableFunction<String, URLConnection, IOException> connectionFactory
    ) throws IOException, InterruptedException {
        Files.createDirectories(cacheFile.getParent());
        Path temporary = Files.createTempFile(cacheFile.getParent(), CATALOG_FILE_NAME, ".tmp");
        try {
            URLConnection connection = connectionFactory.apply("GET");
            try {
                try (var input = connection.getInputStream(); var output = Files.newOutputStream(temporary)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        checkCanceled(monitor);
                        output.write(buffer, 0, count);
                    }
                }
                List<CDataDriverInfo> downloadedDrivers = read(temporary);
                Files.setLastModifiedTime(temporary, FileTime.fromMillis(connection.getLastModified()));
                checkCanceled(monitor);
                try {
                    Files.move(temporary, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return downloadedDrivers;
            } finally {
                disconnect(connection);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @NotNull
    private static List<CDataDriverInfo> read(@NotNull Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return validate(JSONUtils.GSON.fromJson(reader, CatalogFile.class));
        } catch (JsonParseException e) {
            throw new IllegalStateException("Error reading CData driver catalog", e);
        }
    }

    private static void checkCanceled(@NotNull DBRProgressMonitor monitor) throws InterruptedException {
        if (monitor.isCanceled()) {
            throw new InterruptedException("CData driver catalog loading canceled");
        }
    }

    private static void disconnect(@NotNull URLConnection connection) {
        if (connection instanceof HttpURLConnection httpConnection) {
            httpConnection.disconnect();
        }
    }

    @NotNull
    private static List<CDataDriverInfo> validate(@Nullable CatalogFile catalog) {
        if (catalog == null) {
            throw new IllegalStateException("CData driver catalog is empty");
        }
        if (catalog.schemaVersion != SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported CData driver catalog schema: " + catalog.schemaVersion);
        }
        if (catalog.drivers == null || catalog.drivers.isEmpty()) {
            throw new IllegalStateException("CData driver catalog contains no drivers");
        }

        Set<String> dataSources = new HashSet<>();
        Set<String> artifactIds = new HashSet<>();
        for (CDataDriverInfo driver : catalog.drivers) {
            if (driver == null) {
                throw new IllegalStateException("CData driver catalog contains an empty entry");
            }
            requireText(driver.dataSource(), "dataSource");
            requireText(driver.artifactId(), "artifactId");
            if (!driver.artifactId().endsWith(CDataDriverInfo.ARTIFACT_SUFFIX) ||
                driver.artifactId().length() == CDataDriverInfo.ARTIFACT_SUFFIX.length()) {
                throw new IllegalStateException(
                    "CData Maven artifact must be named <source>" + CDataDriverInfo.ARTIFACT_SUFFIX +
                        ": " + driver.artifactId());
            }
            requireText(driver.driverName(), "driverName");
            requireText(driver.purchaseUrl(), "purchaseUrl");
            if (driver.tier() == null) {
                throw new IllegalStateException("CData driver tier is missing for " + driver.dataSource());
            }
            if (driver.versionYear() < 2000) {
                throw new IllegalStateException("Unsupported CData Maven version year for " + driver.dataSource());
            }
            URI purchaseUri = URI.create(driver.purchaseUrl());
            if (!"https".equalsIgnoreCase(purchaseUri.getScheme())) {
                throw new IllegalStateException("CData purchase URL must use HTTPS for " + driver.dataSource());
            }
            requireUnique(dataSources, driver.dataSource(), "data source");
            requireUnique(artifactIds, driver.artifactId(), "Maven artifact");
        }
        return List.copyOf(catalog.drivers);
    }

    private static void requireText(@Nullable String value, @NotNull String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("CData driver catalog field is missing: " + field);
        }
    }

    private static void requireUnique(@NotNull Set<String> values, @NotNull String value, @NotNull String field) {
        if (!values.add(value)) {
            throw new IllegalStateException("Duplicate CData " + field + ": " + value);
        }
    }

    private static final class CatalogFile {
        private int schemaVersion;
        private List<CDataDriverInfo> drivers;
    }
}
