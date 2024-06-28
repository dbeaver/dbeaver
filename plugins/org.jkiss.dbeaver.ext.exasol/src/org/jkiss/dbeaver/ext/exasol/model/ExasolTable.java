/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTableForeignKeyCache;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTableIndexCache;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTablePartitionColumnCache;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ExasolTable extends ExasolTableBase implements DBPScriptObject, DBPReferentialIntegrityController, DBSEntityConstrainable {
    private static final CharSequence TABLE_NAME_PLACEHOLDER = "%table_name%";
    private static final CharSequence FOREIGN_KEY_NAME_PLACEHOLDER = "%foreign_key_name%";
    private static final String DISABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE " + TABLE_NAME_PLACEHOLDER + " MODIFY CONSTRAINT "
        + FOREIGN_KEY_NAME_PLACEHOLDER + " DISABLE";
    private static final String ENABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE " + TABLE_NAME_PLACEHOLDER + " MODIFY CONSTRAINT "
        + FOREIGN_KEY_NAME_PLACEHOLDER + " ENABLE";

    private long sizeRaw;
    private long sizeCompressed;
    private final String tablePrefix;
    private final ExasolTablePartitionColumnCache tablePartitionColumnCache = new ExasolTablePartitionColumnCache();

    public static class TableAdditionalInfo {
        volatile boolean loaded = false;

        boolean isLoaded() { return loaded; }
    }
    
    public static class AdditionalInfoValidator implements IPropertyCacheValidator<ExasolTable> {
        @Override
        public boolean isPropertyCached(ExasolTable object, Object propertyId)
        {
            return object.getAdditionalInfo().isLoaded();
        }
    }
    
    private final AdditionalInfo additionalInfo = new AdditionalInfo();
    
    
    private ExasolTable getObject()
    {
    	return this;
    }

    public class AdditionalInfo extends TableAdditionalInfo {
        private Boolean hasDistKey;
        private long tablecount;
        private float deletePercentage;
        private Boolean hasPartitionKey;
        private Timestamp lastCommit;
        private Timestamp createTime;
        
        @Property(viewable = true, expensive = false,  editable = false, order = 90)
        public Boolean getHasDistKey(DBRProgressMonitor monitor) throws DBCException {
            return hasDistKey;
        }
        
        @Property(viewable = true, expensive = false,  updatable = false, order = 95)
        public Boolean getHasPartitionKey(DBRProgressMonitor monitor) throws DBCException {
    		return hasPartitionKey;
    	}
        @Property(viewable = true, expensive = false, editable = false, order = 100)
        public Timestamp getLastCommit(DBRProgressMonitor monitor) throws DBCException {
            return lastCommit;
        }

        @Property(viewable = true, expensive = false, editable = false, order = 100)
        public Timestamp getCreateTime(DBRProgressMonitor monitor) throws DBCException {
            return createTime;
        }

        @Property(viewable = true, expensive = false, editable = false, order = 150, category = DBConstants.CAT_STATISTICS, formatter = ByteNumberFormat.class)
        public long getRawsize(DBRProgressMonitor monitor) throws DBCException {
            return sizeRaw;
        }

        @Property(viewable = true, expensive = false, editable = false, order = 200, category = DBConstants.CAT_STATISTICS, formatter = ByteNumberFormat.class)
        public long getCompressedsize(DBRProgressMonitor monitor) throws DBCException {
            return sizeCompressed;
        }

        @Property(viewable = true, expensive = false, editable = false, order = 250, category = DBConstants.CAT_STATISTICS)
        public float getDeletePercentage(DBRProgressMonitor monitor) throws DBCException {
            return this.deletePercentage;
        }    
        
        @Property(viewable = true, expensive = false, editable = false, order = 300, category = DBConstants.CAT_STATISTICS)
        public long getTableCount(DBRProgressMonitor monitor) throws DBCException {
        	return this.tablecount;
        }

    }    
    private static String readAdditionalTableInfo = "/*snapshot execution*/ SELECT" + 
            "    * " + 
            "FROM" + 
            "    (" + 
            "    SELECT" + 
            "        table_schema," + 
            "        table_name," + 
            "        table_owner," + 
            "        table_has_distribution_key," + 
            "        %s" + 
            "        table_comment," + 
            "        table_row_count," + 
            "        delete_percentage," + 
            "        o.created," + 
            "        o.last_commit," + 
            "        o.object_type" + 
            "    FROM" + 
            "        %s_OBJECTS o" + 
            "    INNER JOIN %s_TABLES T ON" + 
            "        o.object_id = t.table_object_id" + 
            "    WHERE" + 
            "        o.object_id = ?" + 
            "        AND t.table_object_id = ? " + 
            "UNION ALL" + 
            "    SELECT" + 
            "        schema_name AS table_schema," + 
            "        object_name AS table_name," + 
            "        'SYS' AS table_owner," + 
            "        FALSE AS table_has_distribution_key," + 
            "        FALSE AS table_has_partition_key," + 
            "        object_comment AS table_comment," + 
            "        -1 AS table_row_count," + 
            "        -1 AS delete_percentage," + 
            "        CAST( NULL AS TIMESTAMP) AS CREATED," + 
            "        CAST( NULL AS TIMESTAMP) AS last_commit," + 
            "        object_type" + 
            "    FROM" + 
            "        SYS.EXA_SYSCAT" + 
            "    WHERE" + 
            "        object_type = 'TABLE'" + 
            "        AND schema_name = ?" + 
            "        AND object_name = ? ) AS o " + 
            "ORDER BY " + 
            "    table_schema," + 
            "    o.table_name" + 
            "";
    private static String readTableSize =         "/*snapshot execution*/ SELECT " + 
            "    * " + 
            "FROM " + 
            "    ( " + 
            "    SELECT " + 
            "        root_name, " + 
            "        object_name, " + 
            "        raw_object_size, " + 
            "        mem_object_size " + 
            "    FROM " + 
            "        %s_OBJECT_SIZES  " + 
            "    WHERE " + 
            "        object_id = ? " + 
            "UNION ALL " + 
            "    SELECT " + 
            "        schema_name AS root_name, " + 
            "        object_name, " + 
            "        -1 AS raw_object_size, " + 
            "        -1 AS mem_object_size " + 
            "    FROM " + 
            "        SYS.EXA_SYSCAT " + 
            "    WHERE " + 
            "        object_type = 'TABLE' " + 
            "        AND schema_name = ? " + 
            "        AND object_name = ? ) AS o " + 
            "ORDER BY " + 
            "    root_name, " + 
            "    object_name";
    
    
    public ExasolTable(DBRProgressMonitor monitor, ExasolSchema schema, ResultSet dbResult) throws DBException {
        super(monitor, schema, dbResult);
        tablePrefix = schema.getDataSource().getTablePrefix(ExasolSysTablePrefix.ALL);
    }

    public ExasolTable(ExasolSchema schema, String name) {
        super(schema, name, false);
        tablePrefix = schema.getDataSource().getTablePrefix(ExasolSysTablePrefix.ALL);
    }

    private void read(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
    	JDBCSession session = DBUtils.openMetaSession(monitor, this, ExasolMessages.read_table_details );
    	
        String sqlTableInfo = String.format(readAdditionalTableInfo,
                getDataSource().ishasPriorityGroups() ? "table_has_partition_key,"  : "false as table_has_partition_key,",
                tablePrefix,
                tablePrefix
                );


        // two statements necessary as the exasol optimizer is very stupid from time to time
        try (JDBCPreparedStatement stmt = session.prepareStatement(sqlTableInfo))
    	{
            stmt.setBigDecimal(1, this.getObjectId());
            stmt.setBigDecimal(2, this.getObjectId());
            stmt.setString(3, this.getSchema().getName());
            stmt.setString(4, this.getName());
            try(JDBCResultSet dbResult = stmt.executeQuery())
            {
                dbResult.next();
                this.additionalInfo.hasDistKey = JDBCUtils.safeGetBoolean(dbResult, "TABLE_HAS_DISTRIBUTION_KEY");
                this.additionalInfo.hasPartitionKey = JDBCUtils.safeGetBoolean(dbResult, "TABLE_HAS_PARTITION_KEY");
                if (this.additionalInfo.hasPartitionKey == null)
                    this.additionalInfo.hasPartitionKey = false;
                this.additionalInfo.lastCommit = JDBCUtils.safeGetTimestamp(dbResult, "LAST_COMMIT");
                this.additionalInfo.deletePercentage = JDBCUtils.safeGetFloat(dbResult, "DELETE_PERCENTAGE");
                this.additionalInfo.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED"); 
                this.additionalInfo.tablecount = JDBCUtils.safeGetLong(dbResult, "TABLE_ROW_COUNT");
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    	    
        String sqlTableSize = String.format(readTableSize,
                tablePrefix
                );

        try (JDBCPreparedStatement stmt = session.prepareStatement(sqlTableSize))
        {
            stmt.setBigDecimal(1, this.getObjectId());
            stmt.setString(2, this.getSchema().getName());
            stmt.setString(3, this.getName());
            try(JDBCResultSet dbResult = stmt.executeQuery())
            {
                dbResult.next();
                this.sizeRaw = JDBCUtils.safeGetLong(dbResult, "RAW_OBJECT_SIZE");
                this.sizeCompressed = JDBCUtils.safeGetLong(dbResult, "MEM_OBJECT_SIZE");
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
        
        additionalInfo.loaded = true;
    }
    
    @Override
    public void refreshObjectState(DBRProgressMonitor monitor)
    		throws DBCException
    {
    	this.read(monitor);
    	this.tablePartitionColumnCache.clearCache();
    	this.getSchema().getIndexCache().clearObjectCache(this);
    	super.refreshObjectState(monitor);
    }
    
    public TableAdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }    

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded && monitor != null) {
                read(monitor);
            }
            return additionalInfo;
        }
    }
    
    // -----------------
    // Properties
    // -----------------
    
    
    // -----------------
    // Associations
    // -----------------

    @NotNull
    @Override
    public List<DBSEntityConstraintInfo> getSupportedConstraints() {
        return List.of(
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.PRIMARY_KEY, ExasolTableUniqueKey.class)
            //DBSEntityConstraintInfo.of(DBSEntityConstraintType.UNIQUE_KEY, ExasolTableUniqueKey.class)
        );
    }


    @Nullable
    @Override
    @Association
    public Collection<ExasolTableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getConstraintCache().getObjects(monitor, getContainer(), this);
    }

    public ExasolTableUniqueKey getConstraint(DBRProgressMonitor monitor, String ukName) throws DBException {
        return getContainer().getConstraintCache().getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public Collection<ExasolTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
    	return getSchema().getAssociationCache().getObjects(monitor, getSchema(), this);
    }
    
    public synchronized DBSTableForeignKey getAssociation(DBRProgressMonitor monitor, String fkName) throws DBException {
        return getContainer().getAssociationCache().getObject(monitor, getContainer(), this, fkName);
    }
    
    
    
    public ExasolTableUniqueKey getPrimaryKey(@NotNull DBRProgressMonitor monitor) throws DBException {
    	if (getConstraints(monitor).isEmpty())
    		return null;
    	return getConstraints(monitor).iterator().next();
    }
    

    // -----------------
    // Business Contract
    // -----------------
    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public JDBCStructCache<ExasolSchema, ExasolTable, ExasolTableColumn> getCache() {
        return getContainer().getTableCache();
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        getContainer().getTableCache().clearChildrenCache(this);

        getContainer().getConstraintCache().clearObjectCache(this);
        getContainer().getAssociationCache().clearObjectCache(this);

        return this;
    }

    @Override
    public ExasolTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public List<ExasolTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return ExasolUtils.generateDDLforTable(monitor, this.getDataSource(), this);
    }

    @Override
    public DBSObjectState getObjectState() {
        // table can only be in state normal
        return DBSObjectState.NORMAL;
    }
    
    public Collection<ExasolTableColumn> getDistributionKey(DBRProgressMonitor monitor) throws DBException
    {
    	ArrayList<ExasolTableColumn> distKeyCols = new ArrayList<ExasolTableColumn>();
    	
    	for(ExasolTableColumn c : getAttributes(monitor))
    	{
    		if (c.isDistKey())
    		{
    			distKeyCols.add(c);
    		}
    	}
    	return distKeyCols;
    }
    public ExasolTablePartitionColumn getPartition(String name) throws DBException {
    	return tablePartitionColumnCache.getCachedObject(name);
	}
    
    public Collection<ExasolTablePartitionColumn> getPartitions(DBRProgressMonitor monitor) throws DBException {
    	return tablePartitionColumnCache.getAllObjects(monitor, this);
    }
    
    public ExasolTablePartitionColumnCache getPartitionCache()
    {
    	return tablePartitionColumnCache;
    }
    
    public Collection<ExasolTableColumn> getAvailableColumns(DBRProgressMonitor monitor) throws DBException
    {
    	return tablePartitionColumnCache.getAvailableTableColumns(this, monitor);
    }
    
    public void setHasPartitionKey(Boolean hasPartitionKey) {
        if (this.additionalInfo.hasPartitionKey == false && hasPartitionKey == true)
            return;
        this.additionalInfo.hasPartitionKey = hasPartitionKey;
        tablePartitionColumnCache.setCache(new ArrayList<ExasolTablePartitionColumn>());
    }
    public void setHasPartitionKey(Boolean hasPartitionKey, Boolean force) {
        if (force)
            this.additionalInfo.hasPartitionKey = hasPartitionKey;
        setHasPartitionKey(hasPartitionKey);
    }
    
    public List<ExasolTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getIndexCache().getObjects(monitor, getSchema(), getObject());
    }
    
    private ExasolTableIndexCache getIndexCache()
    {
        return getSchema().getIndexCache();
    }
    
    @Override
    public Collection<ExasolTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        ExasolTableForeignKeyCache associationCache = getSchema().getAssociationCache();
        Collection<ExasolTableForeignKey> refForeignKeys = new ArrayList<ExasolTableForeignKey>();
        for (ExasolTableForeignKey exasolTableForeignKey : associationCache.getObjects(monitor, getSchema(), null)) {
            if (exasolTableForeignKey.getReferencedTable() == this) {
                refForeignKeys.add(exasolTableForeignKey);
            }

        }
        return refForeignKeys;
    }

    @Override
    public boolean supportsChangingReferentialIntegrity(@NotNull DBRProgressMonitor monitor) throws DBException {
        return !CommonUtils.isEmpty(getAssociations(monitor));
    }

    @Override
    public void enableReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        Collection<ExasolTableForeignKey> foreignKeys = getAssociations(monitor);
        if (CommonUtils.isEmpty(foreignKeys)) {
            return;
        }

        String template;
        if (enable) {
            template = ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        } else {
            template = DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        }
        template = template.replace(TABLE_NAME_PLACEHOLDER, getFullyQualifiedName(DBPEvaluationContext.DDL));

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Changing referential integrity")) {
            try (JDBCStatement statement = session.createStatement()) {
                for (DBPNamedObject fk: foreignKeys) {
                    String sql = template.replace(FOREIGN_KEY_NAME_PLACEHOLDER,  fk.getName());
                    statement.executeUpdate(sql);
                }
            } catch (SQLException e) {
                throw new DBException("Unable to change referential integrity", e);
            }
        }
    }

    @Nullable
    @Override
    public String getChangeReferentialIntegrityStatement(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        if (!supportsChangingReferentialIntegrity(monitor)) {
            return null;
        }
        if (enable) {
            return ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        }
        return DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
    }
}
