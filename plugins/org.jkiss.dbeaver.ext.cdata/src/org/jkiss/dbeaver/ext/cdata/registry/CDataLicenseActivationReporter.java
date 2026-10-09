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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.app.ApplicationRegistry;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.HttpConstants;
import org.jkiss.utils.function.ThrowableSupplier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

final class CDataLicenseActivationReporter extends AbstractJob {
    private static final Log log = Log.getLog(CDataLicenseActivationReporter.class);
    private static final String ACTIVATION_HOST = /*<LM-PROD-URL*/"dbeaver.com"/*/>*/;
    private static final String ACTIVATION_URL = "https://" + ACTIVATION_HOST + "/lmp/externalLicenseActivation";
    private static final int REQUEST_TIMEOUT = 10_000;
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY = 5_000;

    private final byte[] payload;
    private final ThrowableSupplier<HttpURLConnection, IOException> connectionFactory;
    private int attempts;

    CDataLicenseActivationReporter(
        @NotNull byte[] payload,
        @NotNull ThrowableSupplier<HttpURLConnection, IOException> connectionFactory
    ) {
        super("Reporting CData license activation");
        this.payload = payload;
        this.connectionFactory = connectionFactory;
        setSystem(true);
    }

    static void report(
        @NotNull CDataDriverInfo driverInfo,
        @NotNull CDataResolvedDriver resolvedDriver,
        @NotNull CDataLicenseActivationRequest request
    ) {
        try {
            var application = ApplicationRegistry.getInstance().getApplication();
            if (application == null) {
                log.debug("Cannot report CData license activation without product information");
                return;
            }
            byte[] payload = createPayload(
                driverInfo.dataSource(),
                readDriverVersion(resolvedDriver.jarPath()),
                request,
                application.getLicenseProductId(),
                GeneralUtils.getPlainVersion()
            );
            new CDataLicenseActivationReporter(payload, () -> (HttpURLConnection) WebUtils.openURLConnection(
                ACTIVATION_URL, null, null, "POST", 1, REQUEST_TIMEOUT,
                Map.of(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            )).schedule();
        } catch (IOException | RuntimeException e) {
            log.warn("Unable to prepare CData license activation report");
        }
    }

    @NotNull
    static byte[] createPayload(
        @NotNull String driverId,
        @NotNull String driverVersion,
        @NotNull CDataLicenseActivationRequest request,
        @NotNull String productId,
        @NotNull String productVersion
    ) {
        JsonObject activation = new JsonObject();
        activation.addProperty("eventId", UUID.randomUUID().toString());
        activation.addProperty("provider", "cdata");
        activation.addProperty("email", request.email().strip());
        activation.addProperty("externalLicenseId", request.productKey() == null ? null : request.productKey().strip());
        activation.addProperty("externalProductVersion", driverVersion);
        activation.addProperty("licenseType", request.type() == CDataLicenseType.TRIAL ? "trial" : "purchased");
        activation.addProperty("product", "cdata-" + driverId);
        activation.addProperty("internalProduct", productId);
        activation.addProperty("internalProductVersion", productVersion);
        activation.addProperty("activatedAt", Instant.now().toString());
        JsonObject body = new JsonObject();
        body.add("activation", activation);
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    static String readDriverVersion(@NotNull Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var manifest = jar.getManifest();
            String version = manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            if (version == null || version.isBlank()) {
                throw new IOException("CData driver version is missing");
            }
            return version.strip();
        }
    }

    @NotNull
    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }
        attempts++;
        boolean retry;
        try {
            int status = send();
            if (status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE) {
                return Status.OK_STATUS;
            }
            retry = status == HttpURLConnection.HTTP_CLIENT_TIMEOUT || status == HttpConstants.CODE_TOO_MANY_REQUESTS ||
                status >= HttpURLConnection.HTTP_INTERNAL_ERROR;
            log.debug("CData license activation reporting returned HTTP " + status);
        } catch (IOException e) {
            retry = true;
        } catch (RuntimeException e) {
            retry = false;
        }
        if (retry && attempts < MAX_ATTEMPTS && !monitor.isCanceled()) {
            schedule(RETRY_DELAY * attempts);
        } else {
            log.warn("Unable to report CData license activation");
        }
        return Status.OK_STATUS;
    }

    private int send() throws IOException {
        HttpURLConnection connection = connectionFactory.get();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }
}
