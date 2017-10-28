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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Generic table
 */
public class GenericTable extends JDBCTable<GenericDataSource, GenericStructContainer> implements DBPRefreshableObject, DBPSystemObject, DBPScriptObject
{
    private static final Log log = Log.getLog(GenericTable.class);

    private String tableType;
    private boolean isView;
    private boolean isSystem;
    private String description;
    private Long rowCount;
    private List<? extends GenericTrigger> triggers;
    private String ddl;

    public GenericTable(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult)
    {
        super(container, tableName, dbResult != null);
        this.tableType = tableType;
        if (this.tableType == null) {
            this.tableType = "";
        }

        if (dbResult != null) {
            this.description = GenericUtils.safeGetString(container.getTableCache().tableObject, dbResult, JDBCConstants.REMARKS);
        }

        final GenericMetaModel metaModel = container.getDataSource().getMetaModel();
        this.isView = metaModel.isView(this);
        this.isSystem = metaModel.isSystemTable(this);
    }

    @Override
    public TableCache getCache()
    {
        return getContainer().getTableCache();
    }

    @Override
    public DBSObject getParentObject()
    {
        return getContainer().getObject();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        if (isView() && context == DBPEvaluationContext.DDL && !getDataSource().getMetaModel().useCatalogInObjectNames()) {
            // [SQL Server] workaround. You can't use catalog name in operations with views.
            return DBUtils.getFullQualifiedName(
                getDataSource(),
                getSchema(),
                this);
        }
        return DBUtils.getFullQualifiedName(
            getDataSource(),
            getCatalog(),
            getSchema(),
            this);
    }

    @Override
    public boolean isView()
    {
        return this.isView;
    }

    @Override
    public boolean isSystem()
    {
        return this.isSystem;
    }

    @Property(viewable = true, order = 2)
    public String getTableType()
    {
        return tableType;
    }

    @Property(viewable = true, order = 3)
    public GenericCatalog getCatalog()
    {
        return getContainer().getCatalog();
    }

    @Property(viewable = true, order = 4)
    public GenericSchema getSchema()
    {
        return getContainer().getSchema();
    }

