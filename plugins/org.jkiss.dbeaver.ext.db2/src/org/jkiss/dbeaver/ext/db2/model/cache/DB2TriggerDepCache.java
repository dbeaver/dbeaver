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
import org.jkiss.dbeaver.ext.db2.model.DB2Trigger;
import org.jkiss.dbeaver.ext.db2.model.DB2TriggerDep;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Cache for dependencies on DB2 Triggers
 * 
 * @author Denis Forveille
 */
public class DB2TriggerDepCache extends JDBCObjectCache<DB2Trigger, DB2TriggerDep> {

    private static final String SQL = "SELECT * FROM SYSCAT.TRIGDEP WHERE TRIGSCHEMA = ? AND TRIGNAME = ? ORDER BY BSCHEMA,BNAME WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Trigger db2Trigger) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setString(1, db2Trigger.getParentObject().getName());
        dbStat.setString(2, db2Trigger.getName());
        return dbStat;
    }

    @Override
    protected DB2TriggerDep fetchObject(@NotNull JDBCSession session, @NotNull DB2Trigger db2Trigger, @NotNull JDBCResultSet resultSet)
        throws SQLException, DBException
    {
        return new DB2TriggerDep(session.getProgressMonitor(), db2Trigger, resultSet);
    }
}