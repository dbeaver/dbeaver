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
 * An {@code EXECUTE} privilege grant on a Mimer SQL program - lets a program's own {@code
 * ENTER <program> USING <password>} security layer actually be used by another ident. Read from
 * {@code INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerProgram.PrivilegeCache}:
 * {@code WHERE OBJECT_TYPE = 'IDENT' AND OBJECT_NAME = <program> AND PRIVILEGE_TYPE =
 * 'EXECUTE'} - {@code OBJECT_TYPE = 'IDENT'} is the same convention used for group membership).
 * That view only ever shows grants where the connected ident is the GRANTOR or GRANTEE, so this
 * list can be incomplete if a grant was made by a different ident than the one currently
 * connected - same caveat as {@link MimerGroupMember}. See {@link AbstractMimerObjectPrivilege}
 * for the shape this and every other single-privilege-type grant class share - a program has no
 * schema, so {@link #buildQualifiedOwnerName()} is the one owner-kind here with a bare, unqualified
 * name.
 *
 * @author Mimer Information Technology
 */
public class MimerProgramPrivilege extends AbstractMimerObjectPrivilege<MimerProgram> implements DBPNamedObject2 {

    public MimerProgramPrivilege(@NotNull MimerProgram program, @NotNull JDBCResultSet dbResult) {
        super(program, dbResult);
    }

    public MimerProgramPrivilege(@NotNull MimerProgram program, @NotNull String grantee) {
        super(program, grantee);
    }

    @Override
    public void setName(@NotNull String name) {
        setGrantee(name);
    }

    @NotNull
    public MimerProgram getProgram() {
        return owner;
    }

    @NotNull
    @Override
    protected String getPrivilegeType() {
        return "EXECUTE";
    }

    @NotNull
    @Override
    protected String getObjectTypeKeyword() {
        return "PROGRAM";
    }

    @NotNull
    @Override
    protected String buildQualifiedOwnerName() {
        return "\"" + owner.getName().replace("\"", "\"\"") + "\"";
    }
}
