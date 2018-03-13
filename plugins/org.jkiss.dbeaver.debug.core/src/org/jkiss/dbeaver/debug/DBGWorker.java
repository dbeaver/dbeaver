/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

public class DBGWorker implements Callable<Void> {

    private final DBGBaseSession session;
    private final String sql;
    private final DBGEvent before;
    private final DBGEvent after;

    public DBGWorker(DBGBaseSession session, String sqlCommand, DBGEvent begin, DBGEvent end) {
        this.session = session;
        this.sql = sqlCommand;
        this.before = begin;
        this.after = end;
    }

    @Override
    public Void call() throws Exception {

        try (Statement stmt = session.getConnection().createStatement()) {
            session.fireEvent(before);
            stmt.executeQuery(sql);
            session.fireEvent(after);
            return null;
        } catch (SQLException e) {
            String message = String.format("Failed to execute %s", sql);
            throw new Exception(message, e);
        }
    }

}
