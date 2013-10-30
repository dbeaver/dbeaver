/*
 * Copyright (C) 2010-2013 Serge Rieder
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

package org.jkiss.dbeaver;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * DBException
 */
public class DBException extends Exception
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DBException()
    {
    }

    public DBException(String message)
    {
        super(message);
    }

    public DBException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DBException(Throwable cause)
    {
        super(cause instanceof SQLException ? makeMessage((SQLException) cause) : cause.getMessage(), cause);
    }

    public DBException(SQLException ex)
    {
        super(makeMessage(ex), ex);
    }

    public int getErrorCode()
    {
        if (getCause() instanceof SQLException) {
            return ((SQLException)getCause()).getErrorCode();
        } else {
            return -1;
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
        StringBuilder msg = new StringBuilder(CoreMessages.common_error_sql);
        if (ex.getErrorCode() > 0) {
            msg.append(" [").append(ex.getErrorCode()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (!CommonUtils.isEmpty(ex.getSQLState())) {
            msg.append(" [").append(ex.getSQLState()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (!CommonUtils.isEmpty(ex.getMessage())) {
            msg.append(": ").append(SQLUtils.stripTransformations(ex.getMessage())); //$NON-NLS-1$
        }
        if (ex.getNextException() != null) {
            msg.append(ContentUtils.getDefaultLineSeparator()).append(makeMessage(ex.getNextException()));
        }
        return msg.toString();
    }

}
