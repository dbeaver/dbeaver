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
package org.jkiss.dbeaver.ext.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.FileMutex;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

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
            throw new DBException("Interrupted while waiting to validate the CData license", e);
        } catch (IOException e) {
            throw new DBException("Unable to lock the CData license for validation", e);
        }
    }

    @NotNull
    static CDataDriverLicense validate(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CDataResolvedDriver resolvedDriver
    ) throws DBException {
        try {
            CDataProcessExecutor.ProcessResult result = CDataProcessExecutor.execute(
                monitor,
                List.of(
                    GeneralUtils.findJavaExecutable(),
                    "-jar",
                    resolvedDriver.jarPath().toString(),
                    "--check-license"
                ),
                resolvedDriver.jarPath().getParent(),
                "CData license validation",
                List.of()
            );
            CDataDriverLicense parsed = CDataLicenseParser.parseLicenseCheck(result.output(), LocalDate.now());
            if (result.exitCode() != 0) {
                log.warn("CData license validation failed with exit code " + result.exitCode());
                if (parsed.getStatus().isValid()) {
                    return unavailable();
                }
            }
            if (parsed.getStatus() == CDataLicenseStatus.INVALID_KEY && !Files.isRegularFile(resolvedDriver.licensePath())) {
                return new CDataDriverLicense(CDataLicenseStatus.NOT_INSTALLED, "", null);
            }
            if (!parsed.getStatus().isValid()) {
                log.warn("CData reports the license of " + resolvedDriver.jarPath().getFileName() +
                    " as " + parsed.getStatus());
            }
            return parsed;
        } catch (IOException | RuntimeException e) {
            log.warn("Unable to check the CData license", e);
            return unavailable();
        }
    }

    @NotNull
    private static CDataDriverLicense unavailable() {
        return new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, "", null);
    }
}
