/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
