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

import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MavenContextInfo
 */
public class MavenContextInfo implements AutoCloseable {
    static final Log log = Log.getLog(MavenContextInfo.class);

    private List<MavenRepository> repositoryStack = new ArrayList<>();
    private Map<String, MavenRepository> externalRepositories = new LinkedHashMap<>();

    public MavenRepository getCurrentRepository() {
        return repositoryStack.isEmpty() ? null : repositoryStack.get(repositoryStack.size() - 1);
    }

    public void startRepositoryBrowse(MavenRepository repository) {
        repositoryStack.add(repository);
    }

    public void endRepositoryBrowse(MavenRepository repository) {
        MavenRepository removed = repositoryStack.remove(repositoryStack.size() - 1);
        if (removed == null || removed != repository) {
            log.error("Wrong artifact: " + repository);
        }
    }

    public void trackRepository(MavenRepository repository) {
        externalRepositories.put(repository.getUrl(), repository);
    }

    @Override
    public void close() {
        for (MavenRepository repository : externalRepositories.values()) {
            repository.saveCache();
        }
    }

    /*
    private final List<MavenArtifactVersion> artifactStack = new ArrayList<>();

    public Collection<MavenRepository> getActiveRepositories() {
        Map<String, MavenRepository> repositories = new LinkedHashMap<>();
        for (int i = artifactStack.size(); i > 0; i--) {
            for (MavenProfile profile : artifactStack.get(i - 1).getProfiles()) {
                if (profile.isActive()) {
                    List<MavenRepository> profileRepositories = profile.getRepositories();
                    if (profileRepositories != null) {
                        for (MavenRepository repository : profileRepositories) {
                            repositories.put(repository.getId(), repository);
                        }
                    }
                }
            }
        }
        return repositories.values();
    }

    void beginArtifactProcessing(MavenArtifactVersion artifactVersion) {
        artifactStack.add(artifactVersion);
    }

    void endArtifactProcessing(MavenArtifactVersion artifactVersion) {
        MavenArtifactVersion removed = artifactStack.remove(artifactStack.size() - 1);
        if (removed == null || removed != artifactVersion) {
            log.error("Wrong artifact: " + artifactVersion);
        } else {
            // Save repository cache immediately
            for (MavenProfile profile : artifactVersion.getProfiles()) {
                if (profile.isActive()) {
                    List<MavenRepository> profileRepositories = profile.getRepositories();
                    if (profileRepositories != null) {
                        for (MavenRepository repository : profileRepositories) {
                            repository.saveCache();
                        }
                    }
                }
            }
        }
    }
*/

}
