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
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolObjectType;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;

import java.sql.SQLException;
import java.util.List;


public class ExasolStructureAssistant extends JDBCStructureAssistant<ExasolExecutionContext> {


    private static final Log LOG = Log.getLog(ExasolStructureAssistant.class);


    private static final DBSObjectType[] SUPP_OBJ_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.VIEW, ExasolObjectType.COLUMN, ExasolObjectType.SCHEMA, ExasolObjectType.SCRIPT, ExasolObjectType.FOREIGNKEY, ExasolObjectType.PRIMARYKEY};
    private static final DBSObjectType[] HYPER_LINKS_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.COLUMN, ExasolObjectType.VIEW, ExasolObjectType.SCHEMA, ExasolObjectType.SCRIPT, ExasolObjectType.FOREIGNKEY, ExasolObjectType.PRIMARYKEY};
    private static final DBSObjectType[] AUTOC_OBJ_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.VIEW, ExasolObjectType.COLUMN, ExasolObjectType.SCHEMA, ExasolObjectType.SCRIPT};



    private String sqlConstraintsAll = "/*snapshot execution*/ SELECT CONSTRAINT_SCHEMA,CONSTRAINT_TABLE, CONSTRAINT_TYPE, CONSTRAINT_NAME FROM SYS.";
    private String sqlConstraintsSchema;
    private String sqlProceduresAll = "/*snapshot execution*/ SELECT SCRIPT_SCHEMA, SCRIPT_NAME FROM SYS.";
    private String sqlProcedureSchema;
    private static final String SQL_TABLES_ALL = "/*snapshot execution*/ SELECT table_schem,table_name as column_table,table_type from \"$ODBCJDBC\".ALL_TABLES WHERE TABLE_NAME = '%s' AND TABLE_TYPE = '%s'";
    private static final String SQL_TABLES_SCHEMA = "/*snapshot execution*/ SELECT table_schem,table_name as column_table,table_type from \"$ODBCJDBC\".ALL_TABLES WHERE TABLE_SCHEM = '%s' AND TABLE_NAME LIKE '%s%' AND TABLE_TYPE = '%s'";
    private static final String SQL_COLS_SCHEMA = "/*snapshot execution*/ SELECT TABLE_SCHEM,TABLE_NAME as column_table,COLUMN_NAME from \"$ODBCJDBC\".ALL_COLUMNS WHERE TABLE_SCHEM like '%s' and COLUMN_NAME LIKE '%s%%'";


    private ExasolDataSource dataSource;


    // -----------------
    // Constructors
    // -----------------
    public ExasolStructureAssistant(ExasolDataSource dataSource) {
        this.dataSource = dataSource;
        this.sqlConstraintsAll = sqlConstraintsAll + dataSource.getTablePrefix(ExasolSysTablePrefix.ALL) + "_CONSTRAINTS WHERE CONSTRAINT_TYPE <> 'NOT NULL' "
        		+ " AND CONSTRAINT_NAME like '%s' AND CONSTRAINT_TYPE = '%s'";
        this.sqlConstraintsSchema = sqlConstraintsAll + " AND CONSTRAINT_SCHEMA = '%s'";
        this.sqlProceduresAll = sqlProceduresAll + dataSource.getTablePrefix(ExasolSysTablePrefix.ALL) + "_SCRIPTS WHERE SCRIPT_NAME like '%s'";
        this.sqlProcedureSchema = sqlProceduresAll + " AND SCRIPT_SCHEMA = '%s'";
    }


    // -----------------
    // Method Interface
    // -----------------
    @Override
    public DBSObjectType[] getSupportedObjectTypes() {
        return SUPP_OBJ_TYPES;
    }

    @Override
    public DBSObjectType[] getSearchObjectTypes() {
        return getSupportedObjectTypes();
    }


    @Override
    public DBSObjectType[] getHyperlinkObjectTypes() {
        return HYPER_LINKS_TYPES;
    }


    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes() {
        return AUTOC_OBJ_TYPES;
    }


    
    @NotNull
    @Override
    protected void findObjectsByMask(ExasolExecutionContext executionContext, JDBCSession session, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, boolean caseSensitive, boolean globalSearch, int maxResults, List<DBSObjectReference> references) throws DBException, SQLException
    {
        LOG.debug("Search Mask:" + objectNameMask + " Object Type:" + objectType.getTypeName());

        ExasolSchema schema = parentObject instanceof ExasolSchema ? (ExasolSchema) parentObject : null;
        if (schema == null && !globalSearch) {
            schema = executionContext.getContextDefaults().getDefaultSchema();
        }
        
        if (objectType == ExasolObjectType.TABLE) {
            findTableObjectByName(session, schema, objectNameMask, maxResults, references, "TABLE");
        } else if (objectType == ExasolObjectType.VIEW) {
            findTableObjectByName(session, schema, objectNameMask, maxResults, references, "VIEW");
        } else if (objectType == ExasolObjectType.FOREIGNKEY) {
            findConstraintsByMask(session, schema, objectNameMask, maxResults, references, "FOREIGN KEY");
        } else if (objectType == ExasolObjectType.PRIMARYKEY) {
            findConstraintsByMask(session, schema, objectNameMask, maxResults, references, "PRIMARY KEY");
        } else if (objectType == ExasolObjectType.SCRIPT) {
            findProceduresByMask(session, schema, objectNameMask, maxResults, references);
        } else if (objectType == ExasolObjectType.COLUMN) {
            findTableColumnsByMask(session, schema, objectNameMask, maxResults, references);
        }

    }

	private void findTableColumnsByMask(JDBCSession session, ExasolSchema schema, String objectNameMask, int maxResults,
			List<DBSObjectReference> references) throws SQLException, DBException {
    	DBRProgressMonitor monitor = session.getProgressMonitor();
    	
    	//don't use parameter marks because of performance
    	try (JDBCStatement dbstat = session.createStatement())
    	{
    		try (JDBCResultSet dbResult = dbstat.executeQuery(
    					String.format(SQL_COLS_SCHEMA, (schema == null ? "%" : ExasolUtils.quoteString(schema.getName())),ExasolUtils.quoteString(objectNameMask))
    				)
    			)
    		{
    			int num = maxResults;
    			while (dbResult.next() && num-- > 0)
    			{
    				if (monitor.isCanceled()) {
    					break;
    				}
    				final String schemaName = JDBCUtils.safeGetString(dbResult, "TABLE_SCHEM");
    				final String tableName  = JDBCUtils.safeGetString(dbResult, "COLUMN_TABLE");
    				final String columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
    				references.add(new AbstractObjectReference(columnName, dataSource.getSchema(monitor, schemaName), null, ExasolTableBase.class, RelationalObjectType.TYPE_TABLE_COLUMN) {
						
						@Override
						public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
							ExasolSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
							if (tableSchema == null)
							{
								throw new DBException("Table schema '" + schemaName + "' not found");
							}
							ExasolTable table = tableSchema.getTableCache().getObject(monitor, tableSchema, tableName);
							if (table == null) {
								ExasolView view = tableSchema.getViewCache().getObject(monitor, tableSchema, tableName);
								if (view == null)
									throw new DBException("nor Table or view with name '" + tableName + "'  found in schema '" + schemaName + "'");
								return view;
							}
							return table;
						}
					});
    			}
    		}
    	}
		
	}


	private void findProceduresByMask(JDBCSession session, ExasolSchema schema, String objectNameMask, int maxResults,
			List<DBSObjectReference> references) throws SQLException, DBException {
    	DBRProgressMonitor monitor = session.getProgressMonitor();
    	//don't use parameter marks because of performance
    	String sql = "";
    	if (schema == null)
    	{
    		sql = String.format(sqlProceduresAll, ExasolUtils.quoteString(objectNameMask));
    	} else {
    		sql = String.format(sqlProcedureSchema, ExasolUtils.quoteString(schema.getName()), ExasolUtils.quoteString(objectNameMask));
    	}
    	try (JDBCStatement dbstat = session.createStatement())
    	{
    		try (JDBCResultSet dbResult = dbstat.executeQuery(sql))
    		{
    			int num = maxResults;
    			while (dbResult.next() && num-- > 0)
    			{
    				if (monitor.isCanceled()) {
    					break;
    				}
    				final String schemaName = JDBCUtils.safeGetString(dbResult, "SCRIPT_SCHEMA");
    				final String scriptName       = JDBCUtils.safeGetString(dbResult, "SCRIPT_NAME");
    				
    				references.add(new AbstractObjectReference(scriptName, dataSource.getSchema(monitor, schemaName), null, ExasolScript.class, RelationalObjectType.TYPE_PROCEDURE) {
						
						@Override
						public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
							ExasolSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
							if (tableSchema == null)
							{
								throw new DBException("Table schema '" + schemaName + "' not found");
							}
							
							ExasolScript script = tableSchema.scriptCache.getObject(monitor, tableSchema, scriptName);
							
							if (script == null) {
								throw new DBException("Script '" + script + "'  not found in schema '" + schemaName + "'");
							}
							return script;
						}
					});
    			}
    		}
    	}
	}


	private void findConstraintsByMask(JDBCSession session, ExasolSchema schema, String objectNameMask, int maxResults,
			List<DBSObjectReference> references, String constType) throws SQLException, DBException {
    	DBRProgressMonitor monitor = session.getProgressMonitor();
    	//don't use parameter marks because of performance
    	String sql = "";
    	if (schema == null)
    	{
    		sql = String.format(sqlConstraintsAll, ExasolUtils.quoteString(objectNameMask), constType);
    	} else {
    		sql = String.format(sqlConstraintsSchema, ExasolUtils.quoteString(schema.getName()), constType, ExasolUtils.quoteString(objectNameMask));
    	}
    	try (JDBCStatement dbstat = session.createStatement())
    	{
    		try (JDBCResultSet dbResult = dbstat.executeQuery(sql))
    		{
    			int num = maxResults;
    			while (dbResult.next() && num-- > 0)
    			{
    				if (monitor.isCanceled()) {
    					break;
    				}
    				final String schemaName = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_SCHEMA");
    				final String tableName  = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TABLE");
    				final String constName       = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME");
    				final Class<?> classType;
    				
    				if (constType.equals("PRIMARY KEY"))
    				{
    					classType = ExasolTableUniqueKey.class;
    				} else if (constType.equals("FOREIGN KEY"))
    				{
    					classType = ExasolTableForeignKey.class;
    				} else {
    					throw new DBException("Unkown constraint type" + constType);
    				}
    				
    				references.add(new AbstractObjectReference(constName, dataSource.getSchema(monitor, schemaName), null, classType, RelationalObjectType.TYPE_CONSTRAINT) {
						
						@Override
						public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
							ExasolSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
							if (tableSchema == null)
							{
								throw new DBException("Table schema '" + schemaName + "' not found");
							}
							ExasolTable table = tableSchema.getTable(monitor, tableName);
							
							if (table == null)
							{
								throw new DBException("Table '" + tableName + "' not found in schema  '" + schemaName + "' not found");
							}
							if (classType.equals(ExasolTableForeignKey.class)) {
								ExasolTableForeignKey foreignKey = (ExasolTableForeignKey) table.getAssociation(monitor, constName);
								if (foreignKey == null)
									throw new DBException("Foreign Key  '" + constName + "' for Table '" + tableName + "' not found in schema '" + schemaName + "'");
								return foreignKey;
							} else  {
								ExasolTableUniqueKey primaryKey = table.getConstraint(monitor, constName);
								if (primaryKey == null) 
									throw new DBException("Primary Key '" + constName + "' for Table '" + tableName + "' not found in schema '" + schemaName + "'");
								return primaryKey;
							}
						}
					});
    			}
    		}
    	}				

	}

    private void findTableObjectByName(JDBCSession session, ExasolSchema schema, String objectNameMask, int maxResults,
			List<DBSObjectReference> references, String type) throws SQLException, DBException {
    	DBRProgressMonitor monitor = session.getProgressMonitor();
    	//don't use parameter marks because of performance
    	
    	String sql = "";
    	if (schema == null)
    	{
    		sql = String.format(SQL_TABLES_ALL, ExasolUtils.quoteString(objectNameMask), type);
    	} else {
    		sql = String.format(SQL_TABLES_SCHEMA, ExasolUtils.quoteString(schema.getName()), ExasolUtils.quoteString(objectNameMask), type);
    	}
    	try (JDBCStatement dbstat = session.createStatement())
    	{
    		try (JDBCResultSet dbResult = dbstat.executeQuery(sql))
    		{
    			int num = maxResults;
    			while (dbResult.next() && num-- > 0)
    			{
    				if (monitor.isCanceled()) {
    					break;
    				}
    				final String schemaName = JDBCUtils.safeGetString(dbResult, "TABLE_SCHEM");
    				final String tableName  = JDBCUtils.safeGetString(dbResult, "COLUMN_TABLE");
    				references.add(new AbstractObjectReference(tableName, dataSource.getSchema(monitor, schemaName), null, ExasolTableBase.class, RelationalObjectType.TYPE_TABLE_COLUMN) {
						
						@Override
						public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
							ExasolSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
							if (tableSchema == null)
							{
								throw new DBException("Table schema '" + schemaName + "' not found");
							}
							if (type == "VIEW") {
								ExasolView view = tableSchema.getViewCache().getObject(monitor, tableSchema, tableName);
								if (view == null)
									throw new DBException("View '" + tableName + "' not found in schema '" + schemaName + "'");
								return view;
							} else if (type == "TABLE") {
								ExasolTable table = tableSchema.getTableCache().getObject(monitor, tableSchema, tableName);
								if (table == null) 
									throw new DBException("Table '" + tableName + "' not found in schema '" + schemaName + "'");
								return table;
							} else {
								throw new DBException("Object type " + type + " unknown");
							}
						}
					});
    			}
    		}
    	}				
	}
 

	@Override
	protected JDBCDataSource getDataSource() {
		return this.dataSource;
	}


}

