/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.registry.driver;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBFileController;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.connection.DBPDriverLibraryProvider;
import org.jkiss.dbeaver.model.connection.DBPDriverLoader;
import org.jkiss.dbeaver.model.dpi.DBPApplicationDPI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceDrivers;
import org.jkiss.dbeaver.utils.VersionUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DriverLoaderDescriptor
 */
public class DriverLoaderDescriptor implements DBPDriverLoader {

    public static final String DEFAULT_LOADER_ID = "default";

    private static final Log log = Log.getLog(DriverLoaderDescriptor.class);

    private final DriverDescriptor driver;
    private final List<DBPDriverLibraryProvider> libraryProviders = new ArrayList<>();
    private final Map<DBPDriverLibrary, List<DriverFileInfo>> resolvedFiles = new HashMap<>();

    /**
     * Parent classloader of every driver classloader that loads global libraries.
     * <p>
     * Initializes upon the initialization of the very first driver.
     */
    private static ClassLoader rootClassLoader;
    private final String loaderId;

    private Class<?> driverClass;
    private boolean isLoaded;
    private DriverClassLoader classLoader;

    private transient boolean isFailed = false;

    protected DriverLoaderDescriptor(@NotNull String loaderId, @NotNull DriverDescriptor driver) {
        this.loaderId = loaderId;
        this.driver = driver;
    }

