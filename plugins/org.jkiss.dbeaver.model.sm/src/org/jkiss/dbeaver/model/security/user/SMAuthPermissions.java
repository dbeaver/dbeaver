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

import java.util.Objects;
import java.util.Set;

public class SMAuthPermissions {
    @Nullable
    private final String userId;
    @NotNull
    private final String sessionId;
    @NotNull
    private final Set<String> permissions;

    public SMAuthPermissions(
        @Nullable String userId,
        @NotNull String sessionId,
        @NotNull Set<String> permissions
    ) {
        this.userId = userId;
        this.permissions = permissions;
        this.sessionId = sessionId;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @NotNull
    public String getSessionId() {
        return sessionId;
    }

    @NotNull
    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SMAuthPermissions that = (SMAuthPermissions) o;
        return Objects.equals(userId, that.userId)
            && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, permissions);
    }

}
