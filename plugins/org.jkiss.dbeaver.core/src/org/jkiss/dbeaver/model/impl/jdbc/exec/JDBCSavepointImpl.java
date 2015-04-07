/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;

import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Savepoint
 */
public class JDBCSavepointImpl implements DBCSavepoint, Savepoint {

    static final Log log = Log.getLog(JDBCSavepointImpl.class);

    private JDBCExecutionContext context;
    private Savepoint original;

    public JDBCSavepointImpl(JDBCExecutionContext context, Savepoint savepoint)
    {
        this.context = context;
        this.original = savepoint;
    }

    @Override
    public int getId()
    {
        try {
            return original.getSavepointId();
        }
        catch (SQLException e) {
            log.error(e);
            return 0;
        }
    }

    @Override
    public String getName()
    {
        try {
            return original.getSavepointName();
        }
        catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public DBCExecutionContext getContext()
    {
        return context;
    }

    @Override
    public int getSavepointId()
        throws SQLException
    {
        return original.getSavepointId();
    }

    @Override
    public String getSavepointName()
        throws SQLException
    {
        return original.getSavepointName();
    }

    public Savepoint getOriginal()
    {
        return original;
    }
}
