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

package org.jkiss.dbeaver;

import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * DBException
 */
public class DBException extends Exception
{
    private final DBPDataSource dataSource;

    public DBException(String message)
    {
        super(message);
        this.dataSource = null;
    }

    public DBException(String message, Throwable cause)
    {
        super(message, cause);
        this.dataSource = null;
    }

    public DBException(Throwable cause, DBPDataSource dataSource)
    {
        super(cause instanceof SQLException ? makeMessage((SQLException) cause) : cause.getMessage(), cause);
        this.dataSource = dataSource;
    }


    public DBException(String message, Throwable cause, DBPDataSource dataSource)
    {
        super(message, cause);
        this.dataSource = dataSource;
    }

    public DBPDataSource getDataSource()
    {
        if (dataSource != null) {
            return dataSource;
        }
        Throwable cause = getCause();
        if (cause instanceof DBException) {
            return ((DBException) cause).getDataSource();
        }
        return null;
    }

    public int getErrorCode()
    {
        Throwable cause = getCause();
        if (cause instanceof SQLException) {
            return ((SQLException) cause).getErrorCode();
        } else if (cause instanceof DBException) {
            return ((DBException) cause).getErrorCode();
        } else {
            return -1;
        }
    }

    /**
     * SQL state or other standard error code.
     * For JDBC/SQL drivers it refers to SQL99 state or XOpen state
     */
    public String getDatabaseState()
    {
        Throwable cause = getCause();
        if (cause instanceof SQLException) {
            return ((SQLException) cause).getSQLState();
        } else if (cause instanceof DBException) {
            return ((DBException) cause).getDatabaseState();
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DBException) {
            if (obj == this) {
                return true;
            }
            Throwable ex1 = (Throwable)obj;
            Throwable ex2 = this;
            while (ex1 != null) {
                if (!CommonUtils.equalObjects(ex1.getMessage(), ex2.getMessage())) {
                    return false;
                }
                ex1 = ex1.getCause();
                ex2 = ex2.getCause();
                if ((ex1 == null && ex2 != null) || (ex2 == null && ex1 != null)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static String makeMessage(SQLException ex)
    {
        StringBuilder msg = new StringBuilder(ModelMessages.common_error_sql);
        if (ex.getErrorCode() > 0) {
            msg.append(" [").append(ex.getErrorCode()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (!CommonUtils.isEmpty(ex.getSQLState())) {
            msg.append(" [").append(ex.getSQLState()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (!CommonUtils.isEmpty(ex.getMessage())) {
            msg.append(": ").append(SQLUtils.stripTransformations(ex.getMessage())); //$NON-NLS-1$
        }
        return msg.toString();
    }

}
