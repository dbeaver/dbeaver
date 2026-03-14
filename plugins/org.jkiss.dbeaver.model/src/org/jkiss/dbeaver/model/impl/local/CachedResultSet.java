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

package org.jkiss.dbeaver.model.impl.local;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps column names and rows.
 * Simple cache for transferring result sets and/or serialization.
 */
public class CachedResultSet {
    private static final Log log = Log.getLog(CachedResultSet.class);

    private final String[] columnNames;
    protected final List<Object[]> rows = new ArrayList<>();
    private final String query;

    public CachedResultSet(@NotNull String query, @NotNull String[] columnNames) {
        this.query = query;
        this.columnNames = columnNames;
    }

    public CachedResultSet(@NotNull String query, @NotNull ResultSetMetaData rsMeta) throws SQLException {
        this.query = query;
        int columnCount = rsMeta.getColumnCount();
        this.columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = rsMeta.getColumnName(i + 1);
        }
    }

    public String getQuery() {
        return query;
    }

    public void addRow(@NotNull Object[] row) {
        if (row.length != columnNames.length) {
            throw new IllegalArgumentException(
                "Row values count (" + row.length + ") doesn't match column count (" + columnNames.length + ")");
        }
        rows.add(row);
    }

    public void addRow(@NotNull ResultSet rs) throws SQLException {
        int columnCount = columnNames.length;
        Object[] row = new Object[columnCount];
        for (int i = 0; i < columnCount; i++) {
            try {
                row[i] = rs.getObject(i + 1);
            } catch (SQLException e) {
                log.debug(e.getMessage());
                row[i] = null;
            }
        }
        rows.add(row);
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    @NotNull
    public String[] getColumnNames() {
        return columnNames;
    }

    @NotNull
    public List<Object[]> getRows() {
        return rows;
    }
}
