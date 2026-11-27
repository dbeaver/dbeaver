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
 * Loads packages from RDB$PACKAGES (Firebird 3+).
 */
public class FireBirdPackageCache extends JDBCObjectCache<FireBirdDataSource, FireBirdPackage> {

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull FireBirdDataSource owner)
        throws SQLException {
        // SELECT * because RDB$SQL_SECURITY only exists since Firebird 4; missing columns are read as null
        return session.prepareStatement("SELECT * FROM RDB$PACKAGES ORDER BY RDB$PACKAGE_NAME");
    }

    @Nullable
    @Override
    protected FireBirdPackage fetchObject(
        @NotNull JDBCSession session,
        @NotNull FireBirdDataSource owner,
        @NotNull JDBCResultSet resultSet
    ) {
        String name = JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$PACKAGE_NAME");
        if (name == null) {
            return null;
        }
        // RDB$SQL_SECURITY is null if the database default is used
        Boolean sqlSecurityDefiner = null;
        if (JDBCUtils.safeGetObject(resultSet, "RDB$SQL_SECURITY") != null) {
            sqlSecurityDefiner = JDBCUtils.safeGetBoolean(resultSet, "RDB$SQL_SECURITY");
        }
        return new FireBirdPackage(
            owner,
            name,
            JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$OWNER_NAME"),
            JDBCUtils.safeGetString(resultSet, "RDB$PACKAGE_HEADER_SOURCE"),
            JDBCUtils.safeGetString(resultSet, "RDB$PACKAGE_BODY_SOURCE"),
            JDBCUtils.safeGetInt(resultSet, "RDB$VALID_BODY_FLAG") != 0,
            sqlSecurityDefiner,
            JDBCUtils.safeGetString(resultSet, "RDB$DESCRIPTION"),
            JDBCUtils.safeGetInt(resultSet, "RDB$SYSTEM_FLAG") != 0
        );
    }
}
