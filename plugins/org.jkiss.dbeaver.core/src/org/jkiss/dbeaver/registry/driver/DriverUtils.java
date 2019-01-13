/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.ProductBundleRegistry;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DriverUtils
 */
public class DriverUtils {
    private static final Log log = Log.getLog(DriverUtils.class);

    public static final String ZIP_EXTRACT_DIR = "zip-cache";

    public static boolean isBetaVersion(@NotNull String versionInfo) {
        return versionInfo.contains("beta") || versionInfo.contains("alpha");
    }

    @Nullable
    public static String findLatestVersion(@NotNull Collection<String> allVersions) {
        String latest = null;
        for (String version : allVersions) {
            if (isBetaVersion(version)) {
                continue;
            }
            if (latest == null || compareVersions(version, latest) > 0) {
                latest = version;
            }
        }
        if (latest == null) {
            // Now use beta versions too
            for (String version : allVersions) {
                if (latest == null || compareVersions(version, latest) > 0) {
                    latest = version;
                }
            }
        }
        return latest;
    }

    public static int compareVersions(@NotNull String v1, @NotNull String v2) {
        StringTokenizer st1 = new StringTokenizer(v1, ".-_");
        StringTokenizer st2 = new StringTokenizer(v2, ".-_");
        while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
            String t1 = st1.nextToken();
            String t2 = st2.nextToken();
            try {
                int cmp = Integer.parseInt(t1) - Integer.parseInt(t2);
                if (cmp != 0) {
                    return cmp;
                }
            } catch (NumberFormatException e) {
                // Non-numeric versions - use lexicographical compare
                int cmp = t1.compareTo(t2);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }
        if (st1.hasMoreTokens()) {
            return 1;
        } else if (st2.hasMoreTokens()) {
            return -1;
        } else {
            return 0;
        }
    }

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

    static List<File> extractZipArchives(List<File> files) {
        if (files.isEmpty()) {
            return files;
        }
        List<File> jarFiles = new ArrayList<>();
        for (File inputFile : files) {
            jarFiles.add(inputFile);
            if (!inputFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
                continue;
            }
            // Seems to be a zip. Let's try it.
            try (InputStream is = new FileInputStream(inputFile)) {
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
                log.debug("Error processing zip archive '" + inputFile.getName() + "': " + e.getMessage());
            }
        }

        return jarFiles;
    }

    private static void checkAndExtractEntry(File sourceFile, InputStream zipStream, ZipEntry zipEntry, List<File> jarFiles) throws IOException {
        String sourceName = sourceFile.getName();
        if (sourceName.endsWith(".zip")) {
            sourceName = sourceName.substring(0, sourceName.length() - 4);
        }
        File localCacheDir =
            new File(
                new File(DriverDescriptor.getCustomDriversHome(), ZIP_EXTRACT_DIR),
                sourceName);
        if (!localCacheDir.exists() && !localCacheDir.mkdirs()) {
            throw new IOException("Can't create local cache folder '" + localCacheDir.getAbsolutePath() + "'");
        }
        File localFile = new File(localCacheDir, zipEntry.getName());
        jarFiles.add(localFile);
        if (localFile.exists()) {
            // Already extracted
            return;
        }
        try (FileOutputStream os = new FileOutputStream(localFile)) {
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

}
