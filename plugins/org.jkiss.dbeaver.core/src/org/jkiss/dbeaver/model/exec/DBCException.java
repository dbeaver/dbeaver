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

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;

import java.sql.SQLException;

/**
 * DBCException
 */
public class DBCException extends DBException
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -3658711845969963915L;

    public DBCException(String message)
    {
        super(message);
    }

    public DBCException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DBCException(Throwable cause, DBPDataSource dataSource)
    {
        super(cause, dataSource);
    }

    public DBCException(SQLException ex, DBPDataSource dataSource)
    {
        super(ex, dataSource);
    }

    public DBCException(String message, Throwable cause, DBPDataSource dataSource) {
        super(message, cause, dataSource);
    }
}
