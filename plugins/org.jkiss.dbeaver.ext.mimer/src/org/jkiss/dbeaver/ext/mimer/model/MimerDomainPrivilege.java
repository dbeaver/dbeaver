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

/**
 * One {@code USAGE} privilege grant on a domain, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerDomain.PrivilegeCache}) - same
 * shape as {@link MimerSequencePrivilege}.
 *
 * @author Mimer Information Technology
 */
public class MimerDomainPrivilege implements DBSObject, DBPSaveableObject {

    private final MimerDomain domain;
    private String grantee;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerDomainPrivilege(@NotNull MimerDomain domain, @NotNull JDBCResultSet dbResult) {
        this.domain = domain;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerDomainPrivilege(@NotNull MimerDomain domain, @NotNull String grantee) {
        this.domain = domain;
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
        return domain;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return domain.getDataSource();
    }

    @NotNull
    public MimerDomain getDomain() {
        return domain;
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT USAGE ON DOMAIN \"" + domain.getSchema().getName() + "\".\"" + domain.getName() + "\" TO \"" + grantee + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE USAGE ON DOMAIN \"" + domain.getSchema().getName() + "\".\"" + domain.getName() + "\" FROM \"" + grantee + "\"";
    }
}
