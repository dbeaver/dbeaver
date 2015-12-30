/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Array;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PostgreSchema
 */
public class PostgreSchema implements DBSSchema, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer, PostgreObject {

    static final Log log = Log.getLog(PostgreSchema.class);

    private PostgreDatabase database;
    private int oid;
    private String name;
    private int ownerId;
    private PostgreCharset defaultCharset;
    private boolean persisted;

    final ClassCache classCache = new ClassCache();
    final ConstraintCache constraintCache = new ConstraintCache();
    final ProceduresCache proceduresCache = new ProceduresCache();
    final TriggerCache triggerCache = new TriggerCache();
    final IndexCache indexCache = new IndexCache();

    public PostgreSchema(PostgreDatabase database, String name, ResultSet dbResult)
        throws SQLException
    {
        this.database = database;
        this.name = name;

        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetInt(dbResult, "oid");
        this.ownerId = JDBCUtils.safeGetInt(dbResult, "nspowner");
        this.defaultCharset = null;
        this.persisted = true;
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public PostgreDatabase getDatabase() {
        return database;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Override
    public int getObjectId() {
        return this.oid;
    }

    @Property(viewable = true, order = 4)
    public int getOwnerId() {
        return ownerId;
    }

    @NotNull
    public PostgreCharset getDefaultCharset() {
        return defaultCharset;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
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
    public Collection<PostgreIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, this, null);
    }

    @Association
    public Collection<PostgreTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return classCache.getTypedObjects(monitor, this, PostgreTable.class);
    }

