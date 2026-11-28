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
 * Loads character sets from RDB$CHARACTER_SETS.
 */
public class FireBirdCharacterSetCache extends JDBCObjectCache<FireBirdDataSource, FireBirdCharacterSet> {

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull FireBirdDataSource owner)
        throws SQLException {
        return session.prepareStatement(
            "SELECT RDB$CHARACTER_SET_NAME,RDB$CHARACTER_SET_ID,RDB$BYTES_PER_CHARACTER,RDB$DEFAULT_COLLATE_NAME,RDB$DESCRIPTION\n" +
                "FROM RDB$CHARACTER_SETS ORDER BY RDB$CHARACTER_SET_NAME");
    }

    @Nullable
    @Override
    protected FireBirdCharacterSet fetchObject(
        @NotNull JDBCSession session,
        @NotNull FireBirdDataSource owner,
        @NotNull JDBCResultSet resultSet
    ) {
        String name = JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$CHARACTER_SET_NAME");
        if (name == null) {
            return null;
        }
        return new FireBirdCharacterSet(
            owner,
            name,
            JDBCUtils.safeGetInt(resultSet, "RDB$CHARACTER_SET_ID"),
            JDBCUtils.safeGetInt(resultSet, "RDB$BYTES_PER_CHARACTER"),
            JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$DEFAULT_COLLATE_NAME"),
            JDBCUtils.safeGetString(resultSet, "RDB$DESCRIPTION")
        );
    }
}
