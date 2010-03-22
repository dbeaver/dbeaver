package org.jkiss.dbeaver;

import net.sf.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * DBException
 */
public class DBException extends Exception
{
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
        super(cause);
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
            msg.append(": ").append(ex.getMessage());
        }
        if (ex.getNextException() != null) {
            msg.append("\n").append(makeMessage(ex.getNextException()));
        }
        return msg.toString();
    }
}
