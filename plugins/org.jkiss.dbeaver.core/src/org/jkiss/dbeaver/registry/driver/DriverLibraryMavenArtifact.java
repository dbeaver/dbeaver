/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.maven.*;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * DriverLibraryDescriptor
 */
public class DriverLibraryMavenArtifact extends DriverLibraryAbstract
{
    private static final Log log = Log.getLog(DriverLibraryMavenArtifact.class);

    public static final String PATH_PREFIX = "maven:/";

    private MavenArtifactReference reference;
    protected MavenArtifactVersion localVersion;
    private String preferredVersion;

    public DriverLibraryMavenArtifact(DriverDescriptor driver, FileType type, String path, String preferredVersion) {
        super(driver, type, path);
        initArtifactReference(preferredVersion);
    }

    public DriverLibraryMavenArtifact(DriverDescriptor driver, IConfigurationElement config) {
        super(driver, config);
        initArtifactReference(null);
    }

    private void initArtifactReference(String preferredVersion) {
        if (path.endsWith("]")) {
            int divPos = path.lastIndexOf('[');
            if (divPos != -1) {
                String version = path.substring(divPos + 1, path.length() - 1);
                path = path.substring(0, divPos);
                if (preferredVersion == null) {
                    preferredVersion = version;
                }
            }
        }
        this.reference = new MavenArtifactReference(path);
        this.preferredVersion = preferredVersion;
    }

    @Override
    public String getDescription()
    {
        if (localVersion != null) {
            return localVersion.getDescription();
        }
        return null;
    }

    @Override
    public boolean isDownloadable()
    {
        if (localVersion != null) {
            //return !"pom".equals(localVersion.getPackaging());
        }
        return true;
    }

    @NotNull
    @Override
    public Collection<String> getAvailableVersions(DBRProgressMonitor monitor) throws IOException {
        MavenArtifactVersion artifactVersion = getArtifactVersion(monitor);
        if (artifactVersion != null) {
            Collection<String> availableVersions = artifactVersion.getArtifact().getAvailableVersions(monitor, reference.getVersion());
            if (availableVersions != null) {
                return availableVersions;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getPreferredVersion() {
        return preferredVersion;
    }

    @Override
    public void setPreferredVersion(String version) {
        this.preferredVersion = version;
        this.localVersion = null;
    }

    @Override
    public void resetVersion() {
        this.localVersion = null;
        MavenRegistry.getInstance().resetArtifactInfo(reference);
    }

    @Override
    public boolean isSecureDownload(DBRProgressMonitor monitor) {
        try {
            MavenArtifactVersion localVersion = resolveLocalVersion(monitor, false);
            if (localVersion == null) {
                return true;
            }
            return localVersion.getArtifact().getRepository().isSecureRepository();
        } catch (IOException e) {
            log.warn("Error resolving artifact version", e);
            return true;
        }
    }

    @Nullable
    protected MavenArtifactVersion getArtifactVersion(DBRProgressMonitor monitor) {
        if (this.localVersion == null) {
            MavenArtifactReference ref = reference;
            if (preferredVersion != null) {
                ref = new MavenArtifactReference(reference.getGroupId(), reference.getArtifactId(), reference.getClassifier(), preferredVersion);
            }
            this.localVersion = MavenRegistry.getInstance().findArtifact(monitor, null, ref);
        }
        return this.localVersion;
    }

    @Nullable
    @Override
    public String getExternalURL(DBRProgressMonitor monitor) {
        MavenArtifactVersion localVersion = getArtifactVersion(monitor);
        if (localVersion != null) {
            return localVersion.getExternalURL(MavenArtifact.FILE_JAR);
        }
        return null;
    }


    @Nullable
    @Override
    public File getLocalFile()
    {
        // Try to get local file
        File platformFile = detectLocalFile();
        if (platformFile != null && platformFile.exists()) {
            // Relative file do not exists - use plain one
            return platformFile;
        }
        // Nothing fits - just return plain url
        return platformFile;
    }

    private File detectLocalFile()
    {
        if (localVersion != null) {
            return localVersion.getCacheFile();
        }
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBPDriverLibrary> getDependencies(@NotNull DBRProgressMonitor monitor) throws IOException {
        List<DriverLibraryMavenDependency> dependencies = new ArrayList<>();
        MavenArtifactVersion localVersion = resolveLocalVersion(monitor, false);
        if (localVersion != null) {

            List<MavenArtifactDependency> artifactDeps = localVersion.getDependencies();
            if (!CommonUtils.isEmpty(artifactDeps)) {
                for (MavenArtifactDependency dependency : artifactDeps) {
                    if (isDependencyExcluded(monitor, dependency)) {
                        continue;
                    }

                    MavenArtifactVersion depArtifact = MavenRegistry.getInstance().findArtifact(monitor, localVersion, dependency);
                    if (depArtifact != null) {
                        dependencies.add(
                            new DriverLibraryMavenDependency(
                                this,
                                depArtifact,
                                dependency));
                    } else {
                        dependency.setBroken(true);
                    }
                }
            }
        }

        return dependencies;
    }

    protected boolean isDependencyExcluded(DBRProgressMonitor monitor, MavenArtifactDependency dependency) {
        return false;
    }

    @NotNull
    public String getDisplayName() {
        return reference.toString();
    }

    @Override
    public String getId() {
        return reference.getId();
    }

    @Override
    public String getVersion() {
        if (preferredVersion != null) {
            return preferredVersion;
        }
        if (localVersion != null) {
            return localVersion.getVersion();
        }

        return reference.getVersion();
    }

    @NotNull
    @Override
    public DBIcon getIcon() {
        return UIIcon.APACHE;
    }

    public void downloadLibraryFile(@NotNull DBRProgressMonitor monitor, boolean forceUpdate, String taskName) throws IOException, InterruptedException {
        //monitor.beginTask(taskName + " - update localVersion information", 1);
        try {
            MavenArtifactVersion localVersion = resolveLocalVersion(monitor, forceUpdate);
            if (localVersion.getArtifact().getRepository().getType() == MavenRepository.RepositoryType.LOCAL) {
                // No need to download local artifacts
                return;
            }
        } finally {
            //monitor.done();
        }
        super.downloadLibraryFile(monitor, forceUpdate, taskName);
    }

    @Nullable
    @Override
    protected DBAAuthInfo getAuthInfo(DBRProgressMonitor monitor) {
        MavenArtifactVersion localVersion = getArtifactVersion(monitor);
        if (localVersion != null) {
            return localVersion.getArtifact().getRepository().getAuthInfo();
        }
        return null;
    }

    protected MavenArtifactVersion resolveLocalVersion(DBRProgressMonitor monitor, boolean forceUpdate) throws IOException {
        if (forceUpdate) {
            MavenRegistry.getInstance().resetArtifactInfo(reference);
        }
        MavenArtifactVersion version = getArtifactVersion(monitor);
        if (version == null) {
            throw new IOException("Maven artifact '" + path + "' not found");
        }
        return version;
    }

}
