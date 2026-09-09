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

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.function.ThrowableFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

public class CDataDriverCatalogTest extends DBeaverUnitTest {
    private static final long LAST_MODIFIED = 1_700_000_000_000L;
    private static final String CATALOG = """
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
            },
            {
              "dataSource": "azureanalysisservices",
              "artifactId": "aas-jdbc",
              "driverName": "Azure Analysis Services JDBC Driver",
              "versionYear": 2026,
              "tier": "PROFESSIONAL",
              "purchaseUrl": "https://www.cdata.com/order/options.aspx?sku=OARN-VSDBVR"
            },
            {
              "dataSource": "jira",
              "artifactId": "jira-jdbc",
              "driverName": "Jira JDBC Driver",
              "versionYear": 2025,
              "tier": "PREMIUM",
              "purchaseUrl": "https://www.cdata.com/order/options.aspx?sku=BJRM-VSDBVR"
            }
          ]
        }
        """;

    @TempDir
    Path tempDirectory;

    private Path cacheFile;
    private DBRProgressMonitor monitor;
    private HttpURLConnection headConnection;
    private HttpURLConnection downloadConnection;
    private ThrowableFunction<String, URLConnection, IOException> connectionFactory;

    @BeforeEach
    public void setUp() throws IOException {
        cacheFile = tempDirectory.resolve("cdata-drivers.json");
        monitor = Mockito.mock(DBRProgressMonitor.class);
        headConnection = Mockito.mock(HttpURLConnection.class);
        downloadConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(headConnection.getLastModified()).thenReturn(LAST_MODIFIED);
        Mockito.when(downloadConnection.getLastModified()).thenReturn(LAST_MODIFIED);
        Mockito.when(downloadConnection.getInputStream()).thenAnswer(
            invocation -> new ByteArrayInputStream(CATALOG.getBytes(StandardCharsets.UTF_8)));
        connectionFactory = method -> switch (method) {
            case "HEAD" -> headConnection;
            case "GET" -> downloadConnection;
            default -> throw new AssertionError("Unexpected HTTP method: " + method);
        };
    }

    @Test
    public void downloadMissingCatalog() throws Exception {
        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals(3, drivers.size());
        Assertions.assertEquals("adwords", drivers.get(0).dataSource());
        Assertions.assertEquals("googleads-jdbc", drivers.get(0).artifactId());
        Assertions.assertEquals("googleads", drivers.get(0).jdbcName());
        Assertions.assertEquals("{26\\..*}", drivers.get(0).mavenVersionPattern());
        Assertions.assertEquals("aas-jdbc", drivers.get(1).artifactId());
        Assertions.assertEquals("aas", drivers.get(1).jdbcName());
        Assertions.assertEquals("{25\\..*}", drivers.get(2).mavenVersionPattern());
        Assertions.assertEquals(CDataDriverTier.PREMIUM, drivers.get(2).tier());
        Assertions.assertEquals("https://www.cdata.com/order/options.aspx?sku=BJRM-VSDBVR", drivers.get(2).purchaseUrl());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        Assertions.assertEquals(LAST_MODIFIED, Files.getLastModifiedTime(cacheFile).toMillis());
        Mockito.verifyNoInteractions(headConnection);
        Mockito.verify(downloadConnection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void doNotDownloadUnchangedCatalog() throws Exception {
        saveCachedCatalog(LAST_MODIFIED);

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());

        Mockito.verify(headConnection).disconnect();
        Mockito.verifyNoInteractions(downloadConnection);
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
    }

    @Test
    public void replaceOutdatedCatalog() throws Exception {
        saveCachedCatalog(LAST_MODIFIED - 1000);
        String updatedCatalog = CATALOG.replace("2026", "2027");
        Mockito.when(downloadConnection.getInputStream()).thenReturn(
            new ByteArrayInputStream(updatedCatalog.getBytes(StandardCharsets.UTF_8)));

        var drivers = CDataDriverCatalog.load(monitor, cacheFile, connectionFactory);

        Assertions.assertEquals(2027, drivers.getFirst().versionYear());
        Assertions.assertEquals(updatedCatalog, Files.readString(cacheFile));
        Assertions.assertEquals(LAST_MODIFIED, Files.getLastModifiedTime(cacheFile).toMillis());
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
    public void handleUnavailableServerWithoutCache() throws Exception {
        connectionFactory = method -> {
            throw new IOException("Server unavailable");
        };

        Assertions.assertTrue(CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).isEmpty());
        Assertions.assertFalse(Files.exists(cacheFile));
        assertNoTemporaryFiles();
    }

    @Test
    public void doNotReplaceCacheWithInvalidCatalog() throws Exception {
        saveCachedCatalog(LAST_MODIFIED - 1000);
        for (String invalidCatalog : List.of(
            "{", "null", "{\"schemaVersion\":2,\"drivers\":[]}",
            CATALOG.replace("\"schemaVersion\": 2", "\"schemaVersion\": 999"),
            CATALOG.replace("\"artifactId\"", "\"unusedField\""),
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
    public void redownloadCorruptCache() throws Exception {
        Files.writeString(cacheFile, "invalid JSON");
        Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(LAST_MODIFIED));

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
        Assertions.assertEquals(CATALOG, Files.readString(cacheFile));
        Mockito.verifyNoInteractions(headConnection);
    }

    @Test
    public void downloadWhenServerDoesNotProvideModificationTime() throws Exception {
        saveCachedCatalog(LAST_MODIFIED);
        Mockito.when(headConnection.getLastModified()).thenReturn(0L);
        Mockito.when(downloadConnection.getLastModified()).thenReturn(0L);

        Assertions.assertEquals(3, CDataDriverCatalog.load(monitor, cacheFile, connectionFactory).size());
        Mockito.verify(downloadConnection).getInputStream();
        Assertions.assertEquals(0, Files.getLastModifiedTime(cacheFile).toMillis());
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

    private void saveCachedCatalog(long lastModified) throws IOException {
        Files.writeString(cacheFile, CATALOG);
        Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(lastModified));
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(tempDirectory)) {
            Assertions.assertTrue(files.allMatch(file -> file.equals(cacheFile)));
        }
    }
}
