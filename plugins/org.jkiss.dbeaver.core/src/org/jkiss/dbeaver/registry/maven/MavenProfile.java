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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maven build profile
 */
public class MavenProfile {

    private final String id;
    Map<String, String> properties = new LinkedHashMap<>();
    List<MavenArtifactDependency> dependencies;
    List<MavenArtifactDependency> dependencyManagement;
    List<MavenRepository> repositories;

    boolean active;

    public MavenProfile(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<MavenArtifactDependency> getDependencies() {
        return dependencies;
    }

    public List<MavenArtifactDependency> getDependencyManagement() {
        return dependencyManagement;
    }

    public boolean isActive() {
        return active;
    }

    public List<MavenRepository> getRepositories() {
        return repositories;
    }

    void addRepository(MavenRepository repository) {
        if (repositories == null) {
            repositories = new ArrayList<>();
        }
        repositories.add(repository);
    }

    @Override
    public String toString() {
        return id;
    }

}
