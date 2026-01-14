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
import org.jkiss.dbeaver.model.meta.Property;

import java.util.List;

public class PostgreDefaultPrivilege extends PostgreObjectPrivilege {

    private PostgrePrivilegeGrant.Kind underKind;
    private final PostgreRoleReference grantor;

    public PostgreDefaultPrivilege(
        @Nullable PostgrePrivilegeOwner owner,
        @Nullable PostgreRoleReference grantee,
        @Nullable PostgreRoleReference grantor,
        @NotNull List<PostgrePrivilegeGrant> privileges
    ) {
        super(owner, grantee, privileges);
        this.grantor = grantor;
    }

    @Nullable
    public PostgrePrivilegeGrant.Kind getUnderKind() {
        return underKind;
    }

    public void setUnderKind(PostgrePrivilegeGrant.Kind underKind) {
        this.underKind = underKind;
    }

    void setUnderKind(@NotNull String kind) {
        if (PostgreClass.RelKind.r.getCode().equals(kind)) {
            underKind = PostgrePrivilegeGrant.Kind.TABLE;
        } else if (PostgreClass.RelKind.S.getCode().equals(kind)) {
            underKind = PostgrePrivilegeGrant.Kind.SEQUENCE;
        } else if ("f".equals(kind)) { // Here "f" is not from RelKind
            underKind = PostgrePrivilegeGrant.Kind.FUNCTION;
        } else if ("T".equals(kind)) { // Here "T" is not from RelKind
            underKind = PostgrePrivilegeGrant.Kind.TYPE;
        }
    }

    @Nullable
    public PostgreRoleReference getGrantor() {
        return grantor;
    }

    @Property(viewable = true, order = 1)
    @NotNull
    public String getName() {
        return getOwner() == null ? "": getOwner().getName() + "." + underKind;
    }
}
