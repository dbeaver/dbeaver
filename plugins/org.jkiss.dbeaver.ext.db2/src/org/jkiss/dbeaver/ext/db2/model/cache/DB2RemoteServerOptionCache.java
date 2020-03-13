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
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2RemoteServer;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2RemoteServerOption;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Cache for DB2 Federated Remote Server Options
 * 
 * @author Denis Forveille
 */
public class DB2RemoteServerOptionCache extends JDBCObjectCache<DB2RemoteServer, DB2RemoteServerOption> {

    private static String SQL;

    static {
        StringBuilder sb = new StringBuilder(256);
        sb.append("SELECT *");
        sb.append("  FROM SYSCAT.SERVEROPTIONS");
        sb.append(" WHERE SERVERNAME = ?");// 1
        // DF: Strange. The following columns are supposed to be the PK
        // but in practoce, they are always null...
        // sb.append("   AND WRAPNAME = ?"); // 2
        // sb.append("   AND SERVERTYPE = ?");// 3
        // sb.append("   AND SERVERVERSION = ?");// 4
        sb.append(" ORDER BY OPTION");
        sb.append(" WITH UR");
        SQL = sb.toString();
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2RemoteServer remoteServer) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setString(1, remoteServer.getName());
        // dbStat.setString(2, remoteServer.getDb2Wrapper().getName());
        // dbStat.setString(3, remoteServer.getDataType());
        // dbStat.setString(4, remoteServer.getVersion());
        return dbStat;
    }

    @Override
    protected DB2RemoteServerOption fetchObject(@NotNull JDBCSession session, @NotNull DB2RemoteServer remoteServer, @NotNull JDBCResultSet resultSet)
        throws SQLException, DBException
    {
        return new DB2RemoteServerOption(remoteServer, resultSet);
    }
}