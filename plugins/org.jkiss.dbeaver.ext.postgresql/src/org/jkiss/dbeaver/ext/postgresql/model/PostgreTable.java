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
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.SimpleObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * PostgreTable
 */
public class PostgreTable extends PostgreTableBase
{

    private static final String INNODB_COMMENT = "InnoDB free";

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private long rowCount;
        private long autoIncrement;
        private String description;
        private Date createTime;
        private PostgreCharset charset;
        private PostgreCollation collation;
        private long avgRowLength;
        private long dataLength;

        @Property(viewable = true, editable = true, updatable = true, order = 4) public long getAutoIncrement() { return autoIncrement; }
        @Property(viewable = false, editable = true, updatable = true, listProvider = CharsetListProvider.class, order = 5) public PostgreCharset getCharset() { return charset; }
        @Property(viewable = true, editable = true, updatable = true, order = 100) public String getDescription() { return description; }

        @Property(category = "Statistics", viewable = true, order = 10) public long getRowCount() { return rowCount; }
        @Property(category = "Statistics", viewable = true, order = 11) public long getAvgRowLength() { return avgRowLength; }
        @Property(category = "Statistics", viewable = true, order = 12) public long getDataLength() { return dataLength; }
        @Property(category = "Statistics", viewable = false, order = 13) public Date getCreateTime() { return createTime; }

        public void setAutoIncrement(long autoIncrement) { this.autoIncrement = autoIncrement; }
        public void setDescription(String description) { this.description = description; }

        public void setCharset(PostgreCharset charset) { this.charset = charset; this.collation = charset == null ? null : charset.getDefaultCollation(); }
        public void setCollation(PostgreCollation collation) { this.collation = collation; }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<PostgreTable> {
        @Override
        public boolean isPropertyCached(PostgreTable object, Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private SimpleObjectCache<PostgreTable, PostgreTableForeignKey> foreignKeys = new SimpleObjectCache<>();

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public PostgreTable(PostgreSchema catalog)
    {
        super(catalog);
    }

    public PostgreTable(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Override
    @Association
    public synchronized Collection<PostgreTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Read indexes using cache
        return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
    }

    @Nullable
    @Override
    @Association
    public synchronized Collection<PostgreTableConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().constraintCache.getObjects(monitor, getContainer(), this);
    }

    public PostgreTableConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return getContainer().constraintCache.getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public Collection<PostgreTableForeignKey> getReferences(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadForeignKeys(monitor, true);
    }

    @Override
    public synchronized Collection<PostgreTableForeignKey> getAssociations(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!foreignKeys.isCached()) {
            List<PostgreTableForeignKey> fkList = loadForeignKeys(monitor, false);
            foreignKeys.setCache(fkList);
        }
        return foreignKeys.getCachedObjects();
    }

    public PostgreTableForeignKey getAssociation(DBRProgressMonitor monitor, String fkName)
        throws DBException
    {
        return DBUtils.findObject(getAssociations(monitor), fkName);
    }

    public DBSObjectCache<PostgreTable, PostgreTableForeignKey> getForeignKeyCache()
    {
        return foreignKeys;
    }

