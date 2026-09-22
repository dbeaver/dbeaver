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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A Mimer SQL user-defined type ({@code CREATE TYPE ... AS <type>|(...)}, see {@code
 * INFORMATION_SCHEMA.USER_DEFINED_TYPES}), with **create/drop/comment/grant** support. One class
 * for both categories - {@link #getCategory} is {@code DISTINCT} or {@code STRUCTURED} - matching
 * how {@link MimerProcedure} covers both Procedures and Functions; {@link
 * MimerSchema#getDistinctTypes}/{@link MimerSchema#getStructuredTypes} give the two tree folders
 * their own filtered view, and {@link org.jkiss.dbeaver.ext.mimer.edit.MimerUserDefinedTypeManager#detectCategory}
 * pre-seeds a new type's category from whichever folder was clicked, same "one class, folder
 * detection" pattern as {@link MimerProcedure}/{@link org.jkiss.dbeaver.ext.mimer.edit.MimerProcedureManager}.
 * {@link #getDataType} is only meaningful for a {@code DISTINCT} type (the type it's based on) -
 * {@code STRUCTURED} types have {@link #getAttributes} instead.
 *
 * @author Mimer Information Technology
 */
public class MimerUserDefinedType implements DBSObject, DBPRefreshableObject, DBPSaveableObject {

    private final MimerSchema schema;
    private String name;
    private String category;
    private boolean instantiable;
    private boolean isFinal;
    private String dataType;
    private String collation;
    private String attributesBody;
    private final AttributeCache attributeCache = new AttributeCache();
    private final MethodCache methodCache = new MethodCache();
    private final MethodSpecCache methodSpecCache = new MethodSpecCache();
    private final PrivilegeCache privilegeCache = new PrivilegeCache();
    private String comment;
    private boolean persisted = true;

    MimerUserDefinedType(@NotNull MimerSchema schema, @NotNull JDBCResultSet dbResult) {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "USER_DEFINED_TYPE_NAME");
        this.category = JDBCUtils.safeGetStringTrimmed(dbResult, "USER_DEFINED_TYPE_CATEGORY");
        this.instantiable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_INSTANTIABLE"));
        this.isFinal = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_FINAL"));
        this.dataType = MimerUtils.formatDomainDataType(
            CommonUtils.notEmpty(JDBCUtils.safeGetStringTrimmed(dbResult, "DATA_TYPE")),
            JDBCUtils.safeGetLong(dbResult, "CHARACTER_MAXIMUM_LENGTH"),
            JDBCUtils.safeGetInteger(dbResult, "NUMERIC_PRECISION"),
            JDBCUtils.safeGetInteger(dbResult, "NUMERIC_SCALE"));
    }

    /**
     * For a brand-new, not-yet-created type - see {@link org.jkiss.dbeaver.ext.mimer.edit.MimerUserDefinedTypeManager}.
     */
    public MimerUserDefinedType(@NotNull MimerSchema schema, @NotNull String category, @NotNull String name) {
        this.schema = schema;
        this.category = category;
        this.name = name;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Property(viewable = true, order = 2)
    public String getCategory() {
        return category;
    }

    /**
     * The base type this {@code DISTINCT} type is defined over - not meaningful for a {@code
     * STRUCTURED} type (see {@link #getAttributes} instead). CREATE-only setter carries the
     * create dialog's pre-formatted type text (e.g. {@code "VARCHAR(20)"}) verbatim, same
     * convention as {@code MimerDomain#setDataType}.
     */
    @Property(viewable = true, order = 3)
    public String getDataType() {
        return dataType;
    }

    public void setDataType(@NotNull String dataType) {
        this.dataType = dataType;
    }

    /**
     * CREATE-only ({@code DISTINCT} types only) - never re-hydrated on load.
     */
    public void setCollation(@Nullable String collation) {
        this.collation = collation;
    }

    /**
     * CREATE-only ({@code STRUCTURED} types only) - the fully-rendered attribute list body (e.g.
     * {@code "\"a\" INTEGER, \"b\" VARCHAR(10)"}), built by the create dialog's attribute grid.
     */
    public void setAttributesBody(@NotNull String attributesBody) {
        this.attributesBody = attributesBody;
    }

    @Property(viewable = true, order = 4)
    public boolean isInstantiable() {
        return instantiable;
    }

    @Property(viewable = true, order = 5)
    public boolean isFinal() {
        return isFinal;
    }

    @NotNull
    public MimerSchema getSchema() {
        return schema;
    }

    @Association
    public Collection<MimerUdtAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return attributeCache.getAllObjects(monitor, this);
    }

    @Association
    public List<DBSObject> getConstructorMethodsAndSpecs(@NotNull DBRProgressMonitor monitor) throws DBException {
        return methodsAndSpecsByKind(monitor, "CONSTRUCTOR METHOD");
    }

    @Association
    public List<DBSObject> getInstanceMethodsAndSpecs(@NotNull DBRProgressMonitor monitor) throws DBException {
        return methodsAndSpecsByKind(monitor, "INSTANCE METHOD");
    }

    @Association
    public List<DBSObject> getStaticMethodsAndSpecs(@NotNull DBRProgressMonitor monitor) throws DBException {
        return methodsAndSpecsByKind(monitor, "STATIC METHOD");
    }

    /**
     * Combines defined methods ({@link MimerUdtMethod}, from {@link #methodCache}) with
     * not-yet-defined specifications ({@link MimerUdtMethodSpec}, from {@link #methodSpecCache})
     * into one list, filtered to a single kind - shown as sibling rows in the same tree folder.
     * {@link MimerUdtMethodSpec}
     * implements {@code DBPStatefulObject} to get a distinct "pending" overlay on its icon
     * without needing a whole separate icon of its own.
     */
    @NotNull
    private List<DBSObject> methodsAndSpecsByKind(@NotNull DBRProgressMonitor monitor, @NotNull String kind) throws DBException {
        List<DBSObject> result = new ArrayList<>();
        for (MimerUdtMethod method : methodCache.getAllObjects(monitor, this)) {
            if (kind.equalsIgnoreCase(method.getMethodKind())) {
                result.add(method);
            }
        }
        for (MimerUdtMethodSpec spec : methodSpecCache.getAllObjects(monitor, this)) {
            if (kind.equalsIgnoreCase(spec.getMethodKind())) {
                result.add(spec);
            }
        }
        return result;
    }

    public DBSObjectCache<MimerUserDefinedType, MimerUdtMethodSpec> getMethodSpecCache() {
        return methodSpecCache;
    }

    public DBSObjectCache<MimerUserDefinedType, MimerUdtMethod> getMethodCache() {
        return methodCache;
    }

    @Association
    public Collection<MimerUserDefinedTypePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerUserDefinedType, MimerUserDefinedTypePrivilege> getPrivilegeCache() {
        return privilegeCache;
    }

    /**
     * {@code CREATE TYPE "schema"."name" AS <type> [COLLATE <collation>]} for a {@code DISTINCT}
     * type, or {@code CREATE TYPE "schema"."name" AS (<attributesBody>)} for a {@code STRUCTURED}
     * one.
     */
    @NotNull
    public String buildCreateDDL() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TYPE \"").append(schema.getName()).append("\".\"").append(name).append("\" AS\n");
        if ("STRUCTURED".equalsIgnoreCase(category)) {
            sb.append('(').append(attributesBody).append(')');
        } else {
            sb.append(dataType);
            if (!CommonUtils.isEmpty(collation)) {
                sb.append(" COLLATE ").append(collation);
            }
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@code COMMENT ON TYPE "schema"."name" IS '...'}. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerUserDefinedTypeManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, schema.getName(), null, name, "USER DEFINED TYPE");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
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
        return schema;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) schema.getDataSource();
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        attributeCache.clearCache();
        methodCache.clearCache();
        methodSpecCache.clearCache();
        privilegeCache.clearCache();
        comment = null;
        return this;
    }

    static class AttributeCache extends JDBCObjectCache<MimerUserDefinedType, MimerUdtAttribute> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUserDefinedType type) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ATTRIBUTE_NAME, ORDINAL_POSITION, IS_NULLABLE, ATTRIBUTE_DEFAULT,\n" +
                "       DATA_TYPE, ATTRIBUTE_UDT_SCHEMA, ATTRIBUTE_UDT_NAME,\n" +
                "       CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE,\n" +
                "       COLLATION_SCHEMA, COLLATION_NAME\n" +
                "FROM INFORMATION_SCHEMA.ATTRIBUTES\n" +
                "WHERE UDT_SCHEMA = ? AND UDT_NAME = ?\n" +
                "ORDER BY ORDINAL_POSITION");
            dbStat.setString(1, type.getSchema().getName());
            dbStat.setString(2, type.getName());
            return dbStat;
        }

        @Override
        protected MimerUdtAttribute fetchObject(@NotNull JDBCSession session, @NotNull MimerUserDefinedType type, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerUdtAttribute(type, resultSet);
        }
    }

    /**
     * Loads every fully-defined method regardless of kind in one query - {@link
     * #methodsAndSpecsByKind} splits the result for the three tree folders (Constructor/Instance/
     * Static Methods), same shape as {@link MimerProcedure}'s Procedure/Function split.
     */
    static class MethodCache extends JDBCObjectCache<MimerUserDefinedType, MimerUdtMethod> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUserDefinedType type) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ROUTINE_NAME, SPECIFIC_NAME, ROUTINE_TYPE\n" +
                "FROM INFORMATION_SCHEMA.ROUTINES\n" +
                "WHERE UDT_SCHEMA = ? AND UDT_NAME = ?\n" +
                "  AND ROUTINE_TYPE IN ('INSTANCE METHOD', 'STATIC METHOD', 'CONSTRUCTOR METHOD')\n" +
                "ORDER BY ROUTINE_NAME");
            dbStat.setString(1, type.getSchema().getName());
            dbStat.setString(2, type.getName());
            return dbStat;
        }

        @Override
        protected MimerUdtMethod fetchObject(@NotNull JDBCSession session, @NotNull MimerUserDefinedType type, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String methodName = JDBCUtils.safeGetString(resultSet, "ROUTINE_NAME");
            String specificName = JDBCUtils.safeGetString(resultSet, "SPECIFIC_NAME");
            String methodKind = JDBCUtils.safeGetStringTrimmed(resultSet, "ROUTINE_TYPE");
            return new MimerUdtMethod(type, methodName, specificName, methodKind);
        }
    }

    /**
     * Loads every declared-but-not-yet-defined method specification - a spec "graduates" out of
     * this list (and into {@link #methodCache} instead) the moment {@code CREATE SPECIFIC METHOD}
     * gives it a matching {@code ROUTINES} row, which is exactly what the {@code NOT EXISTS}
     * clause below checks.
     */
    static class MethodSpecCache extends JDBCObjectCache<MimerUserDefinedType, MimerUdtMethodSpec> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUserDefinedType type) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT MS.SPECIFIC_SCHEMA, MS.SPECIFIC_NAME, MS.METHOD_NAME, MS.IS_STATIC, MS.IS_CONSTRUCTOR,\n" +
                "       MS.DATA_TYPE, MS.RETURN_UDT_SCHEMA, MS.RETURN_UDT_NAME,\n" +
                "       MS.CHARACTER_MAXIMUM_LENGTH, MS.NUMERIC_PRECISION, MS.NUMERIC_SCALE,\n" +
                "       MS.IS_DETERMINISTIC, MS.SQL_DATA_ACCESS\n" +
                "FROM INFORMATION_SCHEMA.METHOD_SPECIFICATIONS MS\n" +
                "WHERE MS.UDT_SCHEMA = ? AND MS.UDT_NAME = ?\n" +
                "  AND NOT EXISTS (\n" +
                "    SELECT 1 FROM INFORMATION_SCHEMA.ROUTINES R\n" +
                "    WHERE R.UDT_SCHEMA = MS.UDT_SCHEMA AND R.ROUTINE_SCHEMA = MS.UDT_SCHEMA\n" +
                "      AND R.UDT_NAME = MS.UDT_NAME AND R.SPECIFIC_SCHEMA = MS.SPECIFIC_SCHEMA\n" +
                "      AND R.SPECIFIC_NAME = MS.SPECIFIC_NAME AND R.ROUTINE_NAME = MS.METHOD_NAME)\n" +
                "ORDER BY MS.METHOD_NAME");
            dbStat.setString(1, type.getSchema().getName());
            dbStat.setString(2, type.getName());
            return dbStat;
        }

        @Override
        protected MimerUdtMethodSpec fetchObject(@NotNull JDBCSession session, @NotNull MimerUserDefinedType type, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerUdtMethodSpec(type, resultSet);
        }
    }

    /**
     * {@code GRANTOR = '_SYSTEM'} rows (Mimer SQL's synthetic "the owner implicitly holds USAGE"
     * grant) are excluded - {@code EXT_OBJECT_PRIVILEGES} carries these for a databank's owner
     * too (see {@code MimerDatabankPrivilege.PrivilegeCache}); same convention applied here on
     * the assumption it's a general Mimer SQL behaviour, not databank-specific.
     */
    static class PrivilegeCache extends JDBCObjectCache<MimerUserDefinedType, MimerUserDefinedTypePrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerUserDefinedType type) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE OBJECT_TYPE = 'USER DEFINED TYPE' AND OBJECT_SCHEMA = ? AND OBJECT_NAME = ? AND PRIVILEGE_TYPE = 'USAGE'\n" +
                "  AND GRANTOR <> '_SYSTEM'\n" +
                "ORDER BY GRANTEE");
            stmt.setString(1, type.getSchema().getName());
            stmt.setString(2, type.getName());
            return stmt;
        }

        @Override
        protected MimerUserDefinedTypePrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerUserDefinedType type, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerUserDefinedTypePrivilege(type, resultSet);
        }
    }
}
