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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.HttpConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class CDataLicenseActivationReporterTest extends DBeaverUnitTest {
    @TempDir
    Path tempDirectory;

    private final DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
    private CDataLicenseActivationReporter reporter;

    @AfterEach
    public void cancelReport() {
        if (reporter != null) {
            reporter.cancel();
        }
    }

    @Test
    public void trialPayloadMatchesPublicApi() {
        JsonObject body = JsonParser.parseString(new String(payload(CDataLicenseType.TRIAL), StandardCharsets.UTF_8)).getAsJsonObject();
        Assertions.assertEquals(1, body.size());
        JsonObject activation = body.getAsJsonObject("activation");
        Assertions.assertNotNull(UUID.fromString(activation.get("eventId").getAsString()));
        Assertions.assertNotNull(Instant.parse(activation.get("activatedAt").getAsString()));
        Assertions.assertEquals("cdata", activation.get("provider").getAsString());
        Assertions.assertEquals("user@example.com", activation.get("email").getAsString());
        Assertions.assertTrue(activation.get("externalLicenseId").isJsonNull());
        Assertions.assertEquals("26.0.1234", activation.get("externalProductVersion").getAsString());
        Assertions.assertEquals("trial", activation.get("licenseType").getAsString());
        Assertions.assertEquals("cdata-salesforce", activation.get("product").getAsString());
        Assertions.assertEquals("dbeaver-ce", activation.get("internalProduct").getAsString());
        Assertions.assertEquals("26.2.0", activation.get("internalProductVersion").getAsString());
    }

    @Test
    public void purchasedPayloadUsesProductKeyAndNewEventId() {
        JsonObject first = JsonParser.parseString(new String(payload(CDataLicenseType.PURCHASED), StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonObject("activation");
        JsonObject second = JsonParser.parseString(new String(payload(CDataLicenseType.PURCHASED), StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonObject("activation");
        Assertions.assertEquals("purchased", first.get("licenseType").getAsString());
        Assertions.assertEquals("test-product-key", first.get("externalLicenseId").getAsString());
        Assertions.assertNotEquals(first.get("eventId"), second.get("eventId"));
    }

    @Test
    public void readsVersionOfActivatedJar() throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "25.0.9876");
        Path jar = tempDirectory.resolve("cdata.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            output.finish();
        }
        Assertions.assertEquals("25.0.9876", CDataLicenseActivationReporter.readDriverVersion(jar));
    }

    @Test
    public void missingJarVersionIsNotReportedAsCatalogVersion() throws IOException {
        Path jar = tempDirectory.resolve("cdata.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.finish();
        }
        Assertions.assertThrows(IOException.class, () -> CDataLicenseActivationReporter.readDriverVersion(jar));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 299, 301, 307, 400, 401, 403, 404, 415})
    public void successAndPermanentErrorsDoNotRetry(int status) throws IOException {
        byte[] payload = payload(CDataLicenseType.TRIAL);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpURLConnection connection = connection(status, output);
        reporter = new CDataLicenseActivationReporter(payload, () -> connection);
        Assertions.assertSame(Status.OK_STATUS, reporter.runDirectly(monitor));
        Assertions.assertEquals(Job.NONE, reporter.getState());
        Assertions.assertArrayEquals(payload, output.toByteArray());
        Mockito.verify(connection).setInstanceFollowRedirects(false);
        Mockito.verify(connection).setFixedLengthStreamingMode(payload.length);
        Mockito.verify(connection).disconnect();
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 503})
    public void retriesReuseWholeEventAndStopAfterThreeAttempts(int status) throws IOException {
        byte[] payload = payload(CDataLicenseType.PURCHASED);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpURLConnection connection = connection(status, output);
        reporter = new CDataLicenseActivationReporter(payload, () -> connection);
        for (int attempt = 1; attempt <= 3; attempt++) {
            output.reset();
            Assertions.assertSame(Status.OK_STATUS, reporter.runDirectly(monitor));
            Assertions.assertArrayEquals(payload, output.toByteArray());
            Assertions.assertEquals(attempt < 3 ? Job.SLEEPING : Job.NONE, reporter.getState());
            reporter.cancel();
        }
        Mockito.verify(connection, Mockito.times(3)).disconnect();
    }

    @Test
    public void networkFailureRetriesAndSuccessStopsRetrying() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpURLConnection connection = connection(HttpConstants.CODE_OK, output);
        Mockito.when(connection.getResponseCode()).thenThrow(new IOException("network failure")).thenReturn(HttpConstants.CODE_OK);
        reporter = new CDataLicenseActivationReporter(payload(CDataLicenseType.TRIAL), () -> connection);
        Assertions.assertSame(Status.OK_STATUS, reporter.runDirectly(monitor));
        Assertions.assertEquals(Job.SLEEPING, reporter.getState());
        reporter.cancel();
        Assertions.assertSame(Status.OK_STATUS, reporter.runDirectly(monitor));
        Assertions.assertEquals(Job.NONE, reporter.getState());
        Mockito.verify(connection, Mockito.times(2)).disconnect();
    }

    @Test
    public void canceledReportDoesNotOpenConnection() {
        Mockito.when(monitor.isCanceled()).thenReturn(true);
        reporter = new CDataLicenseActivationReporter(payload(CDataLicenseType.TRIAL), () -> {
            Assertions.fail("Canceled report opened a connection");
            return null;
        });
        Assertions.assertSame(Status.CANCEL_STATUS, reporter.runDirectly(monitor));
        Assertions.assertEquals(Job.NONE, reporter.getState());
    }

    @Test
    public void unexpectedReportingFailureStopsRetrying() {
        reporter = new CDataLicenseActivationReporter(payload(CDataLicenseType.TRIAL), () -> {
            throw new IllegalStateException("unavailable");
        });
        Assertions.assertSame(Status.OK_STATUS, reporter.runDirectly(monitor));
        Assertions.assertEquals(Job.NONE, reporter.getState());
    }

    @NotNull
    private byte[] payload(@NotNull CDataLicenseType type) {
        return CDataLicenseActivationReporter.createPayload(
            "salesforce", "26.0.1234",
            new CDataLicenseActivationRequest("Test user", " user@example.com ", type, " test-product-key "),
            "dbeaver-ce", "26.2.0"
        );
    }

    @NotNull
    private HttpURLConnection connection(int status, @NotNull ByteArrayOutputStream output) throws IOException {
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getOutputStream()).thenReturn(output);
        Mockito.when(connection.getResponseCode()).thenReturn(status);
        return connection;
    }
}
