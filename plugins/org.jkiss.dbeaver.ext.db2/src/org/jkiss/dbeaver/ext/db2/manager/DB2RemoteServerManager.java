/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2RemoteServer;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

/**
 * DB2 Federated Server Manager
 * 
 * @author Denis Forveille
 */
public class DB2RemoteServerManager extends DB2AbstractDropOnlyManager<DB2RemoteServer, DB2DataSource> {

    private static final String SQL_DROP = "DROP SERVER %s";

    @Override
    public String buildDropStatement(DB2RemoteServer db2RemoteServer)
    {
        String name = db2RemoteServer.getName();
        return String.format(SQL_DROP, name);
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2DataSource, DB2RemoteServer> getObjectsCache(DB2RemoteServer db2RemoteServer)
    {
        return db2RemoteServer.getDataSource().getRemoteServerCache();
    }
}
