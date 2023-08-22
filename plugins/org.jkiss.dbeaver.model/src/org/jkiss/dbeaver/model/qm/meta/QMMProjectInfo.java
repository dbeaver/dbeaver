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
package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.dbeaver.model.app.DBPProject;

import java.util.UUID;

public class QMMProjectInfo {
    @Include
    private final String id;
    private final String name;
    private final String path;
    private final boolean isAnonymous;
    private final UUID uuid;

    public QMMProjectInfo(DBPProject project) {
        this.id = project.getId();
        this.name = project.getName();
        this.uuid = project.getProjectID();
        this.path = project.getAbsolutePath().toString();
        var projectSession = project
            .getSessionContext()
            .findSpaceSession(project);
        this.isAnonymous = projectSession == null || projectSession.getSessionPrincipal() == null;
    }

    private QMMProjectInfo(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.uuid = UUID.randomUUID();
        this.path = builder.path;
        this.isAnonymous = builder.isAnonymous;

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPath() {
        return path;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String name;
        private String path;
        private boolean isAnonymous;

        public Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setAnonymous(boolean anonymous) {
            isAnonymous = anonymous;
            return this;
        }

        public QMMProjectInfo build() {
            return new QMMProjectInfo(this);
        }
    }
}
