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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
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
 * Cache for DB2 Triggers
 * 
 * @author Denis Forveille
 */
public class DB2TriggerCache extends JDBCObjectCache<DB2Schema, DB2Trigger> {

    private static final String SQL_TRIG_ALL = "SELECT * FROM SYSCAT.TRIGGERS WHERE TRIGSCHEMA = ? ORDER BY TRIGNAME WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Schema db2Schema) throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(SQL_TRIG_ALL);
        dbStat.setString(1, db2Schema.getName());
        return dbStat;
    }

    @Override
    protected DB2Trigger fetchObject(JDBCSession session, DB2Schema db2Schema, ResultSet dbResult) throws SQLException,
        DBException
    {

        // Look for related table
        String tableSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
        String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABNAME");
        DB2Table db2Table = DB2Utils.findTableBySchemaNameAndName(session.getProgressMonitor(), db2Schema.getDataSource(),
            tableSchemaName, tableName);

        return new DB2Trigger(session.getProgressMonitor(), db2Schema, db2Table, dbResult);
    }
}
