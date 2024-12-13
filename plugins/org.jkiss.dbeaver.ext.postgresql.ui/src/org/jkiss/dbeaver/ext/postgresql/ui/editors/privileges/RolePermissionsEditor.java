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
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RolePermissionsEditor extends PostgresPermissionsEditor<DBSObject> {
    @Override
    protected PostgrePrivilege grantPrivilege(
        PostgrePrivilegeType privilegeType,
        DBSObject object
    ) {
        if (object instanceof DBNDatabaseFolder folder) {
            return defaultPrivilege(privilegeType, folder);
        } else {
            return grantRolePrivilege(privilegeType, (PostgrePrivilegeOwner) object);
        }
    }

    @Override
    protected PostgrePrivilege revokePrivilege(
        PostgrePrivilegeType privilegeType,
        DBSObject object
    ) {
        if (object instanceof DBNDatabaseFolder folder) {
            return revokeDefaultPrivilege(privilegeType, folder);
        } else {
            return revokeRolePrivilege(privilegeType, (PostgrePrivilegeOwner) object);
        }
    }

    @Override
    protected PostgrePrivilegeType[] getSupportedPrivilegeTypes(DBSObject object) {
        Class<?> objectType = defineObjectType(object);

        return Arrays.stream(getDatabaseObject().getDataSource().getSupportedPrivilegeTypes())
            .filter(it -> it.isValid() && it.supportsType(objectType))
            .toArray(PostgrePrivilegeType[]::new);
    }

    @Override
    protected boolean doesSupportObject(DBSObject object) {
        return object instanceof PostgrePrivilegeOwner || !PostgreSchema.class.isAssignableFrom(defineObjectType(object));
    }

    @Override
    protected PermissionInfo laodPermissionInfo(DBRProgressMonitor monitor) throws DBException {
        DBNDatabaseNode node = DBNUtils.getNodeByObject(monitor, getDatabaseObject().getDatabase(), true);
        return new PermissionInfo(
            getDatabaseObject().getPrivileges(monitor, false),
            DBNUtils.getChildFolder(monitor, node, PostgreSchema.class)
        );
    }

    @Override
    protected DatabaseNavigatorTreeFilter navigatorTreeFilter() {
        return new DatabaseObjectFilter();
    }

    private PostgrePrivilege defaultPrivilege(
        PostgrePrivilegeType privilegeType,
        DBNDatabaseFolder folder
    ) {
        DBSObject parentObject = ((DBNDatabaseItem) folder.getParentNode()).getObject();
        if (!(parentObject instanceof PostgreSchema)) {
            throw new IllegalArgumentException("Unsupported parent object: " + parentObject);
        }

        return objectToPrivileges.computeIfAbsent(getObjectName(folder), key -> {
            PostgreRole role = (PostgreRole) getDatabaseObject();
            PostgreSchema owner = (PostgreSchema) parentObject;
            PostgreDefaultPrivilege defaultPrivilege = new PostgreDefaultPrivilege(
                owner,
                role.getRoleReference(),
                null,
                List.of(createGrant(owner, role, privilegeType))
            );
            defaultPrivilege.setUnderKind(defineKind(folder.getChildrenClass()));
            return defaultPrivilege;
        });
    }

    private PostgrePrivilege revokeDefaultPrivilege(
        PostgrePrivilegeType privilegeType,
        DBNDatabaseFolder folder
    ) {
        return objectToPrivileges.computeIfPresent(getObjectName(folder), (key, value) -> {
            value.removePermission(privilegeType);
            return value;
        });
    }

    private PostgrePrivilege grantRolePrivilege(
        PostgrePrivilegeType privilegeType,
        PostgrePrivilegeOwner owner
    ) {
        PostgrePrivilegeGrant.Kind kind = defineKind(owner);
        String objectName = defineObjectName(owner);
        String schemaName = owner.getSchema().getName();

        PostgreRole role = (PostgreRole) getDatabaseObject();

        return objectToPrivileges.compute(getObjectName(owner), (key, value) -> {
            if (value == null) {
                return new PostgreRolePrivilege(
                    role,
                    kind,
                    schemaName,
                    objectName,
                    List.of(createGrant(owner, role, privilegeType))
                );
            } else {
                value.addPermission(createGrant(owner, role, privilegeType));
                return value;
            }
        });
    }

    private PostgrePrivilege revokeRolePrivilege(
        PostgrePrivilegeType privilegeType,
        PostgrePrivilegeOwner owner
    ) {
        return objectToPrivileges.computeIfPresent(getObjectName(owner), (key, value) -> {
            value.removePermission(privilegeType);
            return value;
        });
    }

    private Class<?> defineObjectType(DBSObject object) {
        if (object instanceof DBNDatabaseFolder folder
            && getDatabaseObject() != null
            && getDatabaseObject().getDataSource().getServerType().supportsDefaultPrivileges()
        ) {
            return folder.getChildrenClass();
        } else {
            return object.getClass();
        }
    }

    private PostgrePrivilegeGrant.Kind defineKind(Class<? extends DBSObject> object) {
        if (DBSSequence.class.isAssignableFrom(object)) {
            return PostgrePrivilegeGrant.Kind.SEQUENCE;
        } else if (DBSProcedure.class.isAssignableFrom(object)) {
            return PostgrePrivilegeGrant.Kind.FUNCTION;
        } else {
            return PostgrePrivilegeGrant.Kind.TABLE;
        }
    }

    private PostgrePrivilegeGrant.Kind defineKind(PostgrePrivilegeOwner owner) {
        if (owner instanceof PostgreProcedure procedure) {
            if (procedure.getKind() == PostgreProcedureKind.p) {
                return PostgrePrivilegeGrant.Kind.PROCEDURE;
            } else {
                return PostgrePrivilegeGrant.Kind.FUNCTION;
            }
        } else if (owner instanceof PostgreSequence) {
            return PostgrePrivilegeGrant.Kind.SEQUENCE;
        } else if (owner instanceof PostgreSchema) {
            return PostgrePrivilegeGrant.Kind.SCHEMA;
        } else if (owner instanceof PostgreTableBase) {
            return PostgrePrivilegeGrant.Kind.TABLE;
        } else {
            throw new IllegalArgumentException("Unexpected object: " + owner);
        }
    }

    private String defineObjectName(DBSObject object) {
        if (object instanceof PostgreProcedure procedure) {
            return procedure.getUniqueName();
        } else {
            return object.getName();
        }
    }

    private static class DatabaseObjectFilter extends DatabaseNavigatorTreeFilter {
        @Override
        public boolean isLeafObject(Object object) {
            if (object instanceof DBNDatabaseItem) {
                DBSObject dbObject = ((DBNDatabaseItem) object).getObject();
                return
                    dbObject instanceof DBSEntity ||
                        dbObject instanceof DBSProcedure ||
                        dbObject instanceof DBSTableIndex ||
                        dbObject instanceof DBSPackage ||
                        dbObject instanceof DBSSequence ||
                        dbObject instanceof DBAUser;
            }
            return false;
        }
    }
}
