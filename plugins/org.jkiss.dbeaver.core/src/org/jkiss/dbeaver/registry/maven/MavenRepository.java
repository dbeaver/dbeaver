/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry.maven;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maven repository manager.
 */
public class MavenRepository
{
    private static final Log log = Log.getLog(MavenRepository.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mavenRepository";

    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";

    public enum RepositoryType {
        GLOBAL,     // Globally defined repositories (came from plugin.xml)
        LOCAL,      // Local (deployed locally) repository. It is singleton
        CUSTOM,     // User-defined repository
        EXTERNAL    // POM-defined repository
    }

    private final String id;
    private final String name;
    private final String url;
    private final RepositoryType type;
    private final String scope;

    private Map<String, MavenArtifact> cachedArtifacts = new LinkedHashMap<>();

    public MavenRepository(IConfigurationElement config)
    {
        this(
            config.getAttribute(RegistryConstants.ATTR_ID),
            config.getAttribute(RegistryConstants.ATTR_NAME),
            config.getAttribute(RegistryConstants.ATTR_URL),
            config.getAttribute(RegistryConstants.ATTR_SCOPE),
            RepositoryType.GLOBAL);
    }

    public MavenRepository(String id, String name, String url, String scope, RepositoryType type) {
        this.id = id;
        this.name = CommonUtils.isEmpty(name) ? id : name;
        if (!url.endsWith("/")) url += "/";
        this.url = url;
        this.scope = scope;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getScope() {
        return scope;
    }

    public RepositoryType getType() {
        return type;
    }

    @Nullable
    public synchronized MavenArtifactVersion findArtifact(DBRProgressMonitor monitor, @NotNull MavenArtifactReference ref) {
        boolean newArtifact = false;
        MavenArtifact artifact = cachedArtifacts.get(ref.getId());
        if (artifact == null) {
            artifact = new MavenArtifact(this, ref.getGroupId(), ref.getArtifactId(), ref.getClassifier());
            newArtifact = true;
        }
        try {
            MavenArtifactVersion version = artifact.resolveVersion(monitor, ref.getVersion());
            if (newArtifact) {
                cachedArtifacts.put(ref.getId(), artifact);
            }
            return version;
        } catch (IOException e) {
            // Generally it is ok. Artifact not present in this repository
            log.debug("Maven artifact '" + ref + "' not found in repository '" + this + "': " + e.getMessage());
            return null;
        }
    }

    synchronized void resetArtifactCache(@NotNull MavenArtifactReference artifactReference) {
        cachedArtifacts.remove(artifactReference.getId());
    }

    File getLocalCacheDir()
    {
        String extPath;
        switch (type) {
            case EXTERNAL:
                try {
                    URL repoUrl = new URL(this.url);
                    extPath = ".external/" + repoUrl.getHost() + "/" + repoUrl.getPath();
                } catch (MalformedURLException e) {
                    extPath = ".external/" + id;
                }
                break;
            default:
                extPath = id;
                break;
        }
        File homeFolder = new File(DBeaverActivator.getInstance().getStateLocation().toFile(), "maven/" + extPath);
        if (!homeFolder.exists()) {
            if (!homeFolder.mkdirs()) {
                log.warn("Can't create maven repository '" + name + "' cache folder '" + homeFolder + "'");
            }
        }

        return homeFolder;
    }

    @Override
    public String toString() {
        return url;
    }

}
