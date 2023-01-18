/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
