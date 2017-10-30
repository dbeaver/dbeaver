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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

/**
 * @author Karl
 */
public class ExasolTable extends ExasolTableBase implements DBPRefreshableObject, DBPNamedObject2, DBPScriptObject {

    private Boolean hasDistKey;
    private Timestamp lastCommit;
    private long sizeRaw;
    private long sizeCompressed;
    private float deletePercentage;
    private Timestamp createTime;
    private Boolean hasRead;
    private static String readAdditionalInfo =         "select * from ("
									            + "select" +
									            "	table_schema," +
									            "	table_name," +
									            "	table_owner," +
									            "	table_has_distribution_key," +
									            "	table_comment," +
									            "	delete_percentage," +
									            "	o.created," +
									            "	o.last_commit," +
									            "	s.raw_object_size," +
									            "	s.mem_object_size," +
									            "   s.object_type" +
									            " from" +
									            "		EXA_ALL_OBJECTS o" +
									            "	inner join" +
									            "		EXA_ALL_TABLES T" +
									            "	on" +
									            "		o.root_name = t.table_schema and" +
									            "		t.table_name = o.object_name and" +
									            "		o.object_type = 'TABLE'" +
									            "	inner join " +
									            "		EXA_ALL_OBJECT_SIZES s" +
									            "	on" +
									            "		o.root_name = s.root_name and" +
									            "		o.object_name = s.object_name and" +
									            "		o.object_type = s.object_type" +
									            "   where o.root_name = '%s' and o.object_name = '%s' and t.table_schema = '%s' and t.table_name = '%s'" +
									            " union all "
									            + " select schema_name as table_schema,"
									            + " object_name as table_name,"
									            + " 'SYS' as table_owner,"
									            + " false as table_has_distribution_key,"
									            + " object_comment as table_comment,"
									            + " 0 as delete_percentage,"
									            + " cast( null as timestamp) as created,"
									            + " cast( null as timestamp) as last_commit,"
									            + " 0 as raw_object_size,"
									            + " 0 as mem_object_size,"
									            + " object_type"
									            + " from SYS.EXA_SYSCAT WHERE object_type = 'TABLE' and schema_name = '%s' and object_name = '%s'"
									            + ") as o"
									            + "	order by table_schema,o.table_name";
    
	

    public ExasolTable(DBRProgressMonitor monitor, ExasolSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
        hasRead=false;

    }

    public ExasolTable(ExasolSchema schema, String name) {
        super(schema, name, false);
        hasRead=false;
    }

    private void read(DBRProgressMonitor monitor) throws DBCException
    {
    	JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read Table Details");
    	try (JDBCStatement stmt = session.createStatement())
    	{
    		String sql = String.format(readAdditionalInfo,
    				ExasolUtils.quoteString(this.getSchema().getName()),
    				ExasolUtils.quoteString(this.getName()),
    				ExasolUtils.quoteString(this.getSchema().getName()),
    				ExasolUtils.quoteString(this.getName()),
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
    	        this.hasRead = true;
    		}
    		
    	} catch (SQLException e) {
    		throw new DBCException(e,getDataSource());
		}
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
    @Property(viewable = false, expensive = true,  editable = false, order = 90, category = ExasolConstants.CAT_BASEOBJECT)
    public Boolean getHasDistKey(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return hasDistKey;
    }

    @Property(viewable = false, expensive = true, editable = false, order = 100, category = ExasolConstants.CAT_BASEOBJECT)
    public Timestamp getLastCommit(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return lastCommit;
    }

    @Property(viewable = false, expensive = true, editable = false, order = 100, category = ExasolConstants.CAT_DATETIME)
    public Timestamp getCreateTime(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return createTime;
    }

    @Property(viewable = false, expensive = true, editable = false, order = 150, category = ExasolConstants.CAT_STATS)
    public String getRawsize(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return ExasolUtils.humanReadableByteCount(sizeRaw, true);
    }

    @Property(viewable = false, expensive = true, editable = false, order = 200, category = ExasolConstants.CAT_STATS)
    public String getCompressedsize(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return ExasolUtils.humanReadableByteCount(sizeCompressed, true);
    }

    @Property(viewable = false, expensive = true, editable = false, order = 250, category = ExasolConstants.CAT_STATS)
    public float getDeletePercentage(DBRProgressMonitor monitor) throws DBCException {
    	if (! hasRead)
    		read(monitor);
        return this.deletePercentage;
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

    
}
