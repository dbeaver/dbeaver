/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security.permissions;

import org.jkiss.code.NotNull;

import java.util.Objects;

public class SMPermission {
    @NotNull
    private final String permissionId;
    private final boolean isEnabled;

    public SMPermission(@NotNull String permissionId, boolean isEnabled) {
        this.permissionId = permissionId;
        this.isEnabled = isEnabled;
    }

    @NotNull
    public String getPermissionId() {
        return permissionId;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        SMPermission that = (SMPermission) object;
        return isEnabled == that.isEnabled && Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionId, isEnabled);
    }
}
