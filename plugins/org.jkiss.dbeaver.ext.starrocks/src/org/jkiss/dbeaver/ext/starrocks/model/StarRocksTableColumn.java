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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;

import java.sql.ResultSet;

/**
 * StarRocks Table Column - used for both tables and views.
 */
public class StarRocksTableColumn extends JDBCTableColumn<StarRocksTableBase> {

    public StarRocksTableColumn(StarRocksTableBase table, ResultSet dbResult) throws DBException {
        super(table, true);
        // Parse SHOW FULL COLUMNS result: Field | Type | Collation | Null | Key | Default | Extra | Privileges | Comment
        setName(JDBCUtils.safeGetString(dbResult, "Field"));
        setTypeName(JDBCUtils.safeGetString(dbResult, "Type"));
        String nullableStr = JDBCUtils.safeGetString(dbResult, "Null");
        setRequired(!"YES".equalsIgnoreCase(nullableStr));
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return getTable().getDataSource();
    }
}
