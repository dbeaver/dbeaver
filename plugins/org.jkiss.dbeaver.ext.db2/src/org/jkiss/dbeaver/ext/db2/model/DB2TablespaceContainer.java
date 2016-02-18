/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
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
    private Long totalPages;
    private Long usablePages;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2TablespaceContainer(DB2Tablespace tablespace, ResultSet dbResult)
    {
        super(tablespace, JDBCUtils.safeGetString(dbResult, "CONTAINER_NAME"), true);

        this.tablespace = tablespace;

        this.containerId = JDBCUtils.safeGetLong(dbResult, "CONTAINER_ID");
        this.containerType = JDBCUtils.safeGetString(dbResult, "CONTAINER_TYPE");
        this.totalPages = JDBCUtils.safeGetLong(dbResult, "TOTAL_PAGES");
        this.usablePages = JDBCUtils.safeGetLong(dbResult, "USABLE_PAGES");
    }

    public DB2Tablespace getTablespace()
    {
        return tablespace;
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
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

    @Property(viewable = true, editable = false, order = 3)
    public String getContainerType()
    {
        return containerType;
    }

    @Property(viewable = true, editable = false, order = 4, category = DB2Constants.CAT_STATS)
    public Long getTotalPages()
    {
        return totalPages;
    }

    @Property(viewable = true, editable = false, order = 5, category = DB2Constants.CAT_STATS)
    public Long getUsablePages()
    {
        return usablePages;
    }

}
