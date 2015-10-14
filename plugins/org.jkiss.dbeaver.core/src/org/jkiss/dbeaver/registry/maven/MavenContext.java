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

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.*;

/**
 * Maven resolve context
 */
public class MavenContext implements AutoCloseable {

    private final DBRProgressMonitor monitor;
    private final Date initTime = new Date();
    private final Map<String, String> properties = new HashMap<>();
    private final Map<MavenArtifactVersion, List<MavenRepository>> artifactRepositories = new LinkedHashMap<>();
    private final List<MavenRepository> activeRepositories = new ArrayList<>();

    public MavenContext(DBRProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public DBRProgressMonitor getMonitor() {
        return monitor;
    }

    public Date getInitTime() {
        return initTime;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<MavenRepository> getActiveRepositories() {
        return activeRepositories;
    }

    public void addRepositories(MavenArtifactVersion artifactVersion, List<MavenRepository> repositories) {
        artifactRepositories.put(artifactVersion, repositories);
    }

    public void removeRepositories(MavenArtifactVersion artifactVersion) {
        artifactRepositories.remove(artifactVersion);
    }

    @Override
    public void close() {

    }
}
