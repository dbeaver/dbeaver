/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbeaver.db.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.FileMutex;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

final class CDataLicenseValidator {
    private static final Log log = Log.getLog(CDataLicenseValidator.class);
    private static final Duration LICENSE_LOCK_TIMEOUT = Duration.ofMinutes(5);

    private CDataLicenseValidator() {
    }

    @NotNull
    static FileMutex acquireLicenseLock(@NotNull CDataResolvedDriver resolvedDriver) throws DBException {
        Path licensePath = resolvedDriver.licensePath();
        Path lockPath = licensePath.resolveSibling(licensePath.getFileName() + ".lock");
        try {
            Files.createDirectories(lockPath.getParent());
            return FileMutex.tryLock(lockPath, LICENSE_LOCK_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("Interrupted while waiting to validate the CDATA license", e);
        } catch (IOException e) {
            throw new DBException("Unable to lock the CDATA license for validation", e);
        }
    }

    @NotNull
    static CDataDriverLicense validate(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CDataResolvedDriver resolvedDriver
    ) throws DBException {
        try {
            Path probeLocation = Path.of(CDataLicenseProbe.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
            Path probeClass = Path.of(CDataLicenseProbe.class.getName().replace('.', File.separatorChar) + ".class");
            if (Files.isDirectory(probeLocation) && !Files.isRegularFile(probeLocation.resolve(probeClass))) {
                Path devClasses = probeLocation.resolve("target/classes");
                if (Files.isRegularFile(devClasses.resolve(probeClass))) {
                    probeLocation = devClasses;
                }
            }
            List<String> command = new ArrayList<>();
            command.add(GeneralUtils.findJavaExecutable());
            command.add("-cp");
            command.add(probeLocation + File.pathSeparator + resolvedDriver.jarPath());
            command.add(CDataLicenseProbe.class.getName());
            command.add(resolvedDriver.driverClassName());

            CDataProcessExecutor.ProcessResult result = CDataProcessExecutor.execute(
                monitor,
                command,
                resolvedDriver.jarPath().getParent(),
                "CDATA license validation",
                List.of()
            );
            if (result.exitCode() != 0) {
                String error = decode(result.output(), CDataLicenseProbe.ERROR_PREFIX);
                log.warn("CDATA license validation probe failed with exit code " + result.exitCode() +
                    (error == null ? "" : ": " + error));
                return unavailable();
            }
            String license = decode(result.output(), CDataLicenseProbe.LICENSE_PREFIX);
            String nodeId = decode(result.output(), CDataLicenseProbe.NODE_PREFIX);
            if (license == null || nodeId == null) {
                log.warn("CDATA license validation probe returned an incomplete response");
                return unavailable();
            }
            CDataDriverLicense parsed = CDataLicenseParser.parseInformation(Map.of("License", license, "NodeId", nodeId));
            if (parsed.getStatus() == CDataLicenseStatus.VALIDATION_UNAVAILABLE) {
                log.warn("CDATA license information is not recognized: " + license.replaceAll("\\s+", " ").strip());
            }
            return parsed;
        } catch (IOException | URISyntaxException | RuntimeException e) {
            log.warn("CDATA license validation probe could not be started", e);
            return unavailable();
        }
    }

    private static String decode(String output, String prefix) {
        for (String line : output.lines().toList()) {
            if (line.startsWith(prefix)) {
                return new String(Base64.getDecoder().decode(line.substring(prefix.length())), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static CDataDriverLicense unavailable() {
        return new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, "", null);
    }
}
