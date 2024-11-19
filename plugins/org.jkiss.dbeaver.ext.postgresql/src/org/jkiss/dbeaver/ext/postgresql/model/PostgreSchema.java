/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.dpi.DPIObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.SubTaskProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PostgreSchema
 */
@DPIObject
@DPIElement
public class PostgreSchema implements
    DBSSchema,
    PostgreTableContainer,
    DBPNamedObject2,
    DBPSaveableObject,
    DBPRefreshableObject,
    DBPSystemObject,
    DBSProcedureContainer,
    DBPObjectStatisticsCollector,
    PostgreObject,
    PostgreScriptObject,
    PostgrePrivilegeOwner,
    DBPScriptObjectExt2,
    DBSNamespaceContainer,
    DBSVisibilityScopeProvider
{

    private static final Log log = Log.getLog(PostgreSchema.class);

    private final PostgreDatabase database;
    protected long oid;
    protected String name;
    protected String description;
    protected long ownerId;
    private Object schemaAcl;
    protected boolean persisted;

    private final ExtensionCache extensionCache;
    private final AggregateCache aggregateCache;
    private final TableCache tableCache;
    private final ConstraintCache constraintCache;
    private final ProceduresCache proceduresCache;
    private final IndexCache indexCache;
    private final PostgreDataTypeCache dataTypeCache;
    private ArrayList<PostgrePrivilege> defaultPrivileges;
    protected volatile boolean hasStatistics;

    PostgreSchema(PostgreDatabase database, String name) {
        this.database = database;
        this.name = name;

        extensionCache = new ExtensionCache();
        aggregateCache = new AggregateCache();
        tableCache = createTableCache();
        constraintCache = createConstraintCache();
        indexCache = database.getDataSource().getServerType().supportsIndexes() ? new IndexCache() : null;
        proceduresCache = createProceduresCache();
        dataTypeCache = new PostgreDataTypeCache();
    }

    @Override
    public List<DBSObjectContainer> getPublicScopes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return List.of(
            this.database.getSchema(monitor, PostgreConstants.PUBLIC_SCHEMA_NAME),
            this.database.getSchema(monitor, PostgreConstants.CATALOG_SCHEMA_NAME)
        );
    }

    @NotNull
    protected TableCache createTableCache() {
        return new TableCache();
    }

    @NotNull
    protected ConstraintCache createConstraintCache() {
        return new ConstraintCache();
    }

    @NotNull
    protected ProceduresCache createProceduresCache() {
        return new ProceduresCache();
    }

    public PostgreSchema(PostgreDatabase database, String name, ResultSet dbResult)
        throws SQLException {
        this(database, name);

        this.loadInfo(dbResult);
    }

    public PostgreSchema(PostgreDatabase database, String name, PostgreRole owner) {
        this(database, name);
        this.ownerId = owner == null ? 0 : owner.getObjectId();
    }

    protected void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "nspowner");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.schemaAcl = JDBCUtils.safeGetObject(dbResult, "nspacl");
        this.persisted = true;
    }

    @NotNull
    //@Property(viewable = false, order = 2)
    public PostgreDatabase getDatabase() {
        return database;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Property(viewable = false, order = 2)
    @Override
    public long getObjectId() {
        return this.oid;
    }

    @Property(order = 4)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return database.getDataSource().getServerType().supportsRoles() ? database.getRoleById(monitor, ownerId) : null;
    }

    void addDefaultPrivileges(List<PostgrePrivilege> resultPrivileges) {
        if (defaultPrivileges == null) {
            defaultPrivileges = new ArrayList<>();
        }
        defaultPrivileges.addAll(resultPrivileges);
    }

    @Override
    public Collection<PostgrePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        List<PostgrePrivilege> postgrePrivileges = new ArrayList<>(
            PostgreUtils.extractPermissionsFromACL(monitor, this, schemaAcl, false));
        if (defaultPrivileges == null) {
            defaultPrivileges = new ArrayList<>();
            if (getDataSource().getServerType().supportsDefaultPrivileges()) {
                readDefaultPrivileges(monitor);
            }
        }
        postgrePrivileges.addAll(defaultPrivileges);
        return postgrePrivileges;
    }

    @Override
    public String generateChangeOwnerQuery(@NotNull String owner, @NotNull Map<String, Object> options) {
        return null;
    }

    public void setOwner(PostgreRole role) {
        this.ownerId = role == null ? 0 : role.getObjectId();
    }

    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public PostgreDatabase getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    @Override
    public PostgreSchema getSchema() {
        return this;
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
    public List<PostgreExtension> getExtensions(DBRProgressMonitor monitor)
        throws DBException {
        return extensionCache.getAllObjects(monitor, this);
    }

    @Association
    public List<PostgreAggregate> getAggregateFunctions(DBRProgressMonitor monitor)
        throws DBException {
        return aggregateCache.getAllObjects(monitor, this);
    }

    @Association
    public List<PostgreIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getIndexes(monitor, null);
    }

    public List<PostgreIndex> getIndexes(@NotNull DBRProgressMonitor monitor, @Nullable PostgreTableBase parent) throws DBException {
        if (indexCache == null) {
            return List.of();
        }
        return indexCache.getObjects(monitor, this, parent);
    }

    @Nullable
    public PostgreIndex getIndex(@NotNull DBRProgressMonitor monitor, long indexId) throws DBException {
        if (indexCache == null) {
            return null;
        }
        for (PostgreIndex index : indexCache.getAllObjects(monitor, this)) {
            if (index.getObjectId() == indexId) {
                return index;
            }
        }
        return null;
    }

    public PostgreTableBase getTable(DBRProgressMonitor monitor, long tableId)
        throws DBException {
        for (PostgreClass table : getTableCache().getAllObjects(monitor, this)) {
            if (table.getObjectId() == tableId) {
                return (PostgreTableBase) table;
            }
        }

        return null;
    }

    public TableCache getTableCache() {
        return this.tableCache;
    }

    public ConstraintCache getConstraintCache() {
        return this.constraintCache;
    }

    public ProceduresCache getProceduresCache() {
        return this.proceduresCache;
    }

    @Nullable
    public IndexCache getIndexCache() {
        return indexCache;
    }

    public PostgreDataTypeCache getDataTypeCache() {
        return dataTypeCache;
    }

    @Association
    public List<? extends PostgreTable> getTables(DBRProgressMonitor monitor)
        throws DBException {
        final ArrayList<? extends PostgreTable> tables = getTableCache().getTypedObjects(monitor, this, PostgreTable.class)
            .stream()
            .filter(table -> !table.isPartition() && !(table instanceof PostgreTableForeign))
            .collect(Collectors.toCollection(ArrayList::new));
        if (getDataSource().supportsReadingKeysWithColumns()) {
            // Read constraints with columns
            constraintCache.getAllObjects(monitor, this);
        }
        return tables;
    }

    @Association
    public List<? extends PostgreTable> getForeignTables(DBRProgressMonitor monitor) throws DBException {
        final ArrayList<? extends PostgreTable> tables = getTableCache().getTypedObjects(monitor, this, PostgreTableForeign.class)
            .stream()
            .filter(table -> !table.isPartition())
            .collect(Collectors.toCollection(ArrayList::new));
        if (getDataSource().supportsReadingKeysWithColumns()) {
            // Read constraints with columns
            constraintCache.getAllObjects(monitor, this);
        }
        return tables;
    }

    @Association
    public List<PostgreView> getViews(DBRProgressMonitor monitor)
        throws DBException {
        return getTableCache().getTypedObjects(monitor, this, PostgreView.class);
    }

    @Association
    public List<PostgreMaterializedView> getMaterializedViews(DBRProgressMonitor monitor)
        throws DBException {
        return getTableCache().getTypedObjects(monitor, this, PostgreMaterializedView.class);
    }

    @Association
    public PostgreMaterializedView getMaterializedView(DBRProgressMonitor monitor, String name)
            throws DBException {
        return getTableCache().getObject(monitor, this, name, PostgreMaterializedView.class);
    }

    @Association
    public List<PostgreSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException {
        return getTableCache().getTypedObjects(monitor, this, PostgreSequence.class);
    }

    @Association
    public PostgreSequence getSequence(DBRProgressMonitor monitor, String name)
        throws DBException {
        return getTableCache().getObject(monitor, this, name, PostgreSequence.class);
    }

    @Association
    public List<PostgreProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException {
        return getProceduresCache().getAllObjects(monitor, this);
    }

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException {
        return getProceduresCache().getObject(monitor, this, procName);
    }

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, long oid)
        throws DBException {
        for (PostgreProcedure proc : getProceduresCache().getAllObjects(monitor, this)) {
            if (proc.getObjectId() == oid) {
                return proc;
            }
        }
        return null;
    }

    @Override
    public List<? extends JDBCTable> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return tableCache.getTypedObjects(monitor, this, PostgreTableReal.class);
    }

    @Override
    public JDBCTable getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return getTableCache().getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return PostgreTableRegular.class;
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException {
        monitor.subTask("Cache tables");
        getTableCache().getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            getTableCache().loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache constraints");
            constraintCache.getAllObjects(monitor, this);
            monitor.subTask("Cache indexes");
            if (indexCache != null) {
                indexCache.getAllObjects(monitor, this);
            }
            if (getDataSource().getServerType().supportsInheritance()) {
                monitor.subTask("Cache inheritance");
                try {
                    cacheTableInheritance(monitor);
                } catch (DBException e) {
                    log.error(e);
                }
            }

        }
    }

    private void cacheTableInheritance(DBRProgressMonitor monitor) throws DBException {
        for (PostgreTable table : this.getTables(monitor)) {
            table.resetSuperInheritance();
        }
        resetPartitionsInheritance(monitor);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table inheritance info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT i.inhrelid relid, pc.relnamespace parent_ns, pc.oid parent_oid, i.inhseqno\n" +
                    "FROM pg_catalog.pg_inherits i, pg_class rc, pg_class pc\n" +
                    "WHERE rc.oid=i.inhrelid AND rc.relnamespace=? AND pc.oid=i.inhparent")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        final long tableId = JDBCUtils.safeGetLong(dbResult, "relid");
                        final long parentSchemaId = JDBCUtils.safeGetLong(dbResult, "parent_ns");
                        final long parentTableId = JDBCUtils.safeGetLong(dbResult, "parent_oid");
                        PostgreSchema parentSchema = getDatabase().getSchema(monitor, parentSchemaId);
                        if (parentSchema == null) {
                            log.warn("Can't find parent table's schema '" + parentSchemaId + "'");
                            continue;
                        }
                        PostgreTableBase parentTable = parentSchema.getTable(monitor, parentTableId);
                        if (parentTable == null) {
                            log.warn("Can't find parent table '" + parentTableId + "' in '" + parentSchema.getName() + "'");
                            continue;
                        }
                        PostgreTableBase curTable = getTable(monitor, tableId);
                        if (curTable instanceof PostgreTable) {
                            int seqNum = JDBCUtils.safeGetInt(dbResult, "inhseqno");
                            ((PostgreTable) curTable).addSuperTableInheritance(parentTable, seqNum);
                        }
                    }
                }
                // No nullify all other tables inheritance
                for (PostgreTableBase table : getTables(monitor)) {
                    if (table instanceof PostgreTable) {
                        ((PostgreTable) table).nullifyEmptySuperTableInheritance();
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    private void resetPartitionsInheritance(DBRProgressMonitor monitor) throws DBException {
        for (PostgreTable table : getTableCache().getTypedObjects(monitor, this, PostgreTable.class)) {
            if (table.isPartition()) {
                table.resetSuperInheritance();
            }
        }
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        extensionCache.clearCache();
        tableCache.clearCache();
        constraintCache.clearCache();
        proceduresCache.clearCache();
        if (indexCache != null) {
            indexCache.clearCache();
        }
        defaultPrivileges = null;
        hasStatistics = false;

        PostgreSchema schema = database.schemaCache.refreshObject(monitor, database, this);
        database.cacheDataTypes(monitor, true);
        return schema;
    }

    @DPIElement(cache = true)
    @Override
    public boolean isSystem() {
        return
            isCatalogSchema() ||
                PostgreConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(name) ||
                name.startsWith(PostgreConstants.SYSTEM_SCHEMA_PREFIX);
    }

    @DPIElement(cache = true)
    public boolean isUtility() {
        return isUtilitySchema(name);
    }

    @DPIElement(cache = true)
    public boolean isExternal() {
        return false;
    }

    public static boolean isUtilitySchema(String schema) {
        return schema.startsWith(PostgreConstants.TOAST_SCHEMA_PREFIX) ||
            schema.startsWith(PostgreConstants.TEMP_SCHEMA_PREFIX);
    }

    //@Property
    @Association
    public List<PostgreDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        return dataTypeCache.getAllObjects(monitor, this).stream()
            .sorted(Comparator
                .comparing((DBSTypedObject type) -> type.getTypeName().startsWith("_")) // Sort the array data types at the end of the list
                .thenComparing(DBSTypedObject::getTypeName))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isPublicSchema() {
        return PostgreConstants.PUBLIC_SCHEMA_NAME.equals(name);
    }

    public boolean isCatalogSchema() {
        return PostgreConstants.CATALOG_SCHEMA_NAME.equals(name);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("-- DROP SCHEMA ").append(DBUtils.getQuotedIdentifier(this)).append(";\n\n");
        sql.append("CREATE SCHEMA ").append(DBUtils.getQuotedIdentifier(this));
        PostgreRole owner = getOwner(monitor);
        if (owner != null) {
            sql.append(" AUTHORIZATION ").append(DBUtils.getQuotedIdentifier(owner));
        }
        sql.append(";\n");
        if (!CommonUtils.isEmpty(getDescription()) && CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
            sql.append("\nCOMMENT ON SCHEMA ").append(DBUtils.getQuotedIdentifier(this))
                .append(" IS ").append(SQLUtils.quoteString(this, getDescription()));
            sql.append(";\n");
        }

        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS)) {
            // Show DDL for all schema objects (do not include CREATE EXTENSION)
            monitor.beginTask("Cache schema", 1);
            cacheStructure(monitor, DBSObjectContainer.STRUCT_ALL);
            monitor.done();
/*
            Collection<PostgreExtension> extensions = getExtensions(monitor);
            for (PostgreExtension ext : extensions) {
                addDDLLine(sql, ext.getObjectDefinitionText(monitor, options));
            }
*/
            Collection<PostgreDataType> dataTypes = getDataTypes(monitor);
            monitor.beginTask("Load data types", dataTypes.size());
            boolean readAllTypes = getDatabase().getDataSource().supportReadingAllDataTypes();
            for (PostgreDataType dataType : dataTypes) {
                if (!readAllTypes && (dataType.hasAttributes() || dataType.isArray())) {
                    // Skipp table types and arrays
                    continue;
                }
                addDDLLine(sql, dataType.getObjectDefinitionText(monitor, options));
                if (monitor.isCanceled()) {
                    break;
                }
                monitor.worked(1);
            }
            monitor.done();

            if (!monitor.isCanceled()) {
                Collection<PostgreTableBase> tablesOrViews = getTableCache().getAllObjects(monitor, this);

                List<PostgreTableBase> allTables = new ArrayList<>();
                for (PostgreTableBase tableOrView : tablesOrViews) {
                    monitor.subTask(tableOrView.getName());
                    if (tableOrView instanceof PostgreSequence) {
                        addDDLLine(sql, tableOrView.getObjectDefinitionText(monitor, options));
                    } else {
                        allTables.add(tableOrView);
                    }
                }
                DBStructUtils.generateTableListDDL(new SubTaskProgressMonitor(monitor), sql, allTables, new HashMap<>(options), false);
                monitor.done();
            }
            if (!monitor.isCanceled()) {
                Collection<PostgreProcedure> procedures = getProcedures(monitor);
                monitor.beginTask("Load procedures", procedures.size());
                for (PostgreProcedure procedure : procedures) {
                    monitor.subTask(procedure.getName());
                    addDDLLine(sql, procedure.getObjectDefinitionText(monitor, options));
                    monitor.worked(1);
                    if (monitor.isCanceled()) {
                        break;
                    }
                }
                monitor.done();
            }
        }

        List<DBEPersistAction> actions = new ArrayList<>();
        PostgreUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
        if (!actions.isEmpty()) {
            sql.append("\n\n");
            sql.append(SQLUtils.generateScript(getDataSource(), actions.toArray(new DBEPersistAction[0]), false));
        }


        return sql.toString();
    }

    private void addDDLLine(StringBuilder sql, String ddl) {
        if (!CommonUtils.isEmpty(ddl)) {
            sql.append("\n").append(ddl);
        }
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Schema DDL is read-only");
    }

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics || !getDataSource().getServerType().supportsTableStatistics();
    }

    void resetStatistics() {
        this.hasStatistics = false;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (!getDataSource().getServerType().supportsTableStatistics() || hasStatistics && !forceRefresh) {
            return;
        }
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read relation statistics")) {
            try (JDBCPreparedStatement dbStat = ((JDBCSession)session).prepareStatement(
                "select c.oid," +
                    "pg_catalog.pg_total_relation_size(c.oid) as total_rel_size," +
                    "pg_catalog.pg_relation_size(c.oid) as rel_size\n" +
                    "FROM pg_class c\n" +
                    "WHERE c.relnamespace=?"))
            {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        long tableId = dbResult.getLong(1);
                        PostgreTableBase table = getTable(monitor, tableId);
                        if (table instanceof PostgreTableReal) {
                            ((PostgreTableReal) table).fetchStatistics(dbResult);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException("Error reading schema relation statistics", e);
            }
        } finally {
            hasStatistics = true;
        }
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return DBPScriptObject.OPTION_INCLUDE_PERMISSIONS.equals(option) || DBPScriptObject.OPTION_INCLUDE_COMMENTS.equals(option)
               || DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS.equals(option);
    }

    public void readSchemaInfo(DBRProgressMonitor monitor) {
        try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Read schema id")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT s.oid as schema_id\n" +
                            "from pg_catalog.pg_namespace s\n" +
                            "WHERE s.nspname =?"))
            {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        oid = dbResult.getLong(1);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error reading schema information ", e);
        }
    }

    @Nullable
    @Override
    public DBSNamespace getNamespaceForObjectType(@NotNull DBSObjectType objectType) {
        if (PostgreNamespace.supportsObjectType(objectType)) {
            return new PostgreNamespace(this);
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public DBSNamespace[] getAllNamespaces() {
        return new DBSNamespace[] { new PostgreNamespace(this) };
    }

    private void readDefaultPrivileges(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read default schema privileges")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM pg_default_acl WHERE defaclnamespace = ?")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.nextRow()) {
                        Object acl = JDBCUtils.safeGetObject(dbResult, "defaclacl");
                        if (acl == null) {
                            log.debug("Can't read schema default permissions for " + getName());
                            continue;
                        }
                        String objectType = JDBCUtils.safeGetString(dbResult, "defaclobjtype");
                        if (CommonUtils.isEmpty(objectType)) {
                            log.debug("Can't read default permissions object type for " + getName());
                            continue;
                        }
                        List<PostgrePrivilege> privileges =
                            PostgreUtils.extractPermissionsFromACL(session.getProgressMonitor(), this, acl, true);
                        for (PostgrePrivilege privilege : privileges) {
                            if (privilege instanceof PostgreDefaultPrivilege) {
                                PostgreDefaultPrivilege defaultPrivilege = (PostgreDefaultPrivilege) privilege;
                                defaultPrivilege.setUnderKind(objectType);
                                defaultPrivileges.add(defaultPrivilege);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("Can't read default privileges for schema " + getName());
            }
        }
    }

    class ExtensionCache extends JDBCObjectCache<PostgreSchema, PostgreExtension> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT \n" + 
                    " e.oid,\n" + 
                    " cfg.tbls,\n" +
                    " e.* \n" + 
                    "FROM \n" + 
                    " pg_catalog.pg_extension e \n" + 
                    " join pg_namespace n on n.oid =e.extnamespace\n" +
                    " left join  (\n" + 
                    "         select\n" + 
                    "            ARRAY_AGG(ns.nspname || '.' ||  cls.relname) tbls, oid_ext\n" + 
                    "          from\n" + 
                    "            (\n" + 
                    "            select\n" + 
                    "                unnest(e1.extconfig) oid , e1.oid oid_ext\n" + 
                    "            from\n" + 
                    "                pg_catalog.pg_extension e1 ) c \n" + 
                    "                join    pg_class cls on cls.oid = c.oid \n" + 
                    "                join pg_namespace ns on ns.oid = cls.relnamespace\n" + 
                    "            group by oid_ext        \n" + 
                    "         ) cfg on cfg.oid_ext = e.oid\n" + 
                    "\nWHERE e.extnamespace=?\n" +
                    "ORDER BY e.oid"  
            );
            dbStat.setLong(1, PostgreSchema.this.getObjectId());
            return dbStat;
        }

        @Override
        protected PostgreExtension fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            //return new PostgreExtension(owner, dbResult);
            return null;
        }
    }

    class AggregateCache extends JDBCObjectCache<PostgreSchema, PostgreAggregate> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p.oid AS proc_oid,p.proname AS proc_name,a.*\n" +
                    "FROM pg_catalog.pg_aggregate a,pg_catalog.pg_proc p\n" +
                    "WHERE p.oid=a.aggfnoid AND p.pronamespace=?\n" +
                    "ORDER BY p.proname"
            );
            dbStat.setLong(1, PostgreSchema.this.getObjectId());
            return dbStat;
        }

        @Override
        protected PostgreAggregate fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreAggregate(session.getProgressMonitor(), owner, dbResult);
        }
    }

    public class TableCache extends JDBCStructLookupCache<PostgreTableContainer, PostgreTableBase, PostgreTableColumn> {

        protected TableCache() {
            super("relname");
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreTableContainer container, @Nullable PostgreTableBase object, @Nullable String objectName) throws SQLException {
            StringBuilder sql = new StringBuilder();
            PostgreDataSource dataSource = getDataSource();
            sql.append("SELECT c.oid,c.*,d.description");
            if (dataSource.isServerVersionAtLeast(10, 0)) {
                sql.append(",pg_catalog.pg_get_expr(c.relpartbound, c.oid) as partition_expr,  pg_catalog.pg_get_partkeydef(c.oid) as partition_key ");
            }
            sql.append("\nFROM pg_catalog.pg_class c\n")
                .append("LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=c.oid AND d.objsubid=0 AND d.classoid='pg_class'::regclass\n");
            sql.append("WHERE c.relnamespace=? AND c.relkind not in ('i','I','c')")
                .append(object == null && objectName == null ? "" : " AND relname=?");
            final JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setLong(1, getObjectId());
            if (object != null || objectName != null)
                dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected PostgreTableBase fetchObject(@NotNull JDBCSession session, @NotNull PostgreTableContainer container, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final String kindString = getDataSource().getServerType().supportsPartitions()
                                      && CommonUtils.equalObjects(JDBCUtils.safeGetString(dbResult, "relkind"), PostgreClass.RelKind.r.getCode())
                                      && isPartitionTableRow(dbResult)
                                      ? PostgreClass.RelKind.R.getCode() : JDBCUtils.safeGetString(dbResult, "relkind");
            
            PostgreClass.RelKind kind = PostgreClass.RelKind.valueOf(kindString);
            return container.getDataSource().getServerType().createRelationOfClass(PostgreSchema.this, kind, dbResult);
        }

        protected boolean isPartitionTableRow(@NotNull JDBCResultSet dbResult) {
            return JDBCUtils.safeGetBoolean(dbResult, "relispartition");
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull PostgreTableContainer container, @Nullable PostgreTableBase forTable)
            throws SQLException {
            boolean supportsSequences = container.getDataSource().getServerType().supportsSequences();

            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.relname,a.*,pg_catalog.pg_get_expr(ad.adbin, ad.adrelid, true) as def_value,dsc.description" +
                    getTableColumnsQueryExtraParameters(container.getSchema(), forTable) +
                    (supportsSequences ? ",dep.objid" : "") +
                    "\nFROM pg_catalog.pg_attribute a" +
                    "\nINNER JOIN pg_catalog.pg_class c ON (a.attrelid=c.oid)" +
                    "\nLEFT OUTER JOIN pg_catalog.pg_attrdef ad ON (a.attrelid=ad.adrelid AND a.attnum = ad.adnum)" +
                    "\nLEFT OUTER JOIN pg_catalog.pg_description dsc ON (c.oid=dsc.objoid AND a.attnum = dsc.objsubid)" +
                    (supportsSequences ? "\nLEFT OUTER JOIN pg_depend dep on dep.refobjid = a.attrelid AND dep.deptype = 'i' " +
                        "and dep.refobjsubid = a.attnum and dep.classid = dep.refclassid" : "") +
                    "\nWHERE NOT a.attisdropped AND c.relkind not in ('i','I','c')" +
                    (forTable != null ? " AND c.oid=?" : " AND c.relnamespace=?") +
                    "\nORDER BY a.attnum");
            if (forTable != null) {
                dbStat.setLong(1, forTable.getObjectId());
            } else {
                dbStat.setLong(1, PostgreSchema.this.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected PostgreTableColumn fetchChild(@NotNull JDBCSession session, @NotNull PostgreTableContainer container, @NotNull PostgreTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            try {
                return table.createTableColumn(session.getProgressMonitor(), PostgreSchema.this, dbResult);
            } catch (DBException e) {
                log.warn("Error reading attribute info", e);
                return null;
            }
        }

    }

    protected String getTableColumnsQueryExtraParameters(PostgreTableContainer owner, PostgreTableBase forTable) {
        return "";
    }

    /**
     * Constraint cache implementation
     */
    public class ConstraintCache extends JDBCCompositeCache<PostgreTableContainer, PostgreTableBase, PostgreTableConstraintBase<?>, PostgreTableConstraintColumn> {
        protected ConstraintCache() {
            super(getTableCache(), PostgreTableBase.class, "tabrelname", "conname");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreTableContainer container, PostgreTableBase forParent) throws SQLException {
            StringBuilder sql = new StringBuilder(
                "SELECT c.oid,c.*,t.relname as tabrelname,rt.relnamespace as refnamespace,d.description" +
                    (!getDataSource().getServerType().supportsPGConstraintExpressionColumn() ? ", null as consrc_copy" :
                        ", case when c.contype='c' then \"substring\"(pg_get_constraintdef(c.oid), 7) else null end consrc_copy") +
                    "\nFROM pg_catalog.pg_constraint c" +
                    "\nINNER JOIN pg_catalog.pg_class t ON t.oid=c.conrelid" +
                    "\nLEFT OUTER JOIN pg_catalog.pg_class rt ON rt.oid=c.confrelid" +
                    "\nLEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=c.oid AND d.objsubid=0 AND d.classoid='pg_constraint'::regclass" +
                    "\nWHERE ");
            if (forParent == null) {
                sql.append("t.relnamespace=?");
            } else {
                sql.append("c.conrelid=?");
            }
            sql.append("\nORDER BY c.oid");
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forParent == null) {
                dbStat.setLong(1, container.getSchema().getObjectId());
            } else {
                dbStat.setLong(1, forParent.getObjectId());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected PostgreTableConstraintBase<?> fetchObject(JDBCSession session, PostgreTableContainer container, PostgreTableBase table, String childName, JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "conname");
            String type = JDBCUtils.safeGetString(resultSet, "contype");
            if (type == null) {
                log.warn("Null constraint type");
                return null;
            }
            DBSEntityConstraintType constraintType;
            switch (type) {
                case "c":
                    constraintType = DBSEntityConstraintType.CHECK;
                    break;
                case "f":
                    constraintType = DBSEntityConstraintType.FOREIGN_KEY;
                    break;
                case "p":
                    constraintType = DBSEntityConstraintType.PRIMARY_KEY;
                    break;
                case "u":
                    constraintType = DBSEntityConstraintType.UNIQUE_KEY;
                    break;
                case "t":
                    constraintType = PostgreConstants.CONSTRAINT_TRIGGER;
                    break;
                case "x":
                    constraintType = PostgreConstants.CONSTRAINT_EXCLUSIVE;
                    break;
                default:
                    log.warn("Unsupported PG constraint type: " + type);
                    return null;
            }
            try {
                if (constraintType == DBSEntityConstraintType.FOREIGN_KEY) {
                    return new PostgreTableForeignKey(table, name, resultSet);
                } else {
                    return new PostgreTableConstraint(table, name, constraintType, resultSet);
                }
            } catch (DBException e) {
                log.error(e);
                return null;
            }
        }

        @Nullable
        @Override
        protected PostgreTableConstraintColumn[] fetchObjectRow(JDBCSession session, PostgreTableBase table, PostgreTableConstraintBase<?> constraint, JDBCResultSet resultSet)
            throws SQLException, DBException {
            Number[] keyNumbers = PostgreUtils.safeGetNumberArray(resultSet, "conkey");
            if (keyNumbers == null) {
                return null;
            }
            final DBRProgressMonitor monitor = resultSet.getSession().getProgressMonitor();
            if (constraint instanceof PostgreTableForeignKey) {
                final PostgreTableForeignKey foreignKey = (PostgreTableForeignKey) constraint;
                final PostgreTableBase refTable = foreignKey.getAssociatedEntity();
                if (refTable == null) {
                    log.warn("Unresolved reference table of '" + foreignKey.getName() + "'");
                    return null;
                }
                Number[] keyRefNumbers = PostgreUtils.safeGetNumberArray(resultSet, "confkey");
                Collection<? extends PostgreTableColumn> attributes = table.getAttributes(monitor);
                Collection<? extends PostgreTableColumn> refAttributes = refTable.getAttributes(monitor);
                assert keyRefNumbers != null && attributes != null && refAttributes != null;
                int colCount = keyNumbers.length;
                int refColCount = keyRefNumbers.length;
                PostgreTableForeignKeyColumn[] fkCols = new PostgreTableForeignKeyColumn[colCount];
                for (int i = 0; i < colCount; i++) {
                    short colNumber = keyNumbers[i].shortValue(); // Column number - 1-based
                    if (i >= refColCount) {
                        log.debug("Number of foreign columns is less than constraint columns (" + refColCount + " < " + colCount + ") in " + constraint.getFullyQualifiedName(DBPEvaluationContext.DDL));
                        break;
                    }
                    final PostgreTableColumn attr = PostgreUtils.getAttributeByNum(attributes, colNumber);
                    final PostgreTableColumn refAttr = PostgreUtils.getAttributeByNum(refAttributes, keyRefNumbers[i].intValue());
                    if (attr == null) {
                        log.warn("Bad foreign key attribute index: " + colNumber);
                        continue;
                    }
                    if (refAttr == null) {
                        log.warn("Bad reference table '" + refTable + "' attribute index: " + colNumber);
                        continue;
                    }
                    PostgreTableForeignKeyColumn cCol = new PostgreTableForeignKeyColumn(foreignKey, attr, i, refAttr);
                    fkCols[i] = cCol;
                }
                return fkCols;

            } else {
                Collection<? extends PostgreTableColumn> attributes = table.getAttributes(monitor);
                assert attributes != null;
                int colCount = Array.getLength(keyNumbers);
                PostgreTableConstraintColumn[] cols = new PostgreTableConstraintColumn[colCount];
                for (int i = 0; i < colCount; i++) {
                    Number colNumber = (Number) Array.get(keyNumbers, i); // Column number - 1-based
                    final PostgreAttribute attr = PostgreUtils.getAttributeByNum(attributes, colNumber.intValue());
                    if (attr == null) {
                        log.warn("Bad constraint attribute index: " + colNumber);
                        continue;
                    }
                    PostgreTableConstraintColumn cCol = new PostgreTableConstraintColumn(constraint, attr, i);
                    cols[i] = cCol;
                }
                return cols;
            }
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, PostgreTableConstraintBase<?> object, List<PostgreTableConstraintColumn> children) {
            object.cacheAttributes(monitor, children, false);
        }

        @Override
        protected void cacheChildren2(DBRProgressMonitor monitor, PostgreTableConstraintBase<?> object, List<PostgreTableConstraintColumn> children) {
            object.cacheAttributes(monitor, children, true);
        }
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<PostgreTableContainer, PostgreTableBase, PostgreIndex, PostgreIndexColumn> {
        protected IndexCache() {
            super(getTableCache(), PostgreTableBase.class, "tabrelname", "relname");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreTableContainer container, PostgreTableBase forTable)
            throws SQLException {
            boolean supportsExprIndex = getDataSource().isServerVersionAtLeast(7, 4);
            StringBuilder sql = new StringBuilder();
            sql.append(
                "SELECT i.*,i.indkey as keys,c.relname,c.relnamespace,c.relam,c.reltablespace,tc.relname as tabrelname,dsc.description");
            if (supportsExprIndex) {
                sql.append(",pg_catalog.pg_get_expr(i.indpred, i.indrelid) as pred_expr");
                sql.append(",pg_catalog.pg_get_expr(i.indexprs, i.indrelid, true) as expr");
            }
            if (getDataSource().getServerType().supportsRelationSizeCalc()) {
                sql.append(",pg_catalog.pg_relation_size(i.indexrelid) as index_rel_size");
                sql.append(",pg_catalog.pg_stat_get_numscans(i.indexrelid) as index_num_scans");
            }
            sql.append(
                "\nFROM pg_catalog.pg_index i" +
                    "\nINNER JOIN pg_catalog.pg_class c ON c.oid=i.indexrelid" +
                    "\nINNER JOIN pg_catalog.pg_class tc ON tc.oid=i.indrelid" +
                    "\nLEFT OUTER JOIN pg_catalog.pg_description dsc ON i.indexrelid=dsc.objoid" +
                    "\nWHERE ");
            if (forTable != null) {
                sql.append(" i.indrelid=?");
            } else {
                sql.append(" c.relnamespace=?");
            }
            //sql.append(" AND NOT i.indisprimary");
            sql.append(" ORDER BY c.relname");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable != null) {
                dbStat.setLong(1, forTable.getObjectId());
            } else {
                dbStat.setLong(1, PostgreSchema.this.getObjectId());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected PostgreIndex fetchObject(JDBCSession session, PostgreTableContainer container, PostgreTableBase parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreIndex(
                session.getProgressMonitor(),
                parent,
                indexName,
                dbResult);
        }

        @Nullable
        @Override
        protected PostgreIndexColumn[] fetchObjectRow(
            JDBCSession session,
            PostgreTableBase parent, PostgreIndex object, JDBCResultSet dbResult)
            throws SQLException, DBException {
            long[] keyNumbers = PostgreUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "keys"));
            if (keyNumbers == null) {
                return null;
            }
            long[] indColClasses = PostgreUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "indclass"));
            int[] keyOptions = PostgreUtils.getIntVector(JDBCUtils.safeGetObject(dbResult, "indoption"));
            String expr = JDBCUtils.safeGetString(dbResult, "expr");
            Collection<? extends PostgreTableColumn> attributes = parent.getAttributes(dbResult.getSession().getProgressMonitor());
            assert attributes != null;
            PostgreAccessMethod accessMethod = object.getAccessMethod(session.getProgressMonitor());

            PostgreIndexColumn[] result = new PostgreIndexColumn[keyNumbers.length];
            for (int i = 0; i < keyNumbers.length; i++) {
                long colNumber = keyNumbers[i];
                String attrExpression = null;
                final PostgreAttribute attr = PostgreUtils.getAttributeByNum(attributes, (int) colNumber);
                if (attr == null) {
                    if (colNumber == 0 && expr != null) {
                        // It's ok, function index or something
                        attrExpression = JDBCUtils.queryString(session, "select pg_catalog.pg_get_indexdef(?, ?, true)", object.getObjectId(), i + 1);
                    } else {
                        log.warn("Bad index attribute index: " + colNumber);
                    }
                }
                int options = keyOptions == null || keyOptions.length < keyNumbers.length ? 0 : keyOptions[i];
                long colOpClass = indColClasses == null || indColClasses.length < keyNumbers.length ? 0 : indColClasses[i];

                // https://stackoverflow.com/questions/18121103/how-to-get-the-index-column-orderasc-desc-nulls-first-from-postgresql
                // We can't rely on pg_am flags anymore because they awere removed in 9.6+
                boolean isAscending =  (options & 1) == 0;

                PostgreIndexColumn col = new PostgreIndexColumn(
                    object,
                    attr,
                    attrExpression,
                    i,
                    isAscending,
                    colOpClass,
                    false);
                result[i] = col;
            }
            return result;
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, PostgreIndex index, List<PostgreIndexColumn> rows) {
            index.setColumns(rows);
        }
    }

    /**
     * Procedures cache implementation
     */
    public static class ProceduresCache extends JDBCObjectLookupCache<PostgreSchema, PostgreProcedure> {

        public ProceduresCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @Nullable PostgreProcedure object, @Nullable String objectName) throws SQLException {
            PostgreServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
            boolean versionAtLeast7 = session.getDataSource().isServerVersionAtLeast(7, 2);
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p." + oidColumn + " as poid,p.*," +
                    (session.getDataSource().isServerVersionAtLeast(8, 4) ? "pg_catalog.pg_get_expr(p.proargdefaults, 0)" : "NULL") + " as arg_defaults,d.description\n" +
                    "FROM pg_catalog." + serverType.getProceduresSystemTable() + " p\n" +
                    "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=p." + oidColumn +
                    (versionAtLeast7 ? " and d.classoid='pg_proc'::regclass " : "") + // to avoid objects duplication
                    (versionAtLeast7 ? " AND d.objsubid = 0" : "") + // no links to columns
                    "\nWHERE p.pronamespace=?" +
                    (object == null ? "" : " AND p." + oidColumn + "=?") +
                    "\nORDER BY p.proname"
            );
            dbStat.setLong(1, owner.getObjectId());
            if (object != null) {
                dbStat.setLong(2, object.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected PostgreProcedure fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreProcedure(session.getProgressMonitor(), owner, dbResult);
        }

    }

}
