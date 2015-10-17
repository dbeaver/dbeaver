/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
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
    static final Log log = Log.getLog(DriverLibraryMavenArtifact.class);

    public static final String PATH_PREFIX = "maven:/";

    private final MavenArtifactReference reference;
    protected MavenArtifactVersion localVersion;

    public DriverLibraryMavenArtifact(DriverDescriptor driver, FileType type, String path) {
        super(driver, type, path);
        reference = new MavenArtifactReference(path);
    }

    public DriverLibraryMavenArtifact(DriverDescriptor driver, IConfigurationElement config) {
        super(driver, config);
        reference = new MavenArtifactReference(path);
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
        return true;
    }

    @NotNull
    @Override
    public Collection<String> getAvailableVersions(DBRProgressMonitor monitor) throws IOException {
        MavenArtifactVersion artifactVersion = getArtifactVersion(monitor);
        if (artifactVersion != null) {
            Collection<String> availableVersion = artifactVersion.getArtifact().getAvailableVersion(monitor);
            if (availableVersion != null) {
                return availableVersion;
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public DBPDriverLibrary createVersion(DBRProgressMonitor monitor, @NotNull String version) throws IOException {
        MavenArtifactReference newReference = new MavenArtifactReference(
            this.reference.getGroupId(),
            this.reference.getArtifactId(),
            this.reference.getClassifier(),
            version);
        MavenArtifactVersion newVersion = MavenRegistry.getInstance().findArtifact(monitor, null, newReference);
        if (newVersion == null) {
            throw new IOException("Can't resolve artifact version " + newReference);
        }
        return new DriverLibraryMavenArtifact(getDriver(), getType(), PATH_PREFIX + newVersion.toString());
    }

    @Override
    public void resetVersion() {
        this.localVersion = null;
        MavenRegistry.getInstance().resetArtifactInfo(reference);
    }

    @Nullable
    protected MavenArtifactVersion getArtifactVersion(DBRProgressMonitor monitor) {
        if (this.localVersion == null) {
            this.localVersion = MavenRegistry.getInstance().findArtifact(monitor, null, reference);
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
