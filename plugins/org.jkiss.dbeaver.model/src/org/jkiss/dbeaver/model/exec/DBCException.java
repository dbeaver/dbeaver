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

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBCException
 */
public class DBCException extends DBDatabaseException
{
    private static final long serialVersionUID = 1L;

    private DBCExecutionContext executionContext;

    public DBCException(String message)
    {
        super(message);
    }

    public DBCException(String message, Throwable cause)
    {
        super(message, cause);
        if (cause instanceof DBCException) {
            this.executionContext = ((DBCException) cause).executionContext;
        }
    }

    public DBCException(Throwable cause, DBCExecutionContext executionContext)
    {
        super(cause, executionContext.getDataSource());
        this.executionContext = executionContext;
    }

    public DBCException(String message, Throwable cause, DBCExecutionContext executionContext) {
        super(message, cause, executionContext.getDataSource());
        this.executionContext = executionContext;
    }

    /**
     * Deprecated. Use constructor with execution context
     */
    protected DBCException(String message, Throwable cause, DBPDataSource dataSource) {
        super(message, cause, dataSource);
        if (cause instanceof DBCException) {
            this.executionContext = ((DBCException) cause).executionContext;
        }
    }


    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }
}
