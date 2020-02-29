/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTableCache;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTableForeignKeyCache;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTableUniqueKeyCache;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolViewCache;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolGrantee;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolJDBCObjectSimpleCacheLiterals;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;


public class ExasolSchema extends ExasolGlobalObject implements DBSSchema, DBPNamedObject2,  DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer, DBPScriptObject {

    private static final List<String> SYSTEM_SCHEMA = Arrays.asList("SYS","EXA_STATISTICS");
    private static final Log log = Log.getLog(ExasolSchema.class);    
    private String name;
    private String owner;
    private Timestamp createTime;
    private String remarks;
    private Integer objectId;
    private String tablePrefix;
    private BigDecimal rawObjectSize;
    private BigDecimal memObjectSize;
    private BigDecimal rawObjectSizeLimit;
    private Boolean refreshed = false; 


    // ExasolSchema's children
    public final DBSObjectCache<ExasolSchema, ExasolScript> scriptCache;

    public final DBSObjectCache<ExasolSchema, ExasolFunction> functionCache;
    private ExasolViewCache viewCache = new ExasolViewCache();
    private ExasolTableCache tableCache = new ExasolTableCache();

    // ExasolTable's children
    private final ExasolTableUniqueKeyCache constraintCache = new ExasolTableUniqueKeyCache(tableCache);
    private final ExasolTableForeignKeyCache associationCache = new ExasolTableForeignKeyCache(tableCache);

    public ExasolSchema(ExasolDataSource exasolDataSource, String name, String owner) {
        super(exasolDataSource, true);
        this.tablePrefix = exasolDataSource.getTablePrefix(ExasolSysTablePrefix.ALL);
        this.name = name;
        this.owner = owner;
        this.scriptCache = new ExasolJDBCObjectSimpleCacheLiterals<>(
        		ExasolScript.class,
        		"select "
        		+ "script_name,script_owner,script_language,script_type,script_result_type,script_text,script_comment,b.created "
        		+ "from SYS." + tablePrefix + "_SCRIPTS a inner join SYS." + tablePrefix + "_OBJECTS b "
        		+ "on a.SCRIPT_OBJECT_ID  = b.object_id and b.object_type = 'SCRIPT' where a.script_schema = '%s' "
        		+ "order by script_name",
        		name);

        this.functionCache = new ExasolJDBCObjectSimpleCacheLiterals<>(ExasolFunction.class,
                "SELECT\n" + 
                "    F.*,\n" + 
                "    O.CREATED\n" + 
                "FROM\n" + 
                "    SYS." +  tablePrefix + "_FUNCTIONS F\n" + 
                "INNER JOIN SYS." + tablePrefix + "_OBJECTS O ON\n" + 
                "    F.FUNCTION_OBJECT_ID = O.OBJECT_ID\n" + 
                "WHERE\n" + 
                "    F.FUNCTION_SCHEMA = '%s' and O.OBJECT_TYPE = 'FUNCTION' AND o.ROOT_NAME = '%s'\n" + 
                "ORDER BY\n" + 
                "    FUNCTION_NAME\n", 
                name,name);
        
        
        

    }

    public ExasolSchema(ExasolDataSource exasolDataSource, ResultSet dbResult) throws DBException {

        this(
                exasolDataSource, 
                JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_NAME"), 
                JDBCUtils.safeGetString(dbResult, "OWNER")
            );
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.remarks = JDBCUtils.safeGetString(dbResult, "OBJECT_COMMENT");
        this.name = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.objectId = JDBCUtils.safeGetInt(dbResult, "SCHEMA_OBJECT_ID");


    }
    
    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName() {
        return this.name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public Collection<ExasolTableBase> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<ExasolTableBase> allChildren = new ArrayList<>();
        allChildren.addAll(tableCache.getAllObjects(monitor, this));
        allChildren.addAll(viewCache.getAllObjects(monitor, this));
        return allChildren;
    }

    @Override
    public ExasolTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {

        ExasolTableBase child = tableCache.getObject(monitor, this, childName);
        if (child == null) {
            child = viewCache.getObject(monitor, this, childName);
        }
        return child;
    }

