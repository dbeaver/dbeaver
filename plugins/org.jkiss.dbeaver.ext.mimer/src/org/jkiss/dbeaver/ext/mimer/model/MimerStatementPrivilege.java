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
 * An {@code EXECUTE} privilege grant on a Mimer SQL statement, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerStatement.PrivilegeCache}: {@code
 * WHERE OBJECT_TYPE = 'STATEMENT' AND OBJECT_NAME = <statement> AND PRIVILEGE_TYPE = 'EXECUTE'} -
 * this row type uses {@code OBJECT_TYPE = 'STATEMENT'} verbatim (unlike group
 * membership, whose rows carry {@code OBJECT_TYPE = 'IDENT'} instead, this one matches the
 * {@code ON STATEMENT} DDL keyword). That view only ever shows grants where the connected ident
 * is the GRANTOR or GRANTEE, so this list can be incomplete if a grant was made by a different
 * ident than the one currently connected - same caveat as {@link MimerGroupMember}/{@link
 * MimerProgramPrivilege}.
 *
 * @author Mimer Information Technology
 */
public class MimerStatementPrivilege implements DBSObject, DBPNamedObject2, DBPSaveableObject {

    private final MimerStatement statement;
    private String grantee;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerStatementPrivilege(@NotNull MimerStatement statement, @NotNull JDBCResultSet dbResult) {
        this.statement = statement;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerStatementPrivilege(@NotNull MimerStatement statement, @NotNull String grantee) {
        this.statement = statement;
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
        return statement;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return statement.getDataSource();
    }

    @NotNull
    public MimerStatement getStatement() {
        return statement;
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT EXECUTE ON STATEMENT \"" + statement.getSchema().getName() + "\".\"" + statement.getName()
            + "\" TO \"" + grantee + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE EXECUTE ON STATEMENT \"" + statement.getSchema().getName() + "\".\"" + statement.getName()
            + "\" FROM \"" + grantee + "\"";
    }
}
