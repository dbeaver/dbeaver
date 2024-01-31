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

import java.util.Map;

public class SMUser extends SMSubject {

    private String[] userTeams;
    private boolean enabled;
    private String authRole;

    public SMUser(
        @NotNull String userId,
        boolean enabled,
        @Nullable String authRole
    ) {
        this(userId, null, new String[0], enabled, authRole, true);
    }

    public SMUser(
        @NotNull String userId,
        boolean enabled,
        @Nullable String authRole,
        boolean secretStorage
    ) {
        this(userId, null, new String[0], enabled, authRole, secretStorage);
    }


    public SMUser(
        @NotNull String userId,
        @Nullable Map<String, String> metaParameters,
        @NotNull String[] teams,
        boolean enabled,
        @Nullable String authRole,
        boolean secretStorage
    ) {
        super(userId, metaParameters, secretStorage);
        this.userTeams = teams;
        this.enabled = enabled;
        this.authRole = authRole;
    }

    @NotNull
    @Override
    public String getName() {
        return subjectId;
    }

    @Property(viewable = true, order = 1)
    @NotNull
    public String getUserId() {
        return subjectId;
    }

    @NotNull
    public String[] getUserTeams() {
        return userTeams;
    }

    public void setUserTeams(@NotNull String[] userTeams) {
        this.userTeams = userTeams;
    }

    @Property(viewable = true, order = 3)
    public boolean isEnabled() {
        return enabled;
    }

    public void enableUser(boolean enabled) {
        this.enabled = enabled;
    }

    @Property(viewable = true, order = 2)
    public String getAuthRole() {
        return authRole;
    }

    public void setAuthRole(String authRole) {
        this.authRole = authRole;
    }
}