    @Override
    public Class<ExasolTableBase> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
    	return ExasolTableBase.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        if (((scope & STRUCT_ENTITIES) != 0)) {
            monitor.subTask("Cache tables");
            tableCache.getAllObjects(monitor, this);
            monitor.subTask("Cache Views");
            viewCache.getAllObjects(monitor, this);

        }
        if (((scope & STRUCT_ATTRIBUTES) != 0)) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
            monitor.subTask("Cache Views");
            viewCache.loadChildren(monitor, this, null);
        }

        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table unique keys");
            constraintCache.getObjects(monitor, this, null);
            monitor.subTask("Cache table foreign keys");
            associationCache.getObjects(monitor, this, null);

        }


    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<ExasolTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getTypedObjects(monitor, this, ExasolTable.class);
    }

    public ExasolTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
        return tableCache.getObject(monitor, this, name, ExasolTable.class);
    }

    @Association
    public Collection<ExasolView> getViews(DBRProgressMonitor monitor) throws DBException {
        return viewCache.getTypedObjects(monitor, this, ExasolView.class);
    }

    public ExasolView getView(DBRProgressMonitor monitor, String name) throws DBException {
        return viewCache.getObject(monitor, this, name, ExasolView.class);
    }


    @Override
    public boolean isSystem() {
        // TODO Auto-generated method stub
        return SYSTEM_SCHEMA.contains(name);
    }

    @Override
    public Collection<ExasolScript> getProcedures(DBRProgressMonitor monitor) throws DBException {

        return scriptCache.getAllObjects(monitor, this).stream()
    			.filter(o -> o.getType().equals("SCRIPTING"))
    			.collect(Collectors.toCollection(ArrayList::new));
    }
    
    public Collection<ExasolScript> getUdfs(DBRProgressMonitor monitor) throws DBException {
    	
    	return scriptCache.getAllObjects(monitor, this).stream()
    			.filter(o -> o.getType().equals("UDF"))
    			.collect(Collectors.toCollection(ArrayList::new));
    }

    public ExasolScript getUdf(DBRProgressMonitor monitor, String name) throws DBException {

        return scriptCache.getObject(monitor, this, name);
    }


    public Collection<ExasolScript> getAdapter(DBRProgressMonitor monitor) throws DBException {

        return scriptCache.getAllObjects(monitor, this).stream()
    			.filter(o -> o.getType().equals("ADAPTER"))
    			.collect(Collectors.toCollection(ArrayList::new));

    }

    public ExasolScript getAdapter(DBRProgressMonitor monitor, String name) throws DBException {

        return scriptCache.getObject(monitor, this, name);
    }
    
    
    @Override
    public ExasolScript getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {

        return scriptCache.getObject(monitor, this, uniqueName);
    }

    public Collection<ExasolFunction> getFunctions(DBRProgressMonitor monitor) throws DBException {
        return functionCache.getAllObjects(monitor, this);
    }
    
    public ExasolFunction getFunction(DBRProgressMonitor monitor,String name) throws DBException {
        return functionCache.getObject(monitor, this, name);
    }

    
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        
        ((ExasolDataSource) getDataSource()).refreshObject(monitor);
        functionCache.clearCache();
        scriptCache.clearCache();
        tableCache.clearCache();
        viewCache.clearCache();


        constraintCache.clearCache();
        associationCache.clearCache();
        refreshed=false;
        return this;
    }

    @Override
    public String toString() {
        return "Schema " + name;
    }
    
    private void refresh(DBRProgressMonitor monitor) throws DBCException
    {
    	if (!refreshed && this.objectId != null) {
	    	JDBCSession session = DBUtils.openMetaSession(monitor, this, ExasolMessages.read_schema_details );
	    	try (JDBCPreparedStatement stmt = session.prepareStatement("SELECT * FROM SYS."+getDataSource().getTablePrefix(ExasolSysTablePrefix.ALL)+"_OBJECT_SIZES WHERE OBJECT_ID = ?"))
	    	{
	    		stmt.setInt(1, this.objectId);
	    		try (JDBCResultSet dbResult = stmt.executeQuery()) 
	    		{
	    			dbResult.next();
	    	        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
	    	        this.rawObjectSize = JDBCUtils.safeGetBigDecimal(dbResult, "RAW_OBJECT_SIZE");
	    	        this.memObjectSize = JDBCUtils.safeGetBigDecimal(dbResult, "MEM_OBJECT_SIZE");
	    	        this.rawObjectSizeLimit = JDBCUtils.safeGetBigDecimal(dbResult, "RAW_OBJECT_SIZE_LIMIT");
	    		}
	    		
	    	} catch (SQLException e) {
	    		throw new DBCException(e, session.getExecutionContext());
			}
    	}
		
	}

    @Property(viewable = true, editable = false, order = 2)
    public Timestamp getCreateTime(DBRProgressMonitor monitor) throws DBCException {
    	refresh(monitor);
        return createTime;
    }

    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 3)
    public String getDescription() {
        return remarks;
    }
    
    public void setDescription(String newRemarks)
    {
    	remarks = newRemarks;
    }

    @Property(viewable = true, editable = false, updatable = true,  order = 4, listProvider = OwnerListProvider.class)
    public String getOwner() {
        return owner;
    }

    
    
    @Property(viewable = true, editable = false, updatable =  false,  order = 5)
    public String getRawObjectSize() {
    	if (rawObjectSize == null)
    		return "N/A";
		return ExasolUtils.humanReadableByteCount(rawObjectSize.longValue(),false);
	}

    @Property(viewable = true, editable = false, updatable =  false,  order = 6)
	public String getMemObjectSize() {
    	if (memObjectSize == null)
    		return "N/A";
		return ExasolUtils.humanReadableByteCount(memObjectSize.longValue(),false);
	}

    @Property(viewable = true, editable = true, updatable = true,  order = 7)
	public BigDecimal getRawObjectSizeLimit() {
		return rawObjectSizeLimit;
	}
    
    public void setRawObjectSizeLimit(BigDecimal limit) {
    	this.rawObjectSizeLimit = limit;
    }
    

	public void setOwner(ExasolGrantee owner)
    {
        this.owner = owner.getName();
    }

    public ExasolTableCache getTableCache() {
        return tableCache;
    }

    public ExasolViewCache getViewCache() {
        return viewCache;
    }


    public ExasolTableUniqueKeyCache getConstraintCache() {
        return constraintCache;
    }

    public ExasolTableForeignKeyCache getAssociationCache() {
        return associationCache;
    }

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
			throws DBException
	{
		return ExasolUtils.generateDDLforSchema(monitor, this);
	}
	
	
	public static class OwnerListProvider implements IPropertyValueListProvider<ExasolSchema> {
		
		@Override
		public boolean allowCustomValue() {
			return false;
		}
		
		public Object[] getPossibleValues(ExasolSchema object)
		{
			ExasolDataSource dataSource = object.getDataSource();
			try {
				Collection<ExasolGrantee> grantees = dataSource.getAllGrantees(new VoidProgressMonitor());
				return grantees.toArray(new Object[grantees.size()]);
			} catch (DBException e) {
				log.error(e);
				return new  Object[0];
			}
		}
		
	}
	
	
	public Boolean isPhysicalSchema()
	{
	    return true;
	}
}
