package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

public class ExasolVirtualSchemaParameter implements DBSObject {
	
	private String name;
	private String value;
	private ExasolVirtualSchema schema;
	private Boolean isPersisted;

	public ExasolVirtualSchemaParameter(ExasolVirtualSchema schema, ResultSet dbResult)
	{
		this.schema = schema;
		this.name = JDBCUtils.safeGetString(dbResult, "PROPERTY_NAME");
		this.value = JDBCUtils.safeGetString(dbResult, "PROPERTY_VALUE");
		isPersisted = true;
	}

	@Override
	@Property(viewable = true, order = 10)
	public String getName()
	{
		return name;
	}
	
	@Property(viewable = true, order = 20)
	public String getValue()
	{
		return value;
	}

	@Override
	public boolean isPersisted()
	{
		return isPersisted;
	}

	@Override
	public String getDescription()
	{
		return null;
	}

	@Override
	public ExasolVirtualSchema getParentObject()
	{
		return schema;
	}

	@Override
	public ExasolDataSource getDataSource()
	{
		return schema.getDataSource();
	}

}
