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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A Mimer SQL user-defined type method that has been <b>declared but not yet given a body</b>.
 * Read from {@code INFORMATION_SCHEMA.METHOD_SPECIFICATIONS}, filtered with a {@code NOT
 * EXISTS} check down to only the specs with no matching {@code INFORMATION_SCHEMA.ROUTINES}
 * row yet.
 * <p>
 * Creating a method is a genuine two-phase process in Mimer SQL (see {@code
 * .../SQL_Statements/ALTER_TYPE.htm} and {@code .../udts/udts.htm}). First, {@code ALTER TYPE
 * "s"."t" ADD [CONSTRUCTOR|INSTANCE|STATIC] METHOD "name"(params) RETURNS &lt;type&gt; ...}
 * declares just the signature - creating this class, via {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerUdtMethodSpecManager}. Then {@code CREATE SPECIFIC
 * METHOD "s"."specificName" BEGIN ... END} separately supplies the body, through this same
 * class's editable Source tab. Once that runs, the object "graduates" to a real {@link
 * MimerUdtMethod} row on next refresh - it now has a matching {@code ROUTINES} row, so the
 * {@code NOT EXISTS} filter excludes it from this class's own cache.
 * <p>
 * Shown as a sibling row alongside real {@link MimerUdtMethod}s in the same
 * Instance/Static/Constructor Methods folder. {@link #getObjectState} always reports {@link
 * DBSObjectState#UNKNOWN}, which DBeaver's stock tree rendering overlays onto whatever base
 * icon this class gets - visually marking it as pending without needing a dedicated icon of its
 * own.
 * <p>
 * Editing an already-defined {@link MimerUdtMethod}'s body is out of scope for this class - it's
 * not established whether Mimer SQL's plain {@code DROP SPECIFIC METHOD} (no {@code ALTER TYPE}
 * prefix, used once a method has a body) leaves the type-level spec behind for a fresh {@code
 * CREATE SPECIFIC METHOD}, or removes the whole method including its specification.
 *
 * @author Mimer Information Technology
 */
public class MimerUdtMethodSpec implements DBSObject, DBPSaveableObject, DBPStatefulObject, DBSObjectWithScript {

    private final MimerUserDefinedType type;
    private String methodName;
    private String specificName;
    private String methodKind;
    private String returnDataType;
    private boolean deterministic;
    private String accessOption;
    private String parameterList = "";
    private String source;
    private boolean persisted = true;
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();

    public MimerUdtMethodSpec(@NotNull MimerUserDefinedType type, @NotNull JDBCResultSet dbResult) {
        this.type = type;
        this.methodName = JDBCUtils.safeGetString(dbResult, "METHOD_NAME");
        this.specificName = JDBCUtils.safeGetString(dbResult, "SPECIFIC_NAME");
        this.methodKind = resolveMethodKind(dbResult);
        this.deterministic = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_DETERMINISTIC"));
        this.accessOption = JDBCUtils.safeGetStringTrimmed(dbResult, "SQL_DATA_ACCESS");

        String rawDataType = JDBCUtils.safeGetStringTrimmed(dbResult, "DATA_TYPE");
        if ("USER-DEFINED".equalsIgnoreCase(rawDataType)) {
            this.returnDataType = JDBCUtils.safeGetString(dbResult, "RETURN_UDT_SCHEMA") + "."
                + JDBCUtils.safeGetString(dbResult, "RETURN_UDT_NAME");
        } else {
            this.returnDataType = MimerUtils.formatDomainDataType(
                CommonUtils.notEmpty(rawDataType),
                JDBCUtils.safeGetLong(dbResult, "CHARACTER_MAXIMUM_LENGTH"),
                JDBCUtils.safeGetInteger(dbResult, "NUMERIC_PRECISION"),
                JDBCUtils.safeGetInteger(dbResult, "NUMERIC_SCALE"));
        }
    }

    /**
     * For a brand-new specification, not yet declared on the server - see {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerUdtMethodSpecManager}.
     */
    public MimerUdtMethodSpec(@NotNull MimerUserDefinedType type, @NotNull String methodKind, @NotNull String methodName) {
        this.type = type;
        this.methodKind = methodKind;
        this.methodName = methodName;
        this.persisted = false;
    }

