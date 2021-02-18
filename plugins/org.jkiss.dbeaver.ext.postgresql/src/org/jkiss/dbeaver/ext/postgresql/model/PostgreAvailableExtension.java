/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPSystemInfoObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

public class PostgreAvailableExtension implements PostgreObject, DBPSystemInfoObject {
    
    private final PostgreDatabase database;
    private final String name;
    private final long oid;
    private final String description;
    private final String version;
    private final String installed_version;
    private final boolean installed;
    
    public  PostgreAvailableExtension(PostgreDatabase database,ResultSet dbResult)
    
        {
            this.database = database;
            this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
            this.name = JDBCUtils.safeGetString(dbResult, "name");
            this.version = JDBCUtils.safeGetString(dbResult, "default_version");
            this.installed_version = JDBCUtils.safeGetString(dbResult, "installed_version");
            installed = installed_version != null;
            this.description = JDBCUtils.safeGetString(dbResult, "comment");
        }
    
     @Override
    public DBSObject getParentObject() {
        return null;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }
    
    @Property(viewable = true, order = 2)
    public String getVersion() {
        return version;
    }
    
    @Property(viewable = true, order = 3)
    public String getInstalledVersion() {
        return installed_version;
    }

    @Override
    @Property(viewable = true, order = 4)
    public String getDescription() {
        return description;
    }

     @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

    public boolean isInstalled() {
        return installed;
    }
    
    

}
