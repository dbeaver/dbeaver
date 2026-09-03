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

import com.dbeaver.db.cdata.CDataLicenseUIService;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.driver.DriverLoaderDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.FileMutex;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarFile;

final class CDataDriverLoaderDescriptor extends DriverLoaderDescriptor {
    private static final String JDBC_DRIVER_SERVICE = "META-INF/services/java.sql.Driver";
    private static final long VALIDATION_CACHE_NANOS = TimeUnit.MINUTES.toNanos(5);
    private final AtomicLong licenseGeneration = new AtomicLong();
    private volatile CDataResolvedDriver resolvedDriver;
    private volatile boolean licenseValidated;
    private volatile long loadedLicenseGeneration = -1;
    private volatile String inspectedLicenseFingerprint;
    private volatile String loadedLicenseFingerprint;
    private volatile long lastValidationNanos;
    private volatile Path sourceDriverJar;
    private volatile Path canonicalDriverJar;

    CDataDriverLoaderDescriptor(@NotNull String loaderId, @NotNull CDataDriverDescriptor driver) {
        super(loaderId, driver);
    }

    @Override
    public synchronized void loadDriver(@NotNull DBRProgressMonitor monitor, boolean forceReload) throws DBException {
        loadDriver(monitor, forceReload, false);
    }

    private void loadDriver(
        @NotNull DBRProgressMonitor monitor,
        boolean forceReload,
        boolean forceValidation
    ) throws DBException {
        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        boolean reloadDriver = forceReload ||
            loadedLicenseGeneration != licenseGeneration.get() ||
            !Objects.equals(loadedLicenseFingerprint, inspectedLicenseFingerprint);
        if (licenseValidated && !reloadDriver && !forceValidation) {
            super.loadDriver(monitor, false);
            return;
        }

        CDataDriverLicense license = validateLicense(monitor, reloadDriver);
        if (license.isValidLicense()) {
            return;
        }

        synchronized (driver.getLicenseActivationLock()) {
            license = validateLicense(monitor, loadedLicenseGeneration != licenseGeneration.get());
            if (license.isValidLicense()) {
                return;
            }
            CDataLicenseUIService uiService = DBWorkbench.getService(CDataLicenseUIService.class);
            if (uiService == null) {
                throw new CDataLicenseRequiredException(driver, license.getStatus());
            }
            CDataDriverLicense activatedLicense = uiService.activateLicense(driver);
            if (activatedLicense == null) {
                throw new CDataLicenseRequiredException(driver, license.getStatus());
            }
            if (!activatedLicense.isValidLicense()) {
                throw new CDataLicenseRequiredException(driver, activatedLicense.getStatus());
            }
            CDataDriverLicense verifiedLicense = validateLicense(
                monitor,
                loadedLicenseGeneration != licenseGeneration.get()
            );
            if (!verifiedLicense.isValidLicense()) {
                throw new CDataLicenseRequiredException(driver, verifiedLicense.getStatus());
            }
        }
    }

    @NotNull
    @Override
    public synchronized <T> T getDriverInstance(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (licenseValidated && System.nanoTime() - lastValidationNanos >= VALIDATION_CACHE_NANOS) {
            loadDriver(monitor, false, true);
        } else if (!licenseValidated ||
            loadedLicenseGeneration != licenseGeneration.get() ||
            !Objects.equals(loadedLicenseFingerprint, inspectedLicenseFingerprint)) {
            loadDriver(monitor);
        }
        return super.getDriverInstance(monitor);
    }

    @Override
    public synchronized void resetDriverInstance() {
        super.resetDriverInstance();
        licenseValidated = false;
        loadedLicenseGeneration = -1;
        inspectedLicenseFingerprint = null;
        loadedLicenseFingerprint = null;
        lastValidationNanos = 0;
        resolvedDriver = null;
        sourceDriverJar = null;
        canonicalDriverJar = null;
    }

    @NotNull
    @Override
    public List<Path> validateFilesPresence(@NotNull DBRProgressMonitor monitor) {
        List<Path> files = super.validateFilesPresence(monitor);
        Path sourceJar = sourceDriverJar;
        Path canonicalJar = canonicalDriverJar;
        if (sourceJar == null || canonicalJar == null) {
            return files;
        }
        List<Path> runtimeFiles = new ArrayList<>(files.size());
        for (Path file : files) {
            runtimeFiles.add(isSameFile(file, sourceJar) ? canonicalJar : file);
        }
        return runtimeFiles;
    }

    private static boolean isSameFile(@NotNull Path first, @NotNull Path second) {
        try {
            return Files.isSameFile(first, second);
        } catch (IOException e) {
            return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
        }
    }

