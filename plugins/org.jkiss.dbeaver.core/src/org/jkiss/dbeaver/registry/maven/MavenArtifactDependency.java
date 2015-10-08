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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.util.List;

/**
 * Maven artifact license references
 */
public class MavenArtifactDependency extends MavenArtifactReference {

    public enum Scope {
        COMPILE,
        PROVIDED,
        RUNTIME,
        TEST,
        SYSTEM,
        IMPORT
    }

    private Scope scope;
    private boolean optional;
    private List<MavenArtifactReference> exclusions;

    public MavenArtifactDependency(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, Scope scope, boolean optional) {
        super(groupId, artifactId, version);
        this.scope = scope;
        this.optional = optional;
    }

    public Scope getScope() {
        return scope;
    }

    public boolean isOptional() {
        return optional;
    }

    public List<MavenArtifactReference> getExclusions() {
        return exclusions;
    }

    public MavenLocalVersion resolveDependency(DBRProgressMonitor monitor) throws IOException {
        MavenArtifact depArtifact = MavenRegistry.getInstance().findArtifact(this);
        if (depArtifact != null) {
            return depArtifact.resolveVersion(monitor, getVersion(), false);
        }
        return null;
    }

    void addExclusion(MavenArtifactReference ref) {
        exclusions.add(ref);
    }

}
