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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverLibraryRepository;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * NativeClientDistributionDescriptor
 */
public class NativeClientDistributionDescriptor {
    private static final Log log = Log.getLog(NativeClientDistributionDescriptor.class);

    private final List<NativeClientFileDescriptor> files = new ArrayList<>();
    private OSDescriptor os;
    private String targetPath;
    private String remotePath;
    private String resourcePath;

    public NativeClientDistributionDescriptor(IConfigurationElement config) {
        String osName = config.getAttribute(RegistryConstants.ATTR_OS);
        this.os = osName == null ? null : new OSDescriptor(
            osName,
            config.getAttribute(RegistryConstants.ATTR_ARCH));

        this.targetPath = config.getAttribute("targetPath");
        this.remotePath = config.getAttribute("remotePath");
        this.resourcePath = config.getAttribute("resourcePath");
        for (IConfigurationElement fileElement : config.getChildren("file")) {
            if (DriverUtils.matchesBundle(fileElement)) {
                this.files.add(new NativeClientFileDescriptor(fileElement));
            }
        }
    }

    public OSDescriptor getOs() {
        return os;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public boolean downloadFiles(DBRProgressMonitor monitor, DBPNativeClientLocation location) throws DBException, InterruptedException {
        File targetPath = location.getPath();
        List<NativeClientFileDescriptor> filesToDownload = new ArrayList<>();
        for (NativeClientFileDescriptor file : files) {
            String fileName = file.getName();
            File targetFile = new File(targetPath, fileName);
            if (!targetFile.exists()) {
                filesToDownload.add(file);
            }
        }

        if (filesToDownload.isEmpty()) {
            return true;
        }

        if (!targetPath.exists()) {
            if (!targetPath.mkdirs()) {
                throw new DBException("Can't create target folder '" + targetPath.getAbsolutePath() + "'");
            }
        }
        for (int i = 0; i < filesToDownload.size(); i++) {
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            NativeClientFileDescriptor file = filesToDownload.get(i);
            String fileName = file.getName();
            File targetFile = new File(targetPath, fileName);

            String fileRemotePath = remotePath + "/" + file.getName();
            String localResourcePath = resourcePath + "/" + file.getName();

            {
                // Try to extract local resource file
                URL url = DataSourceProviderRegistry.getInstance().findResourceURL(localResourcePath);
                if (url != null) {
                    try {
                        url = FileLocator.toFileURL(url);
                        File localFile = new File(url.getFile());
                        if (localFile.exists()) {
                            try (InputStream is = new FileInputStream(localFile)) {
                                try (OutputStream os = new FileOutputStream(targetFile)) {
                                    ContentUtils.copyStreams(is, localFile.length(), os, monitor);
                                }
                                continue;
                            } catch (IOException e) {
                                if (targetFile.exists()) {
                                    if (!targetFile.delete()) {
                                        log.debug("Error deleting client file '" + targetFile.getAbsolutePath() + "'");
                                    }
                                }
                                log.debug("IO error copying resource file '" + localResourcePath + "'", e);
                            }
                        }
                    } catch (IOException ex) {
                        log.debug("Error locating resource file '" + localResourcePath + "'", ex);
                    }
                }
            }
            // Try to download remote file
            if (fileRemotePath.startsWith(DriverLibraryRepository.PATH_PREFIX)) {
                // Repository file
                fileRemotePath = fileRemotePath.substring(DriverLibraryRepository.PATH_PREFIX.length());
                String primarySource = DriverDescriptor.getDriversPrimarySource();
                if (!primarySource.endsWith("/") && !fileRemotePath.startsWith("/")) {
                    primarySource += '/';
                }
                String externalURL = primarySource + fileRemotePath;
                String taskName = "Download native client file '" + fileName + "'" + " (" + (i + 1) + "/" + filesToDownload.size() + ")";
                monitor.beginTask(taskName, 1);
                try {
                    WebUtils.downloadRemoteFile(monitor,
                        taskName,
                        externalURL,
                        targetFile,
                        null);
                } catch (IOException e) {
                    log.debug("Error downloading file '" + fileName + "'", e);
                    throw new DBException("Error downloading file '" + fileName + "': " + e.getMessage());
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return os.toString();
    }
}
