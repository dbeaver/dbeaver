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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

import java.util.StringJoiner;

/**
 *   If you specify an ON DUPLICATE KEY UPDATE clause and a row to be inserted would cause a duplicate value in a UNIQUE index or PRIMARY KEY, an UPDATE of the old row occurs.
 *   It can be better then MySQLInsertReplaceMethod class with REPLACE INTO, because REPLACE INTO first deletes a row, and then insert a new one.
 * Example:
 *   INSERT INTO insert_duplicate_values(id, name, another_name) VALUES (5,'Ben','Solo')
 *     ON DUPLICATE KEY UPDATE id=VALUES(id), name=VALUES(name), another_name=VALUES(another_name);
 */

public class MySQLInsertReplaceMethodUpdate implements DBDInsertReplaceMethod {
    @NotNull
    @Override
    public String getOpeningClause(@NotNull DBSTable table, @NotNull DBRProgressMonitor monitor) {
        return "INSERT INTO";
    }

    @Override
    public String getTrailingClause(@NotNull DBSTable table, @NotNull DBRProgressMonitor monitor, DBSAttributeBase[] attributes) {
        StringBuilder query = new StringBuilder();
        query.append(" ON DUPLICATE KEY UPDATE ");
        appendUpdateCase(query, table, attributes);
        return query.toString();
    }

    private void appendUpdateCase(@NotNull StringBuilder query, @NotNull DBSTable table, DBSAttributeBase[] attributes) {
        final StringJoiner names = new StringJoiner(",");
        for (DBSAttributeBase attribute : attributes) {
            if (DBUtils.isPseudoAttribute(attribute)) {
                continue;
            }
            String attrName = getAttributeName(table, attribute);
            names.add(attrName + "=VALUES(" + attrName + ")");
        }
        query.append(names);
    }

    private String getAttributeName(@NotNull DBSTable table, @NotNull DBSAttributeBase attribute) {
        // Do not quote pseudo attribute name
        return DBUtils.isPseudoAttribute(attribute) ? attribute.getName() : DBUtils.getObjectFullName(table.getDataSource(), attribute, DBPEvaluationContext.DML);
    }
}
