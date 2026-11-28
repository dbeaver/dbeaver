/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Loads users from SEC$USERS (Firebird 3+). Users without administrator rights only see themselves.
 */
public class FireBirdUserCache extends JDBCObjectCache<FireBirdDataSource, FireBirdUser> {

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull FireBirdDataSource owner)
        throws SQLException {
        return session.prepareStatement(
            "SELECT SEC$USER_NAME,SEC$FIRST_NAME,SEC$MIDDLE_NAME,SEC$LAST_NAME,SEC$ACTIVE,SEC$ADMIN,SEC$PLUGIN,SEC$DESCRIPTION\n" +
                "FROM SEC$USERS ORDER BY SEC$USER_NAME,SEC$PLUGIN");
    }

    @Nullable
    @Override
    protected FireBirdUser fetchObject(
        @NotNull JDBCSession session,
        @NotNull FireBirdDataSource owner,
        @NotNull JDBCResultSet resultSet
    ) {
        String name = JDBCUtils.safeGetStringTrimmed(resultSet, "SEC$USER_NAME");
        if (name == null) {
            return null;
        }
        return new FireBirdUser(
            owner,
            name,
            JDBCUtils.safeGetString(resultSet, "SEC$FIRST_NAME"),
            JDBCUtils.safeGetString(resultSet, "SEC$MIDDLE_NAME"),
            JDBCUtils.safeGetString(resultSet, "SEC$LAST_NAME"),
            JDBCUtils.safeGetBoolean(resultSet, "SEC$ACTIVE"),
            JDBCUtils.safeGetBoolean(resultSet, "SEC$ADMIN"),
            JDBCUtils.safeGetStringTrimmed(resultSet, "SEC$PLUGIN"),
            JDBCUtils.safeGetString(resultSet, "SEC$DESCRIPTION")
        );
    }
}
