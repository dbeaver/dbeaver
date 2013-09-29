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
import java.util.ArrayList;
import java.util.Collection;

/**
 * DB2 Super class for Users and Groups
 * 
 * @author Denis Forveille
 */
public abstract class DB2UserBase extends DB2GlobalObject implements DBAUser, DBPRefreshableObject {

    private final DB2UserAuthCache userAuthCache = new DB2UserAuthCache();

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
    // Business Contract
    // -----------------

    public abstract DB2AuthIDType getType();

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        userAuthCache.clearCache();
        return true;
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2UserAuthBase> getUserAuths(DBRProgressMonitor monitor) throws DBException
    {
        return userAuthCache.getObjects(monitor, this);
    }

    // DF: all of those could probably cached also...

    @Association
    public Collection<DB2UserAuthTable> getTablesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2UserAuthTable> listTablesAuths = new ArrayList<DB2UserAuthTable>();
        for (DB2UserAuthBase db2UserAuth : userAuthCache.getObjects(monitor, this)) {
            if (db2UserAuth instanceof DB2UserAuthTable) {
                listTablesAuths.add((DB2UserAuthTable) db2UserAuth);
            }

        }
        return listTablesAuths;
    }

    @Association
    public Collection<DB2UserAuthView> getViewsAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2UserAuthView> listViewsAuths = new ArrayList<DB2UserAuthView>();
        for (DB2UserAuthBase db2UserAuth : userAuthCache.getObjects(monitor, this)) {
            if (db2UserAuth instanceof DB2UserAuthView) {
                listViewsAuths.add((DB2UserAuthView) db2UserAuth);
            }

        }
        return listViewsAuths;
    }

    @Association
    public Collection<DB2UserAuthIndex> getIndexesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2UserAuthIndex> listIndexesAuths = new ArrayList<DB2UserAuthIndex>();
        for (DB2UserAuthBase db2UserAuth : userAuthCache.getObjects(monitor, this)) {
            if (db2UserAuth instanceof DB2UserAuthIndex) {
                listIndexesAuths.add((DB2UserAuthIndex) db2UserAuth);
            }

        }
        return listIndexesAuths;
    }

    @Association
    public Collection<DB2UserAuthSequence> getSequencesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2UserAuthSequence> listSequenceAuths = new ArrayList<DB2UserAuthSequence>();
        for (DB2UserAuthBase db2UserAuth : userAuthCache.getObjects(monitor, this)) {
            if (db2UserAuth instanceof DB2UserAuthSequence) {
                listSequenceAuths.add((DB2UserAuthSequence) db2UserAuth);
            }

        }
        return listSequenceAuths;
    }

    @Association
    public Collection<DB2UserAuthTablespace> getTablespacesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2UserAuthTablespace> listTablespacesAuths = new ArrayList<DB2UserAuthTablespace>();
        for (DB2UserAuthBase db2UserAuth : userAuthCache.getObjects(monitor, this)) {
            if (db2UserAuth instanceof DB2UserAuthTablespace) {
                listTablespacesAuths.add((DB2UserAuthTablespace) db2UserAuth);
            }

        }
        return listTablespacesAuths;
    }

    @Association
    public Collection<DB2UserAuthSchema> getSchemasAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2UserAuthSchema> listSchemasAuths = new ArrayList<DB2UserAuthSchema>();
        for (DB2UserAuthBase db2UserAuth : userAuthCache.getObjects(monitor, this)) {
            if (db2UserAuth instanceof DB2UserAuthSchema) {
                listSchemasAuths.add((DB2UserAuthSchema) db2UserAuth);
            }

        }
        return listSchemasAuths;
    }

    @Association
    public Collection<DB2UserAuthPackage> getPackagesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2UserAuthPackage> listPackagesAuths = new ArrayList<DB2UserAuthPackage>();
        for (DB2UserAuthBase db2UserAuth : userAuthCache.getObjects(monitor, this)) {
            if (db2UserAuth instanceof DB2UserAuthPackage) {
                listPackagesAuths.add((DB2UserAuthPackage) db2UserAuth);
            }

        }
        return listPackagesAuths;
    }

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
