/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private static final long serialVersionUID = 1L;

    private final DBPDataSource dataSource;
    private final boolean hasMessage;

    public DBException(String message)
    {
        super(message);
        this.dataSource = null;
        this.hasMessage = true;
    }

    public DBException(String message, Throwable cause)
    {
        super(message, cause);
        this.dataSource = null;
        this.hasMessage = message != null;
    }

    public DBException(Throwable cause, DBPDataSource dataSource)
    {
        super(cause instanceof SQLException ? makeMessage((SQLException) cause) : cause.getMessage(), cause);
        this.dataSource = dataSource;
        this.hasMessage = false;
    }


    public DBException(String message, Throwable cause, DBPDataSource dataSource)
    {
        super(message, cause);
        this.dataSource = dataSource;
        this.hasMessage = message != null;
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

    public boolean hasMessage() {
        return hasMessage;
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