    @Association
    public Collection<PostgreTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        List<PostgreTrigger> triggers = new ArrayList<>();
        for (PostgreTrigger trigger : getContainer().triggerCache.getAllObjects(monitor, getContainer())) {
            if (trigger.getTable() == this) {
                triggers.add(trigger);
            }
        }
        return triggers;
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);
        getContainer().indexCache.clearObjectCache(this);
        getContainer().constraintCache.clearObjectCache(this);
        foreignKeys.clearCache();
        synchronized (additionalInfo) {
            additionalInfo.loaded = false;
        }
        return true;
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        PostgreDataSource dataSource = getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW TABLE STATUS FROM " + DBUtils.getQuotedIdentifier(getContainer()) + " LIKE '" + getName() + "'")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        // filer table description (for INNODB it contains some system information)
                        String desc = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_TABLE_COMMENT);
                        if (desc != null) {
                            if (desc.startsWith(INNODB_COMMENT)) {
                                desc = "";
                            } else if (!CommonUtils.isEmpty(desc)) {
                                int divPos = desc.indexOf("; " + INNODB_COMMENT);
                                if (divPos != -1) {
                                    desc = desc.substring(0, divPos);
                                }
                            }
                            additionalInfo.description = desc;
                        }
                        additionalInfo.rowCount = JDBCUtils.safeGetLong(dbResult, PostgreConstants.COL_TABLE_ROWS);
                        additionalInfo.autoIncrement = JDBCUtils.safeGetLong(dbResult, PostgreConstants.COL_AUTO_INCREMENT);
                        additionalInfo.createTime = JDBCUtils.safeGetTimestamp(dbResult, PostgreConstants.COL_CREATE_TIME);
                        additionalInfo.avgRowLength = JDBCUtils.safeGetLong(dbResult, PostgreConstants.COL_AVG_ROW_LENGTH);
                        additionalInfo.dataLength = JDBCUtils.safeGetLong(dbResult, PostgreConstants.COL_DATA_LENGTH);
                    }
                    additionalInfo.loaded = true;
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, dataSource);
        }
    }

    private List<PostgreTableForeignKey> loadForeignKeys(DBRProgressMonitor monitor, boolean references)
        throws DBException
    {
        List<PostgreTableForeignKey> fkList = new ArrayList<>();
        if (!isPersisted()) {
            return fkList;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table relations")) {
            Map<String, PostgreTableForeignKey> fkMap = new HashMap<>();
            Map<String, PostgreTableConstraint> pkMap = new HashMap<>();
            JDBCDatabaseMetaData metaData = session.getMetaData();
            // Load indexes
            JDBCResultSet dbResult;
            if (references) {
                dbResult = metaData.getExportedKeys(
                    getContainer().getName(),
                    null,
                    getName());
            } else {
                dbResult = metaData.getImportedKeys(
                    getContainer().getName(),
                    null,
                    getName());
            }
            try {
                while (dbResult.next()) {
                    String pkTableCatalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_CAT);
                    String pkTableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_NAME);
                    String pkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKCOLUMN_NAME);
                    String fkTableCatalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKTABLE_CAT);
                    String fkTableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKTABLE_NAME);
                    String fkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKCOLUMN_NAME);
                    int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);
                    int updateRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.UPDATE_RULE);
                    int deleteRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DELETE_RULE);
                    String fkName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FK_NAME);
                    String pkName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PK_NAME);

                    DBSForeignKeyModifyRule deleteRule = JDBCUtils.getCascadeFromNum(deleteRuleNum);
                    DBSForeignKeyModifyRule updateRule = JDBCUtils.getCascadeFromNum(updateRuleNum);

                    PostgreTable pkTable = getContainer().getDatabase().findTable(monitor, pkTableCatalog, pkTableName);
                    if (pkTable == null) {
                        log.warn("Can't find PK table " + pkTableName);
                        continue;
                    }
                    PostgreTable fkTable = getContainer().getDatabase().findTable(monitor, fkTableCatalog, fkTableName);
                    if (fkTable == null) {
                        log.warn("Can't find FK table " + fkTableName);
                        continue;
                    }
                    PostgreTableColumn pkColumn = pkTable.getAttribute(monitor, pkColumnName);
                    if (pkColumn == null) {
                        log.warn("Can't find PK table " + pkTable.getFullQualifiedName() + " column " + pkColumnName);
                        continue;
                    }
                    PostgreTableColumn fkColumn = fkTable.getAttribute(monitor, fkColumnName);
                    if (fkColumn == null) {
                        log.warn("Can't find FK table " + fkTable.getFullQualifiedName() + " column " + fkColumnName);
                        continue;
                    }

                    // Find PK
                    PostgreTableConstraint pk = null;
                    if (pkName != null) {
                        pk = DBUtils.findObject(pkTable.getConstraints(monitor), pkName);
                        if (pk == null) {
                            log.warn("Unique key '" + pkName + "' not found in table " + pkTable.getFullQualifiedName());
                        }
                    }
                    if (pk == null) {
                        Collection<PostgreTableConstraint> constraints = pkTable.getConstraints(monitor);
                        if (constraints != null) {
                            for (PostgreTableConstraint pkConstraint : constraints) {
                                if (pkConstraint.getConstraintType().isUnique() && DBUtils.getConstraintAttribute(monitor, pkConstraint, pkColumn) != null) {
                                    pk = pkConstraint;
                                    break;
                                }
                            }
                        }
                    }
                    if (pk == null) {
                        log.warn("Can't find primary key for table " + pkTable.getFullQualifiedName());
                        // Too bad. But we have to create new fake PK for this FK
                        String pkFullName = pkTable.getFullQualifiedName() + "." + pkName;
                        pk = pkMap.get(pkFullName);
                        if (pk == null) {
                            pk = new PostgreTableConstraint(pkTable, pkName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
                            pk.addColumn(new PostgreTableConstraintColumn(pk, pkColumn, keySeq));
                            pkMap.put(pkFullName, pk);
                        }
                    }

                    // Find (or create) FK
                    PostgreTableForeignKey fk = null;
                    if (references) {
                        fk = DBUtils.findObject(fkTable.getAssociations(monitor), fkName);
                        if (fk == null) {
                            log.warn("Can't find foreign key '" + fkName + "' for table " + fkTable.getFullQualifiedName());
                            // No choice, we have to create fake foreign key :(
                        } else {
                            if (!fkList.contains(fk)) {
                                fkList.add(fk);
                            }
                        }
                    }

                    if (fk == null) {
                        fk = fkMap.get(fkName);
                        if (fk == null) {
                            fk = new PostgreTableForeignKey(fkTable, fkName, null, pk, deleteRule, updateRule, true);
                            fkMap.put(fkName, fk);
                            fkList.add(fk);
                        }
                        PostgreTableForeignKeyColumnTable fkColumnInfo = new PostgreTableForeignKeyColumnTable(fk, fkColumn, keySeq, pkColumn);
                        fk.addColumn(fkColumnInfo);
                    }
                }
            } finally {
                dbResult.close();
            }
            return fkList;
        } catch (SQLException ex) {
            throw new DBException(ex, getDataSource());
        }
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        return "";
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Table DDL is read-only");
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return additionalInfo.description;
    }

    public static class CharsetListProvider implements IPropertyValueListProvider<PostgreTable> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(PostgreTable object)
        {
            return object.getDataSource().getCharsets().toArray();
        }
    }

}
