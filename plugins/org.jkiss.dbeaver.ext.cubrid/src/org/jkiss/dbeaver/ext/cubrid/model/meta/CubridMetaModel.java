/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model.meta;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUser;
import org.jkiss.dbeaver.ext.cubrid.model.CubridView;
import org.jkiss.dbeaver.ext.cubrid.model.plan.CubridQueryPlanner;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CubridMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(CubridMetaModel.class);

    public String getTableOrViewName(GenericTableBase base) 
    {
        if (base != null) {
            if (base.isView()) {
                return ((CubridView) base).getUniqueName();
            } else {
                return ((CubridTable) base).getUniqueName();
            }
        }
        return null;
    }

    @Override
    public List<GenericSchema> loadSchemas(JDBCSession session, GenericDataSource dataSource, GenericCatalog catalog)
            throws DBException
    {
        List<GenericSchema> users = new ArrayList<>();
        try {
            final JDBCPreparedStatement dbStat = session.prepareStatement("select * from db_user");
            dbStat.executeStatement();
            JDBCResultSet dbResult = dbStat.getResultSet();

            while (dbResult.next()) {
                String name = JDBCUtils.safeGetStringTrimmed(dbResult, "name");
                String description = JDBCUtils.safeGetStringTrimmed(dbResult, "comment");
                CubridUser user = new CubridUser(dataSource, name, description);
                users.add(user);
            }
        } catch (SQLException e) {
        	log.error("Cannot load user", e);
        }
        return users;
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase object,
            @Nullable String objectName)
            throws SQLException
    {
        String sql = "select a.*,a.class_name as TABLE_NAME, case when class_type = 'CLASS' then 'TABLE' \r\n"
                + "when class_type = 'VCLASS' then 'VIEW' end as TABLE_TYPE, \r\n"
                + "b.current_val from db_class a LEFT JOIN db_serial b on \r\n"
                + "a.class_name = b.class_name "
                + "where a.owner_name = ?";

        final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, owner.getName());
        return dbStat;
    }

    @Override
    public JDBCStatement prepareTableColumnLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase forTable)
            throws SQLException
    {
        return session.getMetaData().getColumns(null, null, this.getTableOrViewName(forTable), null).getSourceStatement();
    }

    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase forTable)
            throws SQLException, DBException
    {
        return session.getMetaData().getPrimaryKeys(null, null, this.getTableOrViewName(forTable)).getSourceStatement();
    }

    @Override
    public JDBCStatement prepareForeignKeysLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase forTable)
            throws SQLException
    {
        return session.getMetaData().getImportedKeys(null, null, this.getTableOrViewName(forTable)).getSourceStatement();
    }

    @Override
    public GenericTableBase createTableImpl(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @NotNull GenericMetaObject tableObject,
            @NotNull JDBCResultSet dbResult)
    {
        String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
        String tableType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);
        GenericTableBase table = createTableOrViewImpl(owner, tableName, tableType, dbResult);
        return table;
    }

    @Override
    public GenericTableBase createTableOrViewImpl(
            GenericStructContainer container,
            @Nullable String tableName,
            @Nullable String tableType,
            @Nullable JDBCResultSet dbResult)
    {
        if (tableType != null && isView(tableType)) {
            return new CubridView(container, tableName, tableType, dbResult);
        }
        return new CubridTable(container, tableName, tableType, dbResult);
    }
    
    @Override
    public DBCQueryPlanner getQueryPlanner(@NotNull GenericDataSource dataSource) 
    {
        return new CubridQueryPlanner((CubridDataSource) dataSource);
    }
}
