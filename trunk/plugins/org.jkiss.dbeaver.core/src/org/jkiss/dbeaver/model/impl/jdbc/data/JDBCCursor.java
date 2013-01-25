/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;

import java.sql.ResultSet;

/**
 * Result set holder
 */
public class JDBCCursor extends JDBCResultSetImpl implements DBDCursor {

    static final Log log = LogFactory.getLog(JDBCCursor.class);

    public JDBCCursor(JDBCExecutionContext context, ResultSet original, String description)
    {
        super(context, original, description);
    }

    @Override
    public boolean isNull()
    {
        return false;
    }

    @Override
    public DBDValue makeNull()
    {
        throw new IllegalStateException(CoreMessages.model_jdbc_cant_create_null_cursor);
    }

    @Override
    public void release()
    {
        super.close();
    }

    @Override
    public String toString()
    {
        return getStatement().getDescription();
    }

}
