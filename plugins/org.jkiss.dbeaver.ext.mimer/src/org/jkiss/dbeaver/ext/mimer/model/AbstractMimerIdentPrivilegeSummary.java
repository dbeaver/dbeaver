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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Shared shape for a read-only "what privileges does this ident have" summary row - {@link
 * MimerIdentTablePrivilege}/{@link MimerIdentColumnPrivilege}/{@link MimerIdentObjectPrivilege}
 * each add their own object-identifying fields (which differ in count and meaning per privilege
 * kind) plus a subclass-specific {@code getName()}, but otherwise duplicated this exact
 * ident/grantor/grantable/getDescription/isPersisted/getParentObject/getDataSource shape in full.
 * {@code Grantor}/{@code Grantable} are given a high fixed {@code order} here so they always
 * render after whatever subclass-specific fields precede them, regardless of how many there are.
 *
 * @author Mimer Information Technology
 */
public abstract class AbstractMimerIdentPrivilegeSummary implements DBSObject {

    protected final DBSObject ident;
    private final String grantor;
    private final boolean grantable;

    protected AbstractMimerIdentPrivilegeSummary(@NotNull DBSObject ident, @NotNull JDBCResultSet dbResult) {
        this.ident = ident;
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
    }

    @Property(viewable = true, order = 90)
    public String getGrantor() {
        return grantor;
    }

    @Property(viewable = true, order = 91)
    public boolean isGrantable() {
        return grantable;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return ident;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) ident.getDataSource();
    }
}
