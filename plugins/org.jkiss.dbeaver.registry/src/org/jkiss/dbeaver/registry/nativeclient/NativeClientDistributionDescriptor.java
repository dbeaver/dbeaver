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

package org.jkiss.dbeaver.registry.nativeclient;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenArtifact;
import org.jkiss.dbeaver.registry.driver.DriverLibraryRepository;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.dbeaver.registry.maven.MavenArtifactVersion;
import org.jkiss.dbeaver.registry.maven.MavenRegistry;
import org.jkiss.dbeaver.registry.maven.MavenRepository;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.SecurityUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NativeClientDistributionDescriptor
 */
public class NativeClientDistributionDescriptor extends AbstractDescriptor {
    private static final Log log = Log.getLog(NativeClientDistributionDescriptor.class);

    private final List<NativeClientFileDescriptor> files = new ArrayList<>();
    private final Set<String> supportedDrivers = new HashSet<>();
    private OSDescriptor os;
    //local resource installation path
    private String targetPath;
    //path to download from (if not bundled)
    private String remotePath;
    //path for bundled resources
    private String resourcePath;

    //executable folder
    private String executionPath;
    private String type;

    public NativeClientDistributionDescriptor(IConfigurationElement config) {
        super(config);
        String osName = config.getAttribute(RegistryConstants.ATTR_OS);
        this.os = osName == null ? null : new OSDescriptor(
            osName,
            config.getAttribute(RegistryConstants.ATTR_ARCH));

        this.targetPath = config.getAttribute("targetPath");
        this.remotePath = config.getAttribute("remotePath");
        this.resourcePath = config.getAttribute("resourcePath");
        this.executionPath = config.getAttribute("executionPath");
        if (CommonUtils.isNotEmpty(config.getAttribute("executionPath"))) {
            this.executionPath = this.targetPath + "/" + config.getAttribute("executionPath");
        } else {
            this.executionPath = this.targetPath;
        }
        for (IConfigurationElement fileElement : config.getChildren("file")) {
            if (DriverUtils.matchesBundle(fileElement)) {
                this.files.add(new NativeClientFileDescriptor(fileElement));
            }
        }
        this.type = config.getAttribute("type");
        for (IConfigurationElement element : config.getChildren(RegistryConstants.TAG_DRIVER)) {
            supportedDrivers.add(element.getAttribute(RegistryConstants.ATTR_ID));
        }
    }

    public OSDescriptor getOs() {
        return os;
    }

    public boolean supports(@NotNull DBPDriver driver) {
        return supportedDrivers.isEmpty() || supportedDrivers.contains(driver.getId());
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getExecutionPath() {
        return executionPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public boolean downloadFiles(DBRProgressMonitor monitor, DBPNativeClientLocation location) throws DBException, InterruptedException {
        File targetPath = location.getPath();
        List<DownloadCandidate> filesToDownload = getDownloadCandidates(targetPath);

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
            DownloadCandidate downloadCandidate = filesToDownload.get(i);

            String fileRemotePath = downloadCandidate.remotePath();
            String localResourcePath = downloadCandidate.localResourcePath();
            File targetFile = downloadCandidate.targetInstallationPath();
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
                String fileName = targetFile.getName();
                // Repository file
                fileRemotePath = fileRemotePath.substring(DriverLibraryRepository.PATH_PREFIX.length());
                String primarySource = DriverDescriptor.getDriversPrimarySource();
                if (!primarySource.endsWith("/") && !fileRemotePath.startsWith("/")) {
                    primarySource += '/';
                }
                String externalURL = primarySource + fileRemotePath;
                String taskName = "Download local client file '" + fileName + "'" + " (" + (i + 1) + "/" + filesToDownload.size() + ")";
                monitor.beginTask(taskName, 1);
                try {
                    WebUtils.downloadRemoteFile(monitor,
                        taskName,
                        externalURL,
                        targetFile.toPath(),
                        null);
                } catch (IOException e) {
                    log.debug("Error downloading file '" + fileName + "'", e);
                    throw new DBException("Error downloading file '" + fileName + "': " + e.getMessage());
                }
            } else if (fileRemotePath.startsWith(DriverLibraryMavenArtifact.PATH_PREFIX)) {
                String mavenPath = fileRemotePath.substring(DriverLibraryMavenArtifact.PATH_PREFIX.length());
                var mavenArtifactRef = new MavenArtifactReference(mavenPath);
                var mavenRegistry = MavenRegistry.getInstance();
                MavenArtifactVersion version = mavenRegistry
                    .findArtifact(
                        new LoggingProgressMonitor(), null, mavenArtifactRef,
                        mavenRegistry.getRepositories()
                            .stream()
                            .filter(MavenRepository::isArtifactory)
                            .toList()
                    );
                if (version == null) {
                    throw new DBException("Can't find maven artifact '" + mavenPath + "'");
                }
                String externalUrl = CommonUtils.isEmpty(type) ? version.getExternalURL()
                    : version.getExternalURL(type);
                try {
                    Path tempFolder = DBWorkbench.getPlatform().getTempFolder(monitor, "driver-files");
                    Path tempFile = tempFolder.resolve(SecurityUtils.makeDigest(localResourcePath));
                    WebUtils.downloadRemoteFile(
                        monitor,
                        "Download native client artifact '" + mavenPath + "'",
                        externalUrl,
                        tempFile,
                        version.getArtifact().getRepository().getAuthInfo()
                    );
                    if ("zip".equalsIgnoreCase(type)) {
                        try (var io = Files.newInputStream(tempFile)) {
                            IOUtils.extractZipArchive(io, targetFile.toPath());
                        }
                    } else {
                        Files.move(tempFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new DBException("Error creating temporary file for '" + localResourcePath + "'", e);
                }
            }
        }
        return true;
    }

    @NotNull
    private List<DownloadCandidate> getDownloadCandidates(File targetPath) {
        List<DownloadCandidate> filesToDownload = new ArrayList<>();
        for (NativeClientFileDescriptor file : files) {
            String fileName = file.getName();
            File targetFile = new File(targetPath, fileName);
            if (!targetFile.exists()) {
                String fileRemotePath = remotePath + "/" + file.getName();
                String localResourcePath = resourcePath + "/" + file.getName();

                filesToDownload.add(new DownloadCandidate(fileRemotePath, targetFile, localResourcePath));
            }
        }
        if (isMavenArtifact()) {
            if (!targetPath.exists()) {
                filesToDownload.add(new DownloadCandidate(remotePath, targetPath, resourcePath));
            }
        }
        return filesToDownload;
    }

    private record DownloadCandidate(
        @NotNull String remotePath,
        @NotNull File targetInstallationPath,
        @NotNull String localResourcePath
    ) {
    }

    private boolean isMavenArtifact() {
        return remotePath.startsWith(DriverLibraryMavenArtifact.PATH_PREFIX);
    }

    @Override
    public String toString() {
        return os.toString();
    }
}
