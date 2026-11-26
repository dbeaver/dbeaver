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
 * Loads roles from RDB$ROLES.
 */
public class FireBirdRoleCache extends JDBCObjectCache<FireBirdDataSource, FireBirdRole> {

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull FireBirdDataSource owner)
        throws SQLException {
        // SELECT * because RDB$DESCRIPTION and RDB$SYSTEM_FLAG only exist since Firebird 2.0
        // and RDB$SYSTEM_PRIVILEGES since Firebird 4; missing columns are read as null
        return session.prepareStatement("SELECT * FROM RDB$ROLES ORDER BY RDB$ROLE_NAME");
    }

    @Nullable
    @Override
    protected FireBirdRole fetchObject(
        @NotNull JDBCSession session,
        @NotNull FireBirdDataSource owner,
        @NotNull JDBCResultSet resultSet
    ) {
        String name = JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$ROLE_NAME");
        if (name == null) {
            return null;
        }
        return new FireBirdRole(
            owner,
            name,
            JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$OWNER_NAME"),
            JDBCUtils.safeGetString(resultSet, "RDB$DESCRIPTION"),
            JDBCUtils.safeGetInt(resultSet, "RDB$SYSTEM_FLAG") != 0,
            FireBirdRole.decodeSystemPrivileges(JDBCUtils.safeGetBytes(resultSet, "RDB$SYSTEM_PRIVILEGES"))
        );
    }
}
