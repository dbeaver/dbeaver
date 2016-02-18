/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2Trigger;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Cache for DB2 Triggers for a given Table
 * 
 * @author Denis Forveille
 */
public class DB2TableTriggerCache extends JDBCObjectCache<DB2Table, DB2Trigger> {

    private static final String SQL_TRIG_TAB = "SELECT * FROM SYSCAT.TRIGGERS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY TRIGNAME WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Table db2Table) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_TRIG_TAB);
        dbStat.setString(1, db2Table.getSchema().getName());
        dbStat.setString(2, db2Table.getName());
        return dbStat;
    }

    @Override
    protected DB2Trigger fetchObject(@NotNull JDBCSession session, @NotNull DB2Table db2Table, @NotNull JDBCResultSet dbResult) throws SQLException,
        DBException
    {

        // Lookup for trigger in right cache..
        String triggerSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGSCHEMA");
        String triggerName = JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGNAME");

        return DB2Utils.findTriggerBySchemaNameAndName(session.getProgressMonitor(), db2Table.getDataSource(), triggerSchemaName,
            triggerName);
    }
}
