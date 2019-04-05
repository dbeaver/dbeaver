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
    @NotNull
    private final String version;
    @NotNull
    private final String id;
    @Nullable
    private final String classifier;

    public MavenArtifactReference(@NotNull String groupId, @NotNull String artifactId, @Nullable String classifier, @NotNull String version) {
        this.groupId = CommonUtils.trim(groupId);
        this.artifactId = CommonUtils.trim(artifactId);
        this.version = CommonUtils.trim(version);
        this.classifier = CommonUtils.trim(classifier);
        this.id = makeId(this);
    }

    public MavenArtifactReference(String ref) {
        String mavenUri = ref;
        int divPos = mavenUri.indexOf('/');
        if (divPos >= 0) {
            mavenUri = mavenUri.substring(divPos + 1);
        }
        divPos = mavenUri.indexOf(':');
        if (divPos < 0) {
            // No artifact ID
            groupId = mavenUri;
            artifactId = mavenUri;
            classifier = null;
            version = DEFAULT_MAVEN_VERSION;
        } else {
            groupId = mavenUri.substring(0, divPos);
            int divPos2 = mavenUri.indexOf(':', divPos + 1);
            if (divPos2 < 0) {
                // No version
                artifactId = mavenUri.substring(divPos + 1);
                classifier = null;
                version = DEFAULT_MAVEN_VERSION;
            } else {
                int divPos3 = mavenUri.indexOf(':', divPos2 + 1);
                if (divPos3 < 0) {
                    // No classifier
                    artifactId = mavenUri.substring(divPos + 1, divPos2);
                    classifier = null;
                    version = mavenUri.substring(divPos2 + 1);
                } else {
                    artifactId = mavenUri.substring(divPos + 1, divPos2);
                    classifier = mavenUri.substring(divPos2 + 1, divPos3);
                    version = mavenUri.substring(divPos3 + 1);
                }
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

    @Override
    @Nullable
    public String getClassifier() {
        return classifier;
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
        return id + ":" + version;
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
        if (identifier.getClassifier() != null) {
            return identifier.getGroupId() + ":" + identifier.getArtifactId() + ":" + identifier.getClassifier();
        } else {
            return identifier.getGroupId() + ":" + identifier.getArtifactId();
        }
    }

}
