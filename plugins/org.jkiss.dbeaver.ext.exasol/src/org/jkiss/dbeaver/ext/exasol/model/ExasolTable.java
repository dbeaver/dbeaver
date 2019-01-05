/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class ExasolTable extends ExasolTableBase implements DBPRefreshableObject, DBPNamedObject2, DBPScriptObject {

    private Boolean hasDistKey;
    private Timestamp lastCommit;
    private long sizeRaw;
    private long sizeCompressed;
    private float deletePercentage;
    private Timestamp createTime;
    private Boolean hasRead;
    private long tablecount;
    private String tablePrefix;
    private Collection<ExasolTablePartitionColumn> tablePartitionColumns;
    private static String readAdditionalInfo =         "SELECT " + 
    		"	* " + 
    		"FROM " + 
    		"	( " + 
    		"	SELECT " + 
    		"		table_schema, " + 
    		"		table_name, " + 
    		"		table_owner, " + 
    		"		table_has_distribution_key, " + 
    		"		table_comment, " + 
    		"		table_row_count, " + 
    		"		delete_percentage, " + 
    		"		o.created, " + 
    		"		o.last_commit, " + 
    		"		s.raw_object_size, " + 
    		"		s.mem_object_size, " + 
    		"		s.object_type " + 
    		"	FROM " + 
    		"		%s_OBJECTS o " + 
    		"	INNER JOIN %s_TABLES T ON " + 
    		"		o.object_id = t.table_object_id " + 
    		"	INNER JOIN %s_OBJECT_SIZES s ON " + 
    		"		o.object_id = s.object_id " + 
    		"	WHERE " + 
    		"		o.object_id = %s AND o.object_id = %s AND t.table_object_id = %s " + 
    		"UNION ALL " + 
    		"	SELECT " + 
    		"		schema_name AS table_schema, " + 
    		"		object_name AS table_name, " + 
    		"		'SYS' AS table_owner, " + 
    		"		FALSE AS table_has_distribution_key, " + 
    		"		object_comment AS table_comment, " +
    		"       -1 AS table_row_count," + 
    		"		-1 AS delete_percentage, " + 
    		"		CAST( NULL AS TIMESTAMP) AS created, " + 
    		"		CAST( NULL AS TIMESTAMP) AS last_commit, " + 
    		"		-1 AS raw_object_size, " + 
    		"		-1 AS mem_object_size, " + 
    		"		object_type " + 
    		"	FROM " + 
    		"		SYS.EXA_SYSCAT " + 
    		"	WHERE " + 
    		"		object_type = 'TABLE' " + 
    		"		AND schema_name = '%s' " + 
    		"		AND object_name = '%s' ) AS o " + 
    		"ORDER BY " + 
    		"	table_schema, " + 
    		"	o.table_name";
    
    
    public ExasolTable(DBRProgressMonitor monitor, ExasolSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
        hasRead=false;
        tablePrefix = schema.getDataSource().getTablePrefix(ExasolSysTablePrefix.ALL);
        tablePartitionColumns = new ArrayList<ExasolTablePartitionColumn>();
    }

    public ExasolTable(ExasolSchema schema, String name) {
        super(schema, name, false);
        hasRead=false;
        tablePrefix = schema.getDataSource().getTablePrefix(ExasolSysTablePrefix.ALL);
        tablePartitionColumns = new ArrayList<ExasolTablePartitionColumn>();
    }

    private void read(DBRProgressMonitor monitor) throws DBCException
    {
    	JDBCSession session = DBUtils.openMetaSession(monitor, this, ExasolMessages.read_table_details );
    	try (JDBCStatement stmt = session.createStatement())
    	{
    		String sql = String.format(readAdditionalInfo,
    				tablePrefix,
    				tablePrefix,
    				tablePrefix,
    				this.getObjectId(),
    				this.getObjectId(),
    				this.getObjectId(),
    				ExasolUtils.quoteString(this.getSchema().getName()),
    				ExasolUtils.quoteString(this.getName())
    				);
    		
    		try (JDBCResultSet dbResult = stmt.executeQuery(sql)) 
    		{
    			dbResult.next();
				this.hasDistKey = JDBCUtils.safeGetBoolean(dbResult, "TABLE_HAS_DISTRIBUTION_KEY");
    	        this.lastCommit = JDBCUtils.safeGetTimestamp(dbResult, "LAST_COMMIT");
    	        this.sizeRaw = JDBCUtils.safeGetLong(dbResult, "RAW_OBJECT_SIZE");
    	        this.sizeCompressed = JDBCUtils.safeGetLong(dbResult, "MEM_OBJECT_SIZE");
    	        this.deletePercentage = JDBCUtils.safeGetFloat(dbResult, "DELETE_PERCENTAGE");
    	        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED"); 
    			this.tablecount = JDBCUtils.safeGetLong(dbResult, "TABLE_ROW_COUNT");
    		}
    		
    	} catch (SQLException e) {
    		throw new DBCException(e,getDataSource());
		}
        this.hasRead = true;
    	
    }
    
    @Override
    public void refreshObjectState(DBRProgressMonitor monitor)
    		throws DBCException
    {
    	this.read(monitor);
    	super.refreshObjectState(monitor);
    }
    
    // -----------------
    // Properties
    // -----------------
    @Property(viewable = true, expensive = false,  editable = false, order = 90)
    public Boolean getHasDistKey(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return hasDistKey;
    }

    @Property(viewable = true, expensive = false, editable = false, order = 100)
    public Timestamp getLastCommit(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return lastCommit;
    }

    @Property(viewable = true, expensive = false, editable = false, order = 100)
    public Timestamp getCreateTime(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return createTime;
    }

    @Property(viewable = true, expensive = false, editable = false, order = 150, category = ExasolConstants.CAT_STATS)
    public String getRawsize(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return ExasolUtils.humanReadableByteCount(sizeRaw, false);
    }

    @Property(viewable = true, expensive = false, editable = false, order = 200, category = ExasolConstants.CAT_STATS)
    public String getCompressedsize(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return ExasolUtils.humanReadableByteCount(sizeCompressed, false);
    }

    @Property(viewable = true, expensive = false, editable = false, order = 250, category = ExasolConstants.CAT_STATS)
    public float getDeletePercentage(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return this.deletePercentage;
    }    
    
    @Property(viewable = true, expensive = false, editable = false, order = 300, category = ExasolConstants.CAT_STATS)
    public long getTableCount(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
    	return this.tablecount;
    }

    
    
    // -----------------
    // Associations
    // -----------------
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
        return getContainer().getAssociationCache().getObjects(monitor, getContainer(), this);
    }

    public DBSTableForeignKey getAssociation(DBRProgressMonitor monitor, String ukName) throws DBException {
        return getContainer().getAssociationCache().getObject(monitor, getContainer(), this, ukName);
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
    	getPartitions();
    	
		for( ExasolTablePartitionColumn col: tablePartitionColumns)
		{
			if (col.getName() == name)
			{
				return col;
			}
		}
		return null;
	}
    
    public Collection<ExasolTablePartitionColumn> getPartitions() throws DBException {

    	if (tablePartitionColumns.isEmpty())
    	{
	    	for( ExasolTableColumn col: getAttributes(new VoidProgressMonitor()))
			{
				if (col.getPartitionKeyOrdinalPosition() != null)
				{
					tablePartitionColumns.add(new ExasolTablePartitionColumn(this, col, col.getPartitionKeyOrdinalPosition().intValue()));
				}
			}
			sortPartitionColumns();
    	}
    	return tablePartitionColumns;
    }
    
    private void sortPartitionColumns()
    {
    	tablePartitionColumns = tablePartitionColumns.stream()
    			.sorted(Comparator.comparing(ExasolTablePartitionColumn::getOrdinalPosition))
    			.collect(Collectors.toCollection(ArrayList::new));
    }

    public void setPartitions(Collection<ExasolTablePartitionColumn> tablePartitionColumns) {
		this.tablePartitionColumns = tablePartitionColumns;
		sortPartitionColumns();
	}
    
    
    public void addPartition(ExasolTablePartitionColumn tablePartitionColumn) {
    	 Collection<ExasolTablePartitionColumn> moveElements = tablePartitionColumns.stream()
    			 .filter(o -> o.getOrdinalPosition() >= tablePartitionColumn.getOrdinalPosition())
    			 .collect(Collectors.toCollection(ArrayList::new));
    	 
    	 if (moveElements.size()>0)
    	 {
    		 for (ExasolTablePartitionColumn c: moveElements)
    		 {
    			 c.setOrdinalPosition(c.getOrdinalPosition()+1);
    		 }
			sortPartitionColumns();
    	 }
    	 tablePartitionColumns.add(tablePartitionColumn);
    	 
	}
    
    
}
