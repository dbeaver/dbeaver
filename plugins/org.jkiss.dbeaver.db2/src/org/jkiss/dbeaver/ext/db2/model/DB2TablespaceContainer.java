/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * DB2 Tablespace Container
 * 
 * @author Denis Forveille
 */
public class DB2TablespaceContainer extends DB2Object<DB2Tablespace> {

    private final DB2Tablespace tablespace;

    private Long containerId;
    private String containerType;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2TablespaceContainer(DB2Tablespace tablespace, ResultSet dbResult)
    {
        super(tablespace, JDBCUtils.safeGetString(dbResult, "CONTAINER_NAME"), true);

        this.tablespace = tablespace;

        this.containerId = JDBCUtils.safeGetLong(dbResult, "CONTAINER_ID");
        this.containerType = JDBCUtils.safeGetString(dbResult, "CONTAINER_TYPE");
    }

    public DB2Tablespace getTablespace()
    {
        return tablespace;
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public Long getContainerId()
    {
        return containerId;
    }

    public void setContainerId(Long containerId)
    {
        this.containerId = containerId;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getContainerType()
    {
        return containerType;
    }

    public void setContainerType(String containerType)
    {
        this.containerType = containerType;
    }

}
