/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;

import java.sql.SQLException;

/**
 * DBCException
 */
public class DBCException extends DBException
{
    private static final long serialVersionUID = 1L;

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
