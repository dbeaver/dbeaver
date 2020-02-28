/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
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
    private static final Log log = Log.getLog(OracleUserProfile.class);

    private String name;
    private List<ProfileResource> resources;

    public OracleUserProfile(OracleDataSource dataSource, ResultSet resultSet) {
        super(dataSource, resultSet != null);
        this.name = JDBCUtils.safeGetString(resultSet, "PROFILE");
    }

    @NotNull
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
        private static final Log log = Log.getLog(ProfileResource.class);

        private String type;
        private String limit;

        public ProfileResource(OracleUserProfile profile, ResultSet resultSet) {
            super(profile, JDBCUtils.safeGetString(resultSet, "RESOURCE_NAME"), true);
            this.type = JDBCUtils.safeGetString(resultSet, "RESOURCE_TYPE");
            this.limit = JDBCUtils.safeGetString(resultSet, "LIMIT");
        }

        @NotNull
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
