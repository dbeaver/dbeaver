/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.sqlite.SQLiteUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQLiteMetaModel
 */
public class SQLiteMetaModel extends GenericMetaModel implements DBCQueryTransformProvider {
    private static final Log log = Log.getLog(SQLiteMetaModel.class);

    private static final Pattern TYPE_WITH_LENGTH_PATTERN = Pattern.compile("(.+)\\s*\\(([0-9]+)\\)");

    public SQLiteMetaModel() {
        super();
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        return SQLiteUtils.readMasterDefinition(monitor, sourceObject, SQLiteObjectType.view, sourceObject.getName(), sourceObject);
    }

    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTableBase sourceObject, Map<String, Object> options) throws DBException {
        String tableDDL = SQLiteUtils.readMasterDefinition(monitor, sourceObject, SQLiteObjectType.table, sourceObject.getName(), sourceObject);
        String indexesDDL = SQLiteUtils.readMasterDefinition(monitor, sourceObject, SQLiteObjectType.index, null, sourceObject);
        if (CommonUtils.isEmpty(indexesDDL)) {
            return tableDDL;
        }
        return tableDDL + "\n" + indexesDDL;
    }

    @Override
    public boolean supportsTableDDLSplit(GenericTableBase sourceObject) {
        return false;
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        return SQLiteUtils.readMasterDefinition(monitor, trigger, SQLiteObjectType.trigger, trigger.getName(), trigger.getTable());
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public boolean supportsDatabaseTriggers(@NotNull GenericDataSource dataSource) {
        return false;
    }

    @Override
    public List<? extends GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws DBException {
        if (table == null) {
            return Collections.emptyList();
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT name FROM sqlite_master WHERE type='trigger' AND tbl_name=?")) {
                dbStat.setString(1, table.getName());
                List<GenericTrigger> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        result.add(new GenericTrigger(container, table, name, null));
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false);
        }
        return null;
    }
    
    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new SQLiteDataSource(monitor, container, this);
    }

    @Override
    public SQLiteDataTypeCache createDataTypeCache(@NotNull GenericStructContainer container) {
        return new SQLiteDataTypeCache(container);
    }

    @Override
    public GenericTableBase createTableImpl(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new SQLiteView(container, tableName, tableType, dbResult);
        } else {
            return new SQLiteTable(container, tableName, tableType, dbResult);
        }
    }

    @Override
    public boolean isSystemTable(GenericTableBase table) {
        return table.getName().startsWith("sqlite_");
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM sqlite_sequence")) {
                List<GenericSequence> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        long value = JDBCUtils.safeGetLong(dbResult, 2);
                        result.add(new GenericSequence(container, name, null, value, 0, Long.MAX_VALUE, 1));
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            // Most likely sqlite_sequence doesn't exist, this means jsut empty sequence list
            log.debug("Error loading SQLite sequences", e);
            return new ArrayList<>();
        }
    }

    @Override
    public GenericTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor, JDBCResultSet dbResult, @NotNull GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) {
        // Check for type length modifier
        Matcher matcher = TYPE_WITH_LENGTH_PATTERN.matcher(typeName);
        if (matcher.matches()) {
            typeName = matcher.group(1);
            columnSize = charLength = Integer.parseInt(matcher.group(2));
        } else {
            columnSize = charLength = -1;
        }

        return new SQLiteTableColumn(table, columnName, typeName, valueType, sourceType, ordinalPos, columnSize, charLength, scale, precision, radix, notNull, remarks, defaultValue, autoIncrement, autoGenerated);
    }

    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        if (column.isAutoIncrement()) {
            return "PRIMARY KEY AUTOINCREMENT";
        }
        return null;
    }
}
