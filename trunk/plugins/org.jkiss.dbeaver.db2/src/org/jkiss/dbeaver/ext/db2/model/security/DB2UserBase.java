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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2GlobalObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * DB2 Super class for Users and Groups
 * 
 * @author Denis Forveille
 */
public abstract class DB2UserBase extends DB2GlobalObject implements DBAUser, DBPRefreshableObject {

    private final static DB2UserAuthCache userAuthCache = new DB2UserAuthCache();

    private String name;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2UserBase(DB2DataSource dataSource, ResultSet resultSet)
    {
        super(dataSource, true);
        this.name = JDBCUtils.safeGetString(resultSet, "AUTHID");
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2UserAuth> getUserAuths(DBRProgressMonitor monitor) throws DBException
    {
        return userAuthCache.getObjects(monitor, this);
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        userAuthCache.clearCache();
        return true;
    }

    public abstract DB2AuthIDType getType();

    // -----------------
    // Properties
    // -----------------
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

}
