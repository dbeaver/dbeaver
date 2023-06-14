/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

/**
 * Maven artifact reference
 */
public class MavenArtifactReference implements IMavenIdentifier
{
    public static final String VERSION_PATTERN_RELEASE = "RELEASE";
    public static final String VERSION_PATTERN_LATEST = "LATEST";
    public static final String VERSION_PATTERN_SNAPSHOT = "SNAPSHOT";

    private static final String DEFAULT_MAVEN_VERSION = VERSION_PATTERN_RELEASE;

    @NotNull
    private final String groupId;
    @NotNull
    private final String artifactId;
    @Nullable
    private final String classifier;
    @NotNull
    private final String version;
    @NotNull
    private final String id;
    @Nullable
    private final String fallbackVersion;
    private boolean resolveOptionalDependencies;

    public MavenArtifactReference(
        @NotNull String groupId,
        @NotNull String artifactId,
        @Nullable String classifier,
        @Nullable String fallbackVersion,
        @NotNull String version
    ) {
        this.groupId = CommonUtils.trim(groupId);
        this.artifactId = CommonUtils.trim(artifactId);
        this.classifier = CommonUtils.trim(classifier);
        this.version = CommonUtils.trim(version);
        this.fallbackVersion = CommonUtils.trim(fallbackVersion);
        this.id = makeId(this);
    }

    /**
     * @param ref artifact reference path.
     *   Can be GROUP:ARTIFACT:FALLABACK_VERSION or
     *   GROUP:ARTIFACT:CLASSIFIER:FALLABACK_VERSION
     */
    public MavenArtifactReference(String ref) {
        String mavenUri = ref;
        int divPos = mavenUri.indexOf('/');
        if (divPos >= 0) {
            mavenUri = mavenUri.substring(divPos + 1);
        }
        version = DEFAULT_MAVEN_VERSION;
        String[] parts = mavenUri.split(":");
        if (parts.length == 1) {
            // No artifact ID
            groupId = mavenUri;
            artifactId = mavenUri;
            classifier = null;
            fallbackVersion = null;
        } else {
            groupId = parts[0];
            artifactId = parts[1];
            if (parts.length == 2) {
                classifier = null;
                fallbackVersion = null;
            } else if (parts.length == 3) {
                classifier = null;
                fallbackVersion = parts[2];
            } else {
                classifier = parts[2];
                fallbackVersion = parts[3];
            }
        }
        id = makeId(this);
    }

    @Override
    @NotNull
    public String getGroupId() {
        return groupId;
    }

    @Override
    @NotNull
    public String getArtifactId() {
        return artifactId;
    }

    @Nullable
    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    @Nullable
    public String getFallbackVersion() {
        return fallbackVersion;
    }

    @Override
    @NotNull
    public String getVersion() {
        return version;
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    public String getPath() {
        return id;
    }

    public boolean isResolveOptionalDependencies() {
        return resolveOptionalDependencies;
    }

    public void setResolveOptionalDependencies(boolean resolveOptionalDependencies) {
        this.resolveOptionalDependencies = resolveOptionalDependencies;
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public int hashCode() {
        return groupId.hashCode() + artifactId.hashCode() + version.hashCode();
    }

    static String makeId(IMavenIdentifier identifier) {
        StringBuilder id = new StringBuilder();
        id.append(identifier.getGroupId()).append(":").append(identifier.getArtifactId());

        if (identifier.getClassifier() != null) {
            id.append(":").append(identifier.getClassifier());
        }
        if (identifier.getFallbackVersion() != null) {
            id.append(":").append(identifier.getFallbackVersion() != null ? identifier.getFallbackVersion() :
                                  identifier.getVersion());
        }
        return id.toString();
    }

}
