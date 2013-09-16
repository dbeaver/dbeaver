/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.info;

import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DB2 Top SQL
 *
 * @author Denis Forveille
 */
public class DB2TopSQL implements DBSObject {

    private DB2DataSource dataSource;

    private Long numExecutions;
    private Long averageExecutionTime;
    private Long statementSorts;
    private Long sortsPerExecution;
    private String text;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2TopSQL(DB2DataSource dataSource, ResultSet dbResult) throws SQLException
    {
        this.dataSource = dataSource;

        this.numExecutions = JDBCUtils.safeGetLong(dbResult, "NUM_EXECUTIONS");
        this.averageExecutionTime = JDBCUtils.safeGetLong(dbResult, "AVERAGE_EXECUTION_TIME_S");
        this.statementSorts = JDBCUtils.safeGetLong(dbResult, "STMT_SORTS");
        this.sortsPerExecution = JDBCUtils.safeGetLong(dbResult, "SORTS_PER_EXECUTION");

        Clob textClob = dbResult.getClob("STMT_TEXT");
        try {
            this.text = (textClob == null ? null : ContentUtils.readToString(textClob.getCharacterStream()));
        } catch (IOException e) {
            throw new SQLException("Can't read text CLOB", e);
        }
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @Override
    public boolean isPersisted()
    {
        return false;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(hidden = true)
    public String getName()
    {
        return null;
    }

    @Property(viewable = true, editable = false, order = 1)
    public Long getNumExecutions()
    {
        return numExecutions;
    }

    @Property(viewable = true, editable = false, order = 2)
    public Long getAverageExecutionTime()
    {
        return averageExecutionTime;
    }

    @Property(viewable = true, editable = false, order = 3)
    public Long getStatementSorts()
    {
        return statementSorts;
    }

    @Property(viewable = true, editable = false, order = 4)
    public Long getSortsPerExecution()
    {
        return sortsPerExecution;
    }

    @Property(viewable = true, editable = false, order = 5)
    // TODO DF: What valueHandler to display CLobs?
    public String getText()
    {
        return text;
    }
}
