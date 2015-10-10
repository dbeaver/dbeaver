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
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.maven.*;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * DriverLibraryDescriptor
 */
public class DriverLibraryMavenArtifact extends DriverLibraryAbstract
{
    static final Log log = Log.getLog(DriverLibraryMavenArtifact.class);

    public static final String PATH_PREFIX = "maven:/";

    private final MavenArtifactReference reference;

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
        return null;
    }

    @Override
    public boolean isDownloadable()
    {
        return true;
    }

    @Override
    public boolean isResolved() {
        MavenArtifact artifact = getMavenArtifact();
        return artifact != null && artifact.getActiveLocalVersion() != null;
    }

    @Nullable
    private MavenArtifact getMavenArtifact() {
        return MavenRegistry.getInstance().findArtifact(reference);
    }

    @Nullable
    protected MavenLocalVersion getMavenLocalVersion() {
        MavenArtifact artifact = getMavenArtifact();
        if (artifact != null) {
            return artifact.getActiveLocalVersion();
        }
        return null;
    }

    @Nullable
    @Override
    public String getExternalURL() {
        MavenLocalVersion localVersion = getMavenLocalVersion();
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
        MavenArtifact artifact = getMavenArtifact();
        if (artifact != null) {
            MavenLocalVersion localVersion = artifact.getActiveLocalVersion();
            if (localVersion == null && artifact.getRepository().isLocal()) {
                // In case of local artifact make version resolve
                try {
                    localVersion = artifact.resolveVersion(VoidProgressMonitor.INSTANCE, reference.getVersion(), true);
                } catch (IOException e) {
                    log.warn("Error resolving local artifact [" + artifact + "] version", e);
                }
            }
            if (localVersion != null) {
                return localVersion.getCacheFile();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBPDriverLibrary> getDependencies(@NotNull DBRProgressMonitor monitor, @Nullable DBPDriverDependencies.DependencyNode ownerNode) throws IOException {
        List<DriverLibraryMavenDependency> dependencies = new ArrayList<>();
        MavenLocalVersion localVersion = resolveLocalVersion(monitor, false);
        if (localVersion != null) {
            MavenArtifactVersion metaData = localVersion.getMetaData(monitor);
            List<MavenArtifactDependency> artifactDeps = metaData.getDependencies(monitor);
            if (!CommonUtils.isEmpty(artifactDeps)) {
                for (MavenArtifactDependency artifactDep : artifactDeps) {
                    if (isDependencyExcluded(monitor, artifactDep, ownerNode)) {
                        continue;
                    }
                    MavenLocalVersion depLocalVersion = artifactDep.resolveDependency(monitor);
                    if (depLocalVersion != null) {
                        dependencies.add(
                            new DriverLibraryMavenDependency(
                                this.getDriver(),
                                depLocalVersion));
                    }
                }
            }
        }

        return dependencies;
    }

    private boolean isDependencyExcluded(DBRProgressMonitor monitor, MavenArtifactDependency dependency, DBPDriverDependencies.DependencyNode ownerNode) {
        for (DBPDriverDependencies.DependencyNode node = ownerNode; node != null; node = node.owner) {
            DBPDriverLibrary library = node.library;
            if (library instanceof DriverLibraryMavenArtifact) {
                MavenLocalVersion ownerVersion = ((DriverLibraryMavenArtifact) library).getMavenLocalVersion();
                if (ownerVersion != null) {
                    for (MavenArtifactDependency ownerDependency : ownerVersion.getMetaData(monitor).getDependencies(monitor)) {
                        List<MavenArtifactReference> ownerDependencyExclusions = ownerDependency.getExclusions();
                        if (ownerDependencyExclusions != null) {
                            for (MavenArtifactReference exReference : ownerDependencyExclusions) {
                                if (exReference.getGroupId().equals(dependency.getGroupId()) && exReference.getArtifactId().equals(dependency.getArtifactId())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @NotNull
    public String getDisplayName() {
        MavenArtifact artifact = getMavenArtifact();
        if (artifact != null) {
            return artifact.getGroupId() + ":" + artifact.getArtifactId();
        }
        return path;
    }

    @Override
    public String getId() {
        return reference.getGroupId() + ":" + reference.getArtifactId();
    }

    @Override
    public String getVersion() {
        MavenLocalVersion version = getMavenLocalVersion();
        if (version != null) {
            return version.getVersion();
        }

        return path;
    }

    @NotNull
    @Override
    public DBIcon getIcon() {
        return UIIcon.APACHE;
    }

    public void downloadLibraryFile(@NotNull DBRProgressMonitor monitor, boolean forceUpdate) throws IOException, InterruptedException {
        monitor.beginTask("Update version information", 1);
        try {
            MavenLocalVersion localVersion = resolveLocalVersion(monitor, forceUpdate);
            if (localVersion.getArtifact().getRepository().isLocal()) {
                // No need to download local artifacts
                return;
            }
        } finally {
            monitor.done();
        }
        super.downloadLibraryFile(monitor, forceUpdate);
    }

    protected MavenLocalVersion resolveLocalVersion(DBRProgressMonitor monitor, boolean forceUpdate) throws IOException {
        if (forceUpdate) {
            MavenRegistry.getInstance().resetArtifactInfo(reference);
        }
        MavenArtifact artifact = MavenRegistry.getInstance().findArtifact(reference);
        if (artifact == null) {
            throw new IOException("Maven artifact '" + path + "' not found");
        }
        if (!forceUpdate) {
            MavenLocalVersion localVersion = artifact.getActiveLocalVersion();
            if (localVersion != null) {
                // Already cached
                return localVersion;
            }
        }
        return artifact.resolveVersion(monitor, reference.getVersion(), true);
    }

}
