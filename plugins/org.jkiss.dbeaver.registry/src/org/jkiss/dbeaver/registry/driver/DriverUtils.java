/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.ProductBundleRegistry;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DriverUtils
 */
public class DriverUtils {
    private static final Log log = Log.getLog(DriverUtils.class);

    public static final String ZIP_EXTRACT_DIR = "zip-cache";

    public static boolean matchesBundle(IConfigurationElement config) {
        // Check bundle
        String bundle = config.getAttribute(RegistryConstants.ATTR_BUNDLE);
        if (!CommonUtils.isEmpty(bundle)) {
            boolean not = false;
            if (bundle.startsWith("!")) {
                not = true;
                bundle = bundle.substring(1);
            }
            boolean hasBundle = ProductBundleRegistry.getInstance().hasBundle(bundle);
            if ((!hasBundle && !not) || (hasBundle && not)) {
                // This file is in bundle which is not included in the product.
                // Or it is marked as exclusive and bundle exists.
                // Skip it in both cases.
                return false;
            }
        }
        return true;
    }

    static void copyZipStream(InputStream inputStream, OutputStream outputStream)
        throws IOException
    {
        byte[] writeBuffer = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
        for (int br = inputStream.read(writeBuffer); br != -1; br = inputStream.read(writeBuffer)) {
            outputStream.write(writeBuffer, 0, br);
        }
        outputStream.flush();
    }

    static List<Path> extractZipArchives(List<Path> files) {
        if (files.isEmpty()) {
            return files;
        }
        List<Path> jarFiles = new ArrayList<>();
        for (Path inputFile : files) {
            jarFiles.add(inputFile);
            if (!inputFile.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
                continue;
            }
            // Seems to be a zip. Let's try it.
            try (InputStream is = Files.newInputStream(inputFile)) {
                try (ZipInputStream zipStream = new ZipInputStream(is)) {
                    for (; ; ) {
                        ZipEntry zipEntry = zipStream.getNextEntry();
                        if (zipEntry == null) {
                            break;
                        }
                        try {
                            if (!zipEntry.isDirectory()) {
                                String zipEntryName = zipEntry.getName();
                                if (zipEntryName.endsWith(".class")) {
                                    // This is a jar with classes. Stop processing.
                                    break;
                                }
                                if (zipEntryName.endsWith(".jar") || zipEntryName.endsWith(".zip")) {
                                    checkAndExtractEntry(inputFile, zipStream, zipEntry, jarFiles);
                                }
                            }
                        } finally {
                            zipStream.closeEntry();
                        }
                    }
                }

            } catch (Exception e) {
                // No a zip
                log.debug("Error processing zip archive '" + inputFile.getFileName() + "': " + e.getMessage());
            }
        }

        return jarFiles;
    }

    private static void checkAndExtractEntry(Path sourceFile, InputStream zipStream, ZipEntry zipEntry, List<Path> jarFiles) throws IOException {
        String sourceName = sourceFile.getFileName().toString();
        if (sourceName.endsWith(".zip")) {
            sourceName = sourceName.substring(0, sourceName.length() - 4);
        }
        Path localCacheDir = DriverDescriptor.getCustomDriversHome().resolve(ZIP_EXTRACT_DIR).resolve(sourceName);
        if (!Files.exists(localCacheDir)) {
            try {
                Files.createDirectories(localCacheDir);
            } catch (IOException e) {
                throw new IOException("Can't create local cache folder '" + localCacheDir.toAbsolutePath() + "'", e);
            }
        }
        Path localFile = localCacheDir.resolve(zipEntry.getName());
        if (!localFile.normalize().startsWith(localCacheDir.normalize())) {
            throw new IOException("Zip entry is outside of the target directory");
        }
        jarFiles.add(localFile);
        if (Files.exists(localFile)) {
            // Already extracted
            return;
        }
        Path localDir = localFile.getParent();
        if (!Files.exists(localDir)) { // in case of localFile located in subdirectory inside zip archive
            try {
                Files.createDirectories(localDir);
            } catch (IOException e) {
                throw new IOException("Can't create local file directory in the cache '" + localDir.toAbsolutePath() + "'", e);
            }
        }
        try (OutputStream os = Files.newOutputStream(localFile)) {
            copyZipStream(zipStream, os);
        }
    }

