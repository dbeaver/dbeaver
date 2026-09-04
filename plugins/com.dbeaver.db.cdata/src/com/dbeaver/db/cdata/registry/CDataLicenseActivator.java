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
package com.dbeaver.db.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class CDataLicenseActivator {
    private static final Log log = Log.getLog(CDataLicenseActivator.class);
    private static final int MAX_DIAGNOSTIC_LENGTH = 2_000;

    private CDataLicenseActivator() {
    }

    @FunctionalInterface
    interface InstalledLicenseValidator {
        @NotNull
        CDataDriverLicense validate() throws DBException;
    }

    @NotNull
    public static CDataDriverLicense activate(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CDataDriverDescriptor driver,
        @NotNull CDataLicenseActivationRequest request
    ) throws DBException {
        if (!driver.beginLicenseActivationProcess()) {
            throw new DBException("CDATA license activation is already in progress");
        }
        monitor.beginTask("Activating CDATA driver license", 1);
        Path activationDirectory = null;
        FileChannel activationLockChannel = null;
        FileLock activationLock = null;
        try {
            CDataResolvedDriver resolvedDriver = driver.resolveDriver(monitor);
            Path jarPath;
            try {
                jarPath = resolvedDriver.jarPath().toRealPath();
            } catch (IOException e) {
                throw new DBException("CDATA driver JAR is not accessible", e);
            }
            if (!Files.isRegularFile(jarPath) || !Files.isReadable(jarPath)) {
                throw new DBException("CDATA driver JAR is not a readable file");
            }
            if (Files.isSymbolicLink(resolvedDriver.licensePath())) {
                throw new DBException("CDATA license path must not be a symbolic link");
            }
            Files.createDirectories(resolvedDriver.licensePath().getParent());
            Path lockPath = resolvedDriver.licensePath().resolveSibling(
                resolvedDriver.licensePath().getFileName() + ".lock"
            );
            activationLockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            try {
                activationLock = activationLockChannel.tryLock();
            } catch (OverlappingFileLockException e) {
                throw new DBException("CDATA license activation is already in progress", e);
            }
            if (activationLock == null) {
                throw new DBException("CDATA license activation is already in progress");
            }

            activationDirectory = Files.createTempDirectory("dbeaver-cdata-license-");
            Path activationJar = activationDirectory.resolve(getCanonicalJarName(resolvedDriver.licensePath()));
            try {
                Files.createLink(activationJar, jarPath);
            } catch (IOException | UnsupportedOperationException e) {
                Files.copy(jarPath, activationJar);
            }
            Path stagedLicense = activationDirectory.resolve(resolvedDriver.licensePath().getFileName());
            CDataProcessExecutor.ProcessResult processResult = execute(monitor, activationJar, request);
            boolean licenseCreated = Files.isRegularFile(stagedLicense);
            CDataLicenseStatus status = CDataLicenseParser.parseActivation(
                processResult.exitCode(),
                processResult.output(),
                request.type(),
                licenseCreated
            );
            CDataDriverLicense license = new CDataDriverLicense(
                status,
                resolvedDriver.licensePath().getFileName().toString(),
                null
            );
            if (!license.isValidLicense()) {
                String failure = formatFailure(processResult, licenseCreated, request);
                log.warn("CDATA license activation failed: status=" + status +
                    ", " + failure);
                throw new CDataLicenseActivationException(status, failure);
            }
            Path backupLicense = activationDirectory.resolve("previous-license.backup");
            CDataDriverLicense verifiedLicense;
            try {
                verifiedLicense = installAndValidateLicense(
                    stagedLicense,
                    resolvedDriver.licensePath(),
                    backupLicense,
                    () -> CDataLicenseValidator.validate(monitor, resolvedDriver)
                );
            } finally {
                driver.invalidateLicenseCaches();
            }
            driver.setCurrentLicense(verifiedLicense);
            monitor.worked(1);
            return verifiedLicense;
        } catch (CDataLicenseActivationException e) {
            throw e;
        } catch (IOException e) {
            DBException exception = new DBException("Unable to prepare CDATA license activation", e);
            log.warn("CDATA license activation could not be completed", exception);
            throw exception;
        } catch (DBException e) {
            log.warn("CDATA license activation could not be completed", e);
            throw e;
        } finally {
            deleteActivationDirectory(activationDirectory);
            closeActivationLock(activationLock, activationLockChannel);
            monitor.done();
            driver.endLicenseActivationProcess();
        }
    }

    @NotNull
    private static CDataProcessExecutor.ProcessResult execute(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Path jarPath,
        @NotNull CDataLicenseActivationRequest request
    ) throws DBException {
        List<String> command = new ArrayList<>();
        try {
            command.add(GeneralUtils.findJavaExecutable());
        } catch (IOException e) {
            throw new DBException("Java executable for CDATA activation was not found", e);
        }
        command.add("-jar");
        command.add(jarPath.toString());
        command.add("--license");
        String activationKey = request.type() == CDataLicenseType.TRIAL ? "TRIAL" : request.productKey();
        List<CDataProcessExecutor.PromptResponse> responses = List.of(
            new CDataProcessExecutor.PromptResponse("Name:", request.name()),
            new CDataProcessExecutor.PromptResponse("Email Address:", request.email()),
            new CDataProcessExecutor.PromptResponse("Product Key:", activationKey, true),
            new CDataProcessExecutor.PromptResponse("Press any key to exit", "")
        );
        return CDataProcessExecutor.execute(
            monitor,
            command,
            jarPath.getParent(),
            "CDATA license activation",
            responses
        );
    }

    @NotNull
    static String getCanonicalJarName(@NotNull Path licensePath) throws DBException {
        String licenseName = licensePath.getFileName().toString();
        if (!licenseName.startsWith("cdata.jdbc.") || !licenseName.endsWith(".lic")) {
            throw new DBException("Unexpected CDATA license file name: " + licenseName);
        }
        return licenseName.substring(0, licenseName.length() - ".lic".length()) + ".jar";
    }

    private static void installLicense(@NotNull Path source, @NotNull Path target) throws DBException {
        restrictLicensePermissions(source);
        Path temporaryLicense = null;
        try {
            Files.createDirectories(target.getParent());
            temporaryLicense = Files.createTempFile(target.getParent(), target.getFileName() + "-", ".tmp");
            Files.copy(
                source,
                temporaryLicense,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            );
            restrictLicensePermissions(temporaryLicense);
            try {
                Files.move(temporaryLicense, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryLicense, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DBException("Unable to install the CDATA license file", e);
        } finally {
            if (temporaryLicense != null) {
                try {
                    Files.deleteIfExists(temporaryLicense);
                } catch (IOException e) {
                    log.warn("Unable to delete a temporary CDATA license file", e);
                }
            }
        }
    }

    @NotNull
    static CDataDriverLicense installAndValidateLicense(
        @NotNull Path stagedLicense,
        @NotNull Path targetLicense,
        @NotNull Path backupLicense,
        @NotNull InstalledLicenseValidator validator
    ) throws DBException {
        boolean hadPreviousLicense = Files.exists(targetLicense, LinkOption.NOFOLLOW_LINKS);
        if (hadPreviousLicense) {
            if (Files.isSymbolicLink(targetLicense) || !Files.isRegularFile(targetLicense)) {
                throw new DBException("CDATA license path is not a regular file");
            }
            try {
                Files.copy(
                    targetLicense,
                    backupLicense,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
                );
            } catch (IOException e) {
                throw new DBException("Unable to back up the current CDATA license file", e);
            }
        }

        try {
            installLicense(stagedLicense, targetLicense);
            CDataDriverLicense verifiedLicense = validator.validate();
            if (!verifiedLicense.isValidLicense()) {
                throw new CDataLicenseActivationException(
                    verifiedLicense.getStatus(),
                    "The installed license did not pass CDATA validation."
                );
            }
            return verifiedLicense;
        } catch (DBException e) {
            rollbackLicense(backupLicense, targetLicense, hadPreviousLicense, e);
            throw e;
        }
    }

    private static void rollbackLicense(
        @NotNull Path backupLicense,
        @NotNull Path targetLicense,
        boolean hadPreviousLicense,
        @NotNull DBException validationFailure
    ) throws DBException {
        try {
            if (hadPreviousLicense) {
                installLicense(backupLicense, targetLicense);
            } else {
                Files.deleteIfExists(targetLicense);
            }
        } catch (IOException | DBException e) {
            DBException rollbackFailure = new DBException("Unable to restore the previous CDATA license file", e);
            rollbackFailure.addSuppressed(validationFailure);
            throw rollbackFailure;
        }
    }

    private static void deleteActivationDirectory(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            List<Path> activationFiles;
            try (var files = Files.list(directory)) {
                activationFiles = files.toList();
            }
            for (Path file : activationFiles) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(directory);
        } catch (IOException e) {
            log.warn("Unable to delete temporary CDATA activation files", e);
        }
    }

    private static void closeActivationLock(FileLock lock, FileChannel channel) {
        try {
            if (lock != null) {
                lock.release();
            }
        } catch (IOException e) {
            log.warn("Unable to release the CDATA license activation lock", e);
        }
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            log.warn("Unable to close the CDATA license activation lock", e);
        }
    }

    private static void restrictLicensePermissions(@NotNull Path licensePath) throws DBException {
        try {
            Files.setPosixFilePermissions(
                licensePath,
                EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
            );
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX file systems apply their platform-specific access rules.
        } catch (IOException e) {
            throw new DBException("Unable to restrict access to the CDATA license file", e);
        }
    }

    @NotNull
    static String sanitizeOutput(
        @NotNull String output,
        @NotNull CDataLicenseActivationRequest request
    ) {
        String sanitized = redact(output, request.name());
        sanitized = redact(sanitized, request.email());
        sanitized = redact(sanitized, request.productKey());
        sanitized = sanitized.replaceAll("\\s+", " ").strip();
        if (sanitized.length() > MAX_DIAGNOSTIC_LENGTH) {
            return sanitized.substring(0, MAX_DIAGNOSTIC_LENGTH) + "...";
        }
        return sanitized;
    }

    @NotNull
    private static String formatFailure(
        @NotNull CDataProcessExecutor.ProcessResult result,
        boolean licenseCreated,
        @NotNull CDataLicenseActivationRequest request
    ) {
        String output = sanitizeOutput(result.output(), request);
        String processDetails = "Exit code: " + result.exitCode() +
            ", license file: " + (licenseCreated ? "created" : "not created") + ".";
        return output.isEmpty() ? processDetails + " CData returned no output." : processDetails + " CData output: " + output;
    }

    @NotNull
    private static String redact(@NotNull String text, String value) {
        return value == null || value.isEmpty() ? text : text.replace(value, "<redacted>");
    }
}
