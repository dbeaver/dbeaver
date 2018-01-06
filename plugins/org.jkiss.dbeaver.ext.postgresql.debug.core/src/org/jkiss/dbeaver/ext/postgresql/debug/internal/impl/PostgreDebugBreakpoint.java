/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.debug.internal.impl;

import java.sql.SQLException;
import java.sql.Statement;

import org.jkiss.dbeaver.debug.DBGBreakpoint;
import org.jkiss.dbeaver.debug.DBGException;

@SuppressWarnings("nls")
public class PostgreDebugBreakpoint implements DBGBreakpoint {

    private final PostgreDebugObject obj;

    private final PostgreDebugSession session;

    private final PostgreDebugBreakpointProperties properties;

    private static final String SQL_SET_GLOBAL = "select pldbg_set_global_breakpoint(?sessionid, ?obj, ?line, ?target)";
    private static final String SQL_SET = "select pldbg_set_breakpoint(?sessionid, ?obj, ?line)";
    private static final String SQL_DROP = "select pldbg_drop_breakpoint(?sessionid, ?obj, ?line)";

    public PostgreDebugBreakpoint(PostgreDebugSession session, PostgreDebugObject obj,
                                  PostgreDebugBreakpointProperties properties) throws DBGException {

        this.session = session;
        this.obj = obj;
        this.properties = properties;
        try (Statement stmt = session.getConnection().createStatement()) {

            String sqlCommand = properties.isGlobal() ? SQL_SET_GLOBAL : SQL_SET;

            stmt.executeQuery(sqlCommand.replaceAll("\\?sessionid", String.valueOf(session.getSessionId()))
                    .replaceAll("\\?obj", String.valueOf(obj.getID()))
                    .replaceAll("\\?line", properties.isOnStart() ? "-1" : String.valueOf(properties.getLineNo()))
                    .replaceAll("\\?target", properties.isAll() ? "null"
                            : String.valueOf(properties.getTargetId())));

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }

    }

    @Override
    public PostgreDebugObject getDebugObject() {
        return obj;
    }

    @Override
    public void drop() throws DBGException {
        try (Statement stmt = session.getConnection().createStatement()) {

            stmt.executeQuery(SQL_DROP.replaceAll("\\?sessionid", String.valueOf(session.getSessionId()))
                    .replaceAll("\\?obj", String.valueOf(obj.getID()))
                    .replaceAll("\\?line", properties.isOnStart() ? "-1" : String.valueOf(properties.getLineNo())));

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }

    }

    @Override
    public PostgreDebugBreakpointProperties getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "PostgreDebugBreakpoint [obj=" + obj + ", session id =" + session.getSessionId() + ", properties="
                + properties + "]";
    }

}
