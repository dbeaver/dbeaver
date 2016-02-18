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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * DB2 Group
 * 
 * @author Denis Forveille
 */
public class DB2Group extends DB2Grantee implements DBAUser {

    // -----------------------
    // Constructors
    // -----------------------

    public DB2Group(DBRProgressMonitor monitor, DB2DataSource dataSource, ResultSet resultSet)
    {
        super(monitor, dataSource, resultSet, "GRANTEE");
    }

    // -----------------------
    // Business Contract
    // -----------------------

    @Override
    public DB2AuthIDType getType()
    {
        return DB2AuthIDType.G;
    }

}
