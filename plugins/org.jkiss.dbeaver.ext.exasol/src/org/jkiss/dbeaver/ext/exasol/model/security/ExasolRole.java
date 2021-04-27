/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.model.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class ExasolRole extends ExasolGrantee  implements DBARole, DBPNamedObject2  {


    private String name;
    private String description;
    private ExasolDataSource dataSource;
    private Timestamp created;

    public ExasolRole(ExasolDataSource dataSource, ResultSet resultSet) {
    	super(dataSource, resultSet);
    	if (resultSet != null) {
	        this.name = JDBCUtils.safeGetString(resultSet, "ROLE_NAME");
	        this.description = JDBCUtils.safeGetStringTrimmed(resultSet, "ROLE_COMMENT");
	        this.dataSource = dataSource;
	        this.created = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
    	} else {
    		this.name = "New Role";
    	}
    }

    public ExasolRole(ExasolDataSource dataSource, String name,
			String description)
	{
    	super(dataSource,false);
		this.dataSource = dataSource;
		this.name = name;
		this.description = description;
	}

	@NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, updatable=true, editable=true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }
    
    @Property(viewable = true, order = 20)
    public Timestamp getCreated()
    {
    	return this.created;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    
    public void setDescription(String description) {
    	this.description = description;
    }
    
    @Override
    public String toString()
    {
    	return "Role "+ getName();
    }

	@Override
	public void setName(String newName)
	{
		this.name = newName;
	}
	
		

}

	

