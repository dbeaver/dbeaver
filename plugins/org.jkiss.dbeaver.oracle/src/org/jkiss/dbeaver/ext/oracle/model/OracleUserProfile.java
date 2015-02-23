/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * OracleUserProfile
 */
public class OracleUserProfile extends OracleGlobalObject
{
    static final Log log = Log.getLog(OracleUserProfile.class);

    private String name;
    private List<ProfileResource> resources;

    public OracleUserProfile(OracleDataSource dataSource, ResultSet resultSet) {
        super(dataSource, resultSet != null);
        this.name = JDBCUtils.safeGetString(resultSet, "PROFILE");
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Association
    public Collection<ProfileResource> getResources(DBRProgressMonitor monitor) throws DBException
    {
        if (resources == null) {
            getDataSource().profileCache.loadChildren(monitor, getDataSource(), this);
        }
        return resources;
    }

    boolean isResourcesCached()
    {
        return resources != null;
    }

    void setResources(List<ProfileResource> resources)
    {
        this.resources = resources;
    }

    /**
     * ProfileResource
     */
    public static class ProfileResource extends OracleObject<OracleUserProfile>
    {
        static final Log log = Log.getLog(ProfileResource.class);

        private String type;
        private String limit;

        public ProfileResource(OracleUserProfile profile, ResultSet resultSet) {
            super(profile, JDBCUtils.safeGetString(resultSet, "RESOURCE_NAME"), true);
            this.type = JDBCUtils.safeGetString(resultSet, "RESOURCE_TYPE");
            this.limit = JDBCUtils.safeGetString(resultSet, "LIMIT");
        }

        @Override
        @Property(viewable = true, order = 1)
        public String getName() {
            return super.getName();
        }

        @Property(viewable = true, order = 2)
        public String getType()
        {
            return type;
        }

        @Property(viewable = true, order = 3)
        public String getLimit()
        {
            return limit;
        }
    }

}
