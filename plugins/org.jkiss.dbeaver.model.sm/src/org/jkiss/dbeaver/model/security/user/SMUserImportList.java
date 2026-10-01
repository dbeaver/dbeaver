/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import java.util.List;

public class SMUserImportList {
    @NotNull
    private final List<SMUserProvisioning> users;
    @Nullable
    private final String authRole;
    @Nullable
    private final String authProviderId;

    public SMUserImportList(@NotNull List<SMUserProvisioning> users, @Nullable String authRole) {
        this(users, authRole, null);
    }

    public SMUserImportList(
        @NotNull List<SMUserProvisioning> users,
        @Nullable String authRole,
        @Nullable String authProviderId
    ) {
        this.users = users;
        this.authRole = authRole;
        this.authProviderId = authProviderId;
    }

    @NotNull
    public List<SMUserProvisioning> getUsers() {
        return users;
    }

    @Nullable
    public String getAuthRole() {
        return authRole;
    }

    @Nullable
    public String getAuthProviderId() {
        return authProviderId;
    }
}
