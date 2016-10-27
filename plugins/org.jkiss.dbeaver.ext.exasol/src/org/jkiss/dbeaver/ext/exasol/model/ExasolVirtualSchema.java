/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolSourceObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

public class ExasolVirtualSchema extends ExasolSchema implements ExasolSourceObject {
	
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
		return dataSource.getSchema(VoidProgressMonitor.INSTANCE, adapterScriptSchema) ;
	}

	@Property(viewable = true, order = 20)
	public ExasolScript getAdapterScriptName() throws DBException
	{
		return this.getAdapterScriptSchema().getProcedure(VoidProgressMonitor.INSTANCE, adapterScriptName);
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
		return virtualSchemaParameterCache.getAllObjects(VoidProgressMonitor.INSTANCE, this);
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor)
			throws DBException
	{
		return this.adapterNotes.replaceAll(",", ",\n");
	}

	@Override
	public ExasolSchema getSchema()
	{
		return this;
	}

	@Override
	public DBSObjectState getObjectState()
	{
		return DBSObjectState.NORMAL;
	}

	@Override
	public void refreshObjectState(DBRProgressMonitor monitor)
			throws DBCException
	{
	}

}
