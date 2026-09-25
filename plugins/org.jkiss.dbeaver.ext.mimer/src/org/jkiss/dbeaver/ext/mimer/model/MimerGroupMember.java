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
 * One member of a Mimer SQL group - a {@code MEMBER} privilege grant, read from
 * {@code INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES} (see {@link MimerGroup.MemberCache}:
 * {@code WHERE OBJECT_TYPE = 'GROUP' AND OBJECT_NAME = <group> AND PRIVILEGE_TYPE =
 * 'MEMBER'}). Note that view only ever shows grants where the connected ident is the
 * GRANTOR or GRANTEE, so a group's member list here can be incomplete if its members
 * were granted membership by a different ident than the one currently connected.
 *
 * @author Mimer Information Technology
 */
public class MimerGroupMember implements DBSObject, DBPNamedObject2, DBPSaveableObject {

    private final MimerGroup group;
    private String memberName;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    public MimerGroupMember(@NotNull MimerGroup group, @NotNull JDBCResultSet dbResult) {
        this.group = group;
        this.memberName = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    public MimerGroupMember(@NotNull MimerGroup group, @NotNull String memberName) {
        this.group = group;
        this.memberName = memberName;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return memberName;
    }

    @Override
    public void setName(String name) {
        this.memberName = name;
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
        return group;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return group.getDataSource();
    }

    @NotNull
    public MimerGroup getGroup() {
        return group;
    }

    @NotNull
    public String buildGrantDDL() {
        String ddl = "GRANT MEMBER ON GROUP \"" + group.getName() + "\" TO \"" + memberName + "\"";
        return grantable ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    public String buildRevokeDDL() {
        return "REVOKE MEMBER ON GROUP \"" + group.getName() + "\" FROM \"" + memberName + "\"";
    }
}
