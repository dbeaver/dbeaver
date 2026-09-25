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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * An {@code EXECUTE} privilege grant on a Mimer SQL program - lets a program's own {@code
 * ENTER <program> USING <password>} security layer actually be used by another ident. Read from
 * {@code INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerProgram.PrivilegeCache}:
 * {@code WHERE OBJECT_TYPE = 'IDENT' AND OBJECT_NAME = <program> AND PRIVILEGE_TYPE =
 * 'EXECUTE'} - {@code OBJECT_TYPE = 'IDENT'} is the same convention used for group membership).
 * That view only ever shows grants where the connected ident is the GRANTOR or GRANTEE, so this list can be
 * incomplete if a grant was made by a different ident than the one currently connected - same
 * caveat as {@link MimerGroupMember}.
 *
 * @author Mimer Information Technology
 */
public class MimerProgramPrivilege implements DBSObject, DBPNamedObject2, DBPSaveableObject {

    private final MimerProgram program;
    private String grantee;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerProgramPrivilege(@NotNull MimerProgram program, @NotNull JDBCResultSet dbResult) {
        this.program = program;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerProgramPrivilege(@NotNull MimerProgram program, @NotNull String grantee) {
        this.program = program;
        this.grantee = grantee;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return grantee;
    }

    @Override
    public void setName(String name) {
        this.grantee = name;
    }

    @Property(viewable = true, order = 2)
    public String getGrantor() {
        return grantor;
    }

    @Property(viewable = true, order = 3)
    public boolean isGrantable() {
        return grantable;
    }

    public void setGrantable(boolean grantable) {
        this.grantable = grantable;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public DBSObject getParentObject() {
        return program;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return program.getDataSource();
    }

    @NotNull
    public MimerProgram getProgram() {
        return program;
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT EXECUTE ON PROGRAM \"" + program.getName() + "\" TO \"" + grantee + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE EXECUTE ON PROGRAM \"" + program.getName() + "\" FROM \"" + grantee + "\"";
    }
}
