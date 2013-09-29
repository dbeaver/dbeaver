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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

/**
 * DB2 Super class for Users, Groups and Roles (=Grantees)
 * 
 * @author Denis Forveille
 */
public abstract class DB2Grantee extends DB2GlobalObject implements DBPRefreshableObject {

    private final DB2AuthCache authCache = new DB2AuthCache();
    private final DB2GranteeRoleCache roleCache = new DB2GranteeRoleCache();

    private String name;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2Grantee(DBRProgressMonitor monitor, DB2DataSource dataSource, ResultSet resultSet)
    {
        super(dataSource, true);
        this.name = JDBCUtils.safeGetString(resultSet, "GRANTEE");
    }

    // -----------------
    // Business Contract
    // -----------------

    public abstract DB2AuthIDType getType();

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        authCache.clearCache();
        roleCache.clearCache();
        return true;
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2RoleAuth> getRoles(DBRProgressMonitor monitor) throws DBException
    {
        return roleCache.getObjects(monitor, this);
    }

    // DF: all of those could probably cached also...

    @Association
    public Collection<DB2AuthTable> getTablesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2AuthTable> listTablesAuths = new ArrayList<DB2AuthTable>();
        for (DB2AuthBase db2Auth : authCache.getObjects(monitor, this)) {
            if (db2Auth instanceof DB2AuthTable) {
                listTablesAuths.add((DB2AuthTable) db2Auth);
            }

        }
        return listTablesAuths;
    }

    @Association
    public Collection<DB2AuthView> getViewsAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2AuthView> listViewsAuths = new ArrayList<DB2AuthView>();
        for (DB2AuthBase db2Auth : authCache.getObjects(monitor, this)) {
            if (db2Auth instanceof DB2AuthView) {
                listViewsAuths.add((DB2AuthView) db2Auth);
            }

        }
        return listViewsAuths;
    }

    @Association
    public Collection<DB2AuthIndex> getIndexesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2AuthIndex> listIndexesAuths = new ArrayList<DB2AuthIndex>();
        for (DB2AuthBase db2Auth : authCache.getObjects(monitor, this)) {
            if (db2Auth instanceof DB2AuthIndex) {
                listIndexesAuths.add((DB2AuthIndex) db2Auth);
            }

        }
        return listIndexesAuths;
    }

    @Association
    public Collection<DB2AuthSequence> getSequencesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2AuthSequence> listSequenceAuths = new ArrayList<DB2AuthSequence>();
        for (DB2AuthBase db2Auth : authCache.getObjects(monitor, this)) {
            if (db2Auth instanceof DB2AuthSequence) {
                listSequenceAuths.add((DB2AuthSequence) db2Auth);
            }

        }
        return listSequenceAuths;
    }

    @Association
    public Collection<DB2AuthTablespace> getTablespacesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2AuthTablespace> listTablespacesAuths = new ArrayList<DB2AuthTablespace>();
        for (DB2AuthBase db2Auth : authCache.getObjects(monitor, this)) {
            if (db2Auth instanceof DB2AuthTablespace) {
                listTablespacesAuths.add((DB2AuthTablespace) db2Auth);
            }

        }
        return listTablespacesAuths;
    }

    @Association
    public Collection<DB2AuthSchema> getSchemasAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2AuthSchema> listSchemasAuths = new ArrayList<DB2AuthSchema>();
        for (DB2AuthBase db2Auth : authCache.getObjects(monitor, this)) {
            if (db2Auth instanceof DB2AuthSchema) {
                listSchemasAuths.add((DB2AuthSchema) db2Auth);
            }

        }
        return listSchemasAuths;
    }

    @Association
    public Collection<DB2AuthPackage> getPackagesAuths(DBRProgressMonitor monitor) throws DBException
    {
        Collection<DB2AuthPackage> listPackagesAuths = new ArrayList<DB2AuthPackage>();
        for (DB2AuthBase db2Auth : authCache.getObjects(monitor, this)) {
            if (db2Auth instanceof DB2AuthPackage) {
                listPackagesAuths.add((DB2AuthPackage) db2Auth);
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