    @NotNull
    private static String resolveMethodKind(@NotNull JDBCResultSet dbResult) {
        if ("YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_STATIC"))) {
            return "STATIC METHOD";
        }
        if ("YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_CONSTRUCTOR"))) {
            return "CONSTRUCTOR METHOD";
        }
        return "INSTANCE METHOD";
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return methodName;
    }

    public void setName(@NotNull String methodName) {
        this.methodName = methodName;
    }

    @Property(viewable = true, order = 2)
    public String getSpecificName() {
        return specificName;
    }

    public void setSpecificName(@Nullable String specificName) {
        this.specificName = specificName;
    }

    /**
     * {@code specificName} when set, else the bare method name - same fallback {@link
     * org.jkiss.dbeaver.ext.generic.model.GenericProcedure#getUniqueName} uses, needed here for
     * the same reason: {@link MimerObjectUsedBy}/{@link MimerObjectUses} must scope by specific
     * name, not bare name, since a method name can overload within one type.
     */
    @NotNull
    String uniqueName() {
        return CommonUtils.isEmpty(specificName) ? getName() : specificName;
    }

    @Property(viewable = true, order = 3)
    public String getMethodKind() {
        return methodKind;
    }

    @Property(viewable = true, order = 4)
    public String getReturnDataType() {
        return returnDataType;
    }

    public void setReturnDataType(@NotNull String returnDataType) {
        this.returnDataType = returnDataType;
    }

    @Property(viewable = true, order = 5)
    public boolean isDeterministic() {
        return deterministic;
    }

    public void setDeterministic(boolean deterministic) {
        this.deterministic = deterministic;
    }

    @Property(viewable = true, order = 6)
    public String getAccessOption() {
        return accessOption;
    }

    public void setAccessOption(@NotNull String accessOption) {
        this.accessOption = accessOption;
    }

    /**
     * The pre-rendered parameter list body (e.g. {@code "\"a\" INTEGER, \"b\" VARCHAR(10)"}),
     * built by the create dialog's parameter grid.
     */
    public void setParameterList(@NotNull String parameterList) {
        this.parameterList = parameterList;
    }

    @NotNull
    public MimerUserDefinedType getType() {
        return type;
    }

    /**
     * Always empty - unlike {@link MimerUdtMethod}, this class doesn't model {@code
     * METHOD_SPECIFICATION_PARAMETERS} (out of scope for this first pass, see the class Javadoc).
     * Still needed so the shared "Parameters" sub-folder these two classes' mixed tree listing
     * shares can expand a pending spec row without a reflection error - an empty folder rather
     * than a broken one.
     */
    @Association
    public List<DBSObject> getParameters(@NotNull DBRProgressMonitor monitor) {
        return Collections.emptyList();
    }

    /**
     * {@code ALTER TYPE "s"."t" ADD [CONSTRUCTOR|INSTANCE|STATIC] METHOD "name"(params) RETURNS
     * &lt;type&gt; [SPECIFIC "n"] &lt;DETERMINISTIC|NOT DETERMINISTIC&gt; &lt;access-option&gt;} -
     * a constructor method has a fixed name (the type's own name) and fixed return type (the type
     * itself), so {@link #parameterList} is the only real input for that kind.
     */
    @NotNull
    public String buildAddSpecificationDDL() {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TYPE \"").append(type.getSchema().getName()).append("\".\"").append(type.getName()).append("\"\n");
        sb.append("ADD ").append(methodKindKeyword()).append(" \"").append(methodName).append("\"(\n");
        sb.append(parameterList).append(")\n");
        sb.append("RETURNS ").append(returnDataType).append('\n');
        if (!CommonUtils.isEmptyTrimmed(specificName)) {
            sb.append("SPECIFIC \"").append(specificName.trim()).append("\" ");
        }
        sb.append(deterministic ? "DETERMINISTIC" : "NOT DETERMINISTIC").append(' ').append(accessOption);
        return sb.toString();
    }

    @NotNull
    private String methodKindKeyword() {
        return switch (methodKind) {
            case "STATIC METHOD" -> "STATIC METHOD";
            case "CONSTRUCTOR METHOD" -> "CONSTRUCTOR METHOD";
            default -> "INSTANCE METHOD";
        };
    }

    /**
     * {@code ALTER TYPE "s"."t" DROP SPECIFIC METHOD "specificName" [RESTRICT|CASCADE]} - drops
     * the whole declaration, since this class only ever represents a spec with no body yet.
     */
    @NotNull
    public String buildDropDDL(boolean cascade) {
        return "ALTER TYPE \"" + type.getSchema().getName() + "\".\"" + type.getName() + "\"\n"
            + "DROP SPECIFIC METHOD \"" + specificName + "\" " + (cascade ? "CASCADE" : "RESTRICT");
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * The {@code @Property} annotation is required - without it the Source tab's Save silently
     * discards edits (see {@code MimerModule}'s identical getter for the full explanation).
     * Always returns a placeholder body - there is nothing to read from the catalog yet, since a
     * spec with no body has no {@code EXT_SOURCE_DEFINITION} row.
     */
    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) {
        if (source == null) {
            source = "BEGIN\n    -- TODO: method body\nEND";
        }
        return source;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        this.source = source;
    }

    /**
     * {@code CREATE SPECIFIC METHOD "s"."specificName" BEGIN <body> END} - supplies the body for
     * this already-declared specification. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerUdtMethodSpecManager} for the write side.
     */
    @NotNull
    public String buildCreateBodyDDL() {
        return "CREATE SPECIFIC METHOD \"" + type.getSchema().getName() + "\".\"" + specificName + "\"\n" + source;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return DBSObjectState.UNKNOWN;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) {
        // Always pending by definition - this class only ever represents a not-yet-defined spec.
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
        return type;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return type.getDataSource();
    }

    @Association
    public Collection<MimerObjectUsedBy> getUsedBy(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usedByCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<MimerObjectUses> getUses(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usesCache.getAllObjects(monitor, this);
    }

    static class UsedByCache extends JDBCObjectCache<MimerUdtMethodSpec, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUdtMethodSpec owner) throws SQLException {
            return MimerObjectUsedBy.prepareUsedByStatementBySpecificName(
                session, owner.getType().getSchema().getName(), owner.uniqueName(), owner.methodKind);
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerUdtMethodSpec owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerUdtMethodSpec, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUdtMethodSpec owner) throws SQLException {
            return MimerObjectUses.prepareUsesStatementBySpecificName(
                session, owner.getType().getSchema().getName(), owner.uniqueName(), owner.methodKind);
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerUdtMethodSpec owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