    public DriverDescriptor getDriver() {
        return driver;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean isFailed() {
        return isFailed;
    }

    @NotNull
    @Override
    public String getLoaderId() {
        return loaderId;
    }

    @NotNull
    @Override
    public List<DBPDriverLibraryProvider> getLibraryProviders() {
        return libraryProviders;
    }

    @Nullable
    @Override
    public DriverClassLoader getClassLoader() {
        return classLoader;
    }

    @NotNull
    @Override
    public <T> T getDriverInstance(@NotNull DBRProgressMonitor monitor)
    throws DBException {
        if (driverClass == null) {
            loadDriver(monitor);
        }
        return (T) createDriverInstance();
    }

    public void resetDriverInstance() {
        this.driverClass = null;
        this.isLoaded = false;

        this.resolvedFiles.clear();
    }

    public void addLibraryProvider(DBPDriverLibraryProvider libraryProvider) {
        libraryProviders.add(libraryProvider);
    }

    private Object createDriverInstance()
    throws DBException {
        try {
            return driverClass.getConstructor().newInstance();
        } catch (InstantiationException ex) {
            throw new DBException("Can't instantiate driver class", ex);
        } catch (IllegalAccessException ex) {
            throw new DBException("Illegal access", ex);
        } catch (ClassCastException ex) {
            throw new DBException("Bad driver class name specified", ex);
        } catch (Throwable ex) {
            throw new DBException("Error during driver instantiation", ex);
        }
    }

    @Override
    public void loadDriver(DBRProgressMonitor monitor)
    throws DBException {
        this.loadDriver(monitor, false);
    }

    public void loadDriver(DBRProgressMonitor monitor, boolean forceReload)
    throws DBException {
        if (isLoaded && !forceReload) {
            return;
        }
        isLoaded = false;

        loadGlobalLibraries();
        loadLibraries(monitor);

        if (driver.isLicenseRequired()) {
            String licenseText = driver.getLicense();
            if (!CommonUtils.isEmpty(licenseText) && !driver.acceptLicense(licenseText)) {
                throw new DBException("You have to accept driver '" + driver.getFullName() + "' license to be able to connect");
            }
        }

        try {
            if (!driver.isCustomDriverLoader()) {
                try {
                    // Load driver classes into core module using plugin class loader
                    driverClass = Class.forName(driver.getDriverClassName(), true, classLoader);
                } catch (Throwable ex) {
                    throw new DBException("Error creating driver '" + driver.getFullName()
                        + "' instance.\nMost likely required jar files are missing.\nYou should configure jars in driver settings.\n\n"
                        + "Reason: can't load driver class '" + driver.getDriverClassName() + "'",
                        ex);
                }

                isLoaded = true;
                isFailed = false;
            }
        } catch (DBException e) {
            isFailed = true;
            throw e;
        }
    }

    private void loadLibraries(DBRProgressMonitor monitor) throws DBException {
        this.classLoader = null;

        List<Path> allLibraryFiles = validateFilesPresence(monitor, false);

        List<URL> libraryURLs = new ArrayList<>();
        // Load libraries
        for (Path file : allLibraryFiles) {
            URL url;
            try {
                url = file.toUri().toURL();
            } catch (MalformedURLException e) {
                log.error(e);
                continue;
            }
            libraryURLs.add(url);
        }
        // Make class loader
        ClassLoader baseClassLoader = rootClassLoader;
        if (baseClassLoader == null) {
            DBPDataSourceProvider dataSourceProvider = driver.getDataSourceProvider();
            if (dataSourceProvider.providesDriverClasses()) {
                // Use driver provider class loader
                baseClassLoader = dataSourceProvider.getClass().getClassLoader();
            } else {
                // Use model classloader
                baseClassLoader = DBPDataSource.class.getClassLoader();
            }
        }
        this.classLoader = new DriverClassLoader(
            this,
            libraryURLs.toArray(new URL[0]),
            baseClassLoader);
    }

    private static synchronized void loadGlobalLibraries() {
        if (rootClassLoader == null) {
            final List<URL> libraries = new ArrayList<>();
            for (String library : DriverDescriptor.getGlobalLibraries()) {
                try {
                    libraries.add(new File(library).toURI().toURL());
                } catch (Exception e) {
                    log.error("Can't load global library '" + library + "'", e);
                }
            }
            if (libraries.isEmpty()) {
                // No point in creating redundant classloader
                return;
            }
            rootClassLoader = new URLClassLoader(libraries.toArray(new URL[0]), DriverDescriptor.class.getClassLoader());
        }
    }

    @Nullable
    public static ClassLoader getRootClassLoader() {
        return rootClassLoader;
    }

    public List<Path> getAllLibraryFiles(DBRProgressMonitor monitor) {
        return validateFilesPresence(monitor, false);
    }

    public void updateFiles() {
        validateFilesPresence(new LoggingProgressMonitor(log), true);
    }

    @Override
    public void validateFilesPresence(@NotNull DBRProgressMonitor monitor) {
        validateFilesPresence(monitor, false);
    }

    @Override
    public boolean needsExternalDependencies() {
        for (DBPDriverLibrary library : getAllLibraries()) {
            if (library.isDisabled() || library.isOptional() || !library.matchesCurrentPlatform()) {
                continue;
            }
            if (library.getType() == DBPDriverLibrary.FileType.license) {
                continue;
            }
            if (!isResolvedLibraryPresent(library)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private List<Path> validateFilesPresence(DBRProgressMonitor monitor, boolean resetVersions) {
        if (DBWorkbench.isDistributed()) {
            // We are in distributed mode
            return syncDistributedDependencies(monitor);
        }
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        if (application.isDetachedProcess()) {
            return syncDpiDependencies(application);
        }

        boolean downloadOk = downloadDriverLibraries(monitor, resetVersions);
        if (!downloadOk) {
            return Collections.emptyList();
        }

        List<Path> result = new ArrayList<>();

        for (DBPDriverLibrary library : getAllLibraries()) {
            if (library.isDisabled() || !library.matchesCurrentPlatform()) {
                // Wrong OS or architecture
                continue;
            }
            if (library.isDownloadable()) {
                List<DriverFileInfo> files = resolvedFiles.get(library);
                if (files != null) {
                    for (DriverFileInfo file : files) {
                        if (file.getFile() != null && !result.contains(file.getFile())) {
                            result.add(file.getFile());
                        }
                    }
                }
            } else {
                if (library.getType() == DBPDriverLibrary.FileType.license) {
                    continue;
                }
                Path localFile = library.getLocalFile();
                if (localFile == null) {
                    continue;
                }

                if (IOUtils.isFileFromDefaultFS(localFile)) {
                    if (Files.isDirectory(localFile)) {
                        result.addAll(readJarsFromDir(localFile));
                    }
                    if (!result.contains(localFile)) {
                        result.add(localFile);
                    }
                } else {
                    Path tempDriversDir = DriverDescriptor.getExternalDriversStorageFolder();
                    Path driverLibsFolder = Files.isDirectory(localFile) ? Path.of(library.getPath()) :
                                            Path.of(library.getPath()).getParent();
                    Path realDriverLibsFolder = tempDriversDir.resolve(driverLibsFolder);

                    List<Path> externalLibraryFiles = new ArrayList<>();

                    if (Files.isDirectory(localFile)) {
                        externalLibraryFiles.addAll(readJarsFromDir(localFile));
                    } else {
                        externalLibraryFiles.add(localFile);
                    }

                    try {
                        for (Path externalLibraryFilePath : externalLibraryFiles) {
                            // toString to avoid conflict between fs
                            String jarName = externalLibraryFilePath.getFileName().toString();
                            Path realLibraryPath = realDriverLibsFolder.resolve(jarName);

                            if (!Files.exists(realLibraryPath.getParent())) {
                                Files.createDirectories(realLibraryPath.getParent());
                            }
                            if (!Files.exists(realLibraryPath) ||
                                Files.getLastModifiedTime(realLibraryPath).toInstant()
                                    .isBefore(Files.getLastModifiedTime(externalLibraryFilePath).toInstant())) {
                                log.info("Copy driver library from from external file system " + externalLibraryFilePath + " to " +
                                    "the temporary location " + realLibraryPath);
                                Files.copy(
                                    externalLibraryFilePath,
                                    realLibraryPath,
                                    StandardCopyOption.REPLACE_EXISTING
                                );
                            }
                            if (!result.contains(realLibraryPath)) {
                                result.add(realLibraryPath);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error during copy of library file '" + library + "'", e);
                    }
                }
            }
        }

        // Check if local files are zip archives with jars inside
        return DriverUtils.extractZipArchives(result);
    }

    private List<DBPDriverLibrary> getAllLibraries() {
        List<DBPDriverLibrary> libraries = new ArrayList<>(driver.getDriverLibraries());
        if (!libraryProviders.isEmpty()) {
            for (DBPDriverLibraryProvider dlp : libraryProviders) {
                libraries.addAll(dlp.getDriverLibraries());
            }
        }
        return libraries;
    }

    @Override
    public boolean downloadDriverLibraries(@NotNull DBRProgressMonitor monitor, boolean resetVersions) {
        final DriverDependencies dependencies = getDriverDependencies(resetVersions, false);
        if (dependencies == null) {
            return true;
        }
        UIServiceDrivers serviceDrivers = DBWorkbench.getService(UIServiceDrivers.class);
        boolean downloadOk = serviceDrivers != null ?
                             serviceDrivers.downloadDriverFiles(monitor, driver, dependencies) :
                             DriverUtils.downloadDriverFiles(monitor, driver, dependencies);
        if (!downloadOk) {
            return false;
        }
        if (resetVersions) {
            Map<DBPDriverLibrary, List<DriverFileInfo>> tempResolvedFiles = new HashMap<>();
            // some drivers need to have embedded driver files so we cannot remove it from resolved files
            resolvedFiles.forEach((key, value) -> {
                if (key.isEmbedded()) {
                    tempResolvedFiles.put(key, value);
                }
            });
            resetDriverInstance();
            resolvedFiles.putAll(tempResolvedFiles);
        }
        for (DBPDriverDependencies.DependencyNode node : dependencies.getLibraryMap()) {
            List<DriverFileInfo> info = new ArrayList<>();
            resolvedFiles.put(node.library, info);
            collectLibraryFiles(node, info);
        }
        driver.getProviderDescriptor().getRegistry().saveDrivers();
        return true;
    }

    @Override
    public boolean isDriverInstalled() {
        return getDriverDependencies(false, true) == null;
    }

    /**
     * Returns driver dependencies if some driver files are not found and can be downloaded.
     */
    @Nullable
    public DriverDependencies getDriverDependencies(boolean resetVersions, boolean skipLicense) {
        boolean localLibsExists = false;
        final List<DBPDriverLibrary> downloadCandidates = new ArrayList<>();
        for (DBPDriverLibrary library : getAllLibraries()) {
            if (library.isDisabled()) {
                // Nothing we can do about it
                continue;
            }
            if (!library.matchesCurrentPlatform()) {
                // Wrong OS or architecture
                continue;
            }
            if (skipLicense && library.getType() == DBPDriverLibrary.FileType.license) {
                // Do not validate driver presence if not a license is absent
                continue;
            }
            if (library.isDownloadable()) {
                boolean allExists = true;
                if (resetVersions) {
                    allExists = false;
                } else {
                    List<DriverFileInfo> files = resolvedFiles.get(library);
                    if (files == null) {
                        allExists = false;
                    } else {
                        if (DBWorkbench.isDistributed()) {
                            break;
                        }
                        for (DriverFileInfo file : files) {
                            if (file.getFile() == null || !Files.exists(getDriverFilePath(file))) {
                                allExists = false;
                                break;
                            }
                        }
                    }
                }
                if (!allExists) {
                    downloadCandidates.add(library);
                }
            } else {
                localLibsExists = true;
            }
        }

        if (downloadCandidates.isEmpty() && (localLibsExists || driver.getDriverFileSources().isEmpty())) {
            return null;
        }
        return new DriverDependencies(downloadCandidates);
    }

    private boolean isResolvedLibraryPresent(@NotNull DBPDriverLibrary library) {
        if (library.isDownloadable()) {
            List<DriverFileInfo> files = resolvedFiles.get(library);
            if (files == null) {
                return false;
            } else {
                for (DriverFileInfo file : files) {
                    if (file.getFile() == null || !Files.exists(getDriverFilePath(file))) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            Path localFile = library.getLocalFile();
            return localFile != null && Files.exists(localFile);
        }
    }

    private Path getDriverFilePath(DriverFileInfo file) {
        if (DBWorkbench.isDistributed()) {
            return DriverDescriptor.getWorkspaceDriversStorageFolder().resolve(file.getFile());
        }
        return file.getFile();
    }


    public Map<DBPDriverLibrary, List<DriverFileInfo>> getResolvedFiles() {
        return resolvedFiles;
    }

    private List<Path> syncDpiDependencies(DBPApplication application) {
        if (application instanceof DBPApplicationDPI driversProvider) {
            List<Path> result = new ArrayList<>();
            List<Path> librariesPath = driversProvider.getDriverLibsLocation(driver.getId());
            for (Path path : librariesPath) {
                if (Files.isDirectory(path)) {
                    result.addAll(readJarsFromDir(path));
                } else {
                    if (!result.contains(path)) {
                        result.add(path);
                    }
                }
            }
            return DriverUtils.extractZipArchives(result);
        } else {
            log.error("Detached process has no ability to find/download driver libraries, " +
                "it must be specified directly"
            );
        }
        return List.of();
    }

    private Collection<? extends Path> readJarsFromDir(Path localFile) {
        try (Stream<Path> list = Files.list(localFile)) {
            return list
                .filter(p -> {
                    String fileName = p.getFileName().toString();
                    return fileName.endsWith(".jar") || fileName.endsWith(".zip");
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error reading driver directory '" + localFile + "'", e);
            return Collections.emptyList();
        }
    }

    /**
     * Sync driver libs with remote server
     */
    private List<Path> syncDistributedDependencies(DBRProgressMonitor monitor) {
        List<Path> localFilePaths = new ArrayList<>();

        final Map<DBPDriverLibrary, List<DriverFileInfo>> downloadCandidates = new LinkedHashMap<>();
        Path driverFolder = DriverDescriptor.getExternalDriversStorageFolder();
        for (DBPDriverLibrary library : getAllLibraries()) {
            if (monitor.isCanceled()) {
                break;
            }
            if (library.isDisabled() || !library.matchesCurrentPlatform()) {
                continue;
            }
            if (library instanceof DriverLibraryLocal localLib) {
                if (localLib.isUseOriginalJar()) {
                    var localFile = localLib.getLocalFile();
                    if (localFile == null) {
                        continue;
                    }
                    localFilePaths.add(localFile);
                    if (Files.isDirectory(localFile)) {
                        localFilePaths.addAll(readJarsFromDir(localFile));
                    }
                }
            }
            List<DriverFileInfo> files = resolvedFiles.get(library);
            if (files != null) {
                for (DriverFileInfo depFile : files) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    Path localDriverFile = driverFolder.resolve(depFile.getFile().toString());
                    if (crcNotMatch(depFile, localDriverFile))
                    {
                        downloadCandidates
                            .computeIfAbsent(library, key -> new ArrayList<>())
                            .add(depFile);
                    } else {
                        localFilePaths.add(localDriverFile);
                    }
                }
            }
        }

        if (!downloadCandidates.isEmpty()) {
            DBFileController fileController = DBWorkbench.getPlatform().getFileController();
            for (var libEntry : downloadCandidates.entrySet()) {
                if (monitor.isCanceled()) {
                    break;
                }
                DBPDriverLibrary library = libEntry.getKey();
                List<DriverFileInfo> libFiles = libEntry.getValue();
                monitor.beginTask("Load driver library '" + library.getDisplayName() + "'", libFiles.size());
                for (DriverFileInfo fileInfo : libFiles) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    try {
                        Path localDriverFile = driverFolder.resolve(fileInfo.getFile().toString());
                        if (!Files.exists(localDriverFile.getParent())) {
                            Files.createDirectories(localDriverFile.getParent());
                        }

                        monitor.subTask("Load driver file '" + fileInfo.getId() + "'");
                        byte[] fileData = fileController.loadFileData(
                            DBFileController.TYPE_DATABASE_DRIVER,
                            DriverUtils.getDistributedLibraryPath(fileInfo.getFile())
                        );
                        Files.write(localDriverFile, fileData, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                        fileInfo.setFileCRC(DriverUtils.calculateFileCRC(localDriverFile));
                        localFilePaths.add(localDriverFile);
                    } catch (Exception e) {
                        log.error("Error downloading driver file '" + fileInfo.getFile() + "'", e);
                    } finally {
                        monitor.worked(1);
                    }
                }
                monitor.done();
            }
        }

        return localFilePaths;
    }

    private static boolean crcNotMatch(@NotNull DriverFileInfo depFile, @NotNull Path localDriverFile) {
        return !Files.exists(localDriverFile) || depFile.getFileCRC() == 0 ||
            depFile.getFileCRC() != DriverUtils.calculateFileCRC(localDriverFile);
    }

    List<DriverFileInfo> getCachedFiles(DBPDriverLibrary library) {
        return resolvedFiles.get(library);
    }

    private void checkDriverVersion(DBRProgressMonitor monitor) throws IOException {
        for (DBPDriverLibrary library : getAllLibraries()) {
            final Collection<String> availableVersions = library.getAvailableVersions(monitor);
            if (!CommonUtils.isEmpty(availableVersions)) {
                final String curVersion = library.getVersion();
                String latestVersion = VersionUtils.findLatestVersion(availableVersions);
                if (latestVersion != null && !latestVersion.equals(curVersion)) {
                    log.debug("Update driver " + driver.getName() + " " + curVersion + "->" + latestVersion);
                }
            }
        }

    }

    public boolean isLibraryResolved(DBPDriverLibrary library) {
        return !library.isDownloadable() || !CommonUtils.isEmpty(resolvedFiles.get(library));
    }

    public Collection<DriverFileInfo> getLibraryFiles(DBPDriverLibrary library) {
        return resolvedFiles.get(library);
    }

    private void collectLibraryFiles(DBPDriverDependencies.DependencyNode node, List<DriverFileInfo> files) {
        if (node.duplicate) {
            return;
        }
        files.add(new DriverFileInfo(node.library));
        for (DBPDriverDependencies.DependencyNode sub : node.dependencies) {
            collectLibraryFiles(sub, files);
        }
    }


    /**
     * Removes all resolved files associated with the given driver library.
     * This effectively resets the library's file list to an empty state.
     *
     * @param library the driver library whose associated files should be removed
     */
    public void removeLibraryFiles(DBPDriverLibrary library) {
        resolvedFiles.put(library, new ArrayList<>());
    }

    public void addLibraryFile(DBPDriverLibrary library, DriverFileInfo fileInfo) {
        List<DriverFileInfo> files = resolvedFiles.computeIfAbsent(library, k -> new ArrayList<>());
        files.add(fileInfo);
    }

    /**
     * Add resolved files to all libraries
     */
    @Override
    public boolean resolveDriverFiles(Path targetFileLocation) {
        List<? extends DBPDriverLibrary> libraries = getAllLibraries();
        if (libraries.isEmpty()) {
            return false;
        }
        // we need to check resolved files from config for remove or maven libraries
        Map<DBPDriverLibrary, List<DriverFileInfo>> tempResolvedFiles = new HashMap<>(resolvedFiles);
        resolvedFiles.clear();
        for (DBPDriverLibrary library : libraries) {
            // We need to sync resolved files with real files of library
            // - Local files are linked directly
            // - Local folders are linked to folder's contents
            if (library instanceof DriverLibraryLocal && !library.isDownloadable()) {
                List<DriverFileInfo> libraryFiles = new ArrayList<>();

                if (library.isCustom()) {
                    // Resolve custom libraries directly from file
                    Path customFile = targetFileLocation
                        .resolve(library.getPath());
                    if (Files.exists(customFile)) {
                        customFile = targetFileLocation.relativize(customFile);
                        DriverFileInfo fileInfo = new DriverFileInfo(
                            library.getId(),
                            library.getVersion(),
                            library.getType(),
                            customFile,
                            library.getPath()
                        );
                        libraryFiles.add(fileInfo);
                        resolvedFiles.put(library, libraryFiles);
                        continue;
                    } else {
                        log.debug("Driver library path '" + library.getPath() + "' cannot be resolved at '" + customFile + "'. Skipping.");
                    }
                }
                Path srcLocalFile = library.getLocalFile();
                if (srcLocalFile == null) {
                    if (library.getType() != DBPDriverLibrary.FileType.license) {
                        log.warn("\t-Driver '" + driver.getFullId() + "' library file '" + library.getPath() + "' is missing");
                    }
                    continue;
                }
                if (!Files.exists(srcLocalFile)) {
                    if (library.getType() != DBPDriverLibrary.FileType.license) {
                        log.warn("\tDriver '" + driver.getFullId() + "' library file '" + srcLocalFile.toAbsolutePath() + "' doesn't exist");
                    }
                    continue;
                }

                String targetPath = library.getPath();
                int divPos = targetPath.indexOf(":");
                if (divPos != -1) {
                    targetPath = targetPath.substring(divPos + 1);
                    while (targetPath.startsWith("/")) targetPath = targetPath.substring(1);
                }

                if (Files.isDirectory(srcLocalFile)) {
                    Path targetFolder = targetFileLocation.resolve(targetPath);

                    try {
                        resolveDirectories(targetFileLocation, library, srcLocalFile, targetFolder, libraryFiles);
                    } catch (IOException e) {
                        log.error("Error resolving directory files at '" + srcLocalFile + "'", e);
                    }
                } else {
                    Path trgLocalFile = targetFileLocation.resolve(targetPath);
                    DriverFileInfo fileInfo = resolveFile(targetFileLocation, library, srcLocalFile, trgLocalFile);
                    if (fileInfo != null) {
                        libraryFiles.add(fileInfo);
                    }
                }

                if (!libraryFiles.isEmpty()) {
                    resolvedFiles.put(library, libraryFiles);
                }

            } else {
                // we need to check that resolved files from drivers.xml are exist
                // we don't want to resolve maven artifact from maven registry (it takes a long time)
                List<DriverFileInfo> libraryResolvedFiles = tempResolvedFiles.get(library);
                if (libraryResolvedFiles == null || libraryResolvedFiles.isEmpty()) {
                    continue;
                }
                List<DriverFileInfo> libraryFiles = new ArrayList<>();
                for (DriverFileInfo fileInfo : libraryResolvedFiles) {
                    try {
                        Path targetFile = IOUtils.isFileFromDefaultFS(targetFileLocation)
                                          ? targetFileLocation.resolve(fileInfo.getFile())
                                          : targetFileLocation.resolve(fileInfo.getFileLocation());

                        if (Files.exists(targetFile)) {
                            libraryFiles.add(fileInfo);
                        }
                    } catch (Exception e) {
                        log.error("Error resolve: " + targetFileLocation + " with " + fileInfo.getFile());
                        log.error(e.getMessage(), e);
                    }
                }
                if (!libraryFiles.isEmpty()) {
                    resolvedFiles.put(library, libraryFiles);
                }
            }
        }
        if (resolvedFiles.isEmpty()) {
            return false;
        }
        driver.setModified(true);
        return true;
    }

    private void resolveDirectories(Path targetFileLocation, DBPDriverLibrary library, Path srcLocalFile, Path trgLocalFile, List<DriverFileInfo> libraryFiles) throws IOException {
        // Resolve directory contents
        try (Stream<Path> list = Files.list(srcLocalFile)) {
            List<Path> srcDirFiles = list.toList();
            for (Path dirFile : srcDirFiles) {
                String fileName = dirFile.getFileName().toString();
                // Skip non-libraries
                if (fileName.endsWith(".txt")) {
                    continue;
                }
                Path trgDirFile = trgLocalFile.resolve(dirFile.getFileName().toString());
                if (Files.isDirectory(dirFile)) {
                    resolveDirectories(targetFileLocation, library, dirFile, trgDirFile, libraryFiles);
                } else {
                    DriverFileInfo fileInfo = resolveFile(targetFileLocation, library, dirFile, trgDirFile);
                    if (fileInfo != null) {
                        libraryFiles.add(fileInfo);
                    }
                }
            }
        }
    }

    private DriverFileInfo resolveFile(Path targetFileLocation, DBPDriverLibrary library, Path srcLocalFile, Path trgLocalFile) {
        Path relPath = targetFileLocation.relativize(trgLocalFile);
        DriverFileInfo info = new DriverFileInfo(trgLocalFile.getFileName().toString(), null, library.getType(),
            relPath, trgLocalFile.toString());
        info.setFileCRC(DriverUtils.calculateFileCRC(srcLocalFile));
        return info;
    }


}
