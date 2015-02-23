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
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2Trigger;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache for DB2 Triggers for a given Table
 * 
 * @author Denis Forveille
 */
public class DB2TableTriggerCache extends JDBCObjectCache<DB2Table, DB2Trigger> {

    private static final String SQL_TRIG_TAB = "SELECT * FROM SYSCAT.TRIGGERS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY TRIGNAME WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Table db2Table) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL_TRIG_TAB);
        dbStat.setString(1, db2Table.getSchema().getName());
        dbStat.setString(2, db2Table.getName());
        return dbStat;
    }

    @Override
    protected DB2Trigger fetchObject(JDBCSession session, DB2Table db2Table, ResultSet dbResult) throws SQLException,
        DBException
    {

        // Lookup for trigger in right cache..
        String triggerSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGSCHEMA");
        String triggerName = JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGNAME");

        return DB2Utils.findTriggerBySchemaNameAndName(session.getProgressMonitor(), db2Table.getDataSource(), triggerSchemaName,
            triggerName);
    }
}
