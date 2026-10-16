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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

/**
 * One {@code EXECUTE} privilege grant on a procedure or function, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerProcedure.PrivilegeCache}).
 * Unlike tables/views (which both use {@code ON TABLE}, see {@link MimerObjectPrivilege}),
 * Mimer SQL's GRANT/REVOKE EXECUTE syntax keys off the routine's own kind - {@code ON PROCEDURE}
 * vs {@code ON FUNCTION}, e.g. {@code GRANT EXECUTE ON PROCEDURE "mimer_store"."age_of_adult"
 * TO ...} in {@code mimer_store.sql}, {@code GRANT EXECUTE ON FUNCTION capitalize TO ...} in the
 * official docs - {@link #getObjectTypeKeyword()} picks the right keyword per instance, the one
 * place this class needs more than {@link AbstractMimerObjectPrivilege}'s usual fixed keyword.
 * <p>
 * The {@code OBJECT_TYPE} filter ({@code 'PROCEDURE'}/{@code 'FUNCTION'}, matching the
 * routine's own kind) matches its DDL keyword, unlike some other privilege types in this
 * catalog (see {@link MimerObjectPrivilege}).
 *
 * @author Mimer Information Technology
 */
public class MimerRoutinePrivilege extends AbstractMimerObjectPrivilege<MimerProcedure> {

    public MimerRoutinePrivilege(@NotNull MimerProcedure procedure, @NotNull JDBCResultSet dbResult) {
        super(procedure, dbResult);
    }

    public MimerRoutinePrivilege(@NotNull MimerProcedure procedure, @NotNull String grantee) {
        super(procedure, grantee);
    }

    @NotNull
    public MimerProcedure getProcedure() {
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
        return owner.getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
    }

    @NotNull
    @Override
    protected String buildQualifiedOwnerName() {
        return "\"" + owner.getSchema().getName().replace("\"", "\"\"")
            + "\".\"" + owner.getName().replace("\"", "\"\"") + "\"";
    }
}
