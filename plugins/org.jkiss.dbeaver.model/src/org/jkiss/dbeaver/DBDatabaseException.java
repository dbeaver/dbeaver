/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * DBDatabaseException
 */
public class DBDatabaseException extends DBException {

    private final DBPDataSource dataSource;
    private final boolean hasMessage;

    public DBDatabaseException(String message) {
        super(message);
        this.dataSource = null;
        this.hasMessage = true;
    }

    public DBDatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.dataSource = null;
        this.hasMessage = message != null;
    }

    public DBDatabaseException(Throwable cause, DBPDataSource dataSource) {
        super(cause instanceof SQLException ? makeMessage((SQLException) cause) : cause.getMessage(), cause);
        this.dataSource = dataSource;
        this.hasMessage = false;
    }


    public DBDatabaseException(String message, Throwable cause, DBPDataSource dataSource) {
        super(message, cause);
        this.dataSource = dataSource;
        this.hasMessage = message != null;
    }

    @Nullable
    public DBPDataSource getDataSource() {
        if (dataSource != null) {
            return dataSource;
        }
        Throwable cause = getCause();
        if (cause instanceof DBDatabaseException dbe) {
            return dbe.getDataSource();
        }
        return null;
    }

    public boolean hasMessage() {
        return hasMessage;
    }

    private static String makeMessage(SQLException ex) {
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
