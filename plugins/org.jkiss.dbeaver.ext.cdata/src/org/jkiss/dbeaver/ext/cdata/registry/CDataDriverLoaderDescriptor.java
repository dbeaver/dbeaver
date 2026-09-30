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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cdata.CDataLicenseUIService;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.driver.DriverLoaderDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.FileMutex;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

final class CDataDriverLoaderDescriptor extends DriverLoaderDescriptor {
    private static final Log log = Log.getLog(CDataDriverLoaderDescriptor.class);
    private static final String JDBC_DRIVER_SERVICE = "META-INF/services/java.sql.Driver";
    private static final String STORAGE_FOLDER = "cdata";
    private static final long VALIDATION_CACHE_NANOS = TimeUnit.MINUTES.toNanos(5);
    private final AtomicLong licenseGeneration = new AtomicLong();
    private volatile CDataResolvedDriver resolvedDriver;
    private volatile CDataLicenseStatus licenseStatus;
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
    public void loadDriver(@NotNull DBRProgressMonitor monitor, boolean forceReload) throws DBException {
        loadDriver(monitor, forceReload, false);
    }

    // activation re-enters the loader from a worker thread; holding its monitor across the dialog deadlocks
    private void loadDriver(
        @NotNull DBRProgressMonitor monitor,
        boolean forceReload,
        boolean forceValidation
    ) throws DBException {
        // downloading may open a dialog, so it must happen before taking a lock
        getAllLibraryFiles(monitor);
        if (tryLoadDriver(monitor, forceReload, forceValidation)) {
            return;
        }

        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        synchronized (driver.getLicenseActivationLock()) {
            // another connection may have activated the license while we waited
            if (tryLoadDriver(monitor, false, true)) {
                return;
            }
            CDataLicenseUIService uiService = DBWorkbench.getService(CDataLicenseUIService.class);
            if (uiService == null || uiService.activateLicense(driver) == null || !tryLoadDriver(monitor, false, true)) {
                throw new CDataLicenseRequiredException(driver, driver.getLicenseStatus());
            }
        }
    }

    private synchronized boolean tryLoadDriver(
        @NotNull DBRProgressMonitor monitor,
        boolean forceReload,
        boolean forceValidation
    ) throws DBException {
        boolean reloadDriver = forceReload || isLicenseChanged();
        if (isDriverLoadable() && !reloadDriver && !forceValidation) {
            super.loadDriver(monitor, false);
            return true;
        }
        validateLicense(monitor, reloadDriver);
        return isDriverLoadable();
    }

    @NotNull
    @Override
    public <T> T getDriverInstance(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (isLicenseValid() && System.nanoTime() - lastValidationNanos >= VALIDATION_CACHE_NANOS) {
            loadDriver(monitor, false, true);
        } else if (!isDriverLoadable() || isLicenseChanged()) {
            loadDriver(monitor, false, false);
        }
        return super.getDriverInstance(monitor);
    }

    @Override
    public synchronized void resetDriverInstance() {
        super.resetDriverInstance();
        resetLicenseState();
        resolvedDriver = null;
        sourceDriverJar = null;
        canonicalDriverJar = null;
    }