    @NotNull
    CDataResolvedDriver resolveDriver(@NotNull DBRProgressMonitor monitor) throws DBException {
        CDataResolvedDriver cached = resolvedDriver;
        if (cached != null) {
            return cached;
        }
        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        String expectedPackage = "cdata.jdbc." + driver.getDriverInfo().jdbcName() + ".";
        for (Path library : getAllLibraryFiles(monitor)) {
            if (!library.getFileName().toString().endsWith(".jar")) {
                continue;
            }
            try {
                library = library.toRealPath();
            } catch (IOException e) {
                throw new DBException("CDATA JDBC driver JAR is not accessible", e);
            }
            try (JarFile jar = new JarFile(library.toFile())) {
                var entry = jar.getJarEntry(JDBC_DRIVER_SERVICE);
                if (entry == null) {
                    continue;
                }
                try (var reader = new BufferedReader(new InputStreamReader(
                    jar.getInputStream(entry),
                    StandardCharsets.UTF_8
                ))) {
                    String className;
                    while ((className = reader.readLine()) != null) {
                        className = className.strip();
                        if (!className.isEmpty() && !className.startsWith("#") && className.startsWith(expectedPackage)) {
                            String packageName = className.substring(0, className.lastIndexOf('.'));
                            Path runtimeJar = prepareCanonicalDriverJar(library, packageName);
                            sourceDriverJar = library;
                            canonicalDriverJar = runtimeJar;
                            resolvedDriver = new CDataResolvedDriver(
                                runtimeJar,
                                className,
                                getLicensePath(packageName)
                            );
                            return resolvedDriver;
                        }
                    }
                }
            } catch (IOException e) {
                throw new DBException("Error reading JDBC driver service from " + library, e);
            }
        }
        throw new DBException("CDATA JDBC driver class is missing from downloaded libraries");
    }

    @NotNull
    private static synchronized Path prepareCanonicalDriverJar(
        @NotNull Path sourceJar,
        @NotNull String packageName
    ) throws DBException {
        try {
            if (sourceJar.getFileName().toString().equals(packageName + ".jar")) {
                return sourceJar;
            }
            Path canonicalJar = getCanonicalDriverPath(packageName, getFileFingerprint(sourceJar));
            Files.createDirectories(canonicalJar.getParent());
            Path lockPath = canonicalJar.resolveSibling(packageName + ".lock");
            try (var channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var ignored = channel.lock()) {
                if (Files.isRegularFile(canonicalJar) && Files.mismatch(sourceJar, canonicalJar) == -1) {
                    return canonicalJar;
                }
                Path temporaryJar = Files.createTempFile(canonicalJar.getParent(), packageName + "-", ".tmp");
                try {
                    Files.copy(
                        sourceJar,
                        temporaryJar,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    );
                    try {
                        Files.move(
                            temporaryJar,
                            canonicalJar,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING
                        );
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(temporaryJar, canonicalJar, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temporaryJar);
                }
            }
            return canonicalJar;
        } catch (IOException e) {
            throw new DBException("Unable to prepare the canonical CDATA driver JAR", e);
        }
    }

    @NotNull
    static Path getCanonicalDriverPath(@NotNull String packageName, @NotNull String fingerprint) {
        return RuntimeUtils.getUserHomePath()
            .resolve(".CData")
            .resolve("drivers")
            .resolve(packageName)
            .resolve(fingerprint)
            .resolve(packageName + ".jar");
    }

    @NotNull
    private static String getFileFingerprint(@NotNull Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        try (var input = new DigestInputStream(Files.newInputStream(file), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    @NotNull
    static Path getLicensePath(@NotNull String packageName) {
        return RuntimeUtils.getUserHomePath().resolve(".CData").resolve(packageName + ".lic");
    }

    @NotNull
    synchronized CDataDriverLicense validateLicense(
        @NotNull DBRProgressMonitor monitor,
        boolean forceReload
    ) throws DBException {
        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        CDataResolvedDriver resolved = resolveDriver(monitor);
        try (FileMutex ignored = CDataLicenseValidator.acquireLicenseLock(resolved)) {
            long validationGeneration = licenseGeneration.get();
            CDataDriverLicense license = inspectLicense(monitor, resolved);
            if (licenseValidated) {
                if (CommonUtils.isEmpty(driver.getDriverClassName())) {
                    driver.setDriverClassName(resolved.driverClassName(), false);
                }
                super.loadDriver(
                    monitor,
                    forceReload ||
                        loadedLicenseGeneration != validationGeneration ||
                        !Objects.equals(loadedLicenseFingerprint, inspectedLicenseFingerprint)
                );
                loadedLicenseGeneration = validationGeneration;
                loadedLicenseFingerprint = inspectedLicenseFingerprint;
            }
            return license;
        } catch (IOException e) {
            throw new DBException("Unable to release the CDATA license validation lock", e);
        }
    }

    void invalidateLicenseCache() {
        licenseGeneration.incrementAndGet();
        licenseValidated = false;
        lastValidationNanos = 0;
    }

    @NotNull
    private CDataDriverLicense inspectLicense(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CDataResolvedDriver resolved
    ) throws DBException {
        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        CDataDriverLicense license = CDataLicenseValidator.validate(monitor, resolved);
        try {
            inspectedLicenseFingerprint = Files.isRegularFile(resolved.licensePath()) ?
                getFileFingerprint(resolved.licensePath()) : null;
        } catch (IOException e) {
            throw new DBException("Unable to inspect the CDATA license file", e);
        }
        driver.setCurrentLicense(license);
        licenseValidated = license.isValidLicense();
        lastValidationNanos = System.nanoTime();
        return license;
    }
}
