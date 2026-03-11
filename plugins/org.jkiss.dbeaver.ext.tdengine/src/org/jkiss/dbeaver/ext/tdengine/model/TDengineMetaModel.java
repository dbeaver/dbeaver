/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.tdengine.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * TDengineMetaModel
 */
public class TDengineMetaModel extends GenericMetaModel {

    public TDengineMetaModel() {
        super();
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase table,
        @Nullable String tableName
    ) throws SQLException {
        // Use information_schema.ins_tables with parameterized query
        // This avoids the backtick escaping issue with special characters in database names
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT table_name AS TABLE_NAME, type AS TABLE_TYPE, table_comment AS REMARKS ");
        sql.append("FROM information_schema.ins_tables ");
        sql.append("WHERE db_name = ?");

        if (table != null || CommonUtils.isNotEmpty(tableName)) {
            sql.append(" AND table_name = ?");
        }

        JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
        dbStat.setString(1, owner.getCatalog() != null ? owner.getCatalog().getName() : owner.getName());

        if (table != null || CommonUtils.isNotEmpty(tableName)) {
            dbStat.setString(2, table != null ? table.getName() : tableName);
        }

        return dbStat;
    }
}
