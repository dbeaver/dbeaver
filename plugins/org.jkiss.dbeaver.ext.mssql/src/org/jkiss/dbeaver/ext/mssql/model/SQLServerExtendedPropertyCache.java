/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ep.*, ");
        if (SQLServerUtils.isDriverBabelfish(session.getDataSource().getContainer().getDriver())) {
            sql.append("NULL AS value_type ");
        }
        else {
            sql.append("TYPE_ID(CAST(SQL_VARIANT_PROPERTY(value, 'BaseType') as nvarchar)) AS value_type ");
        }
        sql.append("FROM ");
        sql.append(SQLServerUtils.getExtendedPropsTableName(owner.getDatabase()));
        sql.append(" ep WHERE ep.major_id=? AND ep.minor_id=? AND ep.class=? ORDER BY ep.minor_id");
        JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
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
