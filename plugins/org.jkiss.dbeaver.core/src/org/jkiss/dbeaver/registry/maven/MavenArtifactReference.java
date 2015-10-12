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

/**
 * Maven artifact reference
 */
public class MavenArtifactReference
{
    public static final String VERSION_PATTERN_RELEASE = "RELEASE";
    public static final String VERSION_PATTERN_LATEST = "LATEST";
    public static final String VERSION_PATTERN_SNAPSHOT = "SNAPSHOT";

    private static final String DEFAULT_MAVEN_VERSION = VERSION_PATTERN_RELEASE;

    @NotNull
    private final String groupId;
    @NotNull
    private final String artifactId;
    @NotNull
    private final String version;
    @NotNull
    private final String id;

    public MavenArtifactReference(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.id = makeId(groupId, artifactId);
    }

    public MavenArtifactReference(String ref) {
        String mavenUri = ref;
        int divPos = mavenUri.indexOf('/');
        if (divPos >= 0) {
            mavenUri = mavenUri.substring(divPos + 1);
        }
        divPos = mavenUri.indexOf(':');
        if (divPos < 0) {
            groupId = mavenUri;
            artifactId = mavenUri;
            version = DEFAULT_MAVEN_VERSION;
            id = makeId(groupId, artifactId);
            return;
        }
        groupId = mavenUri.substring(0, divPos);
        int divPos2 = mavenUri.indexOf(':', divPos + 1);
        if (divPos2 < 0) {
            artifactId = mavenUri.substring(divPos + 1);
            version = DEFAULT_MAVEN_VERSION;
        } else {
            artifactId = mavenUri.substring(divPos + 1, divPos2);
            version = mavenUri.substring(divPos2 + 1);
        }
        id = makeId(groupId, artifactId);
    }

    @NotNull
    public String getGroupId() {
        return groupId;
    }

    @NotNull
    public String getArtifactId() {
        return artifactId;
    }

    @NotNull
    public String getVersion() {
        return version;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public String getPath() {
        return groupId + ":" + artifactId + ":" + version;
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public int hashCode() {
        return groupId.hashCode() + artifactId.hashCode() + version.hashCode();
    }

    static String makeId(String groupId, String artifactId) {
        return groupId + ":" + artifactId;
    }

}
