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
package org.jkiss.dbeaver.ext.exasol.manager.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class ExasolRole extends ExasolGrantee  implements DBARole  {


    private String name;
    private String description;
    private ExasolDataSource dataSource;
    private String priority;
    private Timestamp created;

    public ExasolRole(ExasolDataSource dataSource, ResultSet resultSet) {
    	super(dataSource, resultSet);
    	if (resultSet != null) {
	        this.name = JDBCUtils.safeGetString(resultSet, "ROLE_NAME");
	        this.description = JDBCUtils.safeGetStringTrimmed(resultSet, "ROLE_COMMENT");
	        this.priority = JDBCUtils.safeGetString(resultSet, "ROLE_PRIORITY");
	        this.dataSource = dataSource;
	        this.created = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
    	} else {
    		this.name = "New Role";
    	}
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 10)
    public String getDescription() {
        return description;
    }
    
    @Property(viewable = true, order = 20)
    public String getPriority()
    {
    	return this.priority;
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

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

}

	

