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
 * One {@code USAGE} privilege grant on a sequence, read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerSequence.PrivilegeCache}) - same
 * shape as {@link MimerRoutinePrivilege} (single fixed privilege type, one owner kind).
 *
 * @author Mimer Information Technology
 */
public class MimerSequencePrivilege implements DBSObject, DBPSaveableObject {

    private final MimerSequence sequence;
    private String grantee;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerSequencePrivilege(@NotNull MimerSequence sequence, @NotNull JDBCResultSet dbResult) {
        this.sequence = sequence;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerSequencePrivilege(@NotNull MimerSequence sequence, @NotNull String grantee) {
        this.sequence = sequence;
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
        return sequence;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) sequence.getDataSource();
    }

    @NotNull
    public MimerSequence getSequence() {
        return sequence;
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT USAGE ON SEQUENCE \"" + sequence.getParentObject().getName() + "\".\"" + sequence.getName() + "\" TO \"" + grantee + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE USAGE ON SEQUENCE \"" + sequence.getParentObject().getName() + "\".\"" + sequence.getName() + "\" FROM \"" + grantee + "\"";
    }
}
