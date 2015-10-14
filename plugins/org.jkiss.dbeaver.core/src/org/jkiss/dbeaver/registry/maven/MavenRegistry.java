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
package org.jkiss.dbeaver.registry.maven;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.connection.DBPDriverContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.*;

public class MavenRegistry
{
    public static final String MAVEN_LOCAL_REPO_ID = "local";
    public static final String MAVEN_LOCAL_REPO_NAME = "Local Repository";
    public static final String MAVEN_LOCAL_REPO_FOLDER = "maven-local";

    private static MavenRegistry instance = null;

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

    private void init() {
        long st = System.currentTimeMillis();
        System.out.print("Loading Maven cache...");
        loadStandardRepositories();
        loadCustomRepositories();
        loadCache();
        System.out.println("Done (" + (System.currentTimeMillis() - st) + "ms)");
        // Start config saver
        new ConfigSaver().schedule(ConfigSaver.SAVE_PERIOD);
    }

    private void loadStandardRepositories() {
        // Load repositories info
        {
            IConfigurationElement[] extElements = Platform.getExtensionRegistry().getConfigurationElementsFor(MavenRepository.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                MavenRepository repository = new MavenRepository(ext);
                repositories.add(repository);
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
            true);
    }

    private void loadCache() {
        localRepository.loadCache();
        for (MavenRepository repository : repositories) {
            repository.loadCache();
        }
    }

    public void loadCustomRepositories() {
        // Clear not-found cache
        notFoundArtifacts.clear();

        // Remove all custom repositories
        for (Iterator<MavenRepository> iterator = repositories.iterator(); iterator.hasNext(); ) {
            MavenRepository repository = iterator.next();
            if (!repository.isPredefined()) {
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
            MavenRepository repo = new MavenRepository(repoID, repoID, repoURL, false);
            repositories.add(repo);
        }
    }

    @NotNull
    public List<MavenRepository> getRepositories() {
        return repositories;
    }

    @Nullable
    public MavenArtifactVersion findArtifact(@NotNull DBPDriverContext context, @NotNull MavenArtifactReference ref) {
        String fullId = ref.getId();
        if (notFoundArtifacts.contains(fullId)) {
            return null;
        }
        MavenArtifactVersion artifact = findInRepositories(context, ref);
        if (artifact != null) {
            return artifact;
        }

        // Not found
        notFoundArtifacts.add(fullId);
        return null;
    }

    @Nullable
    public MavenArtifactVersion findCachedArtifact(DBPDriverContext context, @NotNull MavenArtifactReference ref) {
        String fullId = ref.getId();
        if (notFoundArtifacts.contains(fullId)) {
            return null;
        }
        // Try all available repositories (without resolve)
        for (MavenRepository repository : repositories) {
            MavenArtifactVersion artifact = repository.findCachedArtifact(context, ref);
            if (artifact != null) {
                return artifact;
            }
        }
        MavenArtifactVersion artifact = localRepository.findCachedArtifact(context, ref);
        if (artifact != null) {
            return artifact;
        }
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
    private MavenArtifactVersion findInRepositories(@NotNull DBPDriverContext context, @NotNull MavenArtifactReference ref) {
        // Try all available repositories (without resolve)
        for (MavenRepository repository : repositories) {
            MavenArtifactVersion artifact = repository.findArtifact(context, ref);
            if (artifact != null) {
                return artifact;
            }
        }
        MavenArtifactVersion artifact = localRepository.findArtifact(context, ref);
        if (artifact != null) {
            return artifact;
        }
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

    private class ConfigSaver extends AbstractJob {

        public static final int SAVE_PERIOD = 1000;

        protected ConfigSaver() {
            super("Maven local cache persister");
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            for (MavenRepository repository : repositories) {
                repository.saveCacheIfNeeded();
            }

            schedule(SAVE_PERIOD);
            return Status.OK_STATUS;
        }
    }

}
