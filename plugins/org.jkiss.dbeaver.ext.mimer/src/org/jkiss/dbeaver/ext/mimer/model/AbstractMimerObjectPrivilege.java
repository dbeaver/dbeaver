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
 * Shared shape for a privilege grant that always carries exactly one fixed privilege type against
 * one owner (e.g. {@code USAGE} on a sequence, {@code EXECUTE} on a program) - every row is one
 * {@code GRANTEE}/{@code GRANTOR}/{@code IS_GRANTABLE} triple read from {@code
 * INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES}. Subclasses only need to supply the privilege type,
 * the {@code ON <keyword>} DDL keyword, and the owner's own quoted (and, where applicable,
 * schema-qualified) name - see {@link #getPrivilegeType()}/{@link #getObjectTypeKeyword()}/{@link
 * #buildQualifiedOwnerName()}. Extracted from {@code MimerSequencePrivilege}/{@code
 * MimerDomainPrivilege}/{@code MimerProgramPrivilege}/{@code MimerStatementPrivilege}/{@code
 * MimerUserDefinedTypePrivilege}, which used to duplicate this in full - see {@link
 * org.jkiss.dbeaver.ext.mimer.edit.AbstractMimerPrivilegeManager} for the matching
 * create/drop side. Two independent things vary per privilege kind and are deliberately NOT
 * normalized away here since they reflect real (if likely accidental) differences that predate
 * this refactor: whether the owner has a schema (a program's name is unqualified, unlike a
 * sequence/domain/statement/type's) and whether the subclass also implements {@code
 * DBPNamedObject2} (only some of the originals did, enabling the navigator's inline rename
 * gesture on a still-pending new grant row) - both stay entirely up to the subclass.
 *
 * @author Mimer Information Technology
 */
public abstract class AbstractMimerObjectPrivilege<O extends DBSObject> implements DBSObject, DBPSaveableObject, MimerGrantable {

    protected final O owner;
    private String grantee;
    private String grantor;
    private boolean grantable;
    private boolean persisted;

    protected AbstractMimerObjectPrivilege(@NotNull O owner, @NotNull JDBCResultSet dbResult) {
        this.owner = owner;
        this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(dbResult, "GRANTOR");
        this.grantable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_GRANTABLE"));
        this.persisted = true;
    }

    protected AbstractMimerObjectPrivilege(@NotNull O owner, @NotNull String grantee) {
        this.owner = owner;
        this.grantee = grantee;
        this.persisted = false;
    }

    /**
     * The fixed privilege type this class always grants/revokes, e.g. {@code "USAGE"}/{@code
     * "EXECUTE"} - never read from the catalog, since every row backing one of these classes is
     * already known to carry exactly this type (that's what distinguishes this shape from {@link
     * MimerObjectPrivilege}/{@link MimerColumnPrivilege}/{@link MimerRoutinePrivilege}, which each
     * cover more than one selectable privilege type).
     */
    @NotNull
    protected abstract String getPrivilegeType();

    /**
     * The DDL keyword after {@code ON}, e.g. {@code "SEQUENCE"}/{@code "PROGRAM"}.
     */
    @NotNull
    protected abstract String getObjectTypeKeyword();

    /**
     * The owner's own quoted name as it belongs in the DDL - schema-qualified
     * ({@code "\"s\".\"n\""}) for most owners, bare ({@code "\"n\""}) for a program (programs have
     * no schema).
     */
    @NotNull
    protected abstract String buildQualifiedOwnerName();

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return grantee;
    }

    public void setGrantee(@NotNull String grantee) {
        this.grantee = grantee;
    }

    @NotNull
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

    @NotNull
    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) owner.getDataSource();
    }

    @NotNull
    @Override
    public final String buildGrantDDL() {
        String ddl = "GRANT " + getPrivilegeType() + " ON " + getObjectTypeKeyword() + " " + buildQualifiedOwnerName()
            + " TO \"" + getName().replace("\"", "\"\"") + "\"";
        return isGrantable() ? ddl + " WITH GRANT OPTION" : ddl;
    }

    @NotNull
    @Override
    public final String buildRevokeDDL() {
        return "REVOKE " + getPrivilegeType() + " ON " + getObjectTypeKeyword() + " " + buildQualifiedOwnerName()
            + " FROM \"" + getName().replace("\"", "\"\"") + "\"";
    }
}
