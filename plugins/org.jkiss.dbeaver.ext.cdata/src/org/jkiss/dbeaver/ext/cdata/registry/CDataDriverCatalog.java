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
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.SecurityUtils;
import org.jkiss.utils.function.ThrowableFunction;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CDataDriverCatalog {
    private static final Log log = Log.getLog(CDataDriverCatalog.class);
    private static final String CATALOG_FILE_NAME = "cdata-drivers.json";
    private static final String CATALOG_AUTH_TOKEN = "99514d3f1ab861c1de1a71c44d14567ccb0a99599121f578";
    private static final String CATALOG_URL = "https://www.cdata.com/order/dbeavercatalog.rsb?@json&@authtoken=" + CATALOG_AUTH_TOKEN;
    private static final String CATALOG_URL_PROPERTY = "cdataDriversURL";
    private static final int CONNECTION_TIMEOUT = 10_000;
    private static final int LEGACY_SCHEMA_VERSION = 2;
    private static final String ARTIFACT_SUFFIX = "-jdbc";
    private static volatile List<CDataDriverInfo> drivers;
    private static final AbstractJob UPDATE_JOB = new AbstractJob("Load CData driver catalog") {
        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            var product = Platform.getProduct();
            String catalogUrl = System.getProperty(CATALOG_URL_PROPERTY);
            if (catalogUrl == null || catalogUrl.isBlank()) {
                catalogUrl = product == null ? null : product.getProperty(CATALOG_URL_PROPERTY);
            }
            if (catalogUrl == null || catalogUrl.isBlank()) {
                catalogUrl = CATALOG_URL;
            }
            try {
                List<CDataDriverInfo> updated = load(monitor, catalogUrl, CDataDriverSettings.URL, getCacheFile());
                synchronized (CDataDriverCatalog.class) {
                    drivers = updated;
                }
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

    public static synchronized void start() {
        UPDATE_JOB.setSystem(true);
        UPDATE_JOB.schedule();
    }

    public static void stop() {
        UPDATE_JOB.cancel();
    }

    @NotNull
    public static synchronized List<CDataDriverInfo> load() {
        if (drivers == null) {
            drivers = loadCached(getCacheFile());
        }
        return drivers;
    }

    @NotNull
    private static Path getCacheFile() {
        return DBWorkbench.getPlatform().getWorkspace().getMetadataFolder().resolve("cdata").resolve(CATALOG_FILE_NAME);
    }

    @NotNull
    static List<CDataDriverInfo> loadCached(@NotNull Path cacheFile) {
        var settings = CDataDriverSettings.load(cacheFile.resolveSibling(CDataDriverSettings.FILE_NAME));
        List<CDataDriverInfo> cached = readCached(cacheFile, settings);
        return cached.isEmpty() ? readBundled(settings) : cached;
    }

    @NotNull
    private static List<CDataDriverInfo> readCached(
        @NotNull Path cacheFile,
        @NotNull Map<String, CDataDriverSettings> settings
    ) {
        if (Files.isRegularFile(cacheFile)) {
            try {
                return read(cacheFile, true, settings);
            } catch (IOException | IllegalStateException | IllegalArgumentException e) {
                log.warn("Error reading cached CData driver catalog", e);
            }
        }
        return List.of();
    }

    @NotNull
    private static List<CDataDriverInfo> readBundled(@NotNull Map<String, CDataDriverSettings> settings) {
        try (var input = CDataDriverCatalog.class.getResourceAsStream(CATALOG_FILE_NAME)) {
            if (input == null) {
                throw new IOException("Bundled CData driver catalog is missing");
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return read(reader, false, settings, true);
            }
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            log.error("Error reading bundled CData driver catalog", e);
            return List.of();
        }
    }

    @NotNull
    static List<CDataDriverInfo> load(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String catalogUrl,
        @NotNull String settingsUrl,
        @NotNull Path cacheFile
    ) throws InterruptedException {
        var settings = CDataDriverSettings.load(monitor, settingsUrl, cacheFile.resolveSibling(CDataDriverSettings.FILE_NAME));
        return load(monitor, cacheFile, catalogUrl, headers -> WebUtils.openURLConnection(
            monitor, catalogUrl, null, null, "GET", 1, CONNECTION_TIMEOUT, headers, true), settings);
    }

    @NotNull
    static List<CDataDriverInfo> load(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String catalogUrl,
        @NotNull Path cacheFile
    ) throws InterruptedException {
        return load(monitor, cacheFile, catalogUrl, headers -> WebUtils.openURLConnection(
            monitor, catalogUrl, null, null, "GET", 1, CONNECTION_TIMEOUT, headers, true),
            CDataDriverSettings.load(cacheFile.resolveSibling(CDataDriverSettings.FILE_NAME)));
    }

    @NotNull
    static List<CDataDriverInfo> load(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Path cacheFile,
        @NotNull ThrowableFunction<Map<String, String>, URLConnection, IOException> connectionFactory
    ) throws InterruptedException {
        return load(monitor, cacheFile, "", connectionFactory,
            CDataDriverSettings.load(cacheFile.resolveSibling(CDataDriverSettings.FILE_NAME)));
    }

    @NotNull
    private static List<CDataDriverInfo> load(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Path cacheFile,
        @NotNull String catalogUrl,
        @NotNull ThrowableFunction<Map<String, String>, URLConnection, IOException> connectionFactory,
        @NotNull Map<String, CDataDriverSettings> settings
    ) throws InterruptedException {
        String catalogHash = SecurityUtils.sha256Hex(catalogUrl);
        List<CDataDriverInfo> cachedDrivers = readCached(cacheFile, settings);
        boolean hasCachedCatalog = !cachedDrivers.isEmpty();
        if (!hasCachedCatalog) {
            cachedDrivers = readBundled(settings);
        }
        try {
            checkCanceled(monitor);
            Path etagFile = cacheFile.resolveSibling(cacheFile.getFileName() + ".etag");
            String etag = null;
            if (Files.isRegularFile(etagFile)) {
                try {
                    CatalogETag saved = JSONUtils.GSON.fromJson(Files.readString(etagFile), CatalogETag.class);
                    if (saved == null || saved.urlHash() == null) {
                        Files.deleteIfExists(etagFile);
                    } else if (hasCachedCatalog && catalogHash.equals(saved.urlHash())) {
                        etag = saved.etag();
                    }
                } catch (IOException | JsonParseException e) {
                    log.debug("Error reading CData catalog ETag (" + e.getClass().getSimpleName() + ")");
                    Files.deleteIfExists(etagFile);
                }
            }
            URLConnection connection = connectionFactory.apply(etag == null ? Map.of() : Map.of("If-None-Match", etag));
            try {
                if (connection instanceof HttpURLConnection httpConnection
                    && httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    if (etag == null || cachedDrivers.isEmpty()) {
                        throw new IOException("CData catalog returned HTTP 304 without a valid cache");
                    }
                    return cachedDrivers;
                }
                return download(monitor, cacheFile, etagFile, catalogHash, connection, settings);
            } finally {
                disconnect(connection);
            }
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            log.warn("Error updating CData driver catalog; using the cached catalog (" + e.getClass().getSimpleName() + ")");
            return cachedDrivers;
        }
    }

    @NotNull
    private static List<CDataDriverInfo> download(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Path cacheFile,
        @NotNull Path etagFile,
        @NotNull String catalogHash,
        @NotNull URLConnection connection,
        @NotNull Map<String, CDataDriverSettings> settings
    ) throws IOException, InterruptedException {
        Files.createDirectories(cacheFile.getParent());
        Path temporary = Files.createTempFile(cacheFile.getParent(), CATALOG_FILE_NAME, ".tmp");
        try {
            try (var input = connection.getInputStream(); var output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    checkCanceled(monitor);
                    output.write(buffer, 0, count);
                }
            }
            final List<CDataDriverInfo> downloadedDrivers = read(temporary, false, settings);
            checkCanceled(monitor);
            // discard the old validator before replacing its catalog, so a failed write cannot leave a stale ETag
            Files.deleteIfExists(etagFile);
            try {
                Files.move(temporary, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            }
            String etag = connection.getHeaderField("ETag");
            if (etag != null && !etag.isBlank()) {
                try {
                    Files.writeString(etagFile, JSONUtils.GSON.toJson(new CatalogETag(catalogHash, etag)));
                } catch (IOException e) {
                    log.debug("Error saving CData catalog ETag", e);
                }
            }
            return downloadedDrivers;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @NotNull
    private static List<CDataDriverInfo> read(
        @NotNull Path file,
        boolean allowLegacyCache,
        @NotNull Map<String, CDataDriverSettings> settings
    ) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return read(reader, allowLegacyCache, settings, false);
        }
    }

    @NotNull
    private static List<CDataDriverInfo> read(
        @NotNull Reader reader,
        boolean allowLegacyCache,
        @NotNull Map<String, CDataDriverSettings> settings,
        boolean useBundledSettings
    ) {
        try {
            CatalogFile catalog = JSONUtils.GSON.fromJson(reader, CatalogFile.class);
            if (allowLegacyCache && catalog != null && catalog.items == null && catalog.schemaVersion == LEGACY_SCHEMA_VERSION
                && catalog.drivers != null) {
                catalog.items = catalog.drivers.stream().map(CDataDriverCatalog::migrateDriver).toList();
            }
            if (catalog != null && catalog.items != null) {
                catalog.items = catalog.items.stream().map(driver -> {
                    if (driver == null) {
                        return null;
                    }
                    requireText(driver.dataSource(), "datasource");
                    CDataDriverSettings driverSettings = settings.get(driver.dataSource());
                    if (driverSettings == null && useBundledSettings && driver.dataTypeId() != null) {
                        driverSettings = new CDataDriverSettings(driver.dataTypeId(), driver.iconDriver());
                    }
                    return driver.withSettings(driverSettings);
                }).toList();
            }
            return validate(catalog);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Error reading CData driver catalog", e);
        }
    }

    @NotNull
    private static CDataDriverInfo migrateDriver(@Nullable LegacyDriverInfo driver) {
        if (driver == null) {
            throw new IllegalStateException("CData driver catalog contains an empty entry");
        }
        requireText(driver.artifactId(), "artifactId");
        requireText(driver.tier(), "tier");
        if (!driver.artifactId().endsWith(ARTIFACT_SUFFIX) || driver.artifactId().length() == ARTIFACT_SUFFIX.length()) {
            throw new IllegalStateException("Invalid legacy CData Maven artifact ID: " + driver.artifactId());
        }
        return new CDataDriverInfo(
            driver.dataSource(), driver.artifactId().substring(0, driver.artifactId().length() - ARTIFACT_SUFFIX.length()),
            driver.driverName(), driver.versionYear(), CDataDriverTier.valueOf(driver.tier()), driver.purchaseUrl(),
            driver.dataSource(), null
        );
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
        if (catalog.items == null || catalog.items.isEmpty()) {
            throw new IllegalStateException("CData driver catalog contains no drivers");
        }

        Set<String> dataSources = new HashSet<>();
        Set<String> artifactIds = new HashSet<>();
        for (CDataDriverInfo driver : catalog.items) {
            if (driver == null) {
                throw new IllegalStateException("CData driver catalog contains an empty entry");
            }
            requireText(driver.dataSource(), "datasource");
            requireText(driver.objectName(), "objname");
            requireText(driver.dataTypeId(), "datatype_id");
            if (!driver.artifactId().matches("[a-zA-Z0-9_][a-zA-Z0-9_.-]*") || driver.artifactId().contains("..")) {
                throw new IllegalStateException("Invalid CData Maven artifact ID: " + driver.artifactId());
            }
            requireText(driver.driverName(), "driver_name");
            requireText(driver.purchaseUrl(), "purchase_url");
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
        return List.copyOf(catalog.items);
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

    private record CatalogETag(@NotNull String urlHash, @NotNull String etag) {
    }

    private static final class CatalogFile {
        private List<CDataDriverInfo> items;
        private int schemaVersion;
        private List<LegacyDriverInfo> drivers;
    }

    private record LegacyDriverInfo(
        @NotNull String dataSource,
        @NotNull String artifactId,
        @NotNull String driverName,
        int versionYear,
        @NotNull String tier,
        @NotNull String purchaseUrl
    ) {
    }
}
