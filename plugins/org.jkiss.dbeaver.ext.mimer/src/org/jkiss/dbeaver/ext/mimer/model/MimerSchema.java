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
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code GenericSchema}'s name is a {@code private final} field with no setter, and it doesn't
 * implement {@link DBPSaveableObject} - neither works for a class backing {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerSchemaManager}'s create flow, so both are shadowed here
 * (matching {@code DamengSchema}). Deliberately does not implement {@code DBPNamedObject2} -
 * that would expose the navigator's inline "Rename" on existing schemas too, which nothing
 * here backs with an ALTER/RENAME statement.
 *
 * @author Mimer Information Technology
 */
public class MimerSchema extends GenericSchema implements DBPSaveableObject {

    private final DomainCache domainCache = new DomainCache();
    private final ModuleCache moduleCache = new ModuleCache();
    private final UserDefinedTypeCache userDefinedTypeCache = new UserDefinedTypeCache();
    private final StatementCache statementCache = new StatementCache();
    private final SchemaIndexCache schemaIndexCache = new SchemaIndexCache();
    private final CollationCache collationCache = new CollationCache();
    private String name;
    private boolean persisted = true;
    private Map<String, IndexExtInfo> indexExtInfoByKey;
    private String comment;

    public MimerSchema(@NotNull GenericDataSource dataSource, @Nullable GenericCatalog catalog, @NotNull String schemaName) {
        super(dataSource, catalog, schemaName);
        this.name = schemaName;
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

    /**
     * One of Mimer SQL's own built-in schemas ({@link MimerConstants#SYSTEM_SCHEMAS} -
     * INFORMATION_SCHEMA / SYSTEM / MIMER / ODBC / BUILTIN). Can't be dropped - see {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerSchemaManager#canDeleteObject}.
     */
    public boolean isSystemSchema() {
        return name != null && MimerConstants.SYSTEM_SCHEMAS.contains(name.toUpperCase(Locale.ROOT));
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Association
    public Collection<MimerDomain> getDomains(DBRProgressMonitor monitor) throws DBException {
        return domainCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerSchema, MimerDomain> getDomainCache() {
        return domainCache;
    }

    @Association
    public Collection<MimerModule> getModules(DBRProgressMonitor monitor) throws DBException {
        return moduleCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerSchema, MimerModule> getModuleCache() {
        return moduleCache;
    }

    /**
     * All user-defined types (both categories) - see {@link #getDistinctTypes}/{@link
     * #getStructuredTypes} for the filtered views the "Distinct Types"/"Structured Types"
     * folders actually bind to.
     */
    @Association
    public Collection<MimerUserDefinedType> getUserDefinedTypes(DBRProgressMonitor monitor) throws DBException {
        return userDefinedTypeCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerSchema, MimerUserDefinedType> getUserDefinedTypeCache() {
        return userDefinedTypeCache;
    }

    @Association
    public List<MimerUserDefinedType> getDistinctTypes(DBRProgressMonitor monitor) throws DBException {
        return filterByCategory(monitor, "DISTINCT");
    }

    @Association
    public List<MimerUserDefinedType> getStructuredTypes(DBRProgressMonitor monitor) throws DBException {
        return filterByCategory(monitor, "STRUCTURED");
    }

    @Association
    public Collection<MimerStatement> getStatements(DBRProgressMonitor monitor) throws DBException {
        return statementCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerSchema, MimerStatement> getStatementCache() {
        return statementCache;
    }

    /**
     * Every index in the schema, across every table, in one flat list - see {@link
     * MimerSchemaIndex} for why this is a separate, lightweight class rather than resolving to
     * the richer per-table {@link MimerTableIndex} objects.
     */
    @Association
    public Collection<MimerSchemaIndex> getAllIndexes(DBRProgressMonitor monitor) throws DBException {
        return schemaIndexCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<MimerCollation> getCollations(DBRProgressMonitor monitor) throws DBException {
        return collationCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerSchema, MimerCollation> getCollationCache() {
        return collationCache;
    }

    @NotNull
    private List<MimerUserDefinedType> filterByCategory(@NotNull DBRProgressMonitor monitor, @NotNull String category) throws DBException {
        List<MimerUserDefinedType> result = new ArrayList<>();
        for (MimerUserDefinedType type : getUserDefinedTypes(monitor)) {
            if (category.equalsIgnoreCase(type.getCategory())) {
                result.add(type);
            }
        }
        return result;
    }

    /**
     * {@code COMMENT ON SCHEMA "name" IS '...'} - a datasource-global object ({@code
     * OBJECT_SCHEMA IS NULL}). See {@link org.jkiss.dbeaver.ext.mimer.edit.MimerSchemaManager}
     * for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 200)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && persisted) {
            comment = MimerUtils.readObjectComment(monitor, this, null, null, name, "SCHEMA");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        domainCache.clearCache();
        moduleCache.clearCache();
        userDefinedTypeCache.clearCache();
        statementCache.clearCache();
        schemaIndexCache.clearCache();
        collationCache.clearCache();
        indexExtInfoByKey = null;
        comment = null;
        return super.refreshObject(monitor);
    }

    /**
     * Whether {@code tableName.indexName} is clustered and ignores NULLs - both Mimer SQL
     * 11.1+-only, read from {@code EXT_INDEXES} (see {@link MimerTableIndex} for why the
     * driver's own {@code getIndexInfo()} can't be trusted for this). Loads every index in the
     * schema in one query on first use, since {@code createIndexImpl} has no monitor to query
     * with directly; cleared alongside the other caches on {@link #refreshObject}.
     * <p>
     * On a pre-11.1 server {@code EXT_INDEXES} has neither column, so the version check happens
     * here, at the single source of the query, rather than relying on every caller to check
     * {@link MimerDataSource#supportsClusteredIndexes()} first - matches the version-gated
     * column list used the same way in {@link MimerTable#loadAccessPaths} for the analogous
     * {@code EXT_ACCESS_PATHS} columns.
     */
    synchronized IndexExtInfo getIndexExtInfo(@NotNull DBRProgressMonitor monitor, @NotNull String tableName, @NotNull String indexName) throws DBException {
        if (!(getDataSource() instanceof MimerDataSource ds) || !ds.supportsClusteredIndexes()) {
            return IndexExtInfo.NONE;
        }
        if (indexExtInfoByKey == null) {
            Map<String, IndexExtInfo> map = new HashMap<>();
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Mimer SQL clustered index info")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT TABLE_NAME, INDEX_NAME, IS_CLUSTERED, IGNORE_NULLS\n" +
                    "FROM INFORMATION_SCHEMA.EXT_INDEXES\n" +
                    "WHERE INDEX_SCHEMA = ?")
                ) {
                    dbStat.setString(1, getName());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            String table = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
                            String index = JDBCUtils.safeGetString(dbResult, "INDEX_NAME");
                            if (table != null && index != null) {
                                boolean clustered = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_CLUSTERED"));
                                boolean ignoreNulls = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IGNORE_NULLS"));
                                map.put(indexKey(table, index), new IndexExtInfo(clustered, ignoreNulls));
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBException("Error loading Mimer SQL clustered index info", e);
            }
            indexExtInfoByKey = map;
        }
        return indexExtInfoByKey.getOrDefault(indexKey(tableName, indexName), IndexExtInfo.NONE);
    }

    @NotNull
    private static String indexKey(@NotNull String tableName, @NotNull String indexName) {
        return tableName.toUpperCase(Locale.ENGLISH) + "." + indexName.toUpperCase(Locale.ENGLISH);
    }

    record IndexExtInfo(boolean clustered, boolean ignoreNulls) {
        static final IndexExtInfo NONE = new IndexExtInfo(false, false);
    }

    static class DomainCache extends JDBCObjectCache<MimerSchema, MimerDomain> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSchema schema) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DOMAIN_SCHEMA, DOMAIN_NAME, DATA_TYPE,\n" +
                "       CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE, DOMAIN_DEFAULT\n" +
                "FROM INFORMATION_SCHEMA.DOMAINS\n" +
                "WHERE DOMAIN_SCHEMA = ?\n" +
                "ORDER BY DOMAIN_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected MimerDomain fetchObject(@NotNull JDBCSession session, @NotNull MimerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerDomain(schema, resultSet);
        }
    }

    static class ModuleCache extends JDBCObjectCache<MimerSchema, MimerModule> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSchema schema) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT MODULE_SCHEMA, MODULE_NAME\n" +
                "FROM INFORMATION_SCHEMA.MODULES\n" +
                "WHERE MODULE_SCHEMA = ?\n" +
                "ORDER BY MODULE_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected MimerModule fetchObject(@NotNull JDBCSession session, @NotNull MimerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerModule(schema, resultSet);
        }
    }

    static class UserDefinedTypeCache extends JDBCObjectCache<MimerSchema, MimerUserDefinedType> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSchema schema) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT USER_DEFINED_TYPE_NAME, USER_DEFINED_TYPE_CATEGORY, IS_INSTANTIABLE, IS_FINAL,\n" +
                "       DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE,\n" +
                "       COLLATION_SCHEMA, COLLATION_NAME\n" +
                "FROM INFORMATION_SCHEMA.USER_DEFINED_TYPES\n" +
                "WHERE USER_DEFINED_TYPE_SCHEMA = ?\n" +
                "ORDER BY USER_DEFINED_TYPE_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected MimerUserDefinedType fetchObject(@NotNull JDBCSession session, @NotNull MimerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerUserDefinedType(schema, resultSet);
        }
    }

    static class StatementCache extends JDBCObjectCache<MimerSchema, MimerStatement> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSchema schema) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT STATEMENT_SCHEMA, STATEMENT_NAME, IS_SCROLLABLE, IS_FORWARD_ONLY\n" +
                "FROM INFORMATION_SCHEMA.EXT_STATEMENTS\n" +
                "WHERE STATEMENT_SCHEMA = ?\n" +
                "ORDER BY STATEMENT_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected MimerStatement fetchObject(@NotNull JDBCSession session, @NotNull MimerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerStatement(schema, resultSet);
        }
    }

    /**
     * {@code TABLE_NAME LIKE '%'} - the whole schema, backing a schema-level "Indexes" folder
     * that reuses the identical query as the per-table one, just with a wildcard table pattern.
     */
    static class SchemaIndexCache extends JDBCObjectCache<MimerSchema, MimerSchemaIndex> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSchema schema) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT INDEX_NAME, TABLE_NAME, IS_UNIQUE\n" +
                "FROM INFORMATION_SCHEMA.EXT_INDEXES\n" +
                "WHERE INDEX_SCHEMA = ?\n" +
                "ORDER BY INDEX_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected MimerSchemaIndex fetchObject(@NotNull JDBCSession session, @NotNull MimerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSchemaIndex(schema, resultSet);
        }
    }

    static class CollationCache extends JDBCObjectCache<MimerSchema, MimerCollation> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSchema schema) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT COLLATION_NAME, CHARACTER_SET_SCHEMA, CHARACTER_SET_NAME, PAD_ATTRIBUTE\n" +
                "FROM INFORMATION_SCHEMA.COLLATIONS\n" +
                "WHERE COLLATION_SCHEMA = ?\n" +
                "ORDER BY COLLATION_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected MimerCollation fetchObject(@NotNull JDBCSession session, @NotNull MimerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerCollation(schema, resultSet);
        }
    }
}
