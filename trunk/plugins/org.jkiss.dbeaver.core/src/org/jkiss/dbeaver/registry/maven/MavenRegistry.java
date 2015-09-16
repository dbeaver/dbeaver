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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class MavenRegistry
{
    private static MavenRegistry instance = null;

    public synchronized static MavenRegistry getInstance()
    {
        if (instance == null) {
            instance = new MavenRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
            instance.loadCustomRepositories();
        }
        return instance;
    }

    private final List<MavenRepository> repositories = new ArrayList<MavenRepository>();
    // Cache for not found artifact ids. Avoid multiple remote metadata reading
    private final Set<String> notFoundArtifacts = new HashSet<String>();

    private MavenRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        // Load data type providers from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(MavenRepository.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                MavenRepository repository = new MavenRepository(ext);
                repositories.add(repository);
            }
        }
    }

    public void loadCustomRepositories() {
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
            MavenRepository repo = new MavenRepository(repoID, repoID, repoURL);
            repositories.add(repo);
        }
    }

    @NotNull
    public List<MavenRepository> getRepositories() {
        return repositories;
    }

    @Nullable
    public MavenArtifact findArtifact(@NotNull MavenArtifactReference ref) {
        return findArtifact(ref.getGroupId(), ref.getArtifactId());
    }

    @Nullable
    private MavenArtifact findArtifact(@NotNull String groupId, @NotNull String artifactId) {
        String fullId = groupId + ":" + artifactId;
        if (notFoundArtifacts.contains(fullId)) {
            return null;
        }

        for (MavenRepository repository : repositories) {
            MavenArtifact artifact = repository.findArtifact(groupId, artifactId);
            if (artifact != null) {
                return artifact;
            }
        }
        notFoundArtifacts.add(fullId);
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