    public PostgreTableBase getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return classCache.getObject(monitor, this, name, PostgreTable.class);
    }

    public PostgreTableBase getTable(DBRProgressMonitor monitor, int tableId)
        throws DBException
    {
        for (PostgreClass table : classCache.getAllObjects(monitor, this)) {
            if (table.getObjectId() == tableId) {
                return (PostgreTableBase) table;
            }
        }

        return null;
    }

    @Association
    public Collection<PostgreView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return classCache.getTypedObjects(monitor, this, PostgreView.class);
    }

    @Association
    public Collection<PostgreSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException
    {
        return classCache.getTypedObjects(monitor, this, PostgreSequence.class);
    }

    public PostgreSequence getSequence(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return classCache.getObject(monitor, this, name, PostgreSequence.class);
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

    @Association
    public Collection<PostgreTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    public PostgreTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    @Override
    public Collection<PostgreClass> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return classCache.getAllObjects(monitor, this);
    }

    @Override
    public PostgreClass getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return classCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return PostgreTable.class;
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        classCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            classCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache constraints");
            indexCache.getAllObjects(monitor, this);
            constraintCache.getAllObjects(monitor, this);
        }
    }

    @Override
    public synchronized boolean refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        classCache.clearCache();
        indexCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        return true;
    }

    @Override
    public boolean isSystem()
    {
        return PostgreConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(getName()) || PostgreConstants.CATALOG_SCHEMA_NAME.equalsIgnoreCase(getName());
    }

    @Property
    public Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        List<PostgreDataType> types = new ArrayList<>();
        for (PostgreDataType dt : database.datatypeCache.getAllObjects(monitor, database)) {
            if (dt.getParentObject() == this) {
                types.add(dt);
            }
        }
        return types;
    }

    public class ClassCache extends JDBCStructCache<PostgreSchema, PostgreClass, PostgreAttribute> {

        protected ClassCache()
        {
            super("relname");
            setListOrderComparator(DBUtils.<PostgreClass>nameComparator());
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT c.oid,c.* FROM pg_catalog.pg_class c WHERE c.relnamespace=? AND c.relkind not in ('i','c')");
            dbStat.setInt(1, getObjectId());
            return dbStat;
        }

        @Override
        protected PostgreClass fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final String kindString = JDBCUtils.safeGetString(dbResult, "relkind");
            PostgreClass.RelKind kind;
            try {
                kind = PostgreClass.RelKind.valueOf(kindString);
            } catch (IllegalArgumentException e) {
                log.warn("Unexpected class '" + kindString + "'", e);
                return null;
            }
            switch (kind) {
                case r:
                    return new PostgreTable(PostgreSchema.this, dbResult);
                case v:
                    return new PostgreView(PostgreSchema.this, dbResult);
                case S:
                    return new PostgreSequence(PostgreSchema.this, dbResult);
                default:
                    log.warn("Unsupported PostgreClass '" + kind + "'");
                    return null;
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @Nullable PostgreClass forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT c.relname,a.*,pg_catalog.pg_get_expr(ad.adbin, ad.adrelid) as def_value,dsc.description" +
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
                dbStat.setInt(1, forTable.getObjectId());
            } else {
                dbStat.setInt(1, PostgreSchema.this.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected PostgreAttribute fetchChild(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull PostgreClass table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            try {
                return new PostgreAttribute(table, dbResult);
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
            super(classCache, PostgreTableBase.class, "tabrelname", "conname");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreSchema schema, PostgreTableBase forParent) throws SQLException {
            StringBuilder sql = new StringBuilder(
                "SELECT c.oid,c.*,t.relname as tabrelname" +
                "\nFROM pg_catalog.pg_constraint c" +
                "\nINNER JOIN pg_catalog.pg_class t ON t.oid=c.conrelid" +
                "\nWHERE ");
            if (forParent == null) {
                sql.append("t.relnamespace=?");
            } else {
                sql.append("c.conrelid=?");
            }
            sql.append("\nORDER BY c.oid");
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forParent == null) {
                dbStat.setInt(1, schema.getObjectId());
            } else {
                dbStat.setInt(1, forParent.getObjectId());
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
                    log.warn("Unsupported constraint type");
                    return null;
            }
            if (constraintType == DBSEntityConstraintType.FOREIGN_KEY) {
                return new PostgreTableForeignKey(table, name, resultSet);
            } else {
                return new PostgreTableConstraint(table, name, constraintType, resultSet);
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
                Object keyRefNumbers = JDBCUtils.safeGetArray(resultSet, "confkey");
                List<PostgreAttribute> attributes = table.getAttributes(monitor);
                List<PostgreAttribute> refAttributes = foreignKey.getAssociatedEntity().getAttributes(monitor);
                assert attributes != null && refAttributes != null;
                int colCount = Array.getLength(keyNumbers);
                PostgreTableForeignKeyColumn[] fkCols = new PostgreTableForeignKeyColumn[colCount];
                for (int i = 0; i < colCount; i++) {
                    Number colNumber = (Number) Array.get(keyNumbers, i); // Column number - 1-based
                    Number colRefNumber = (Number) Array.get(keyRefNumbers, i);
                    final PostgreAttribute attr = PostgreUtils.getAttributeByNum(attributes, colNumber.intValue());
                    final PostgreAttribute refAttr = PostgreUtils.getAttributeByNum(refAttributes, colRefNumber.intValue());
                    if (attr == null) {
                        log.warn("Bad foreign key attribute index: " + colNumber);
                        continue;
                    }
                    if (refAttr == null) {
                        log.warn("Bad reference table '" + foreignKey.getAssociatedEntity() + "' attribute index: " + colNumber);
                        continue;
                    }
                    PostgreTableForeignKeyColumn cCol = new PostgreTableForeignKeyColumn(foreignKey, attr, i, refAttr);
                    fkCols[i] = cCol;
                }
                return fkCols;

            } else {
                List<PostgreAttribute> attributes = table.getAttributes(monitor);
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
            super(classCache, PostgreTableBase.class, "tabrelname", "relname");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreSchema owner, PostgreTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append(
                "SELECT i.*,i.indkey::int[] as keys,c.relname,c.relnamespace,tc.relname as tabrelname,dsc.description" +
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
            sql.append(" ORDER BY c.relname");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable != null) {
                dbStat.setInt(1, forTable.getObjectId());
            } else {
                dbStat.setInt(1, PostgreSchema.this.getObjectId());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected PostgreIndex fetchObject(JDBCSession session, PostgreSchema owner, PostgreTableBase parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreIndex(
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
            Object keyNumbers = JDBCUtils.safeGetArray(dbResult, "keys");
            if (keyNumbers == null) {
                return null;
            }
            List<PostgreAttribute> attributes = parent.getAttributes(dbResult.getSession().getProgressMonitor());
            assert attributes != null;
            int colCount = Array.getLength(keyNumbers);
            PostgreIndexColumn[] result = new PostgreIndexColumn[colCount];
            for (int i = 0; i < colCount; i++) {
                Number colNumber = (Number) Array.get(keyNumbers, i);
                final PostgreAttribute attr = PostgreUtils.getAttributeByNum(attributes, colNumber.intValue());
                if (attr == null) {
                    log.warn("Bad index attribute index: " + colNumber);
                    continue;
                }
                PostgreIndexColumn col = new PostgreIndexColumn(
                    object,
                    attr,
                    i,
                    true,
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
    class ProceduresCache extends JDBCStructCache<PostgreSchema, PostgreProcedure, PostgreProcedureParameter> {

        ProceduresCache()
        {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + PostgreConstants.META_TABLE_ROUTINES +
                    " WHERE " + PostgreConstants.COL_ROUTINE_SCHEMA + "=?" +
                    " ORDER BY " + PostgreConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, getName());
            return dbStat;
        }

        @Override
        protected PostgreProcedure fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreProcedure(PostgreSchema.this, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @Nullable PostgreProcedure procedure)
            throws SQLException
        {
            // Load procedure columns through Postgre metadata
            // There is no metadata table about proc/func columns -
            // it should be parsed from SHOW CREATE PROCEDURE/FUNCTION query
            // Lets driver do it instead of me
            return session.getMetaData().getProcedureColumns(
                getName(),
                null,
                procedure.getName(),
                null).getSourceStatement();
        }

        @Override
        protected PostgreProcedureParameter fetchChild(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull PostgreProcedure parent, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.LENGTH);
            boolean notNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
            int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
            //int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
            //DBSDataType dataType = getDataSourceContainer().getInfo().getSupportedDataType(typeName);
            DBSProcedureParameterKind parameterType;
            switch (columnTypeNum) {
                case DatabaseMetaData.procedureColumnIn: parameterType = DBSProcedureParameterKind.IN; break;
                case DatabaseMetaData.procedureColumnInOut: parameterType = DBSProcedureParameterKind.INOUT; break;
                case DatabaseMetaData.procedureColumnOut: parameterType = DBSProcedureParameterKind.OUT; break;
                case DatabaseMetaData.procedureColumnReturn: parameterType = DBSProcedureParameterKind.RETURN; break;
                case DatabaseMetaData.procedureColumnResult: parameterType = DBSProcedureParameterKind.RESULTSET; break;
                default: parameterType = DBSProcedureParameterKind.UNKNOWN; break;
            }
            if (CommonUtils.isEmpty(columnName) && parameterType == DBSProcedureParameterKind.RETURN) {
                columnName = "RETURN";
            }
            return new PostgreProcedureParameter(
                parent,
                columnName,
                typeName,
                valueType,
                position,
                columnSize,
                scale, precision, notNull,
                parameterType);
        }
    }

    class TriggerCache extends JDBCObjectCache<PostgreSchema, PostgreTrigger> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SHOW FULL TRIGGERS FROM " + DBUtils.getQuotedIdentifier(PostgreSchema.this));
        }

        @Override
        protected PostgreTrigger fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetString(dbResult, "TABLE");
            PostgreTableBase triggerTable = CommonUtils.isEmpty(tableName) ? null : getTable(session.getProgressMonitor(), tableName);
            return new PostgreTrigger(PostgreSchema.this, triggerTable, dbResult);
        }

    }

}
