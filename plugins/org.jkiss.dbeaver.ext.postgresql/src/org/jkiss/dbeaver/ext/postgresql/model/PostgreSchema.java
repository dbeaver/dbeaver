/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * PostgreSchema
 */
public class PostgreSchema implements DBSSchema, DBPNamedObject2, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer, PostgreObject {

    private static final Log log = Log.getLog(PostgreSchema.class);

    private PostgreDatabase database;
    private long oid;
    private String name;
    private String description;
    private long ownerId;
    private boolean persisted;

    public final CollationCache collationCache = new CollationCache();
    public final ExtensionCache extensionCache = new ExtensionCache();
    public final TableCache tableCache = new TableCache();
    public final ConstraintCache constraintCache = new ConstraintCache();
    public final ProceduresCache proceduresCache = new ProceduresCache();
    public final IndexCache indexCache = new IndexCache();
    public final PostgreDataTypeCache dataTypeCache = new PostgreDataTypeCache();

    public PostgreSchema(PostgreDatabase database, String name, ResultSet dbResult)
        throws SQLException
    {
        this.database = database;
        this.name = name;

        this.loadInfo(dbResult);
    }

    public PostgreSchema(PostgreDatabase database, String name, PostgreRole owner)
    {
        this.database = database;
        this.name = name;
        this.ownerId = owner == null ? 0 : owner.getObjectId();
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "nspowner");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
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
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    public long getObjectId() {
        return this.oid;
    }

