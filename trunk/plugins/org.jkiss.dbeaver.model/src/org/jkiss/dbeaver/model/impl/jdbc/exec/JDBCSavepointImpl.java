/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.Log;
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
