/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * PostgrePermission
 */
public abstract class PostgrePermission implements DBSObject, Comparable<PostgrePermission> {

    public static final short NONE = 0;
    public static final short GRANTED = 1;
    public static final short WITH_GRANT_OPTION = 2;
    public static final short WITH_HIERARCHY = 4;

    public static class ObjectPermission {
        @NotNull
        private PostgrePrivilegeType privilegeType;
        @NotNull
        private String grantor;
        private short permissions;

        public ObjectPermission(@NotNull PostgrePrivilegeType privilegeType, @NotNull String grantor, short permissions) {
            this.privilegeType = privilegeType;
            this.grantor = grantor;
            this.permissions = permissions;
        }

        @NotNull
        public PostgrePrivilegeType getPrivilegeType() {
            return privilegeType;
        }

        @NotNull
        public String getGrantor() {
            return grantor;
        }

        public short getPermissions() {
            return permissions;
        }

        @Override
        public String toString() {
            return privilegeType.toString();
        }
    }

    protected final PostgrePermissionsOwner owner;
    private ObjectPermission[] permissions;

    public PostgrePermission(PostgrePermissionsOwner owner, List<PostgrePrivilege> privileges) {
        this.owner = owner;
        this.permissions = new ObjectPermission[privileges.size()];
        for (int i = 0 ; i < privileges.size(); i++) {
            final PostgrePrivilege privilege = privileges.get(i);
            short permission = GRANTED;
            if (privilege.isGrantable()) permission |= WITH_GRANT_OPTION;
            if (privilege.isWithHierarchy()) permission |= WITH_HIERARCHY;
            this.permissions[i] = new ObjectPermission(privilege.getPrivilegeType(), privilege.getGrantor(), permission);
        }

    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return owner.getDataSource();
    }

    public abstract PostgreObject getTargetObject(DBRProgressMonitor monitor) throws DBException;

    public ObjectPermission[] getPermissions() {
        return permissions;
    }

    public short getPermission(PostgrePrivilegeType privilegeType) {
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].privilegeType == privilegeType) {
                return permissions[i].permissions;
            }
        }
        return NONE;
    }

    // Properties for permissions viewer

    @Property(viewable = true, order = 100, name = "SELECT")
    public boolean hasPermissionSelect() {
        return getPermission(PostgrePrivilegeType.SELECT) != 0;
    }

    @Property(viewable = true, order = 101, name = "INSERT")
    public boolean hasPermissionInsert() {
        return getPermission(PostgrePrivilegeType.INSERT) != 0;
    }

    @Property(viewable = true, order = 102, name = "UPDATE")
    public boolean hasPermissionUpdate() {
        return getPermission(PostgrePrivilegeType.UPDATE) != 0;
    }

    @Property(viewable = true, order = 103, name = "DELETE")
    public boolean hasPermissionDelete() {
        return getPermission(PostgrePrivilegeType.DELETE) != 0;
    }

    @Property(viewable = true, order = 104, name = "TRUNCATE")
    public boolean hasPermissionTruncate() {
        return getPermission(PostgrePrivilegeType.TRUNCATE) != 0;
    }

    @Property(viewable = true, order = 105, name = "REFERENCES")
    public boolean hasPermissionReferences() {
        return getPermission(PostgrePrivilegeType.REFERENCES) != 0;
    }

    @Property(viewable = true, order = 106, name = "TRIGGER")
    public boolean hasPermissionTrigger() {
        return getPermission(PostgrePrivilegeType.TRIGGER) != 0;
    }

}

