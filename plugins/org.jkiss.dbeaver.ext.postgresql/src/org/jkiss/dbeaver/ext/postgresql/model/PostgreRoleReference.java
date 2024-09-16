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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Objects;

public class PostgreRoleReference implements Comparable<PostgreRoleReference> {

    @NotNull
    private final PostgreDatabase database;
    @NotNull
    private final String roleName;
    @Nullable
    private final String roleType;

    public PostgreRoleReference(@NotNull PostgreDatabase database, @NotNull String roleName, @Nullable String roleType) {
        this.database = database;
        this.roleName = roleName;
        this.roleType = roleType;
    }

    @NotNull
    public PostgreDatabase getDatabase() {
        return this.database;
    }

    @NotNull
    public String getRoleName() {
        return this.roleName;
    }

    @Nullable
    public String getRoleType() {
        return this.roleType;
    }

    @NotNull
    public String getDisplayString() {
        return (CommonUtils.isEmpty(this.roleType) ? "" : this.roleType) +  DBUtils.getQuotedIdentifier(this.database.getDataSource(), this.roleName);
    }

    @Override
    public int compareTo(PostgreRoleReference other) {
        int rc = this.database.equals(other.database) ? 0 : this.database.getName().compareTo(other.database.getName());
        if (rc == 0) {
            rc = this.roleName.compareTo(other.roleName);
        }
        if (rc == 0) {
            rc = Objects.compare(this.roleType, other.roleType, String::compareToIgnoreCase);
        }
        return rc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof PostgreRoleReference other) {
            return this.compareTo(other) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.database, this.roleName, this.roleType);
    }

    @Override
    public String toString() {
        return "PostgreRoleReference[" +
            DBUtils.getFullQualifiedName(this.database.getDataSource(), this.database) + ": " +
            (CommonUtils.isEmpty(this.roleType) ? "" : (this.roleType + " ")) +
            this.roleName +
            "]";
    }
}
