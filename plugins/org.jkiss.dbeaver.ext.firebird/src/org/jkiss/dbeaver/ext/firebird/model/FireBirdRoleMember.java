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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * A user or role a Firebird role is granted to.
 */
public class FireBirdRoleMember implements DBSObject {

    @NotNull
    private final FireBirdRole role;
    @NotNull
    private final String name;
    private final boolean memberRole;
    @Nullable
    private final String grantor;
    private final boolean adminOption;
    private final boolean defaultRole;

    public FireBirdRoleMember(
        @NotNull FireBirdRole role,
        @NotNull String name,
        boolean memberRole,
        @Nullable String grantor,
        boolean adminOption,
        boolean defaultRole
    ) {
        this.role = role;
        this.name = name;
        this.memberRole = memberRole;
        this.grantor = grantor;
        this.adminOption = adminOption;
        this.defaultRole = defaultRole;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    /**
     * True if the member is a role, false if it is a user.
     */
    @Property(viewable = true, order = 2)
    public boolean isRole() {
        return memberRole;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public String getGrantor() {
        return grantor;
    }

    @Property(viewable = true, order = 4)
    public boolean isAdminOption() {
        return adminOption;
    }

    /**
     * True if the role is granted as default role, so it is active without being specified at connect (Firebird 4+).
     */
    @Property(viewable = true, order = 5)
    public boolean isDefaultRole() {
        return defaultRole;
    }

    /**
     * GRANT statement that makes this member a member of the role.
     */
    @NotNull
    public String getGrantStatement() {
        StringBuilder sql = new StringBuilder("GRANT ");
        if (defaultRole) {
            sql.append("DEFAULT ");
        }
        sql.append(DBUtils.getQuotedIdentifier(role)).append(" TO ");
        if (memberRole) {
            sql.append("ROLE ").append(DBUtils.getQuotedIdentifier(getDataSource(), name));
        } else if (FireBirdConstants.GRANTEE_PUBLIC.equals(name)) {
            sql.append(FireBirdConstants.GRANTEE_PUBLIC);
        } else {
            sql.append("USER ").append(DBUtils.getQuotedIdentifier(getDataSource(), name));
        }
        if (adminOption) {
            sql.append(" WITH ADMIN OPTION");
        }
        return sql.toString();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public FireBirdRole getParentObject() {
        return role;
    }

    @NotNull
    @Override
    public FireBirdDataSource getDataSource() {
        return role.getDataSource();
    }

    @Override
    public String toString() {
        return name;
    }
}
