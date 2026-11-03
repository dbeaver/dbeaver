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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

/**
 * A {@code USAGE} privilege grant on a Mimer SQL user-defined type, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerUserDefinedType.PrivilegeCache}:
 * {@code WHERE OBJECT_TYPE = 'USER DEFINED TYPE' AND PRIVILEGE_TYPE = 'USAGE'}). That view only
 * ever shows grants where the connected ident is the GRANTOR or GRANTEE, so this list can be
 * incomplete if a grant was made by a different ident than the one currently connected. See
 * {@link AbstractMimerObjectPrivilege} for the shape this and every other single-privilege-type
 * grant class share.
 *
 * @author Mimer Information Technology
 */
public class MimerUserDefinedTypePrivilege extends AbstractMimerObjectPrivilege<MimerUserDefinedType> implements DBPNamedObject2 {

    public MimerUserDefinedTypePrivilege(@NotNull MimerUserDefinedType type, @NotNull JDBCResultSet dbResult) {
        super(type, dbResult);
    }

    public MimerUserDefinedTypePrivilege(@NotNull MimerUserDefinedType type, @NotNull String grantee) {
        super(type, grantee);
    }

    @Override
    public void setName(@NotNull String name) {
        setGrantee(name);
    }

    @NotNull
    public MimerUserDefinedType getType() {
        return owner;
    }

    @NotNull
    @Override
    protected String getPrivilegeType() {
        return "USAGE";
    }

    @NotNull
    @Override
    protected String getObjectTypeKeyword() {
        return "TYPE";
    }

    @NotNull
    @Override
    protected String buildQualifiedOwnerName() {
        return "\"" + owner.getSchema().getName().replace("\"", "\"\"")
            + "\".\"" + owner.getName().replace("\"", "\"\"") + "\"";
    }
}
