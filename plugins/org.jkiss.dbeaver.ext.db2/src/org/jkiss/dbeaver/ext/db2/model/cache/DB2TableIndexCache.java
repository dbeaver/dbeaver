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
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Cache for DB2 Indexes for a given Table
 * 
 * @author Denis Forveille
 */
public class DB2TableIndexCache extends JDBCObjectCache<DB2TableBase, DB2Index> {

    private static final String SQL_INDS_TAB = "SELECT * FROM SYSCAT.INDEXES WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY INDNAME WITH UR";

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2TableBase db2Table)
        throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_INDS_TAB);
        dbStat.setString(1, db2Table.getSchema().getName());
        dbStat.setString(2, db2Table.getName());
        return dbStat;
    }
 
    @Override
    protected DB2Index fetchObject(@NotNull JDBCSession session, @NotNull DB2TableBase db2Table, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {

        // Lookup for indexes in right cache..
        String indexSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "INDSCHEMA");
        String indexName = JDBCUtils.safeGetStringTrimmed(dbResult, "INDNAME");

        return DB2Utils.findIndexBySchemaNameAndName(session.getProgressMonitor(), db2Table.getDataSource(), indexSchemaName,
            indexName);
    }
}
