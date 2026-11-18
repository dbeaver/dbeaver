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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.List;

public class CDataDriverSettingsTest extends DBeaverUnitTest {
    private static final String SETTINGS = """
        {"_meta":{"version":1},"postgresql":{"datatype_id":"postgresql","icon_driver":"postgresql:postgres-jdbc"}}
        """;

    @TempDir
    Path tempDirectory;

    private Path cacheFile;
    private DBRProgressMonitor progressMonitor;
    private HttpURLConnection connection;

    @BeforeEach
    public void setUp() throws IOException {
        cacheFile = tempDirectory.resolve(CDataDriverSettings.FILE_NAME);
        progressMonitor = Mockito.mock(DBRProgressMonitor.class);
        connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getInputStream()).thenAnswer(
            invocation -> new ByteArrayInputStream(SETTINGS.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void downloadSettings() throws Exception {
        var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

        Assertions.assertEquals("postgresql", settings.get("postgresql").dataTypeId());
        Assertions.assertEquals("postgresql:postgres-jdbc", settings.get("postgresql").iconDriver());
        Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
        Mockito.verify(connection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void applyOptionalCategories() throws Exception {
        var info = new CDataDriverInfo("postgresql", "postgresql", "PostgreSQL JDBC Driver", 2026,
            CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", "postgresql", null);
        for (String categories : List.of("", ",\"categories\":null", ",\"categories\":[]", ",\"categories\":[\"sql\",\"analytic\"]")) {
            Files.writeString(cacheFile, "{\"_meta\":{\"version\":1},\"postgresql\":{\"datatype_id\":\"postgresql\"" + categories + "}}");

            var settings = CDataDriverSettings.load(cacheFile).get("postgresql");
            var updated = info.withSettings(settings);

            Assertions.assertEquals(categories.contains("sql") ? List.of("sql", "analytic") : List.of(), updated.categories());
            Assertions.assertEquals("postgresql", updated.dataTypeId());
        }
        Assertions.assertTrue(info.withSettings(null).categories().isEmpty());
    }

    @Test
    public void rejectInvalidCategoriesWithoutReplacingCache() throws Exception {
        Files.writeString(cacheFile, SETTINGS);
        expireCache();
        for (String categories : List.of("[null]", "[\"\"]", "[\" \" ]", "{}", "\"sql\"")) {
            String invalid = "{\"postgresql\":{\"datatype_id\":\"postgresql\",\"categories\":" + categories + "}}";
            Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8)));

            var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

            Assertions.assertEquals("postgresql", settings.get("postgresql").dataTypeId());
            Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void reuseSettingsYoungerThanOneWeekWithoutConnecting() throws Exception {
        Files.writeString(cacheFile, SETTINGS);
        long modified = System.currentTimeMillis() - Duration.ofDays(6).toMillis();
        Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(modified));

        var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> {
            Assertions.fail("Fresh settings must not open a connection");
            return connection;
        });

        Assertions.assertEquals("postgresql", settings.get("postgresql").dataTypeId());
        Assertions.assertEquals(modified, Files.getLastModifiedTime(cacheFile).toMillis());
        Mockito.verifyNoInteractions(connection);
    }

    @Test
    public void replaceCachedSettings() throws Exception {
        Files.writeString(cacheFile, SETTINGS.replace("postgresql", "oldsource"));
        expireCache();

        var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

        Assertions.assertTrue(settings.containsKey("postgresql"));
        Assertions.assertFalse(settings.containsKey("oldsource"));
        Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
    }

    @Test
    public void keepCachedSettingsWhenOffline() throws Exception {
        Files.writeString(cacheFile, SETTINGS);
        expireCache();

        var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> {
            throw new IOException("Server unavailable");
        });

        Assertions.assertEquals("postgresql", settings.get("postgresql").dataTypeId());
        Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
        assertNoTemporaryFiles();
    }

    @Test
    public void useBundledSettingsWhenOfflineWithoutCache() throws Exception {
        Mockito.when(connection.getInputStream()).thenThrow(new IOException("Server unavailable"));

        var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

        Assertions.assertEquals("oracle", settings.get("oracledb").dataTypeId());
        Assertions.assertEquals("generic:oracle", settings.get("oracledb").iconDriver());
        Assertions.assertFalse(Files.exists(cacheFile));
        Mockito.verify(connection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void useBundledSettingsWhenCacheIsInvalid() throws Exception {
        for (String invalid : List.of("{", "null", "{}", "[]", "{\"postgresql\":null}",
            SETTINGS.replace("datatype_id", "unknown"), SETTINGS.replace("postgresql:postgres-jdbc", "invalid"))) {
            Files.writeString(cacheFile, invalid);

            var settings = CDataDriverSettings.load(cacheFile);

            Assertions.assertEquals("oracle", settings.get("oracledb").dataTypeId());
            Assertions.assertEquals("postgresql:postgres-jdbc", settings.get("postgresql").iconDriver());
            Assertions.assertEquals(invalid, Files.readString(cacheFile));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void preferCachedSettingsToBundledSettings() throws Exception {
        Files.writeString(cacheFile, """
            {"_meta":{"version":1},"oracledb":{"datatype_id":"custom_oracle","icon_driver":"custom:oracle"}}
            """);

        var settings = CDataDriverSettings.load(cacheFile);

        Assertions.assertEquals(1, settings.size());
        Assertions.assertEquals("custom_oracle", settings.get("oracledb").dataTypeId());
        Assertions.assertEquals("custom:oracle", settings.get("oracledb").iconDriver());
    }

    @Test
    public void preferBundledSettingsToFreshOlderCache() throws Exception {
        for (String outdated : List.of(
            "{\"postgresql\":{\"datatype_id\":\"old_postgresql\"}}",
            versionedSettings(0, "old_postgresql"),
            "{\"_meta\":{\"datatype_id\":\"cdata_settings\"},\"postgresql\":{\"datatype_id\":\"old_postgresql\"}}"
        )) {
            Files.writeString(cacheFile, outdated);
            long modified = Files.getLastModifiedTime(cacheFile).toMillis();

            var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> {
                Assertions.fail("Fresh settings must not open a connection");
                return connection;
            });

            Assertions.assertEquals("postgresql", settings.get("postgresql").dataTypeId());
            Assertions.assertEquals(List.of("sql"), settings.get("postgresql").categories());
            Assertions.assertEquals(settings, CDataDriverSettings.load(cacheFile));
            Assertions.assertFalse(settings.containsKey("_meta"));
            Assertions.assertEquals(outdated, Files.readString(cacheFile));
            Assertions.assertEquals(modified, Files.getLastModifiedTime(cacheFile).toMillis());
        }
        Mockito.verifyNoInteractions(connection);
    }

    @Test
    public void preferEqualOrNewerCacheToBundledSettings() throws Exception {
        for (int version : List.of(1, 2)) {
            Files.writeString(cacheFile, versionedSettings(version, "custom_postgresql"));

            var settings = CDataDriverSettings.load(cacheFile);

            Assertions.assertEquals(1, settings.size());
            Assertions.assertEquals("custom_postgresql", settings.get("postgresql").dataTypeId());
        }
    }

    @Test
    public void acceptEqualOrNewerDownload() throws Exception {
        for (int version : List.of(1, 2)) {
            Files.deleteIfExists(cacheFile);
            String downloaded = versionedSettings(version, "downloaded_postgresql");
            Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(downloaded.getBytes(StandardCharsets.UTF_8)));

            var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

            Assertions.assertEquals("downloaded_postgresql", settings.get("postgresql").dataTypeId());
            Assertions.assertEquals(downloaded, Files.readString(cacheFile));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void ignoreDownloadOlderThanBundledSettings() throws Exception {
        for (String downloaded : List.of("{\"postgresql\":{\"datatype_id\":\"old_postgresql\"}}",
            versionedSettings(0, "old_postgresql"))) {
            Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(downloaded.getBytes(StandardCharsets.UTF_8)));

            var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

            Assertions.assertEquals(List.of("sql"), settings.get("postgresql").categories());
            Assertions.assertEquals("oracle", settings.get("oracledb").dataTypeId());
            Assertions.assertFalse(Files.exists(cacheFile));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void keepNewerCacheAfterOlderDownload() throws Exception {
        String cached = versionedSettings(3, "cached_postgresql");
        Files.writeString(cacheFile, cached);
        expireCache();
        long modified = Files.getLastModifiedTime(cacheFile).toMillis();
        String downloaded = versionedSettings(2, "downloaded_postgresql");
        Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(downloaded.getBytes(StandardCharsets.UTF_8)));

        var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

        Assertions.assertEquals("cached_postgresql", settings.get("postgresql").dataTypeId());
        Assertions.assertEquals(cached, Files.readString(cacheFile));
        Assertions.assertEquals(modified, Files.getLastModifiedTime(cacheFile).toMillis());
        assertNoTemporaryFiles();
    }

    @Test
    public void useBundledSettingsWhenOlderCacheIsOffline() throws Exception {
        String cached = versionedSettings(0, "old_postgresql");
        Files.writeString(cacheFile, cached);
        expireCache();
        Mockito.when(connection.getInputStream()).thenThrow(new IOException("Server unavailable"));

        var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

        Assertions.assertEquals(List.of("sql"), settings.get("postgresql").categories());
        Assertions.assertEquals(cached, Files.readString(cacheFile));
        assertNoTemporaryFiles();
    }

    @Test
    public void keepCacheAfterInvalidVersionDownload() throws Exception {
        Files.writeString(cacheFile, SETTINGS);
        expireCache();
        for (String version : List.of("-1", "1.5", "2147483648", "\"2\"", "null", "[]", "{}")) {
            String downloaded = SETTINGS.replace("\"version\":1", "\"version\":" + version);
            Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(downloaded.getBytes(StandardCharsets.UTF_8)));

            var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

            Assertions.assertEquals("postgresql", settings.get("postgresql").dataTypeId());
            Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void keepCacheAfterInvalidDownload() throws Exception {
        Files.writeString(cacheFile, SETTINGS);
        expireCache();
        for (String invalid : List.of("{", "null", "{}", "[]", "{\"postgresql\":null}",
            SETTINGS.replace("datatype_id", "unknown"), SETTINGS.replace("postgresql:postgres-jdbc", "invalid"))) {
            Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8)));

            var settings = CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection);

            Assertions.assertEquals("postgresql", settings.get("postgresql").dataTypeId());
            Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
            assertNoTemporaryFiles();
        }
    }

    @Test
    public void keepCacheAfterInterruptedDownload() throws Exception {
        Files.writeString(cacheFile, SETTINGS);
        expireCache();
        InputStream input = Mockito.mock(InputStream.class);
        Mockito.when(input.read(Mockito.any(byte[].class))).thenReturn(10).thenThrow(new IOException("Connection lost"));
        Mockito.when(connection.getInputStream()).thenReturn(input);

        Assertions.assertFalse(CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection).isEmpty());
        Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
        Mockito.verify(input).close();
        Mockito.verify(connection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void cancelDownloadWithoutReplacingCache() throws Exception {
        Files.writeString(cacheFile, SETTINGS);
        expireCache();
        Mockito.when(progressMonitor.isCanceled()).thenReturn(false, true);

        Assertions.assertThrows(InterruptedException.class,
            () -> CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection));
        Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
        Mockito.verify(connection).disconnect();
        assertNoTemporaryFiles();
    }

    @Test
    public void recoverCorruptCache() throws Exception {
        Files.writeString(cacheFile, "invalid JSON");

        Assertions.assertFalse(CDataDriverSettings.load(progressMonitor, cacheFile, () -> connection).isEmpty());
        Assertions.assertEquals(SETTINGS, Files.readString(cacheFile));
        assertNoTemporaryFiles();
    }

    @NotNull
    private static String versionedSettings(int version, @NotNull String dataTypeId) {
        return "{\"_meta\":{\"version\":" + version + "},\"postgresql\":{\"datatype_id\":\"" + dataTypeId + "\"}}";
    }

    private void expireCache() throws IOException {
        Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(System.currentTimeMillis() - Duration.ofDays(7).toMillis()));
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(tempDirectory)) {
            Assertions.assertTrue(files.allMatch(file -> file.equals(cacheFile)));
        }
    }
}
