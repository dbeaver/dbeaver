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
package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;

import java.util.List;

/**
 * PostgreSQL supporting test class
 */
public class PostgreTestUtils {

    @SuppressWarnings("unchecked")
    public static PostgreTableColumn addColumn(PostgreTableBase table, String columnName, String columnType, int ordinalPosition) throws DBException {
        PostgreTableColumn column = new PostgreTableColumn(table);
        column.setName(columnName);
        column.setTypeName(columnType);
        column.setOrdinalPosition(ordinalPosition);
        List<PostgreTableColumn> cachedAttributes = (List<PostgreTableColumn>) table.getCachedAttributes();
        cachedAttributes.add(column);
        return column;
    }
}