    @Nullable
    @Override
    public synchronized Collection<GenericTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return this.getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public GenericTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return this.getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public synchronized Collection<GenericTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Read indexes using cache
        return this.getContainer().getIndexCache().getObjects(monitor, getContainer(), this);
    }

    @Nullable
    @Override
    public synchronized Collection<GenericPrimaryKey> getConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        // ensure all columns are already cached
        getAttributes(monitor);
        return getContainer().getPrimaryKeysCache().getObjects(monitor, getContainer(), this);
    }

    synchronized void addUniqueKey(GenericPrimaryKey constraint) {
        getContainer().getPrimaryKeysCache().cacheObject(constraint);
    }

    @Override
    public Collection<GenericTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return loadReferences(monitor);
    }

    @Override
    public synchronized Collection<GenericTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (getDataSource().getInfo().supportsReferentialIntegrity()) {
            return getContainer().getForeignKeysCache().getObjects(monitor, getContainer(), this);
        }
        return null;
    }

    @Nullable
    public Collection<GenericTable> getSubTables()
    {
        return null;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        this.getContainer().getIndexCache().clearObjectCache(this);
        this.getContainer().getPrimaryKeysCache().clearObjectCache(this);
        this.getContainer().getForeignKeysCache().clearObjectCache(this);
        return this.getContainer().getTableCache().refreshObject(monitor, getContainer(), this);
    }

    // Comment row count calculation - it works too long and takes a lot of resources without serious reason
    @Nullable
    @Property(viewable = true, expensive = true, order = 5, category = "Statistics")
    public synchronized Long getRowCount(DBRProgressMonitor monitor)
    {
        if (rowCount != null) {
            return rowCount;
        }
        if (isView() || !isPersisted()) {
            // Do not count rows for views
            return null;
        }
        if (Boolean.FALSE.equals(getDataSource().getContainer().getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_SELECT_COUNT))) {
            // Select count not supported
            return null;
        }
        if (rowCount == null) {
            // Query row count
            try (DBCSession session = DBUtils.openUtilSession(monitor, getDataSource(), "Read row count")) {
                rowCount = countData(
                    new AbstractExecutionSource(this, session.getExecutionContext(), this), session, null);
            } catch (DBException e) {
                // do not throw this error - row count is optional info and some providers may fail
                log.debug("Can't fetch row count: " + e.getMessage());
//                if (indexes != null) {
//                    rowCount = getRowCountFromIndexes(monitor);
//                }
            }
        }
        if (rowCount == null) {
            rowCount = -1L;
        }

        return rowCount;
    }

    @Nullable
    public Long getRowCountFromIndexes(DBRProgressMonitor monitor)
    {
        try {
            // Try to get cardinality from some unique index
            // Cardinality
            final Collection<GenericTableIndex> indexList = getIndexes(monitor);
            if (!CommonUtils.isEmpty(indexList)) {
                for (GenericTableIndex index : indexList) {
                    if (index.isUnique()/* || index.getIndexType() == DBSIndexType.STATISTIC*/) {
                        final long cardinality = index.getCardinality();
                        if (cardinality > 0) {
                            return cardinality;
                        }
                    }
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
        return null;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_REFRESH)) {
            ddl = null;
        }
        if (ddl == null) {
            if (isView()) {
                ddl = getDataSource().getMetaModel().getViewDDL(monitor, this, options);
            } else if (!isPersisted()) {
                ddl = "";
            } else {
                ddl = getDataSource().getMetaModel().getTableDDL(monitor, this, options);
            }
        }
        return ddl;
    }

    private static class ForeignKeyInfo {
        String pkColumnName;
        String fkTableCatalog;
        String fkTableSchema;
        String fkTableName;
        String fkColumnName;
        int keySeq;
        int updateRuleNum;
        int deleteRuleNum;
        String fkName;
        String pkName;
        int deferabilityNum;
    }

    private synchronized List<GenericTableForeignKey> loadReferences(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!isPersisted() || !getDataSource().getInfo().supportsReferentialIntegrity()) {
            return new ArrayList<>();
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table relations")) {
            // Read foreign keys in two passes
            // First read entire resultset to prevent recursive metadata requests
            // some drivers don't like it
            final GenericMetaObject fkObject = getDataSource().getMetaObject(GenericConstants.OBJECT_FOREIGN_KEY);
            final List<ForeignKeyInfo> fkInfos = new ArrayList<>();
            JDBCDatabaseMetaData metaData = session.getMetaData();
            // Load indexes
            try (JDBCResultSet dbResult = metaData.getExportedKeys(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                getName())) {
                while (dbResult.next()) {
                    ForeignKeyInfo fkInfo = new ForeignKeyInfo();
                    fkInfo.pkColumnName = GenericUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.PKCOLUMN_NAME);
                    fkInfo.fkTableCatalog = GenericUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FKTABLE_CAT);
                    fkInfo.fkTableSchema = GenericUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FKTABLE_SCHEM);
                    fkInfo.fkTableName = GenericUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FKTABLE_NAME);
                    fkInfo.fkColumnName = GenericUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FKCOLUMN_NAME);
                    fkInfo.keySeq = GenericUtils.safeGetInt(fkObject, dbResult, JDBCConstants.KEY_SEQ);
                    fkInfo.updateRuleNum = GenericUtils.safeGetInt(fkObject, dbResult, JDBCConstants.UPDATE_RULE);
                    fkInfo.deleteRuleNum = GenericUtils.safeGetInt(fkObject, dbResult, JDBCConstants.DELETE_RULE);
                    fkInfo.fkName = GenericUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FK_NAME);
                    fkInfo.pkName = GenericUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.PK_NAME);
                    fkInfo.deferabilityNum = GenericUtils.safeGetInt(fkObject, dbResult, JDBCConstants.DEFERRABILITY);
                    fkInfos.add(fkInfo);
                }
            }

            List<GenericTableForeignKey> fkList = new ArrayList<>();
            Map<String, GenericTableForeignKey> fkMap = new HashMap<>();
            for (ForeignKeyInfo info : fkInfos) {
                DBSForeignKeyModifyRule deleteRule = JDBCUtils.getCascadeFromNum(info.deleteRuleNum);
                DBSForeignKeyModifyRule updateRule = JDBCUtils.getCascadeFromNum(info.updateRuleNum);
                DBSForeignKeyDeferability deferability;
                switch (info.deferabilityNum) {
                    case DatabaseMetaData.importedKeyInitiallyDeferred: deferability = DBSForeignKeyDeferability.INITIALLY_DEFERRED; break;
                    case DatabaseMetaData.importedKeyInitiallyImmediate: deferability = DBSForeignKeyDeferability.INITIALLY_IMMEDIATE; break;
                    case DatabaseMetaData.importedKeyNotDeferrable: deferability = DBSForeignKeyDeferability.NOT_DEFERRABLE; break;
                    default: deferability = DBSForeignKeyDeferability.UNKNOWN; break;
                }

                if (info.fkTableName == null) {
                    log.debug("Null FK table name");
                    continue;
                }
                //String fkTableFullName = DBUtils.getFullyQualifiedName(getDataSource(), info.fkTableCatalog, info.fkTableSchema, info.fkTableName);
                GenericTable fkTable = getDataSource().findTable(monitor, info.fkTableCatalog, info.fkTableSchema, info.fkTableName);
                if (fkTable == null) {
                    log.warn("Can't find FK table " + info.fkTableName);
                    continue;
                }
                GenericTableColumn pkColumn = this.getAttribute(monitor, info.pkColumnName);
                if (pkColumn == null) {
                    log.warn("Can't find PK column " + info.pkColumnName);
                    continue;
                }
                GenericTableColumn fkColumn = fkTable.getAttribute(monitor, info.fkColumnName);
                if (fkColumn == null) {
                    log.warn("Can't find FK table " + fkTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + " column " + info.fkColumnName);
                    continue;
                }

                // Find PK
                GenericPrimaryKey pk = null;
                if (!CommonUtils.isEmpty(info.pkName)) {
                    pk = DBUtils.findObject(this.getConstraints(monitor), info.pkName);
                    if (pk == null) {
                        log.debug("Unique key '" + info.pkName + "' not found in table " + this.getFullyQualifiedName(DBPEvaluationContext.DDL));
                    }
                }
                if (pk == null) {
                    Collection<GenericPrimaryKey> uniqueKeys = this.getConstraints(monitor);
                    if (uniqueKeys != null) {
                        for (GenericPrimaryKey pkConstraint : uniqueKeys) {
                            if (pkConstraint.getConstraintType().isUnique() && DBUtils.getConstraintAttribute(monitor, pkConstraint, pkColumn) != null) {
                                pk = pkConstraint;
                                break;
                            }
                        }
                    }
                }
                if (pk == null) {
                    log.warn("Can't find unique key for table " + this.getFullyQualifiedName(DBPEvaluationContext.DDL) + " column " + pkColumn.getName());
                    // Too bad. But we have to create new fake PK for this FK
                    //String pkFullName = getFullyQualifiedName() + "." + info.pkName;
                    pk = new GenericPrimaryKey(this, info.pkName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
                    pk.addColumn(new GenericTableConstraintColumn(pk, pkColumn, info.keySeq));
                    // Add this fake constraint to it's owner
                    this.addUniqueKey(pk);
                }

                // Find (or create) FK
                GenericTableForeignKey fk;
                if (CommonUtils.isEmpty(info.fkName)) {
                    // Make fake FK name
                    info.fkName = info.fkTableName.toUpperCase() + "_FK" + info.keySeq;
                    fk = DBUtils.findObject(fkTable.getAssociations(monitor), info.fkName);
                } else {
                    fk = DBUtils.findObject(fkTable.getAssociations(monitor), info.fkName);
                    if (fk == null) {
                        log.warn("Can't find foreign key '" + info.fkName + "' for table " + fkTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
                        // No choice, we have to create fake foreign key :(
                    }
                }
                if (fk != null && !fkList.contains(fk)) {
                    fkList.add(fk);
                }

                if (fk == null) {
                    fk = fkMap.get(info.fkName);
                    if (fk == null) {
                        fk = new GenericTableForeignKey(fkTable, info.fkName, null, pk, deleteRule, updateRule, deferability, true);
                        fkMap.put(info.fkName, fk);
                        fkList.add(fk);
                    }
                    GenericTableForeignKeyColumnTable fkColumnInfo = new GenericTableForeignKeyColumnTable(fk, fkColumn, info.keySeq, pkColumn);
                    fk.addColumn(fkColumnInfo);
                }
            }

            return fkList;
        } catch (SQLException ex) {
            throw new DBException(ex, getDataSource());
        }
    }

    @Association
    public Collection<? extends GenericTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        if (triggers == null) {
            loadTriggers(monitor);
        }
        return triggers;
    }

    private void loadTriggers(DBRProgressMonitor monitor) throws DBException {
        triggers = getDataSource().getMetaModel().loadTriggers(monitor, getContainer(), this);
        if (triggers == null) {
            triggers = new ArrayList<>();
        } else {
            DBUtils.orderObjects(triggers);
        }
    }

}
