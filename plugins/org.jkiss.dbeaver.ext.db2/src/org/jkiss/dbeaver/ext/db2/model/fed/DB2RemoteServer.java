/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2GlobalObject;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2RemoteServerOptionCache;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * DB2 Federated Remote Server
 * 
 * @author Denis Forveille
 */
public class DB2RemoteServer extends DB2GlobalObject implements DBPRefreshableObject {

    private final DB2RemoteServerOptionCache optionsCache = new DB2RemoteServerOptionCache();

    private String name;
    private DB2Wrapper db2Wrapper;
    private String type;
    private String version;
    private String remarks;

    // -----------------
    // Constructors
    // -----------------

    public DB2RemoteServer(DB2DataSource db2DataSource, ResultSet dbResult) throws DBException
    {
        super(db2DataSource, true);

        this.name = JDBCUtils.safeGetString(dbResult, "SERVERNAME");
        this.type = JDBCUtils.safeGetString(dbResult, "SERVERTYPE");
        this.version = JDBCUtils.safeGetString(dbResult, "SERVERVERSION");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        String db2WrapperName = JDBCUtils.safeGetString(dbResult, "WRAPNAME");
        if (db2WrapperName != null) {
            this.db2Wrapper = getDataSource().getWrapper(new VoidProgressMonitor(), db2WrapperName);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        optionsCache.clearCache();
        return this;
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2RemoteServerOption> getOptions(DBRProgressMonitor monitor) throws DBException
    {
        return optionsCache.getAllObjects(monitor, this);
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
    public DB2Wrapper getDb2Wrapper()
    {
        return db2Wrapper;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getType()
    {
        return type;
    }

    @Property(viewable = true, editable = false, order = 4)
    public String getVersion()
    {
        return version;
    }

    @Property(viewable = true, editable = false, length = PropertyLength.MULTILINE, order = 5)
    public String getRemarks()
    {
        return remarks;
    }

}
