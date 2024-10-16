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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.access.DBAPrivilegeGrant;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * PostgrePrivilege
 */
public abstract class PostgrePrivilege implements DBAPrivilege, Comparable<PostgrePrivilege> {

    public static final short NONE = 0;
    public static final short GRANTED = 1;
    public static final short WITH_GRANT_OPTION = 2;
    public static final short WITH_HIERARCHY = 4;

    public class ObjectPermission implements DBAPrivilegeGrant {
        @NotNull
        private final PostgrePrivilegeType privilegeType;
        @NotNull
        private final PostgreRoleReference grantor;
        private short permissions;

        public ObjectPermission(@NotNull PostgrePrivilegeType privilegeType, @NotNull PostgreRoleReference grantor, short permissions) {
            this.privilegeType = privilegeType;
            this.grantor = grantor;
            this.permissions = permissions;
        }

        @Override
        public DBARole getSubject(@NotNull DBRProgressMonitor monitor) throws DBException {
            return owner instanceof DBARole ? (DBARole) owner : (DBARole) getTargetObject(monitor);
        }

        @Override
        public DBSObject getObject(@NotNull DBRProgressMonitor monitor) throws DBException {
            return owner instanceof DBARole ? getTargetObject(monitor) : owner;
        }

        @Override
        public DBAPrivilege[] getPrivileges() {
            return new DBAPrivilege[] { PostgrePrivilege.this };
        }

        @NotNull
        public PostgrePrivilegeType getPrivilegeType() {
            return privilegeType;
        }

        @Override
        public boolean isGranted() {
            return (permissions & GRANTED) == GRANTED;
        }

        @NotNull
        public PostgreRoleReference getGrantor() {
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

    protected final PostgrePrivilegeOwner owner;
    private ObjectPermission[] permissions;

    public PostgrePrivilege(PostgrePrivilegeOwner owner, List<PostgrePrivilegeGrant> grants) {
        this.owner = owner;
        this.permissions = new ObjectPermission[grants.size()];
        for (int i = 0 ; i < grants.size(); i++) {
            final PostgrePrivilegeGrant privilege = grants.get(i);
            short permission = GRANTED;
            if (privilege.isGrantable()) permission |= WITH_GRANT_OPTION;
            if (privilege.isWithHierarchy()) permission |= WITH_HIERARCHY;
            this.permissions[i] = new ObjectPermission(privilege.getPrivilegeType(), privilege.getGrantor(), permission);
        }

    }

    public DBAPrivilegeGrant[] getGrants() {
        return permissions;
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
    public PostgrePrivilegeOwner getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return owner.getDataSource();
    }

    public PostgrePrivilegeOwner getOwner() {
        return owner;
    }

    public abstract PostgreObject getTargetObject(DBRProgressMonitor monitor) throws DBException;

    public ObjectPermission[] getPermissions() {
        return permissions;
    }

    public PostgrePrivilegeType[] getPrivileges() {
        PostgrePrivilegeType[] ppt = new PostgrePrivilegeType[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            ppt[i] = permissions[i].getPrivilegeType();
        }
        return ppt;
    }

    public short getPermission(PostgrePrivilegeType privilegeType) {
        for (ObjectPermission permission : permissions) {
            if (permission.privilegeType == privilegeType || permission.privilegeType == PostgrePrivilegeType.ALL) {
                return permission.permissions;
            }
        }
        return NONE;
    }

    public boolean hasPermission(@NotNull PostgrePrivilegeType privilegeType) {
        return getPermission(privilegeType) != NONE;
    }

    public void addPermission(@NotNull PostgrePrivilegeGrant privilege) {
        ObjectPermission[] newPermissions = new ObjectPermission[this.permissions.length + 1];
        System.arraycopy(this.permissions, 0, newPermissions, 0, this.permissions.length);

        short permission = GRANTED;
        if (privilege.isGrantable()) permission |= WITH_GRANT_OPTION;
        if (privilege.isWithHierarchy()) permission |= WITH_HIERARCHY;
        newPermissions[this.permissions.length] = new ObjectPermission(
            privilege.getPrivilegeType(),
            privilege.getGrantor(),
            permission
        );

        this.permissions = newPermissions;
    }

    public void removePermission(@NotNull PostgrePrivilegeType privilegeType) {
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].privilegeType == privilegeType) {
                ObjectPermission[] newPermissions = new ObjectPermission[this.permissions.length - 1];
                System.arraycopy(this.permissions, 0, newPermissions, 0, i);
                System.arraycopy(this.permissions, i + 1, newPermissions, i, this.permissions.length - i - 1);
                this.permissions = newPermissions;
                break;
            }
        }
    }

    /**
     * Checks all privileges
     */
    public boolean hasAllPrivileges(Object object) {
        for (PostgrePrivilegeType pt : getDataSource().getSupportedPrivilegeTypes()) {
            if (pt.isValid() && pt.supportsType(object.getClass()) && getPermission(pt) == 0) {
                return false;
            }
        }
        return true;
    }

}

