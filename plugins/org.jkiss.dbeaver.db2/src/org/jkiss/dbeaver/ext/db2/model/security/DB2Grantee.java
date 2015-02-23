/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
import java.util.HashMap;
import java.util.Map;

/**
 * DB2 Super class for Users, Groups and Roles (=Grantees)
 * 
 * @author Denis Forveille
 */
public abstract class DB2Grantee extends DB2GlobalObject implements DBPRefreshableObject {

    private final DB2GranteeAuthCache authCache = new DB2GranteeAuthCache();
    private final DB2GranteeRoleCache roleCache = new DB2GranteeRoleCache();
    private final DB2GranteeDatabaseAuthCache databaseAuthCache = new DB2GranteeDatabaseAuthCache();

    private Map<Class<?>, Collection<? extends DB2AuthBase>> cachePerObject;

    private String name;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2Grantee(DBRProgressMonitor monitor, DB2DataSource dataSource, ResultSet resultSet, String keyColName)
    {
        super(dataSource, true);
        this.name = JDBCUtils.safeGetStringTrimmed(resultSet, keyColName);

        cachePerObject = new HashMap<Class<?>, Collection<? extends DB2AuthBase>>(12);
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
        databaseAuthCache.clearCache();

        cachePerObject.clear();

        return true;
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2AuthColumn> getColumnsAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthColumn.class);
    }

    @Association
    public Collection<DB2DatabaseAuth> getDatabaseAuths(DBRProgressMonitor monitor) throws DBException
    {
        return databaseAuthCache.getObjects(monitor, this);
    }

    @Association
    public Collection<DB2AuthUDF> getFunctionsAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthUDF.class);
    }

    @Association
    public Collection<DB2AuthIndex> getIndexesAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthIndex.class);
    }

    @Association
    public Collection<DB2AuthModule> getModulesAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthModule.class);
    }

    @Association
    public Collection<DB2AuthPackage> getPackagesAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthPackage.class);
    }

    @Association
    public Collection<DB2AuthProcedure> getProceduresAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthProcedure.class);
    }

    @Association
    public Collection<DB2RoleAuth> getRoles(DBRProgressMonitor monitor) throws DBException
    {
        return roleCache.getObjects(monitor, this);
    }

    @Association
    public Collection<DB2AuthSchema> getSchemasAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthSchema.class);
    }

    @Association
    public Collection<DB2AuthSequence> getSequencesAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthSequence.class);
    }

    @Association
    public Collection<DB2AuthTable> getTablesAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthTable.class);
    }

    @Association
    public Collection<DB2AuthTablespace> getTablespacesAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthTablespace.class);
    }

    @Association
    public Collection<DB2AuthVariable> getVariablesAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthVariable.class);
    }

    @Association
    public Collection<DB2AuthView> getViewsAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthView.class);
    }

    @Association
    public Collection<DB2AuthMaterializedQueryTable> getMQTAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthMaterializedQueryTable.class);
    }

    @Association
    public Collection<DB2AuthXMLSchema> getXMLSchemasAuths(DBRProgressMonitor monitor) throws DBException
    {
        return getAuths(monitor, DB2AuthXMLSchema.class);
    }

    // -----------------
    // Helper
    // -----------------

    // Filter
    @SuppressWarnings("unchecked")
    private <T extends DB2AuthBase> Collection<T> getAuths(DBRProgressMonitor monitor, Class<T> authClass) throws DBException
    {
        Collection<T> listAuths = (Collection<T>) cachePerObject.get(authClass.getClass());
        if (listAuths == null) {
            listAuths = new ArrayList<T>();
            for (DB2AuthBase db2Auth : authCache.getObjects(monitor, this)) {
                if (authClass.isInstance(db2Auth)) {
                    listAuths.add((T) db2Auth);
                }
            }
        }
        return listAuths;
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
