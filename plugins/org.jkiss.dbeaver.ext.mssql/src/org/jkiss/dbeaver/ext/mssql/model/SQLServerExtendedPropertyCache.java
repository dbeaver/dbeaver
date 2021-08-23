/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;

import java.sql.SQLException;

public class SQLServerExtendedPropertyCache extends JDBCObjectLookupCache<SQLServerExtendedPropertyOwner, SQLServerExtendedProperty> {
    @NotNull
    @Override
    public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull SQLServerExtendedPropertyOwner owner, @Nullable SQLServerExtendedProperty object, @Nullable String objectName) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT *, TYPE_ID(CAST(SQL_VARIANT_PROPERTY(value, 'BaseType') as nvarchar)) AS value_type" +
            " FROM " + SQLServerUtils.getExtendedPropsTableName(owner.getDatabase()) +
            " WHERE major_id=? AND minor_id=? AND class=? ORDER BY minor_id"
        );
        dbStat.setLong(1, owner.getMajorObjectId());
        dbStat.setLong(2, owner.getMinorObjectId());
        dbStat.setLong(3, owner.getExtendedPropertyObjectClass().getClassId());
        return dbStat;

    }

    @Nullable
    @Override
    protected SQLServerExtendedProperty fetchObject(@NotNull JDBCSession session, @NotNull SQLServerExtendedPropertyOwner owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
        return new SQLServerExtendedProperty(session.getProgressMonitor(), owner, resultSet);
    }
}
