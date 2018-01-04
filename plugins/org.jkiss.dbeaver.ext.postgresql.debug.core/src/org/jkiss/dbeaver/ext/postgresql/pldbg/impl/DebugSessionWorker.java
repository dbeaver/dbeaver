/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.pldbg.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

public class DebugSessionWorker implements Callable<DebugSessionResult> {

    private final Connection conn;
    private String sql = "";

    public void execSQL(String sqlCommand)
    {
        this.sql = sqlCommand;
    }

    public DebugSessionWorker(Connection conn)
    {
        this.conn = conn;
    }

    @Override
    public DebugSessionResult call() throws Exception
    {

        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery(sql);
            return new DebugSessionResult(true, null);

        } catch (SQLException e) {
            return new DebugSessionResult(false, e);
        }
    }

}
