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
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.*;

public class MavenRegistry
{
    private static final Log log = Log.getLog(MavenRegistry.class);

    public static final String MAVEN_LOCAL_REPO_ID = "local";
    public static final String MAVEN_LOCAL_REPO_NAME = "Local Repository";
    public static final String MAVEN_LOCAL_REPO_FOLDER = "maven-local";

    private static MavenRegistry instance = null;
    private final List<String> ignoredArtifactVersions = new ArrayList<>();

    public synchronized static MavenRegistry getInstance()
    {
        if (instance == null) {
            instance = new MavenRegistry();
            instance.init();
        }
        return instance;
    }

    private final List<MavenRepository> repositories = new ArrayList<>();
    private MavenRepository localRepository;
    // Cache for not found artifact ids. Avoid multiple remote metadata reading
    private final Set<String> notFoundArtifacts = new HashSet<>();

    private MavenRegistry()
    {
    }

    boolean isVersionIgnored(String ref) {
        for (String ver : ignoredArtifactVersions) {
            if (ref.startsWith(ver)) {
                return true;
            }
        }
        return false;
    }

    private void init() {
        loadStandardRepositories();
        loadCustomRepositories();
    }

    private void loadStandardRepositories() {
        // Load repositories info
        {
            IConfigurationElement[] extElements = Platform.getExtensionRegistry().getConfigurationElementsFor(MavenRepository.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                if ("repository".equals(ext.getName())) {
                    MavenRepository repository = new MavenRepository(ext);
                    repositories.add(repository);
                } else if ("ignoreArtifactVersion".equals(ext.getName())) {
                    ignoredArtifactVersions.add(ext.getAttribute("id"));
                }
            }
        }
        // Create local repository
        String localRepoURL;
        try {
            localRepoURL = Platform.getInstallLocation().getDataArea(MAVEN_LOCAL_REPO_FOLDER).toString();
        } catch (IOException e) {
            localRepoURL = Platform.getInstallLocation().getURL().toString() + "/" + MAVEN_LOCAL_REPO_FOLDER;
        }
        localRepository = new MavenRepository(
            MAVEN_LOCAL_REPO_ID,
            MAVEN_LOCAL_REPO_NAME,
            localRepoURL,
            null,
            MavenRepository.RepositoryType.LOCAL);
    }

    public void loadCustomRepositories() {
        // Clear not-found cache
        notFoundArtifacts.clear();

        // Remove all custom repositories
        for (Iterator<MavenRepository> iterator = repositories.iterator(); iterator.hasNext(); ) {
            MavenRepository repository = iterator.next();
            if (repository.getType() == MavenRepository.RepositoryType.CUSTOM) {
                iterator.remove();
            }
        }
        // PArse repositories from preferences
        String repoString = DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.UI_MAVEN_REPOSITORIES);
        if (CommonUtils.isEmpty(repoString)) {
            return;
        }
        for (String repoInfo : repoString.split("\\|")) {
            int divPos = repoInfo.indexOf(':');
            if (divPos < 0) {
                continue;
            }
            String repoID = repoInfo.substring(0, divPos);
            String repoURL = repoInfo.substring(divPos + 1);
            MavenRepository repo = new MavenRepository(repoID, repoID, repoURL, null, MavenRepository.RepositoryType.CUSTOM);
            repositories.add(repo);
        }
    }

    @NotNull
    public List<MavenRepository> getRepositories() {
        return repositories;
    }

    @Nullable
    public MavenArtifactVersion findArtifact(@NotNull DBRProgressMonitor monitor, @Nullable MavenArtifactVersion owner, @NotNull MavenArtifactReference ref) {
        String fullId = ref.getId();
        if (notFoundArtifacts.contains(fullId)) {
            return null;
        }
        MavenArtifactVersion artifact = findInRepositories(monitor, owner, ref);
        if (artifact != null) {
            return artifact;
        }

        // Not found
        notFoundArtifacts.add(fullId);
        return null;
    }

    public void resetArtifactInfo(MavenArtifactReference artifactReference) {
        notFoundArtifacts.remove(artifactReference.getId());

        for (MavenRepository repository : repositories) {
            repository.resetArtifactCache(artifactReference);
        }
        localRepository.resetArtifactCache(artifactReference);
    }

    @Nullable
    private MavenArtifactVersion findInRepositories(@NotNull DBRProgressMonitor monitor, MavenArtifactVersion owner, @NotNull MavenArtifactReference ref) {
        MavenRepository currentRepository = owner == null ? null : owner.getArtifact().getRepository();
        if (currentRepository != null) {
            MavenArtifactVersion artifact = currentRepository.findArtifact(monitor, ref);
            if (artifact != null) {
                return artifact;
            }
        }

        // Try all available repositories (without resolve)
        for (MavenRepository repository : repositories) {
            if (repository != currentRepository) {
                if (!CommonUtils.isEmpty(repository.getScope())) {
                    // Check scope (group id)
                    if (!repository.getScope().equals(ref.getGroupId())) {
                        continue;
                    }
                }
                MavenArtifactVersion artifact = repository.findArtifact(monitor, ref);
                if (artifact != null) {
                    return artifact;
                }
            }
        }
        if (owner != null) {
            // Try context repositories
            for (MavenRepository repository : owner.getActiveRepositories()) {
                if (repository != currentRepository) {
                    MavenArtifactVersion artifact = repository.findArtifact(monitor, ref);
                    if (artifact != null) {
                        return artifact;
                    }
                }
            }
        }

        if (localRepository != currentRepository) {
            MavenArtifactVersion artifact = localRepository.findArtifact(monitor, ref);
            if (artifact != null) {
                return artifact;
            }
        }

        log.warn("Maven artifact '" + ref + "' not found in any available repository.");

        return null;
    }

    public MavenRepository findRepository(String id) {
        for (MavenRepository repository : repositories) {
            if (repository.getId().equals(id)) {
                return repository;
            }
        }
        return null;
    }

}
