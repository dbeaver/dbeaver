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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.code.Nullable;

import java.util.HashSet;
import java.util.Set;

public enum RMProjectPermission {
    DATA_SOURCES_VIEW(RMConstants.PERMISSION_PROJECT_DATASOURCES_VIEW),
    DATA_SOURCES_EDIT(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT, Set.of(DATA_SOURCES_VIEW)),

    RESOURCE_VIEW(RMConstants.PERMISSION_PROJECT_RESOURCE_VIEW),
    RESOURCE_EDIT(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT, Set.of(RESOURCE_VIEW)),
    TASK_MANAGER(RMConstants.PERMISSION_TASK_MANAGER),

    PROJECT_ADMIN(RMConstants.PERMISSION_PROJECT_ADMIN, Set.of(RESOURCE_EDIT, DATA_SOURCES_EDIT, TASK_MANAGER));

    private final String permission;
    private final Set<RMProjectPermission> childPermissions;

    RMProjectPermission(String permission) {
        this(permission, Set.of());
    }

    RMProjectPermission(String permission, Set<RMProjectPermission> childPermissions) {
        this.permission = permission;
        this.childPermissions = childPermissions;
    }

    @Nullable
    public static RMProjectPermission fromPermission(String permission) {
        for (RMProjectPermission projectPermission : RMProjectPermission.values()) {
            if (projectPermission.permission.equals(permission)) {
                return projectPermission;
            }
        }

        return null;
    }

    public String getPermissionId() {
        return permission;
    }

    public Set<String> getAllPermissions() {
        var allPermissions = new HashSet<String>();
        allPermissions.add(permission);
        for (RMProjectPermission childPermission : childPermissions) {
            allPermissions.addAll(childPermission.getAllPermissions());
        }
        return allPermissions;
    }
}
