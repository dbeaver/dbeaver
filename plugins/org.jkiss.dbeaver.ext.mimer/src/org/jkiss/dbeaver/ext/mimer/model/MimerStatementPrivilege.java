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
 * An {@code EXECUTE} privilege grant on a Mimer SQL statement, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerStatement.PrivilegeCache}: {@code
 * WHERE OBJECT_TYPE = 'STATEMENT' AND OBJECT_NAME = <statement> AND PRIVILEGE_TYPE = 'EXECUTE'} -
 * this row type uses {@code OBJECT_TYPE = 'STATEMENT'} verbatim (unlike group membership, whose
 * rows carry {@code OBJECT_TYPE = 'IDENT'} instead, this one matches the {@code ON STATEMENT} DDL
 * keyword). That view only ever shows grants where the connected ident is the GRANTOR or GRANTEE,
 * so this list can be incomplete if a grant was made by a different ident than the one currently
 * connected - same caveat as {@link MimerGroupMember}/{@link MimerProgramPrivilege}. See {@link
 * AbstractMimerObjectPrivilege} for the shape this and every other single-privilege-type grant
 * class share.
 *
 * @author Mimer Information Technology
 */
public class MimerStatementPrivilege extends AbstractMimerObjectPrivilege<MimerStatement> implements DBPNamedObject2 {

    public MimerStatementPrivilege(@NotNull MimerStatement statement, @NotNull JDBCResultSet dbResult) {
        super(statement, dbResult);
    }

    public MimerStatementPrivilege(@NotNull MimerStatement statement, @NotNull String grantee) {
        super(statement, grantee);
    }

    @Override
    public void setName(String name) {
        setGrantee(name);
    }

    @NotNull
    public MimerStatement getStatement() {
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
        return "STATEMENT";
    }

    @NotNull
    @Override
    protected String buildQualifiedOwnerName() {
        return "\"" + owner.getSchema().getName() + "\".\"" + owner.getName() + "\"";
    }
}