    public static List<DBPDataSourceContainer> getUsedBy(DBPDriver driver, List<DBPDataSourceContainer> containers) {
        List<DBPDataSourceContainer> usedBy = new ArrayList<>();
        for (DBPDataSourceContainer ds : containers) {
            if (ds.getDriver() == driver) {
                usedBy.add(ds);
            }
        }
        return usedBy;
    }

    public static List<DBPDriver> getRecentDrivers(List<DBPDriver> allDrivers, int total) {
        List<DBPDataSourceContainer> allDataSources = DataSourceRegistry.getAllDataSources();

//        Map<DBPDriver, Integer> connCountMap = new HashMap<>();
//        for (DBPDriver driver : allDrivers) {
//            connCountMap.put(driver, getUsedBy(driver, allDataSources).size());
//        }

        List<DBPDriver> recentDrivers = new ArrayList<>(allDrivers);
        sortDriversByRating(allDataSources, recentDrivers);
        if (recentDrivers.size() > total) {
            return recentDrivers.subList(0, total);
        }
        return recentDrivers;
    }

    public static void sortDriversByRating(List<DBPDataSourceContainer> allDataSources, List<DBPDriver> drivers) {
        try {
            drivers.sort(new DriverScoreComparator(allDataSources));
        } catch (Throwable e) {
            // ignore
        }
    }

    public static List<DBPDriver> getAllDrivers() {
        List<? extends DBPDataSourceProviderDescriptor> providers = DBWorkbench.getPlatform().getDataSourceProviderRegistry().getEnabledDataSourceProviders();

        List<DBPDriver> allDrivers = new ArrayList<>();
        for (DBPDataSourceProviderDescriptor dpd : providers) {
            allDrivers.addAll(dpd.getEnabledDrivers());
        }
        allDrivers.sort(Comparator.comparing(DBPNamedObject::getName));

        return allDrivers;
    }

    public static boolean downloadDriverFiles(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driverDescriptor,
        @NotNull DBPDriverDependencies dependencies
    ) {
        try {
            dependencies.resolveDependencies(monitor);
        } catch (DBException e) {
            log.error("Error resolving dependencies", e);
            return false;
        }
        List<DBPDriverDependencies.DependencyNode> nodes = dependencies.getLibraryList();
        for (DBPDriverDependencies.DependencyNode node : nodes) {
            if (monitor.isCanceled()) {
                break;
            }
            final DBPDriverLibrary lib = node.library;
            try {
                lib.downloadLibraryFile(
                    monitor,
                    false,
                    "Download driver '" + driverDescriptor.getFullName() + "' library '" + lib.getDisplayName() + "'");
            } catch (final IOException e) {
                log.error(e);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    public static class DriverNameComparator implements Comparator<DBPDriver> {

        @Override
        public int compare(DBPDriver o1, DBPDriver o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }

    public static class DriverScoreComparator extends DriverNameComparator {
        private final List<DBPDataSourceContainer> dataSources;

        public DriverScoreComparator(List<DBPDataSourceContainer> dataSources) {
            this.dataSources = dataSources;
        }

        @Override
        public int compare(DBPDriver o1, DBPDriver o2) {
            int ub1 = getUsedBy(o1, dataSources).size() + o1.getPromotedScore();
            int ub2 = getUsedBy(o2, dataSources).size() + o2.getPromotedScore();
            if (ub1 == ub2) {
                return super.compare(o1, o2);
            } else {
                return ub2 - ub1;
            }
        }
    }

}
