/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashSet;
import java.util.Set;

public class SMTeam extends SMSubject {

    @Nullable
    private String teamName;
    @Nullable
    private String description;
    @NotNull
    private Set<String> permissions = new LinkedHashSet<>();


    public SMTeam(@NotNull String teamId) {
        this(teamId, null, null, true);
    }

    public SMTeam(@NotNull String teamId, @Nullable String name, @Nullable String description, boolean secretStorage) {
        super(teamId, null, secretStorage);
        this.teamName = name;
        this.description = description;
    }

    @NotNull
    @Override
    public String getName() {
        return CommonUtils.isEmpty(teamName) ? subjectId : teamName;
    }

    @Property(viewable = true, order = 1)
    public String getTeamId() {
        return subjectId;
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public String getTeamName() {
        return CommonUtils.isEmpty(teamName) ? subjectId : teamName;
    }

    public void setTeamName(@Nullable String teamName) {
        this.teamName = teamName;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NotNull
    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(@NotNull Set<String> permissions) {
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

