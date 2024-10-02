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
package org.jkiss.dbeaver.ext.postgresql.ui.editors.privileges;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;

import java.util.Arrays;
import java.util.List;

public class ObjectPermissionsEditor extends PostgresPermissionsEditor<PostgreRole> {
    private PostgrePrivilegeType[] supportedPrivilegeTypes;

    @Override
    protected PostgrePrivilege grantPrivilege(PostgrePrivilegeType privilegeType, PostgreRole role) {
        PostgrePrivilegeOwner databaseObject = getDatabaseObject();
        PostgrePrivilegeGrant grant = createGrant(databaseObject, role, privilegeType);

        return objectToPrivileges.compute(getObjectName(role), (key, value) -> {
            if (value != null) {
                value.addPermission(grant);
                return value;
            } else {
                return new PostgreObjectPrivilege(
                    databaseObject,
                    role.getRoleReference(),
                    List.of(grant)
                );
            }
        });
    }

    @Override
    protected PostgrePrivilege revokePrivilege(PostgrePrivilegeType privilegeType, PostgreRole role) {
        return objectToPrivileges.computeIfPresent(getObjectName(role), (object, privileges) -> {
            privileges.removePermission(privilegeType);
            return privileges;
        });
    }

    @Override
    protected PostgrePrivilegeType[] getSupportedPrivilegeTypes(DBSObject object) {
        // Privilege types are loaded once and cached since they are not expected to change
        if (supportedPrivilegeTypes == null) {
            supportedPrivilegeTypes = loadSupportedPrivilegeTypes();
        }

        return supportedPrivilegeTypes;
    }

    private PostgrePrivilegeType[] loadSupportedPrivilegeTypes() {
        return Arrays.stream(getDatabaseObject().getDataSource().getSupportedPrivilegeTypes())
            .filter(it -> it.isValid() && it.supportsType(getDatabaseObject().getClass()))
            .toArray(PostgrePrivilegeType[]::new);
    }

    @Override
    protected boolean doesSupportObject(DBSObject object) {
        return object instanceof PostgrePrivilegeOwner;
    }

    @Override
    protected PermissionInfo laodPermissionInfo(DBRProgressMonitor monitor) throws DBException {
        return new PermissionInfo(
            getDatabaseObject().getPrivileges(monitor, false),
            DBNUtils.getNodeByObject(monitor, getDatabaseObject().getDatabase(), true)
        );
    }

    @Override
    protected DatabaseNavigatorTreeFilter navigatorTreeFilter() {
        return new ObjectOwnerFiler();
    }

    private static class ObjectOwnerFiler extends DatabaseNavigatorTreeFilter {
        @Override
        public boolean select(Object element) {
            if (element instanceof DBNDatabaseFolder item) {
                Class<? extends DBSObject> childrenClass = item.getChildrenClass();
                return childrenClass != null && PostgreRole.class.isAssignableFrom(childrenClass);
            }
            return isLeafObject(element);
        }

        @Override
        public boolean isLeafObject(Object object) {
            return object instanceof DBNDatabaseItem item && item.getObject() instanceof PostgreRole;
        }
    }
}
