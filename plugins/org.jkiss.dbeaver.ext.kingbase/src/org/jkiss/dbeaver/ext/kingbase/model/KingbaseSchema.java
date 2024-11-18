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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
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
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.SubTaskProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSNamespace;
import org.jkiss.dbeaver.model.struct.DBSNamespaceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseSchema
 */
@DPIObject
@DPIElement
public class KingbaseSchema implements
    DBSSchema,
    KingbaseTableContainer,
    DBPNamedObject2,
    DBPSaveableObject,
    DBPRefreshableObject,
    DBPSystemObject,
    DBSProcedureContainer,
    DBPObjectStatisticsCollector,
    KingbaseObject,
    KingbaseScriptObject,
    KingbasePrivilegeOwner,
    DBPScriptObjectExt2,
    DBSNamespaceContainer
{

    private static final Log log = Log.getLog(KingbaseSchema.class);

    private final KingbaseDatabase database;
    protected long oid;
    protected String name;
    protected String description;
    protected long ownerId;
    private Object schemaAcl;
    protected boolean persisted;

    private final TableCache tableCache;
    private final ConstraintCache constraintCache;
    private final ProceduresCache proceduresCache;
    private final IndexCache indexCache;
    private final KingbaseDataTypeCache dataTypeCache;
    private ArrayList<KingbasePrivilege> defaultPrivileges;
    protected volatile boolean hasStatistics;

    KingbaseSchema(KingbaseDatabase database, String name) {
        this.database = database;
        this.name = name;

        tableCache = createTableCache();
        constraintCache = createConstraintCache();
        indexCache = database.getDataSource().getServerType().supportsIndexes() ? new IndexCache() : null;
        proceduresCache = createProceduresCache();
        dataTypeCache = new KingbaseDataTypeCache();
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

    public KingbaseSchema(KingbaseDatabase database, String name, ResultSet dbResult)
        throws SQLException {
        this(database, name);

        this.loadInfo(dbResult);
    }

    public KingbaseSchema(KingbaseDatabase database, String name, KingbaseRole owner) {
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
    public KingbaseDatabase getDatabase() {
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
    public KingbaseRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return database.getDataSource().getServerType().supportsRoles() ? database.getRoleById(monitor, ownerId) : null;
    }

    void addDefaultPrivileges(List<KingbasePrivilege> resultPrivileges) {
        if (defaultPrivileges == null) {
            defaultPrivileges = new ArrayList<>();
        }
        defaultPrivileges.addAll(resultPrivileges);
    }

    @Override
    public Collection<KingbasePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        List<KingbasePrivilege> kingbasePrivileges = new ArrayList<>(
            KingbaseUtils.extractPermissionsFromACL(monitor, this, schemaAcl, false));
        if (defaultPrivileges == null) {
            defaultPrivileges = new ArrayList<>();
            if (getDataSource().getServerType().supportsDefaultPrivileges()) {
                readDefaultPrivileges(monitor);
            }
        }
        kingbasePrivileges.addAll(defaultPrivileges);
        return kingbasePrivileges;
    }

    @Override
    public String generateChangeOwnerQuery(@NotNull String owner, @NotNull Map<String, Object> options) {
        return null;
    }

    public void setOwner(KingbaseRole role) {
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
    public KingbaseDatabase getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public KingbaseDataSource getDataSource() {
        return database.getDataSource();
    }

    @Override
    public KingbaseSchema getSchema() {
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
    public List<KingbaseIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getIndexes(monitor, null);
    }

    public List<KingbaseIndex> getIndexes(@NotNull DBRProgressMonitor monitor, @Nullable KingbaseTableBase parent) throws DBException {
        if (indexCache == null) {
            return List.of();
        }
        return indexCache.getObjects(monitor, this, parent);
    }

    @Nullable
    public KingbaseIndex getIndex(@NotNull DBRProgressMonitor monitor, long indexId) throws DBException {
        if (indexCache == null) {
            return null;
        }
        for (KingbaseIndex index : indexCache.getAllObjects(monitor, this)) {
            if (index.getObjectId() == indexId) {
                return index;
            }
        }
        return null;
    }

    public KingbaseTableBase getTable(DBRProgressMonitor monitor, long tableId)
        throws DBException {
        for (KingbaseClass table : getTableCache().getAllObjects(monitor, this)) {
            if (table.getObjectId() == tableId) {
                return (KingbaseTableBase) table;
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

    public KingbaseDataTypeCache getDataTypeCache() {
        return dataTypeCache;
    }

    @Association
    public List<? extends KingbaseTable> getTables(DBRProgressMonitor monitor)
        throws DBException {
        final ArrayList<? extends KingbaseTable> tables = getTableCache().getTypedObjects(monitor, this, KingbaseTable.class)
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
    public List<KingbaseView> getViews(DBRProgressMonitor monitor)
        throws DBException {
        return getTableCache().getTypedObjects(monitor, this, KingbaseView.class);
    }

    @Association
    public List<KingbaseMaterializedView> getMaterializedViews(DBRProgressMonitor monitor)
        throws DBException {
        return getTableCache().getTypedObjects(monitor, this, KingbaseMaterializedView.class);
    }

    @Association
    public KingbaseMaterializedView getMaterializedView(DBRProgressMonitor monitor, String name)
            throws DBException {
        return getTableCache().getObject(monitor, this, name, KingbaseMaterializedView.class);
    }

    @Association
    public List<KingbaseSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException {
        return getTableCache().getTypedObjects(monitor, this, KingbaseSequence.class);
    }

    @Association
    public KingbaseSequence getSequence(DBRProgressMonitor monitor, String name)
        throws DBException {
        return getTableCache().getObject(monitor, this, name, KingbaseSequence.class);
    }

    @Association
    public List<KingbaseProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException {
        return getProceduresCache().getAllObjects(monitor, this);
    }

    public KingbaseProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException {
        return getProceduresCache().getObject(monitor, this, procName);
    }

    public KingbaseProcedure getProcedure(DBRProgressMonitor monitor, long oid)
        throws DBException {
        for (KingbaseProcedure proc : getProceduresCache().getAllObjects(monitor, this)) {
            if (proc.getObjectId() == oid) {
                return proc;
            }
        }
        return null;
    }

    @Override
    public List<? extends JDBCTable> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return tableCache.getTypedObjects(monitor, this, KingbaseTableReal.class);
    }

    @Override
    public JDBCTable getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return getTableCache().getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return KingbaseTableRegular.class;
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
        for (KingbaseTable table : this.getTables(monitor)) {
            table.resetSuperInheritance();
        }
        resetPartitionsInheritance(monitor);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table inheritance info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT i.inhrelid relid, pc.relnamespace parent_ns, pc.oid parent_oid, i.inhseqno\n" +
                    "FROM sys_catalog.sys_inherits i, sys_class rc, sys_class pc\n" +
                    "WHERE rc.oid=i.inhrelid AND rc.relnamespace=? AND pc.oid=i.inhparent")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        final long tableId = JDBCUtils.safeGetLong(dbResult, "relid");
                        final long parentSchemaId = JDBCUtils.safeGetLong(dbResult, "parent_ns");
                        final long parentTableId = JDBCUtils.safeGetLong(dbResult, "parent_oid");
                        KingbaseSchema parentSchema = getDatabase().getSchema(monitor, parentSchemaId);
                        if (parentSchema == null) {
                            log.warn("Can't find parent table's schema '" + parentSchemaId + "'");
                            continue;
                        }
                        KingbaseTableBase parentTable = parentSchema.getTable(monitor, parentTableId);
                        if (parentTable == null) {
                            log.warn("Can't find parent table '" + parentTableId + "' in '" + parentSchema.getName() + "'");
                            continue;
                        }
                        KingbaseTableBase curTable = getTable(monitor, tableId);
                        if (curTable instanceof KingbaseTable) {
                            int seqNum = JDBCUtils.safeGetInt(dbResult, "inhseqno");
                            ((KingbaseTable) curTable).addSuperTableInheritance(parentTable, seqNum);
                        }
                    }
                }
                for (KingbaseTableBase table : getTables(monitor)) {
                    if (table instanceof KingbaseTable) {
                        ((KingbaseTable) table).nullifyEmptySuperTableInheritance();
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    private void resetPartitionsInheritance(DBRProgressMonitor monitor) throws DBException {
        for (KingbaseTable table : getTableCache().getTypedObjects(monitor, this, KingbaseTable.class)) {
            if (table.isPartition()) {
                table.resetSuperInheritance();
            }
        }
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        tableCache.clearCache();
        constraintCache.clearCache();
        proceduresCache.clearCache();
        if (indexCache != null) {
            indexCache.clearCache();
        }
        defaultPrivileges = null;
        hasStatistics = false;

        KingbaseSchema schema = database.schemaCache.refreshObject(monitor, database, this);
        database.cacheDataTypes(monitor, true);
        return schema;
    }

    @DPIElement(cache = true)
    @Override
    public boolean isSystem() {
        return
            isCatalogSchema() || isSysCatalogSchema() ||
                KingbaseConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.ANON_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.DBMS_SQL_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.PERF_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.SRC_RESTRICT_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.SYS_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.SYSAUDIT_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.SYSMAC_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.WMSYS_NAME.equalsIgnoreCase(name) ||
                KingbaseConstants.XLOG_RECORD_READ_NAME.equalsIgnoreCase(name) ||
                name.startsWith(KingbaseConstants.SYSTEM_SCHEMA_PREFIX)||
                name.startsWith(KingbaseConstants.SYS_SYSTEM_SCHEMA_PREFIX);
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
        return schema.startsWith(KingbaseConstants.TOAST_SCHEMA_PREFIX) ||
        	schema.startsWith(KingbaseConstants.SYS_TOAST_SCHEMA_PREFIX) ||
        	schema.startsWith(KingbaseConstants.SYS_TEMP_SCHEMA_PREFIX) ||
            schema.startsWith(KingbaseConstants.TEMP_SCHEMA_PREFIX);
    }

    //@Property
    @Association
    public List<KingbaseDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
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
        return KingbaseConstants.PUBLIC_SCHEMA_NAME.equals(name);
    }

    public boolean isCatalogSchema() {
        return KingbaseConstants.CATALOG_SCHEMA_NAME.equals(name);
    }
    
    public boolean isSysCatalogSchema() {
        return KingbaseConstants.SYS_CATALOG_SCHEMA_NAME.equals(name);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("-- DROP SCHEMA ").append(DBUtils.getQuotedIdentifier(this)).append(";\n\n");
        sql.append("CREATE SCHEMA ").append(DBUtils.getQuotedIdentifier(this));
        KingbaseRole owner = getOwner(monitor);
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
            Collection<KingbaseDataType> dataTypes = getDataTypes(monitor);
            monitor.beginTask("Load data types", dataTypes.size());
            boolean readAllTypes = getDatabase().getDataSource().supportReadingAllDataTypes();
            for (KingbaseDataType dataType : dataTypes) {
                if (!readAllTypes && (dataType.hasAttributes() || dataType.isArray())) {
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
                Collection<KingbaseTableBase> tablesOrViews = getTableCache().getAllObjects(monitor, this);

                List<KingbaseTableBase> allTables = new ArrayList<>();
                for (KingbaseTableBase tableOrView : tablesOrViews) {
                    monitor.subTask(tableOrView.getName());
                    if (tableOrView instanceof KingbaseSequence) {
                        addDDLLine(sql, tableOrView.getObjectDefinitionText(monitor, options));
                    } else {
                        allTables.add(tableOrView);
                    }
                }
                DBStructUtils.generateTableListDDL(new SubTaskProgressMonitor(monitor), sql, allTables, new HashMap<>(options), false);
                monitor.done();
            }
            if (!monitor.isCanceled()) {
                Collection<KingbaseProcedure> procedures = getProcedures(monitor);
                monitor.beginTask("Load procedures", procedures.size());
                for (KingbaseProcedure procedure : procedures) {
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
        KingbaseUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
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
                    "sys_catalog.sys_total_relation_size(c.oid) as total_rel_size," +
                    "sys_catalog.sys_relation_size(c.oid) as rel_size\n" +
                    "FROM sys_class c\n" +
                    "WHERE c.relnamespace=?"))
            {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        long tableId = dbResult.getLong(1);
                        KingbaseTableBase table = getTable(monitor, tableId);
                        if (table instanceof KingbaseTableReal) {
                            ((KingbaseTableReal) table).fetchStatistics(dbResult);
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
                            "from sys_catalog.sys_namespace s\n" +
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
        if (KingbaseNamespace.supportsObjectType(objectType)) {
            return new KingbaseNamespace(this);
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public DBSNamespace[] getAllNamespaces() {
        return new DBSNamespace[] { new KingbaseNamespace(this) };
    }

    private void readDefaultPrivileges(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read default schema privileges")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM sys_default_acl WHERE defaclnamespace = ?")) {
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
                        List<KingbasePrivilege> privileges =
                            KingbaseUtils.extractPermissionsFromACL(session.getProgressMonitor(), this, acl, true);
                        for (KingbasePrivilege privilege : privileges) {
                            if (privilege instanceof KingbaseDefaultPrivilege) {
                                KingbaseDefaultPrivilege defaultPrivilege = (KingbaseDefaultPrivilege) privilege;
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

    public class TableCache extends JDBCStructLookupCache<KingbaseTableContainer, KingbaseTableBase, KingbaseTableColumn> {

        protected TableCache() {
            super("relname");
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull KingbaseTableContainer container, @Nullable KingbaseTableBase object, @Nullable String objectName) throws SQLException {
            StringBuilder sql = new StringBuilder();
            KingbaseDataSource dataSource = getDataSource();
            sql.append("SELECT c.oid,c.*,d.description");
            
            sql.append(",sys_catalog.sys_get_expr(c.relpartbound, c.oid) as partition_expr,  sys_catalog.sys_get_partkeydef(c.oid) as partition_key ");
            
            sql.append("\nFROM sys_catalog.sys_class c\n")
                .append("LEFT OUTER JOIN sys_catalog.sys_description d ON d.objoid=c.oid AND d.objsubid=0 AND d.classoid='sys_class'::regclass\n");
            sql.append("WHERE c.relnamespace=? AND c.relkind not in ('i','I','c')")
                .append(object == null && objectName == null ? "" : " AND relname=?");
            final JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setLong(1, getObjectId());
            if (object != null || objectName != null)
                dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected KingbaseTableBase fetchObject(@NotNull JDBCSession session, @NotNull KingbaseTableContainer container, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final String kindString = getDataSource().getServerType().supportsPartitions()
                                      && CommonUtils.equalObjects(JDBCUtils.safeGetString(dbResult, "relkind"), KingbaseClass.RelKind.r.getCode())
                                      && isPartitionTableRow(dbResult)
                                      ? KingbaseClass.RelKind.R.getCode() : JDBCUtils.safeGetString(dbResult, "relkind");
            
            KingbaseClass.RelKind kind = KingbaseClass.RelKind.valueOf(kindString);
            return container.getDataSource().getServerType().createRelationOfClass(KingbaseSchema.this, kind, dbResult);
        }

        protected boolean isPartitionTableRow(@NotNull JDBCResultSet dbResult) {
            return JDBCUtils.safeGetBoolean(dbResult, "relispartition");
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull KingbaseTableContainer container, @Nullable KingbaseTableBase forTable)
            throws SQLException {
            boolean supportsSequences = container.getDataSource().getServerType().supportsSequences();

            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.relname,a.*,sys_catalog.sys_get_expr(ad.adbin, ad.adrelid, true) as def_value,dsc.description" +
                    getTableColumnsQueryExtraParameters(container.getSchema(), forTable) +
                    (supportsSequences ? ",dep.objid" : "") +
                    "\nFROM sys_catalog.sys_attribute a" +
                    "\nINNER JOIN sys_catalog.sys_class c ON (a.attrelid=c.oid)" +
                    "\nLEFT OUTER JOIN sys_catalog.sys_attrdef ad ON (a.attrelid=ad.adrelid AND a.attnum = ad.adnum)" +
                    "\nLEFT OUTER JOIN sys_catalog.sys_description dsc ON (c.oid=dsc.objoid AND a.attnum = dsc.objsubid)" +
                    (supportsSequences ? "\nLEFT OUTER JOIN sys_depend dep on dep.refobjid = a.attrelid AND dep.deptype = 'i' " +
                        "and dep.refobjsubid = a.attnum and dep.classid = dep.refclassid" : "") +
                    "\nWHERE NOT a.attisdropped AND c.relkind not in ('i','I','c')" +
                    (forTable != null ? " AND c.oid=?" : " AND c.relnamespace=?") +
                    "\nORDER BY a.attnum");
            if (forTable != null) {
                dbStat.setLong(1, forTable.getObjectId());
            } else {
                dbStat.setLong(1, KingbaseSchema.this.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected KingbaseTableColumn fetchChild(@NotNull JDBCSession session, @NotNull KingbaseTableContainer container, @NotNull KingbaseTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            try {
                return table.createTableColumn(session.getProgressMonitor(), KingbaseSchema.this, dbResult);
            } catch (DBException e) {
                log.warn("Error reading attribute info", e);
                return null;
            }
        }

    }

    protected String getTableColumnsQueryExtraParameters(KingbaseTableContainer owner, KingbaseTableBase forTable) {
        return "";
    }

    
    public class ConstraintCache extends JDBCCompositeCache<KingbaseTableContainer, KingbaseTableBase, KingbaseTableConstraintBase<?>, KingbaseTableConstraintColumn> {
        protected ConstraintCache() {
            super(getTableCache(), KingbaseTableBase.class, "tabrelname", "conname");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, KingbaseTableContainer container, KingbaseTableBase forParent) throws SQLException {
            StringBuilder sql = new StringBuilder(
                "SELECT c.oid,c.*,t.relname as tabrelname,rt.relnamespace as refnamespace,d.description" +
                    (!getDataSource().getServerType().supportsKBConstraintExpressionColumn() ? ", null as consrc_copy" :
                        ", case when c.contype='c' then \"substring\"(sys_get_constraintdef(c.oid), 7) else null end consrc_copy") +
                    "\nFROM sys_catalog.sys_constraint c" +
                    "\nINNER JOIN sys_catalog.sys_class t ON t.oid=c.conrelid" +
                    "\nLEFT OUTER JOIN sys_catalog.sys_class rt ON rt.oid=c.confrelid" +
                    "\nLEFT OUTER JOIN sys_catalog.sys_description d ON d.objoid=c.oid AND d.objsubid=0 AND d.classoid='sys_constraint'::regclass" +
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
        protected KingbaseTableConstraintBase<?> fetchObject(JDBCSession session, KingbaseTableContainer container, KingbaseTableBase table, String childName, JDBCResultSet resultSet) throws SQLException, DBException {
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
                    constraintType = KingbaseConstants.CONSTRAINT_TRIGGER;
                    break;
                case "x":
                    constraintType = KingbaseConstants.CONSTRAINT_EXCLUSIVE;
                    break;
                default:
                    log.warn("Unsupported KB constraint type: " + type);
                    return null;
            }
            try {
                if (constraintType == DBSEntityConstraintType.FOREIGN_KEY) {
                    return new KingbaseTableForeignKey(table, name, resultSet);
                } else {
                    return new KingbaseTableConstraint(table, name, constraintType, resultSet);
                }
            } catch (DBException e) {
                log.error(e);
                return null;
            }
        }

        @Nullable
        @Override
        protected KingbaseTableConstraintColumn[] fetchObjectRow(JDBCSession session, KingbaseTableBase table, KingbaseTableConstraintBase<?> constraint, JDBCResultSet resultSet)
            throws SQLException, DBException {
            Number[] keyNumbers = KingbaseUtils.safeGetNumberArray(resultSet, "conkey");
            if (keyNumbers == null) {
                return null;
            }
            final DBRProgressMonitor monitor = resultSet.getSession().getProgressMonitor();
            if (constraint instanceof KingbaseTableForeignKey) {
                final KingbaseTableForeignKey foreignKey = (KingbaseTableForeignKey) constraint;
                final KingbaseTableBase refTable = foreignKey.getAssociatedEntity();
                if (refTable == null) {
                    log.warn("Unresolved reference table of '" + foreignKey.getName() + "'");
                    return null;
                }
                Number[] keyRefNumbers = KingbaseUtils.safeGetNumberArray(resultSet, "confkey");
                Collection<? extends KingbaseTableColumn> attributes = table.getAttributes(monitor);
                Collection<? extends KingbaseTableColumn> refAttributes = refTable.getAttributes(monitor);
                assert keyRefNumbers != null && attributes != null && refAttributes != null;
                int colCount = keyNumbers.length;
                int refColCount = keyRefNumbers.length;
                KingbaseTableForeignKeyColumn[] fkCols = new KingbaseTableForeignKeyColumn[colCount];
                for (int i = 0; i < colCount; i++) {
                    short colNumber = keyNumbers[i].shortValue(); // Column number - 1-based
                    if (i >= refColCount) {
                        log.debug("Number of foreign columns is less than constraint columns (" + refColCount + " < " + colCount + ") in " + constraint.getFullyQualifiedName(DBPEvaluationContext.DDL));
                        break;
                    }
                    final KingbaseTableColumn attr = KingbaseUtils.getAttributeByNum(attributes, colNumber);
                    final KingbaseTableColumn refAttr = KingbaseUtils.getAttributeByNum(refAttributes, keyRefNumbers[i].intValue());
                    if (attr == null) {
                        log.warn("Bad foreign key attribute index: " + colNumber);
                        continue;
                    }
                    if (refAttr == null) {
                        log.warn("Bad reference table '" + refTable + "' attribute index: " + colNumber);
                        continue;
                    }
                    KingbaseTableForeignKeyColumn cCol = new KingbaseTableForeignKeyColumn(foreignKey, attr, i, refAttr);
                    fkCols[i] = cCol;
                }
                return fkCols;

            } else {
                Collection<? extends KingbaseTableColumn> attributes = table.getAttributes(monitor);
                assert attributes != null;
                int colCount = Array.getLength(keyNumbers);
                KingbaseTableConstraintColumn[] cols = new KingbaseTableConstraintColumn[colCount];
                for (int i = 0; i < colCount; i++) {
                    Number colNumber = (Number) Array.get(keyNumbers, i); // Column number - 1-based
                    final KingbaseAttribute attr = KingbaseUtils.getAttributeByNum(attributes, colNumber.intValue());
                    if (attr == null) {
                        log.warn("Bad constraint attribute index: " + colNumber);
                        continue;
                    }
                    KingbaseTableConstraintColumn cCol = new KingbaseTableConstraintColumn(constraint, attr, i);
                    cols[i] = cCol;
                }
                return cols;
            }
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, KingbaseTableConstraintBase<?> object, List<KingbaseTableConstraintColumn> children) {
            object.cacheAttributes(monitor, children, false);
        }

        @Override
        protected void cacheChildren2(DBRProgressMonitor monitor, KingbaseTableConstraintBase<?> object, List<KingbaseTableConstraintColumn> children) {
            object.cacheAttributes(monitor, children, true);
        }
    }

   
    class IndexCache extends JDBCCompositeCache<KingbaseTableContainer, KingbaseTableBase, KingbaseIndex, KingbaseIndexColumn> {
        protected IndexCache() {
            super(getTableCache(), KingbaseTableBase.class, "tabrelname", "relname");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, KingbaseTableContainer container, KingbaseTableBase forTable)
            throws SQLException {
            boolean supportsExprIndex = true;
            StringBuilder sql = new StringBuilder();
            sql.append(
                "SELECT i.*,i.indkey as keys,c.relname,c.relnamespace,c.relam,c.reltablespace,tc.relname as tabrelname,dsc.description");
            if (supportsExprIndex) {
                sql.append(",sys_catalog.sys_get_expr(i.indpred, i.indrelid) as pred_expr");
                sql.append(",sys_catalog.sys_get_expr(i.indexprs, i.indrelid, true) as expr");
            }
            if (getDataSource().getServerType().supportsRelationSizeCalc()) {
                sql.append(",sys_catalog.sys_relation_size(i.indexrelid) as index_rel_size");
                sql.append(",sys_catalog.sys_stat_get_numscans(i.indexrelid) as index_num_scans");
            }
            sql.append(
                "\nFROM sys_catalog.sys_index i" +
                    "\nINNER JOIN sys_catalog.sys_class c ON c.oid=i.indexrelid" +
                    "\nINNER JOIN sys_catalog.sys_class tc ON tc.oid=i.indrelid" +
                    "\nLEFT OUTER JOIN sys_catalog.sys_description dsc ON i.indexrelid=dsc.objoid" +
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
                dbStat.setLong(1, KingbaseSchema.this.getObjectId());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected KingbaseIndex fetchObject(JDBCSession session, KingbaseTableContainer container, KingbaseTableBase parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new KingbaseIndex(
                session.getProgressMonitor(),
                parent,
                indexName,
                dbResult);
        }

        @Nullable
        @Override
        protected KingbaseIndexColumn[] fetchObjectRow(
            JDBCSession session,
            KingbaseTableBase parent, KingbaseIndex object, JDBCResultSet dbResult)
            throws SQLException, DBException {
            long[] keyNumbers = KingbaseUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "keys"));
            if (keyNumbers == null) {
                return null;
            }
            long[] indColClasses = KingbaseUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "indclass"));
            int[] keyOptions = KingbaseUtils.getIntVector(JDBCUtils.safeGetObject(dbResult, "indoption"));
            String expr = JDBCUtils.safeGetString(dbResult, "expr");
            Collection<? extends KingbaseTableColumn> attributes = parent.getAttributes(dbResult.getSession().getProgressMonitor());
            assert attributes != null;
            KingbaseAccessMethod accessMethod = object.getAccessMethod(session.getProgressMonitor());

            KingbaseIndexColumn[] result = new KingbaseIndexColumn[keyNumbers.length];
            for (int i = 0; i < keyNumbers.length; i++) {
                long colNumber = keyNumbers[i];
                String attrExpression = null;
                final KingbaseAttribute attr = KingbaseUtils.getAttributeByNum(attributes, (int) colNumber);
                if (attr == null) {
                    if (colNumber == 0 && expr != null) {
                        // It's ok, function index or something
                        attrExpression = JDBCUtils.queryString(session, "select sys_catalog.sys_get_indexdef(?, ?, true)", object.getObjectId(), i + 1);
                    } else {
                        log.warn("Bad index attribute index: " + colNumber);
                    }
                }
                int options = keyOptions == null || keyOptions.length < keyNumbers.length ? 0 : keyOptions[i];
                long colOpClass = indColClasses == null || indColClasses.length < keyNumbers.length ? 0 : indColClasses[i];

                boolean isAscending =  (options & 1) == 0;

                KingbaseIndexColumn col = new KingbaseIndexColumn(
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
        protected void cacheChildren(DBRProgressMonitor monitor, KingbaseIndex index, List<KingbaseIndexColumn> rows) {
            index.setColumns(rows);
        }
    }

    
    public static class ProceduresCache extends JDBCObjectLookupCache<KingbaseSchema, KingbaseProcedure> {

        public ProceduresCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull KingbaseSchema owner, @Nullable KingbaseProcedure object, @Nullable String objectName) throws SQLException {
            KingbaseServerExtension serverType = owner.getDataSource().getServerType();
            String oidColumn = serverType.getProceduresOidColumn(); // Hack for Redshift SP support
          
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p." + oidColumn + " as poid,p.*," +
                    "sys_catalog.sys_get_expr(p.proargdefaults, 0)"  + " as arg_defaults,d.description\n" +
                    "FROM sys_catalog." + serverType.getProceduresSystemTable() + " p\n" +
                    "LEFT OUTER JOIN sys_catalog.sys_description d ON d.objoid=p." + oidColumn +
                    " and d.classoid='sys_proc'::regclass " + 
                    " AND d.objsubid = 0" + 
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
        protected KingbaseProcedure fetchObject(@NotNull JDBCSession session, @NotNull KingbaseSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new KingbaseProcedure(session.getProgressMonitor(), owner, dbResult);
        }

    }

}
