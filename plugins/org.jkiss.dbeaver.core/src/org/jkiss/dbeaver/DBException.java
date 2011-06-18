/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver;

import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.SQLUtils;

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
        StringBuilder msg = new StringBuilder("SQL Error");
        if (ex.getErrorCode() > 0) {
            msg.append(" [").append(ex.getErrorCode()).append("]");
        }
        if (!CommonUtils.isEmpty(ex.getSQLState())) {
            msg.append(" [").append(ex.getSQLState()).append("]");
        }
        if (!CommonUtils.isEmpty(ex.getMessage())) {
            msg.append(": ").append(SQLUtils.stripTransformations(ex.getMessage()));
        }
        if (ex.getNextException() != null) {
            msg.append("\n").append(makeMessage(ex.getNextException()));
        }
        return msg.toString();
    }

}
