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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

public class ExasolVirtualSchema extends ExasolSchema  {
	
	private String adapterScriptSchema;
	private String adapterScriptName;
	private Timestamp lastRefresh;
	private String adapterNotes;
	private String refreshBy;
	private ExasolDataSource dataSource;
	private DBSObjectCache<ExasolVirtualSchema, ExasolVirtualSchemaParameter> virtualSchemaParameterCache;
	
	public ExasolVirtualSchema(ExasolDataSource exasolDataSource, ResultSet dbResult) throws DBException {
		super(exasolDataSource, dbResult);
		this.adapterNotes = JDBCUtils.safeGetString(dbResult, "ADAPTER_NOTES");
		this.lastRefresh = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REFRESH");
		this.refreshBy = JDBCUtils.safeGetString(dbResult, "LAST_REFRESH_BY");
		
		List<String> fqnAdapter = CommonUtils.splitString(JDBCUtils.safeGetString(dbResult, "ADAPTER_SCRIPT"),'.');
		adapterScriptSchema = fqnAdapter.get(0);
		adapterScriptName = fqnAdapter.get(1);
		
		this.dataSource = exasolDataSource;
		
		virtualSchemaParameterCache = new JDBCObjectSimpleCache<>(
				ExasolVirtualSchemaParameter.class, 
				"select\r\n" + 
				"	property_name,\r\n" + 
				"	property_value\r\n" + 
				"from\r\n" + 
				"	EXA_ALL_VIRTUAL_SCHEMA_PROPERTIES\r\n" + 
				"where\r\n" + 
				"	schema_name = ?\r\n" + 
				"order by\r\n" + 
				"	property_name\r\n" + 
				"", 
				super.getName()
				);
		
	}

	@Property(viewable = true, order = 10)
	public ExasolSchema getAdapterScriptSchema() throws DBException
	{
		return dataSource.getSchema(new VoidProgressMonitor(), adapterScriptSchema) ;
	}

	@Property(viewable = true, order = 20)
	public ExasolScript getAdapterScriptName() throws DBException
	{
		return this.getAdapterScriptSchema().getProcedure(new VoidProgressMonitor(), adapterScriptName);
	}

	@Property(viewable = true, order = 30)
	public Timestamp getLastRefresh()
	{
		return lastRefresh;
	}

	@Property(viewable = true, order = 40)
	public String getRefreshBy()
	{
		return refreshBy;
	}
	
	@Override
	public ExasolDataSource getDataSource()
	{
		return this.dataSource;
	}
	
	public Collection<ExasolVirtualSchemaParameter> getVirtualSchemaParameters() throws DBException
	{
		return virtualSchemaParameterCache.getAllObjects(new VoidProgressMonitor(), this);
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor)
			throws DBException
	{
		return this.adapterNotes.replaceAll(",", ",\n");
	}


	@Override
	public Boolean isPhysicalSchema()
	{
	    return false;
	}


	
	

}
