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

import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.SecurityUtils;
import org.jkiss.utils.function.ThrowableFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CDataDriverCatalogTest extends DBeaverUnitTest {
    private static final long LAST_MODIFIED = 1_700_000_000_000L;
    private static final String LEGACY_CATALOG = """
        {
          "schemaVersion": 2,
          "drivers": [
            {
              "dataSource": "adwords",
              "artifactId": "googleads-jdbc",
              "driverName": "Google AdWords JDBC Driver",
              "versionYear": 2026,
              "tier": "PROFESSIONAL",
              "purchaseUrl": "https://www.cdata.com/order/options.aspx?sku=DZRN-VSDBVR"
            }
          ]
        }
        """;
    private static final String CATALOG = """
        {
          "items": [
            {
              "datasource": "adwords",
              "objname": "googleads",
              "datatype_id": "google_ads",
              "driver_name": "Google AdWords JDBC Driver",
              "annual_price_usd": "99",
              "version_year": "2026",
              "tier": "Professional",
              "purchase_url": "https://www.cdata.com/order/options.aspx?sku=DZRN-VSDBVR"
            },
            {
              "datasource": "azureanalysisservices",
              "objname": "aas",
              "datatype_id": "azureanalysisservices",
              "driver_name": "Azure Analysis Services JDBC Driver",
              "version_year": "2026",
              "tier": "Professional",
              "purchase_url": "https://www.cdata.com/order/options.aspx?sku=OARN-VSDBVR"
            },
            {
              "datasource": "jira",
              "objname": "jira",
              "datatype_id": "jira",
              "driver_name": "Jira JDBC Driver",
              "annual_price_usd": "249",
              "version_year": "2025",
              "tier": "Premium",
              "purchase_url": "https://www.cdata.com/order/options.aspx?sku=BJRM-VSDBVR"
            }
          ]
        }
        """;

    @TempDir
    Path tempDirectory;

    private Path cacheFile;
    private DBRProgressMonitor monitor;
    private HttpURLConnection downloadConnection;
    private ThrowableFunction<Map<String, String>, URLConnection, IOException> connectionFactory;

    @BeforeEach
    public void setUp() throws IOException {
        cacheFile = tempDirectory.resolve("cdata-drivers.json");
        Files.writeString(tempDirectory.resolve(CDataDriverSettings.FILE_NAME), """
            {"adwords":{"datatype_id":"google_ads"}}
            """);
        monitor = Mockito.mock(DBRProgressMonitor.class);
        downloadConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(downloadConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(downloadConnection.getHeaderField("ETag")).thenReturn("\"catalog-v1\"");
        Mockito.when(downloadConnection.getInputStream()).thenAnswer(
            invocation -> new ByteArrayInputStream(CATALOG.getBytes(StandardCharsets.UTF_8)));
        connectionFactory = headers -> {
            downloadConnection.setRequestProperty("If-None-Match", headers.get("If-None-Match"));
            return downloadConnection;
        };
    }

    @Test
    public void downloadMissingCatalog() throws Exception {
        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals(3, drivers.size());
        Assertions.assertEquals("adwords", drivers.get(0).dataSource());
        Assertions.assertEquals("google_ads", drivers.get(0).dataTypeId());
        Assertions.assertEquals(CDataDriverTier.PROFESSIONAL, drivers.get(0).tier());
        Assertions.assertEquals("googleads-jdbc", drivers.get(0).artifactId());
        Assertions.assertEquals("googleads", drivers.get(0).jdbcName());
        Assertions.assertEquals("{26\\..*}", drivers.get(0).mavenVersionPattern());
        Assertions.assertEquals("aas-jdbc", drivers.get(1).artifactId());
        Assertions.assertEquals("aas", drivers.get(1).jdbcName());
        Assertions.assertEquals("{25\\..*}", drivers.get(2).mavenVersionPattern());
        Assertions.assertEquals(CDataDriverTier.PREMIUM, drivers.get(2).tier());
        Assertions.assertEquals(new BigDecimal("99"), drivers.get(0).annualPriceUsd());
        Assertions.assertNull(drivers.get(1).annualPriceUsd());
        Assertions.assertEquals(new BigDecimal("249"), drivers.get(2).annualPriceUsd());
        Assertions.assertEquals("https://www.cdata.com/order/options.aspx?sku=BJRM-VSDBVR", drivers.get(2).purchaseUrl());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        Assertions.assertTrue(Files.readString(etagFile()).contains("catalog-v1"));
        Mockito.verify(downloadConnection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void loadLocalCatalog() throws Exception {
        Path localCatalog = tempDirectory.resolve("local-catalog.json");
        Files.writeString(localCatalog, CATALOG);

        var drivers = CDataDriverCatalog.load(monitor, localCatalog.toUri().toString(), cacheFile);

        Assertions.assertEquals(3, drivers.size());
        Assertions.assertEquals("google_ads", drivers.getFirst().dataTypeId());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
    }

    @Test
    public void doNotDownloadUnchangedCatalog() throws Exception {
        saveCachedCatalog(LAST_MODIFIED);

        saveCachedETag("\"catalog-v1\"");
        Mockito.when(downloadConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals(3, drivers.size());
        Assertions.assertEquals(new BigDecimal("99"), drivers.get(0).annualPriceUsd());
        Assertions.assertEquals(new BigDecimal("249"), drivers.get(2).annualPriceUsd());

        Mockito.verify(downloadConnection).setRequestProperty("If-None-Match", "\"catalog-v1\"");
        Mockito.verify(downloadConnection, Mockito.never()).getInputStream();
        Mockito.verify(downloadConnection).disconnect();
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
    }

    @Test
    public void replaceOutdatedCatalog() throws Exception {
        saveCachedCatalog(LAST_MODIFIED - 1000);
        Mockito.when(downloadConnection.getHeaderField("ETag")).thenReturn("\"catalog-v2\"");
        String updatedCatalog = CATALOG.replace("2026", "2027");
        Mockito.when(downloadConnection.getInputStream()).thenReturn(
            new ByteArrayInputStream(updatedCatalog.getBytes(StandardCharsets.UTF_8)));

        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals(2027, drivers.getFirst().versionYear());
        Assertions.assertEquals(updatedCatalog, Files.readString(cacheFile));
        Assertions.assertTrue(Files.readString(etagFile()).contains("catalog-v2"));
        assertNoTemporaryFiles();
    }

    @Test
    public void keepCachedCatalogWhenServerIsUnavailable() throws Exception {
        saveCachedCatalog(LAST_MODIFIED);
        connectionFactory = method -> {
            throw new IOException("Server unavailable");
        };

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        Mockito.verifyNoInteractions(downloadConnection);
    }

    @Test
    public void keepLegacyCachedCatalogWhenOffline() throws Exception {
        Files.writeString(cacheFile, LEGACY_CATALOG);
        connectionFactory = method -> {
            throw new IOException("Server unavailable");
        };

        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals(1, drivers.size());
        Assertions.assertEquals("adwords", drivers.getFirst().dataSource());
        Assertions.assertEquals("googleads-jdbc", drivers.getFirst().artifactId());
        Assertions.assertEquals("googleads", drivers.getFirst().jdbcName());
        Assertions.assertEquals(CDataDriverTier.PROFESSIONAL, drivers.getFirst().tier());
        Assertions.assertEquals(2026, drivers.getFirst().versionYear());
        Assertions.assertEquals(LEGACY_CATALOG, Files.readString(cacheFile));
        assertNoTemporaryFiles();
    }

    @Test
    public void replaceLegacyCacheWithPublishedCatalog() throws Exception {
        Files.writeString(cacheFile, LEGACY_CATALOG);
        Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(LAST_MODIFIED - 1000));

        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals(3, drivers.size());
        Assertions.assertEquals("google_ads", drivers.getFirst().dataTypeId());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        Mockito.verify(downloadConnection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void rejectInvalidLegacyCache() throws Exception {
        for (String invalidCatalog : List.of(
            LEGACY_CATALOG.replace("googleads-jdbc", "../outside-jdbc"),
            LEGACY_CATALOG.replace("googleads-jdbc", "googleads"),
            LEGACY_CATALOG.replace("\"schemaVersion\": 2", "\"schemaVersion\": 3"),
            LEGACY_CATALOG.replace("\"PROFESSIONAL\"", "null")
        )) {
            Files.writeString(cacheFile, invalidCatalog);
            Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
            Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        }
    }

    @Test
    public void rejectDownloadedLegacyCatalog() throws Exception {
        saveCachedCatalog(LAST_MODIFIED - 1000);
        Mockito.when(downloadConnection.getInputStream()).thenReturn(
            new ByteArrayInputStream(LEGACY_CATALOG.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        assertNoTemporaryFiles();
    }

    @Test
    public void handleUnavailableServerWithoutCache() throws Exception {
        connectionFactory = method -> {
            throw new IOException("Server unavailable");
        };

        Assertions.assertEquals(CDataDriverCatalog.loadCached(cacheFile), CDataDriverCatalog.load(monitor, cacheFile, connectionFactory));
        Assertions.assertFalse(CDataDriverCatalog.loadCached(cacheFile).isEmpty());
        Assertions.assertFalse(Files.exists(cacheFile));
        assertNoTemporaryFiles();
    }

    @Test
    public void loadBundledCatalogWithoutCacheOrSettings() throws Exception {
        Files.delete(tempDirectory.resolve(CDataDriverSettings.FILE_NAME));

        var drivers = CDataDriverCatalog.loadCached(cacheFile);

        Assertions.assertTrue(drivers.size() > 200);
        Assertions.assertEquals("oracle", drivers.stream()
            .filter(driver -> "oracledb".equals(driver.dataSource())).findFirst().orElseThrow().dataTypeId());
        Assertions.assertEquals("postgresql:postgres-jdbc", drivers.stream()
            .filter(driver -> "postgresql".equals(driver.dataSource())).findFirst().orElseThrow().iconDriver());
        Assertions.assertFalse(Files.exists(cacheFile));
    }

    @Test
    public void useBundledSettingsForDownloadedVendorCatalog() throws Exception {
        Files.delete(tempDirectory.resolve(CDataDriverSettings.FILE_NAME));
        String vendorCatalog = CATALOG.replace("adwords", "oracledb").replace("googleads", "oracledb");
        Mockito.when(downloadConnection.getInputStream()).thenReturn(
            new ByteArrayInputStream(vendorCatalog.getBytes(StandardCharsets.UTF_8)));

        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals("oracle", drivers.getFirst().dataTypeId());
        Assertions.assertEquals("generic:oracle", drivers.getFirst().iconDriver());
        Assertions.assertEquals(vendorCatalog, Files.readString(cacheFile));
        Assertions.assertFalse(Files.exists(tempDirectory.resolve(CDataDriverSettings.FILE_NAME)));
    }

    @Test
    public void preferCachedCatalogToBundledCatalog() throws Exception {
        Files.writeString(cacheFile, CATALOG);

        Assertions.assertEquals(3, CDataDriverCatalog.loadCached(cacheFile).size());
    }

    @Test
    public void useBundledCatalogWhenCacheIsCorrupt() throws Exception {
        Files.writeString(cacheFile, "invalid JSON");

        Assertions.assertTrue(CDataDriverCatalog.loadCached(cacheFile).size() > 200);
        Assertions.assertEquals("invalid JSON", Files.readString(cacheFile));
    }

    @Test
    public void returnDriversWhileCatalogRefreshIsQueued() throws Exception {
        var field = CDataDriverCatalog.class.getDeclaredField("UPDATE_JOB");
        field.setAccessible(true);
        var job = (org.eclipse.core.runtime.jobs.Job) field.get(null);
        job.cancel();
        job.join();
        var rule = Mockito.mock(org.eclipse.core.runtime.jobs.ISchedulingRule.class);
        Mockito.when(rule.contains(rule)).thenReturn(true);
        Mockito.when(rule.isConflicting(rule)).thenReturn(true);
        job.setRule(rule);
        var manager = org.eclipse.core.runtime.jobs.Job.getJobManager();
        manager.beginRule(rule, null);
        try {
            CDataDriverCatalog.start();

            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                Assertions.assertFalse(CDataDriverCatalog.load().isEmpty()));
        } finally {
            job.cancel();
            manager.endRule(rule);
            job.join();
            job.setRule(null);
        }
    }

    @Test
    public void doNotReplaceCacheWithInvalidCatalog() throws Exception {
        saveCachedCatalog(LAST_MODIFIED - 1000);
        for (String invalidCatalog : List.of(
            "{", "null", "{\"items\":[]}",
            CATALOG.replace("\"items\"", "\"drivers\""),
            CATALOG.replace("\"objname\"", "\"unusedField\""),
            CATALOG.replace("\"datasource\"", "\"unusedField\""),
            CATALOG.replace("\"Professional\"", "\"Unknown\""),
            CATALOG.replace("\"jira\"", "\"adwords\""),
            CATALOG.replace("2026", "1999")
        )) {
            Mockito.when(downloadConnection.getInputStream()).thenReturn(
                new ByteArrayInputStream(invalidCatalog.getBytes(StandardCharsets.UTF_8)));

            Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
            Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
            Assertions.assertEquals(LAST_MODIFIED - 1000, Files.getLastModifiedTime(cacheFile).toMillis());
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void rejectUnsafeArtifactIds() throws Exception {
        for (String artifactId : List.of(
            "../outside-jdbc", "source/../../outside-jdbc", "/absolute-jdbc", "source\\..\\outside-jdbc",
            "C:\\outside-jdbc", "source:other-jdbc", "source%2fother-jdbc", "source?other-jdbc",
            "source#other-jdbc", "source other-jdbc", "source\nother-jdbc", "source..other-jdbc"
        )) {
            String invalidCatalog = CATALOG.replace("\"googleads\"", JSONUtils.GSON.toJson(artifactId));
            Mockito.when(downloadConnection.getInputStream()).thenAnswer(
                invocation -> new ByteArrayInputStream(invalidCatalog.getBytes(StandardCharsets.UTF_8)));

            Assertions.assertEquals(CDataDriverCatalog.loadCached(cacheFile),
                CDataDriverCatalog.load(monitor, cacheFile, connectionFactory), artifactId);
            Assertions.assertFalse(Files.exists(cacheFile), artifactId);
            assertNoTemporaryFiles();

            saveCachedCatalog(LAST_MODIFIED - 1000);
            var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);
            Assertions.assertEquals("googleads-jdbc", drivers.getFirst().artifactId(), artifactId);
            Assertions.assertEquals(CATALOG, Files.readString(cacheFile), artifactId);
            assertNoTemporaryFiles();
            Files.delete(cacheFile);
        }
    }

    @Test
    public void replaceUnsafeCachedArtifactId() throws Exception {
        Files.writeString(cacheFile, CATALOG.replace("googleads", "../outside"));
        Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(LAST_MODIFIED));

        Assertions.assertEquals("googleads-jdbc", CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).getFirst().artifactId());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
    }

    @Test
    public void acceptSafeMavenArtifactCharacters() throws Exception {
        String catalog = CATALOG.replace("googleads", "Source_2.v3");
        Mockito.when(downloadConnection.getInputStream()).thenReturn(new ByteArrayInputStream(catalog.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertEquals("Source_2.v3", CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).getFirst().jdbcName());
        Assertions.assertEquals(catalog, Files.readString(cacheFile));
    }

    @Test
    public void redownloadCorruptCache() throws Exception {
        Files.writeString(cacheFile, "invalid JSON");
        Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(LAST_MODIFIED));

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
    }

    @Test
    public void removeOldETagWhenServerDoesNotProvideOne() throws Exception {
        saveCachedCatalog(LAST_MODIFIED);
        saveCachedETag("old");
        Mockito.when(downloadConnection.getHeaderField("ETag")).thenReturn(null);

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
        Mockito.verify(downloadConnection).getInputStream();
        Assertions.assertFalse(Files.exists(etagFile()));
    }

    @Test
    public void keepCacheAfterInterruptedDownload() throws Exception {
        saveCachedCatalog(LAST_MODIFIED - 1000);
        InputStream input = Mockito.mock(InputStream.class);
        Mockito.when(input.read(Mockito.any(byte[].class))).thenReturn(10).thenThrow(new IOException("Connection lost"));
        Mockito.when(downloadConnection.getInputStream()).thenReturn(input);

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        Mockito.verify(input).close();
        Mockito.verify(downloadConnection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void cancelDownloadWithoutReplacingCache() throws Exception {
        saveCachedCatalog(LAST_MODIFIED - 1000);
        Mockito.when(monitor.isCanceled()).thenReturn(false, false, true);

        Assertions.assertThrows(InterruptedException.class, () -> CDataDriverCatalog.load(monitor, cacheFile, connectionFactory));
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        Mockito.verify(downloadConnection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void mergeSettingsWithVendorCatalogAndIgnoreVendorMappings() throws Exception {
        String vendorCatalog = CATALOG.replace("google_ads", "untrusted_type");
        Files.writeString(tempDirectory.resolve(CDataDriverSettings.FILE_NAME), """
            {"adwords":{"datatype_id":"google_ads","icon_driver":"generic:google-ads"}}
            """);
        Mockito.when(downloadConnection.getInputStream()).thenReturn(
            new ByteArrayInputStream(vendorCatalog.getBytes(StandardCharsets.UTF_8)));

        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals("google_ads", drivers.getFirst().dataTypeId());
        Assertions.assertEquals("generic:google-ads", drivers.getFirst().iconDriver());
        Assertions.assertEquals("azureanalysisservices", drivers.get(1).dataTypeId());
        Assertions.assertNull(drivers.get(1).iconDriver());
    }

    @Test
    public void acceptVendorCatalogWithoutDataTypes() throws Exception {
        String vendorCatalog = CATALOG.replaceAll("\\s*\"datatype_id\": \"[^\"]*\",", "");
        Mockito.when(downloadConnection.getInputStream()).thenReturn(
            new ByteArrayInputStream(vendorCatalog.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertEquals("google_ads", CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).getFirst().dataTypeId());
    }

    @Test
    public void redownloadCorruptCacheWithoutSendingSavedETag() throws Exception {
        Files.writeString(cacheFile, "invalid JSON");
        saveCachedETag("old");

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
        Mockito.verify(downloadConnection).setRequestProperty("If-None-Match", null);
    }

    @Test
    public void applyChangedSettingsToUnchangedCatalog() throws Exception {
        saveCachedCatalog(LAST_MODIFIED);
        saveCachedETag("old");
        Files.writeString(tempDirectory.resolve(CDataDriverSettings.FILE_NAME), """
            {"adwords":{"datatype_id":"new_type"}}
            """);
        Mockito.when(downloadConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);

        Assertions.assertEquals("new_type", CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).getFirst().dataTypeId());
        Mockito.verify(downloadConnection, Mockito.never()).getInputStream();
    }

    @Test
    public void handleMissingSettingsWithoutCreatingLocalCopy() {
        Path file = tempDirectory.resolve("missing-settings.json");

        Assertions.assertEquals("oracle", CDataDriverSettings.load(file).get("oracledb").dataTypeId());
        Assertions.assertFalse(Files.exists(file));
    }

    @Test
    public void ignoreInvalidSettingsWithoutReplacingLocalFile() throws Exception {
        Path file = tempDirectory.resolve(CDataDriverSettings.FILE_NAME);
        Files.writeString(file, "invalid JSON");

        Assertions.assertEquals("oracle", CDataDriverSettings.load(file).get("oracledb").dataTypeId());
        Assertions.assertEquals("invalid JSON", Files.readString(file));
    }

    @Test
    public void downloadSettingsForUnchangedCatalogAndKeepThemWhenOffline() throws Exception {
        var settingsRequests = new AtomicInteger();
        var catalogRequests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/settings", exchange -> {
            int request = settingsRequests.incrementAndGet();
            if (request == 3) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAVAILABLE, -1);
            } else {
                byte[] content = ("{\"adwords\":{\"datatype_id\":\"type" + request + "\","
                    + "\"icon_driver\":\"generic:google-ads\"}}").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, content.length);
                exchange.getResponseBody().write(content);
            }
            exchange.close();
        });
        server.createContext("/catalog", exchange -> {
            if (catalogRequests.incrementAndGet() > 1) {
                Assertions.assertEquals("\"catalog-v1\"", exchange.getRequestHeaders().getFirst("If-None-Match"));
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_MODIFIED, -1);
            } else {
                byte[] content = CATALOG.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("ETag", "\"catalog-v1\"");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, content.length);
                exchange.getResponseBody().write(content);
            }
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            var first = CDataDriverCatalog.load(monitor, url + "/catalog", url + "/settings", cacheFile).getFirst();
            Assertions.assertEquals("type1", first.dataTypeId());
            Assertions.assertEquals("generic:google-ads", first.iconDriver());
            Assertions.assertEquals("type2",
                CDataDriverCatalog.load(monitor, url + "/catalog", url + "/settings", cacheFile).getFirst().dataTypeId());
            Assertions.assertEquals("type2",
                CDataDriverCatalog.load(monitor, url + "/catalog", url + "/settings", cacheFile).getFirst().dataTypeId());
            Assertions.assertEquals(3, settingsRequests.get());
            Assertions.assertEquals(3, catalogRequests.get());
            assertNoTemporaryFiles();
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void conditionalGetThroughWebUtils() throws Exception {
        var requests = new AtomicInteger();
        var conditionalRequests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/catalog", exchange -> {
            requests.incrementAndGet();
            if ("\"catalog-v1\"".equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                conditionalRequests.incrementAndGet();
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_MODIFIED, -1);
            } else {
                byte[] content = CATALOG.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("ETag", "\"catalog-v1\"");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, content.length);
                exchange.getResponseBody().write(content);
            }
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/catalog";
            Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, url, cacheFile).size());
            var modifiedTime = Files.getLastModifiedTime(cacheFile);
            Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, url, cacheFile).size());
            Assertions.assertEquals(modifiedTime, Files.getLastModifiedTime(cacheFile));
            Assertions.assertEquals(2, requests.get());
            Assertions.assertEquals(1, conditionalRequests.get());
            Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, url + "?changed-source", cacheFile).size());
            Assertions.assertEquals(3, requests.get());
            Assertions.assertEquals(1, conditionalRequests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void keepAuthenticationTokenOutOfLogsAndETag() throws Exception {
        String token = "test-cdata-auth-secret";
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/catalog", exchange -> {
            if (("@authtoken=" + token).equals(exchange.getRequestURI().getRawQuery())) {
                requests.incrementAndGet();
            }
            byte[] content = CATALOG.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("ETag", "catalog-v1");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        });
        server.createContext("/failure", exchange -> {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1);
            exchange.close();
        });
        server.start();
        var output = new ByteArrayOutputStream();
        var previousWriter = Log.getLogWriter();
        Log.setLogWriter(output);
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            String url = base + "/catalog?@authtoken=" + token;
            Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, url, cacheFile).size());
            Assertions.assertEquals(1, requests.get());
            String metadata = Files.readString(etagFile());
            Assertions.assertFalse(metadata.contains(token));
            Assertions.assertFalse(metadata.contains(url));
            Assertions.assertTrue(metadata.contains(SecurityUtils.sha256Hex(url)));

            String failureUrl = base + "/failure?@authtoken=" + token;
            Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, failureUrl, cacheFile).size());
            IOException error = Assertions.assertThrows(IOException.class, () -> WebUtils.openURLConnection(
                monitor, failureUrl, null, null, "GET", 1, 1000, null, true));
            Assertions.assertFalse(error.getMessage().contains(token));
            Assertions.assertNull(error.getCause());
            Assertions.assertTrue(output.toString(StandardCharsets.UTF_8).contains("<redacted>"));
            Assertions.assertFalse(output.toString(StandardCharsets.UTF_8).contains(token));
        } finally {
            Log.setLogWriter(previousWriter);
            server.stop(0);
        }
    }

    @Test
    public void doNotLogCredentialsFromDownloadErrors() throws Exception {
        String token = "test-cdata-stream-secret";
        saveCachedCatalog(LAST_MODIFIED);
        Mockito.when(downloadConnection.getInputStream()).thenThrow(
            new IOException("Cannot read https://example.com/catalog?@authtoken=" + token));
        var output = new ByteArrayOutputStream();
        var previousWriter = Log.getLogWriter();
        Log.setLogWriter(output);
        try {
            Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
            Assertions.assertTrue(output.toString(StandardCharsets.UTF_8).contains("Error updating CData driver catalog"));
            Assertions.assertFalse(output.toString(StandardCharsets.UTF_8).contains(token));
        } finally {
            Log.setLogWriter(previousWriter);
        }
    }

    @Test
    public void removeLegacyETagCredentialsEvenWhenOffline() throws Exception {
        saveCachedCatalog(LAST_MODIFIED);
        connectionFactory = headers -> {
            throw new IOException("Server unavailable");
        };
        for (boolean withCatalog : List.of(true, false)) {
            if (!withCatalog) {
                Files.delete(cacheFile);
            }
            Files.writeString(etagFile(), """
                {"url":"https://example.com/catalog?@authtoken=legacy-secret","etag":"old"}
                """);

            Assertions.assertEquals(CDataDriverCatalog.loadCached(cacheFile).size(),
                CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
            Assertions.assertFalse(Files.exists(etagFile()));
        }
    }

    @Test
    public void removeMalformedLegacyETagCredentialsEvenWhenOffline() throws Exception {
        saveCachedCatalog(LAST_MODIFIED);
        connectionFactory = headers -> {
            throw new IOException("Server unavailable");
        };
        for (boolean withCatalog : List.of(true, false)) {
            if (!withCatalog) {
                Files.delete(cacheFile);
            }
            Files.writeString(etagFile(), """
                {"url":"https://example.com/catalog?@authtoken=legacy-secret","etag":
                """);

            Assertions.assertEquals(CDataDriverCatalog.loadCached(cacheFile).size(),
                CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
            Assertions.assertFalse(Files.exists(etagFile()));
        }
    }

    private void saveCachedETag(@NotNull String etag) throws IOException {
        Files.writeString(etagFile(), JSONUtils.GSON.toJson(Map.of("urlHash", SecurityUtils.sha256Hex(""), "etag", etag)));
    }

    private Path etagFile() {
        return cacheFile.resolveSibling(cacheFile.getFileName() + ".etag");
    }

    private void saveCachedCatalog(long lastModified) throws IOException {
        Files.writeString(cacheFile, CATALOG);
        Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(lastModified));
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(tempDirectory)) {
            Assertions.assertTrue(files.allMatch(file -> file.equals(cacheFile) || file.equals(etagFile())
                || file.getFileName().toString().equals(CDataDriverSettings.FILE_NAME)));
        }
    }
}
