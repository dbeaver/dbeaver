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
package org.jkiss.dbeaver.model.security.user;

import org.jkiss.code.NotNull;

import java.util.Collection;

public class SMObjectPermissions {
    @NotNull
    private final String objectId;
    @NotNull
    private final String[] permissions;

    public SMObjectPermissions(@NotNull String objectId, @NotNull Collection<String> permissions) {
        this.objectId = objectId;
        this.permissions = permissions.toArray(new String[0]);
    }

    public SMObjectPermissions(@NotNull String objectId, @NotNull String[] permissions) {
        this.objectId = objectId;
        this.permissions = permissions;
    }

    @NotNull
    public String getObjectId() {
        return objectId;
    }

    @NotNull
    public String[] getPermissions() {
        return permissions;
    }
}
