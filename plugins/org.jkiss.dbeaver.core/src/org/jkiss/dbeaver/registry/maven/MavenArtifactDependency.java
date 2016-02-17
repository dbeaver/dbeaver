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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
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
    private boolean broken;

    public MavenArtifactDependency(@NotNull String groupId, @NotNull String artifactId, @Nullable String classifier, @NotNull String version, Scope scope, boolean optional) {
        super(groupId, artifactId, classifier, version);
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

    void addExclusion(MavenArtifactReference ref) {
        if (exclusions == null) {
            exclusions = new ArrayList<>();
        }
        exclusions.add(ref);
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }
}