    @Property(order = 4)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, database.roleCache, database, ownerId);
    }

    @Property(viewable = true, order = 100)
    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public PostgreDatabase getParentObject()
    {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
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
    public Collection<PostgreCollation> getCollations(DBRProgressMonitor monitor)
        throws DBException
    {
        return collationCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreExtension> getExtensions(DBRProgressMonitor monitor)
        throws DBException
    {
        return extensionCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, this, null);
    }

    public PostgreTableBase getTable(DBRProgressMonitor monitor, long tableId)
        throws DBException
    {
        for (PostgreClass table : tableCache.getAllObjects(monitor, this)) {
            if (table.getObjectId() == tableId) {
                return (PostgreTableBase) table;
            }
        }

        return null;
    }

    @Association
    public Collection<PostgreTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, PostgreTable.class);
    }

    @Association
    public Collection<PostgreView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, PostgreView.class);
    }

    @Association
    public Collection<PostgreMaterializedView> getMaterializedViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, PostgreMaterializedView.class);
    }

    @Association
    public Collection<PostgreSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, PostgreSequence.class);
    }

    public PostgreSequence getSequence(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, PostgreSequence.class);
    }

    @Association
    public Collection<PostgreProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return proceduresCache.getObject(monitor, this, procName);
    }

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, long oid)
        throws DBException
    {
        for (PostgreProcedure proc : proceduresCache.getAllObjects(monitor, this)) {
            if (proc.getObjectId() == oid) {
                return proc;
            }
        }
        return null;
    }

    @Override
    public Collection<PostgreTableReal> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, PostgreTableReal.class);
    }

    @Override
    public PostgreTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return PostgreTableBase.class;
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        tableCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache constraints");
            constraintCache.getAllObjects(monitor, this);
            indexCache.getAllObjects(monitor, this);
        }
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return database.schemaCache.refreshObject(monitor, database, this);
    }

    @Override
    public boolean isSystem()
    {
        return
            PostgreConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(name) ||
            PostgreConstants.CATALOG_SCHEMA_NAME.equalsIgnoreCase(name) ||
            name.startsWith(PostgreConstants.TOAST_SCHEMA_PREFIX) ||
            name.startsWith(PostgreConstants.TEMP_SCHEMA_PREFIX);
    }

    public boolean isUtility()
    {
        return isUtilitySchema(name);
    }

    public static boolean isUtilitySchema(String schema)
    {
        return schema.startsWith(PostgreConstants.TOAST_SCHEMA_PREFIX) ||
            schema.startsWith(PostgreConstants.TEMP_SCHEMA_PREFIX);
    }

    //@Property
    public Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        List<PostgreDataType> types = new ArrayList<>();
        for (PostgreDataType dt : dataTypeCache.getAllObjects(monitor, this)) {
            if (dt.getParentObject() == this) {
                types.add(dt);
            }
        }
        if (PostgreConstants.CATALOG_SCHEMA_NAME.equals(this.getName())) {
            // Add serial data types
            for (Map.Entry<String,String> serialMapping : PostgreConstants.SERIAL_TYPES.entrySet()) {
                PostgreDataType realType = dataTypeCache.getCachedObject(serialMapping.getValue());
                if (realType != null) {
                    PostgreDataType serialType = new PostgreDataType(realType, serialMapping.getKey());
                    dataTypeCache.cacheObject(serialType);
                }
            }
        }
        DBUtils.orderObjects(types);
        return types;
    }

    @Override
    public String toString() {
        return name;
    }

    class CollationCache extends JDBCObjectCache<PostgreSchema, PostgreCollation> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.oid,c.* FROM pg_catalog.pg_collation c " +
                    "\nWHERE c.collnamespace=?" +
                    "\nORDER BY c.oid"
            );
            dbStat.setLong(1, PostgreSchema.this.getObjectId());
            return dbStat;
        }

        @Override
        protected PostgreCollation fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreCollation(owner, dbResult);
        }
    }

    class ExtensionCache extends JDBCObjectCache<PostgreSchema, PostgreExtension> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT e.oid,e.* FROM pg_catalog.pg_extension e " +
                    "\nWHERE e.extnamespace=?" +
                    "\nORDER BY e.oid"
            );
            dbStat.setLong(1, PostgreSchema.this.getObjectId());
            return dbStat;
        }

        @Override
        protected PostgreExtension fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreExtension(owner, dbResult);
        }
    }

    public class TableCache extends JDBCStructLookupCache<PostgreSchema, PostgreTableBase, PostgreTableColumn> {

        protected TableCache()
        {
            super("relname");
            setListOrderComparator(DBUtils.<PostgreTableBase>nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreSchema postgreSchema, @Nullable PostgreTableBase object, @Nullable String objectName) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.oid,c.*,d.description FROM pg_catalog.pg_class c\n" +
                "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=c.oid AND d.objsubid=0\n" +
                "WHERE c.relnamespace=? AND c.relkind not in ('i','c')" +
                (object == null && objectName == null ? "" : " AND relname=?")
            );
            dbStat.setLong(1, getObjectId());
            if (object != null || objectName != null) dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected PostgreTableBase fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final String kindString = JDBCUtils.safeGetString(dbResult, "relkind");
            PostgreClass.RelKind kind;
            try {
                kind = PostgreClass.RelKind.valueOf(kindString);
            } catch (Throwable e) {
                log.warn("Unexpected class '" + kindString + "'", e);
                return null;
            }
            switch (kind) {
                case r:
                    return new PostgreTableRegular(PostgreSchema.this, dbResult);
                case v:
                    return new PostgreView(PostgreSchema.this, dbResult);
                case m:
                    return new PostgreMaterializedView(PostgreSchema.this, dbResult);
                case f:
                    return new PostgreTableForeign(PostgreSchema.this, dbResult);
                case S:
                    return new PostgreSequence(PostgreSchema.this, dbResult);
                case t:
                    return new PostgreTableRegular(PostgreSchema.this, dbResult);
                default:
                    log.warn("Unsupported PostgreClass '" + kind + "'");
                    return null;
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @Nullable PostgreTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT c.relname,a.*,pg_catalog.pg_get_expr(ad.adbin, ad.adrelid, true) as def_value,dsc.description" +
                "\nFROM pg_catalog.pg_attribute a" +
                "\nINNER JOIN pg_catalog.pg_class c ON (a.attrelid=c.oid)" +
                "\nLEFT OUTER JOIN pg_catalog.pg_attrdef ad ON (a.attrelid=ad.adrelid AND a.attnum = ad.adnum)" +
                "\nLEFT OUTER JOIN pg_catalog.pg_description dsc ON (c.oid=dsc.objoid AND a.attnum = dsc.objsubid)" +
                "\nWHERE NOT a.attisdropped");
            if (forTable != null) {
                sql.append(" AND c.oid=?");
            } else {
                sql.append(" AND c.relnamespace=? AND c.relkind not in ('i','c')");
            }
            sql.append(" ORDER BY a.attnum");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable != null) {
                dbStat.setLong(1, forTable.getObjectId());
            } else {
                dbStat.setLong(1, PostgreSchema.this.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected PostgreTableColumn fetchChild(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull PostgreTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            try {
                return new PostgreTableColumn(table, dbResult);
            } catch (DBException e) {
                log.warn("Error reading attribute info", e);
                return null;
            }
        }

    }

    /**
     * Constraint cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<PostgreSchema, PostgreTableBase, PostgreTableConstraintBase, PostgreTableConstraintColumn> {
        protected ConstraintCache() {
            super(tableCache, PostgreTableBase.class, "tabrelname", "conname");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreSchema schema, PostgreTableBase forParent) throws SQLException {
            StringBuilder sql = new StringBuilder(
                "SELECT c.oid,c.*,t.relname as tabrelname,rt.relnamespace as refnamespace" +
                "\nFROM pg_catalog.pg_constraint c" +
                "\nINNER JOIN pg_catalog.pg_class t ON t.oid=c.conrelid" +
                "\nLEFT OUTER JOIN pg_catalog.pg_class rt ON rt.oid=c.confrelid" +
                "\nWHERE ");
            if (forParent == null) {
                sql.append("t.relnamespace=?");
            } else {
                sql.append("c.conrelid=?");
            }
            sql.append("\nORDER BY c.oid");
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forParent == null) {
                dbStat.setLong(1, schema.getObjectId());
            } else {
                dbStat.setLong(1, forParent.getObjectId());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected PostgreTableConstraintBase fetchObject(JDBCSession session, PostgreSchema schema, PostgreTableBase table, String childName, JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "conname");
            String type = JDBCUtils.safeGetString(resultSet, "contype");
            if (type == null) {
                log.warn("Null constraint type");
                return null;
            }
            DBSEntityConstraintType constraintType;
            switch (type) {
                case "c": constraintType = DBSEntityConstraintType.CHECK; break;
                case "f": constraintType = DBSEntityConstraintType.FOREIGN_KEY; break;
                case "p": constraintType = DBSEntityConstraintType.PRIMARY_KEY; break;
                case "u": constraintType = DBSEntityConstraintType.UNIQUE_KEY; break;
                case "t": constraintType = PostgreConstants.CONSTRAINT_TRIGGER; break;
                case "x": constraintType = PostgreConstants.CONSTRAINT_EXCLUSIVE; break;
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
        protected PostgreTableConstraintColumn[] fetchObjectRow(JDBCSession session, PostgreTableBase table, PostgreTableConstraintBase constraint, JDBCResultSet resultSet)
            throws SQLException, DBException
        {
            Object keyNumbers = JDBCUtils.safeGetArray(resultSet, "conkey");
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
                Object keyRefNumbers = JDBCUtils.safeGetArray(resultSet, "confkey");
                Collection<PostgreTableColumn> attributes = table.getAttributes(monitor);
                Collection<PostgreTableColumn> refAttributes = refTable.getAttributes(monitor);
                assert attributes != null && refAttributes != null;
                int colCount = Array.getLength(keyNumbers);
                PostgreTableForeignKeyColumn[] fkCols = new PostgreTableForeignKeyColumn[colCount];
                for (int i = 0; i < colCount; i++) {
                    Number colNumber = (Number) Array.get(keyNumbers, i); // Column number - 1-based
                    Number colRefNumber = (Number) Array.get(keyRefNumbers, i);
                    final PostgreTableColumn attr = PostgreUtils.getAttributeByNum(attributes, colNumber.intValue());
                    final PostgreTableColumn refAttr = PostgreUtils.getAttributeByNum(refAttributes, colRefNumber.intValue());
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
                Collection<PostgreTableColumn> attributes = table.getAttributes(monitor);
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
        protected void cacheChildren(DBRProgressMonitor monitor, PostgreTableConstraintBase object, List<PostgreTableConstraintColumn> children) {
            object.cacheAttributes(monitor, children, false);
        }

        @Override
        protected void cacheChildren2(DBRProgressMonitor monitor, PostgreTableConstraintBase object, List<PostgreTableConstraintColumn> children) {
            object.cacheAttributes(monitor, children, true);
        }
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<PostgreSchema, PostgreTableBase, PostgreIndex, PostgreIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, PostgreTableBase.class, "tabrelname", "relname");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreSchema owner, PostgreTableBase forTable)
            throws SQLException
        {
            boolean supportsExprIndex = getDataSource().isServerVersionAtLeast(7, 4);
            StringBuilder sql = new StringBuilder();
            sql.append(
                "SELECT i.*,i.indkey as keys,c.relname,c.relnamespace,c.relam,tc.relname as tabrelname,dsc.description");
            if (supportsExprIndex) {
                sql.append(",pg_catalog.pg_get_expr(i.indexprs, i.indrelid, true) as expr");
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
        protected PostgreIndex fetchObject(JDBCSession session, PostgreSchema owner, PostgreTableBase parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
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
            throws SQLException, DBException
        {
            long[] keyNumbers = PostgreUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "keys"));
            if (keyNumbers == null) {
                return null;
            }
            long[] indColClasses = PostgreUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "indclass"));
            int[] keyOptions = PostgreUtils.getIntVector(JDBCUtils.safeGetObject(dbResult, "indoption"));
            String expr = JDBCUtils.safeGetString(dbResult, "expr");
            Collection<PostgreTableColumn> attributes = parent.getAttributes(dbResult.getSession().getProgressMonitor());
            assert attributes != null;
            PostgreIndexColumn[] result = new PostgreIndexColumn[keyNumbers.length];
            for (int i = 0; i < keyNumbers.length; i++) {
                long colNumber = keyNumbers[i];
                String attrExpression = null;
                final PostgreAttribute attr = PostgreUtils.getAttributeByNum(attributes, (int) colNumber);
                if (attr == null) {
                    if (colNumber == 0 && expr != null) {
                        // It's ok, function index or something
                        attrExpression = JDBCUtils.queryString(session, "select pg_catalog.pg_get_indexdef(?, ?, true)", object.getIndexId(), i + 1);
                    } else {
                        log.warn("Bad index attribute index: " + colNumber);
                    }
                }
                int options = keyOptions == null || keyOptions.length < keyNumbers.length ? 0 : keyOptions[i];
                long colOpClass = indColClasses == null || indColClasses.length < keyNumbers.length ? 0 : indColClasses[i];

                PostgreIndexColumn col = new PostgreIndexColumn(
                    object,
                    attr,
                    attrExpression,
                    i,
                        colOpClass != 0 || (options & 0x01) != 0, // This is a kind of lazy hack. Actually this flag depends on access method.
                    colOpClass,
                    false);
                result[i] = col;
            }
            return result;
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, PostgreIndex index, List<PostgreIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Procedures cache implementation
     */
    static class ProceduresCache extends JDBCObjectLookupCache<PostgreSchema, PostgreProcedure> {

        ProceduresCache()
        {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @Nullable PostgreProcedure object, @Nullable String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p.oid,p.*,d.description\n" +
                    "FROM pg_catalog.pg_proc p\n" +
                    "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=p.oid\n" +
                    "WHERE p.pronamespace=?" +
                    (object == null ? "" : " AND p.oid=?") +
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
            throws SQLException, DBException
        {
            return new PostgreProcedure(owner, dbResult);
        }

    }

}
