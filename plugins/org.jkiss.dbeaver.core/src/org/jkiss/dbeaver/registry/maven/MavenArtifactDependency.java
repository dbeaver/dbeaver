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

/**
 * Maven artifact license references
 */
public class MavenArtifactDependency
{
    private MavenArtifactReference artifactReference;
    private String type;
    private boolean optional;

    public MavenArtifactDependency(MavenArtifactReference artifactReference, String type, boolean optional) {
        this.artifactReference = artifactReference;
        this.type = type;
        this.optional = optional;
    }

    public MavenArtifactReference getArtifactReference() {
        return artifactReference;
    }

    public String getType() {
        return type;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public String toString() {
        return artifactReference.toString() + ";type=" + type + "; optional=" + optional;
    }

}
