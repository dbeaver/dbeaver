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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.access.DBAPrivilegeGrant;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * KingbasePrivilege
 */
public abstract class KingbasePrivilege implements DBAPrivilege, Comparable<KingbasePrivilege> {

    public static final short NONE = 0;
    public static final short GRANTED = 1;
    public static final short WITH_GRANT_OPTION = 2;
    public static final short WITH_HIERARCHY = 4;

    public class ObjectPermission implements DBAPrivilegeGrant {
        @NotNull
        private final KingbasePrivilegeType privilegeType;
        @NotNull
        private final KingbaseRoleReference grantor;
        private short permissions;

        public ObjectPermission(@NotNull KingbasePrivilegeType privilegeType, @NotNull KingbaseRoleReference grantor, short permissions) {
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
            return new DBAPrivilege[] { KingbasePrivilege.this };
        }

        @NotNull
        public KingbasePrivilegeType getPrivilegeType() {
            return privilegeType;
        }

        @Override
        public boolean isGranted() {
            return (permissions & GRANTED) == GRANTED;
        }

        @NotNull
        public KingbaseRoleReference getGrantor() {
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

    protected final KingbasePrivilegeOwner owner;
    private ObjectPermission[] permissions;

    public KingbasePrivilege(KingbasePrivilegeOwner owner, List<KingbasePrivilegeGrant> grants) {
        this.owner = owner;
        this.permissions = new ObjectPermission[grants.size()];
        for (int i = 0 ; i < grants.size(); i++) {
            final KingbasePrivilegeGrant privilege = grants.get(i);
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
    public KingbasePrivilegeOwner getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public KingbaseDataSource getDataSource() {
        return owner.getDataSource();
    }

    public KingbasePrivilegeOwner getOwner() {
        return owner;
    }

    public abstract KingbaseObject getTargetObject(DBRProgressMonitor monitor) throws DBException;

    public ObjectPermission[] getPermissions() {
        return permissions;
    }

    public KingbasePrivilegeType[] getPrivileges() {
        KingbasePrivilegeType[] ppt = new KingbasePrivilegeType[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            ppt[i] = permissions[i].getPrivilegeType();
        }
        return ppt;
    }

    public short getPermission(KingbasePrivilegeType privilegeType) {
        for (ObjectPermission permission : permissions) {
            if (permission.privilegeType == privilegeType || permission.privilegeType == KingbasePrivilegeType.ALL) {
                return permission.permissions;
            }
        }
        return NONE;
    }

    public void setPermission(KingbasePrivilegeType privilegeType, boolean permit) {
        for (ObjectPermission permission : permissions) {
            if (permission.privilegeType == privilegeType) {
                if (permit) {
                    permission.permissions |= GRANTED;
                } else {
                    permission.permissions = 0;
                }
            }
        }
    }

    /**
     * Checks all privileges
     */
    public boolean hasAllPrivileges(Object object) {
        for (KingbasePrivilegeType pt : getDataSource().getSupportedPrivilegeTypes()) {
            if (pt.isValid() && pt.supportsType(object.getClass()) && getPermission(pt) == 0) {
                return false;
            }
        }
        return true;
    }

}

