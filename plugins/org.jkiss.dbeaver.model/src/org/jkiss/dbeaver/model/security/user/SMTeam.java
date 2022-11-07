/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security.user;

import java.util.LinkedHashSet;
import java.util.Set;

public class SMTeam extends SMSubject {

    private String name;
    private String description;
    private Set<String> permissions = new LinkedHashSet<>();

    public SMTeam(String teamId) {
        this(teamId, null, null);
    }

    public SMTeam(String teamId, String name, String description) {
        super(teamId, null);
        this.name = name;
        this.description = description;
    }

    public String getTeamId() {
        return subjectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(String permission) {
        this.permissions.add(permission);
    }

    @Override
    public String toString() {
        return getTeamId();
    }
}

