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
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

/**
 * One {@code EXECUTE} privilege grant on a procedure or function, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerProcedure.PrivilegeCache}).
 * Unlike tables/views (which both use {@code ON TABLE}, see {@link MimerObjectPrivilege}),
 * Mimer SQL's GRANT/REVOKE EXECUTE syntax keys off the routine's own kind - {@code ON PROCEDURE}
 * vs {@code ON FUNCTION}, e.g. {@code GRANT EXECUTE ON PROCEDURE "mimer_store"."age_of_adult"
 * TO ...} in {@code mimer_store.sql}, {@code GRANT EXECUTE ON FUNCTION capitalize TO ...} in the
 * official docs - {@link MimerProcedure#getProcedureType()} picks the right keyword.
 * <p>
 * The {@code OBJECT_TYPE} filter ({@code 'PROCEDURE'}/{@code 'FUNCTION'}, matching the
 * routine's own kind) matches its DDL keyword, unlike some other privilege types in this
 * catalog (see {@link MimerObjectPrivilege}).
 *
 * @author Mimer Information Technology
 */
public class MimerRoutinePrivilege implements DBSObject, DBPSaveableObject {

    private final MimerProcedure procedure;
    private String grantee;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerRoutinePrivilege(@NotNull MimerProcedure procedure, @NotNull JDBCResultSet dbResult) {
        this.procedure = procedure;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerRoutinePrivilege(@NotNull MimerProcedure procedure, @NotNull String grantee) {
        this.procedure = procedure;
        this.grantee = grantee;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return grantee;
    }

    public void setGrantee(String grantee) {
        this.grantee = grantee;
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
        return procedure;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) procedure.getDataSource();
    }

    @NotNull
    public MimerProcedure getProcedure() {
        return procedure;
    }

    @NotNull
    private String routineKeyword() {
        return procedure.getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT EXECUTE ON " + routineKeyword() + " \"" + procedure.getSchema().getName() + "\".\"" + procedure.getName() + "\" TO \"" + grantee + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE EXECUTE ON " + routineKeyword() + " \"" + procedure.getSchema().getName() + "\".\"" + procedure.getName() + "\" FROM \"" + grantee + "\"";
    }
}
