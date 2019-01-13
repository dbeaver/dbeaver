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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;

import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Savepoint
 */
public class JDBCSavepointImpl implements DBCSavepoint, Savepoint {

    private static final Log log = Log.getLog(JDBCSavepointImpl.class);

    private JDBCExecutionContext context;
    private Savepoint original;

    public JDBCSavepointImpl(JDBCExecutionContext context, Savepoint savepoint)
    {
        this.context = context;
        this.original = savepoint;
    }

    @Override
    public int getId()
    {
        try {
            return original.getSavepointId();
        }
        catch (SQLException e) {
            log.error(e);
            return 0;
        }
    }

    @Override
    public String getName()
    {
        try {
            return original.getSavepointName();
        }
        catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public DBCExecutionContext getContext()
    {
        return context;
    }

    @Override
    public int getSavepointId()
        throws SQLException
    {
        return original.getSavepointId();
    }

    @Override
    public String getSavepointName()
        throws SQLException
    {
        return original.getSavepointName();
    }

    public Savepoint getOriginal()
    {
        return original;
    }
}
