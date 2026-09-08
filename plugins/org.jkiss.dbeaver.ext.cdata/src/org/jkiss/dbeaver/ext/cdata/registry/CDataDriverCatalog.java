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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CDataDriverCatalog {
    public static final int SCHEMA_VERSION = 2;
    private static final String RESOURCE_PATH = "/drivers/cdata-drivers.json";

    private CDataDriverCatalog() {
    }

    @NotNull
    public static List<CDataDriverInfo> load() {
        try (var stream = CDataDriverCatalog.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("CData driver catalog not found: " + RESOURCE_PATH);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                CatalogFile catalog = JSONUtils.GSON.fromJson(reader, CatalogFile.class);
                return validate(catalog);
            }
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Error reading CData driver catalog", e);
        }
    }

    @NotNull
    private static List<CDataDriverInfo> validate(CatalogFile catalog) {
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
        Set<String> driverSkus = new HashSet<>();
        Set<String> orderSkus = new HashSet<>();
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
            requireText(driver.driverSku(), "driverSku");
            requireText(driver.versionChar(), "versionChar");
            requireText(driver.orderSku(), "orderSku");
            requireText(driver.purchaseUrl(), "purchaseUrl");
            if (driver.tier() == null) {
                throw new IllegalStateException("CData driver tier is missing for " + driver.dataSource());
            }
            if (driver.versionYear() <= 0 || driver.annualPriceUsd() <= 0) {
                throw new IllegalStateException("Invalid CData version or price for " + driver.dataSource());
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
            requireUnique(driverSkus, driver.driverSku(), "driver SKU");
            requireUnique(orderSkus, driver.orderSku(), "order SKU");
        }
        return List.copyOf(catalog.drivers);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("CData driver catalog field is missing: " + field);
        }
    }

    private static void requireUnique(Set<String> values, String value, String field) {
        if (!values.add(value)) {
            throw new IllegalStateException("Duplicate CData " + field + ": " + value);
        }
    }

    private static final class CatalogFile {
        private int schemaVersion;
        private List<CDataDriverInfo> drivers;
    }
}