    private void resetLicenseState() {
        licenseStatus = null;
        loadedLicenseGeneration = -1;
        inspectedLicenseFingerprint = null;
        loadedLicenseFingerprint = null;
        lastValidationNanos = 0;
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
    synchronized CDataResolvedDriver resolveDriver(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<Path> libraries = getAllLibraryFiles(monitor);
        CDataResolvedDriver cached = resolvedDriver;
        // changing the library version does not reset the loader
        if (cached != null && libraries.stream().anyMatch(file -> isSameFile(file, cached.jarPath()))) {
            return cached;
        }
        if (cached != null) {
            resetLicenseState();
            resolvedDriver = null;
        }
        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        String expectedPackage = "cdata.jdbc." + driver.getDriverInfo().jdbcName() + ".";
        for (Path library : libraries) {
            if (!library.getFileName().toString().endsWith(".jar")) {
                continue;
            }
            try {
                library = library.toRealPath();
            } catch (IOException e) {
                throw new DBException("CData JDBC driver JAR is not accessible", e);
            }
            try (JarFile jar = new JarFile(library.toFile())) {
                String className = readDriverClassName(jar, expectedPackage);
                if (className == null) {
                    continue;
                }
                String packageName = className.substring(0, className.lastIndexOf('.'));
                Path runtimeJar = prepareCanonicalDriverJar(library, packageName, readMajorVersion(jar));
                sourceDriverJar = library;
                canonicalDriverJar = runtimeJar;
                resolvedDriver = new CDataResolvedDriver(runtimeJar, className, getLicensePath(runtimeJar));
                return resolvedDriver;
            } catch (IOException e) {
                throw new DBException("Error reading JDBC driver service from " + library, e);
            }
        }
        throw new DBException("CData JDBC driver class is missing from downloaded libraries");
    }

    synchronized boolean isCurrentResolvedDriver(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CDataResolvedDriver target
    ) throws DBException {
        if (!isDriverInstalled()) {
            return false;
        }
        return isSameFile(resolveDriver(monitor).jarPath(), target.jarPath());
    }

    // CData only finds licenses beside a JAR named cdata.jdbc.<source>.jar, unlike Maven artifact names
    @NotNull
    private static synchronized Path prepareCanonicalDriverJar(
        @NotNull Path sourceJar,
        @NotNull String packageName,
        @NotNull String majorVersion
    ) throws DBException {
        try {
            Path canonicalJar = getCanonicalDriverPath(packageName, majorVersion);
            Files.createDirectories(canonicalJar.getParent());
            if (Files.isRegularFile(canonicalJar)) {
                if (Files.isSameFile(sourceJar, canonicalJar) || Files.mismatch(sourceJar, canonicalJar) == -1) {
                    return canonicalJar;
                }
                try {
                    Files.delete(canonicalJar);
                } catch (IOException e) {
                    // Windows may keep the loaded JAR locked until restart
                    log.warn("Unable to replace the CData driver JAR " + canonicalJar, e);
                    return canonicalJar;
                }
            }
            try {
                Files.createLink(canonicalJar, sourceJar);
            } catch (IOException | UnsupportedOperationException e) {
                // hard links may be unavailable or cross file systems
                Files.copy(sourceJar, canonicalJar, StandardCopyOption.REPLACE_EXISTING);
            }
            return canonicalJar;
        } catch (IOException e) {
            throw new DBException("Unable to prepare the canonical CData driver JAR", e);
        }
    }

    // licenses cover one major version and must survive driver build updates
    @NotNull
    static Path getCanonicalDriverPath(@NotNull String packageName, @NotNull String majorVersion) {
        return getStoragePath()
            .resolve("drivers")
            .resolve(packageName)
            .resolve(majorVersion)
            .resolve(packageName + ".jar");
    }

    @NotNull
    static String readMajorVersion(@NotNull JarFile jar) throws IOException {
        Manifest manifest = jar.getManifest();
        String version = manifest == null ? null : manifest.getMainAttributes().getValue("Implementation-Version");
        String major = CommonUtils.isEmpty(version) ? "" : version.split("\\.")[0];
        return major.matches("\\d+") ? major : "unknown";
    }

    static boolean hasLicenseForAnotherMajor(@NotNull CDataResolvedDriver resolved) {
        Path majorFolder = resolved.licensePath().getParent();
        Path licenseName = resolved.licensePath().getFileName();
        try (var majorFolders = Files.list(majorFolder.getParent())) {
            return majorFolders.anyMatch(folder ->
                !folder.equals(majorFolder) && Files.isRegularFile(folder.resolve(licenseName)));
        } catch (IOException e) {
            return false;
        }
    }

    @NotNull
    static Path getStoragePath() {
        return DBWorkbench.getPlatform().getApplication().getGlobalDataPath()
            .resolve(DBConstants.DEFAULT_DRIVERS_FOLDER)
            .resolve(STORAGE_FOLDER);
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
    static Path getLicensePath(@NotNull Path driverJar) {
        String jarName = driverJar.getFileName().toString();
        return driverJar.resolveSibling(jarName.substring(0, jarName.length() - ".jar".length()) + ".lic");
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
            if (isDriverLoadable()) {
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
            throw new DBException("Unable to release the CData license validation lock", e);
        }
    }

    @NotNull
    List<CDataPersistedLicense> inspectPersistedLicenses(@NotNull DBRProgressMonitor monitor) throws DBException {
        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        String packageName = "cdata.jdbc." + driver.getDriverInfo().jdbcName();
        Path storageFolder = getStoragePath();
        Path packageFolder = storageFolder.resolve("drivers").resolve(packageName);
        List<Path> licensePaths;
        try {
            if (Files.exists(packageFolder) &&
                !packageFolder.toRealPath().startsWith(storageFolder.toRealPath())) {
                throw new IOException("The CData driver storage folder resolves outside the application data directory");
            }
            licensePaths = findPersistedLicensePaths(packageFolder, packageName);
        } catch (IOException e) {
            throw new DBException("Unable to list persisted CData licenses", e);
        }

        List<CDataPersistedLicense> licenses = new ArrayList<>(licensePaths.size());
        for (Path licensePath : licensePaths) {
            if (monitor.isCanceled()) {
                break;
            }
            String majorVersion = licensePath.getParent().getFileName().toString();
            Path jarPath = licensePath.resolveSibling(packageName + ".jar");
            try {
                if (!Files.isRegularFile(jarPath) || Files.isSymbolicLink(jarPath)) {
                    throw new DBException("The driver JAR for major version " + majorVersion + " is missing");
                }
                String driverClassName = readDriverClassName(jarPath, packageName + ".", majorVersion);
                CDataResolvedDriver resolved = new CDataResolvedDriver(jarPath, driverClassName, licensePath);
                try (FileMutex ignored = CDataLicenseValidator.acquireLicenseLock(resolved)) {
                    licenses.add(new CDataPersistedLicense(
                        majorVersion,
                        CDataLicenseValidator.validate(monitor, resolved),
                        resolved
                    ));
                }
            } catch (IOException | DBException e) {
                licenses.add(new CDataPersistedLicense(
                    majorVersion,
                    new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, "", e.getMessage()),
                    null
                ));
            }
        }
        return licenses;
    }

    @NotNull
    static List<Path> findPersistedLicensePaths(@NotNull Path packageFolder, @NotNull String packageName) throws IOException {
        if (!Files.isDirectory(packageFolder)) {
            return List.of();
        }
        if (Files.isSymbolicLink(packageFolder)) {
            throw new IOException("The CData driver storage folder must not be a symbolic link");
        }
        try (var majorFolders = Files.list(packageFolder)) {
            return majorFolders
                .filter(folder -> Files.isDirectory(folder) && !Files.isSymbolicLink(folder))
                .map(folder -> folder.resolve(packageName + ".lic"))
                .filter(path -> Files.isRegularFile(path) && !Files.isSymbolicLink(path))
                .sorted(Comparator.comparing(path -> path.getParent().getFileName().toString()))
                .toList();
        }
    }

    @NotNull
    private static String readDriverClassName(
        @NotNull Path jarPath,
        @NotNull String expectedPackage,
        @NotNull String expectedMajorVersion
    ) throws DBException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            if (!expectedMajorVersion.equals(readMajorVersion(jar))) {
                throw new DBException("The persisted CData driver JAR major version does not match its folder");
            }
            String className = readDriverClassName(jar, expectedPackage);
            if (className != null) {
                return className;
            }
        } catch (IOException e) {
            throw new DBException("Error reading the persisted CData driver JAR", e);
        }
        throw new DBException("CData JDBC driver class is missing from the persisted JAR");
    }

    @Nullable
    @ForTest
    static String readDriverClassName(@NotNull JarFile jar, @NotNull String expectedPackage) throws IOException {
        var entry = jar.getJarEntry(JDBC_DRIVER_SERVICE);
        if (entry == null) {
            return null;
        }
        try (var reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8))) {
            String className;
            while ((className = reader.readLine()) != null) {
                className = className.strip();
                if (className.startsWith(expectedPackage)) {
                    return className;
                }
            }
        }
        return null;
    }

    void invalidateLicenseCache() {
        licenseGeneration.incrementAndGet();
        licenseStatus = null;
        lastValidationNanos = 0;
    }

    private boolean isLicenseValid() {
        CDataLicenseStatus status = licenseStatus;
        return status != null && status.isValid();
    }

    private boolean isLicenseChanged() {
        return loadedLicenseGeneration != licenseGeneration.get() ||
            !Objects.equals(loadedLicenseFingerprint, inspectedLicenseFingerprint);
    }

    private boolean isDriverLoadable() {
        CDataLicenseStatus status = licenseStatus;
        return status != null && status.allowsDriverUsage();
    }

    synchronized boolean reportExpiredLicense(@NotNull CDataLicenseStatus status) {
        if (licenseStatus != null && licenseStatus.isExpired()) {
            return false;
        }
        licenseStatus = status;
        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        driver.setCurrentLicense(new CDataDriverLicense(status, "", null));
        return true;
    }

    @NotNull
    private CDataDriverLicense inspectLicense(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CDataResolvedDriver resolved
    ) throws DBException {
        CDataDriverLicense license;
        if (!Files.isRegularFile(resolved.licensePath()) && hasLicenseForAnotherMajor(resolved)) {
            license = new CDataDriverLicense(CDataLicenseStatus.WRONG_MAJOR_VERSION, "", null);
        } else {
            license = CDataLicenseValidator.validate(monitor, resolved);
        }
        return updateInspectedLicense(resolved, license);
    }

    @NotNull
    @ForTest
    CDataDriverLicense updateInspectedLicense(
        @NotNull CDataResolvedDriver resolved,
        @NotNull CDataDriverLicense license
    ) throws DBException {
        try {
            String fingerprint = Files.isRegularFile(resolved.licensePath()) ?
                getFileFingerprint(resolved.licensePath()) : null;
            // the probe may not recognize a license rejected by the running driver
            if (licenseStatus != null && licenseStatus.isExpired() && Objects.equals(fingerprint, inspectedLicenseFingerprint)) {
                license = new CDataDriverLicense(licenseStatus, license.getLicenseId(), null);
            }
            inspectedLicenseFingerprint = fingerprint;
        } catch (IOException e) {
            throw new DBException("Unable to inspect the CData license file", e);
        }
        CDataDriverDescriptor driver = (CDataDriverDescriptor) getDriver();
        driver.setCurrentLicense(license);
        licenseStatus = license.getStatus();
        lastValidationNanos = System.nanoTime();
        return license;
    }
}
