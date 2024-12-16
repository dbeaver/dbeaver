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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItem.*;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemVisitor;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

public class SQLQueryCompletionExtraTextProvider implements SQLQueryCompletionItemVisitor<String> {

    public static SQLQueryCompletionExtraTextProvider INSTANCE = new SQLQueryCompletionExtraTextProvider();

    private SQLQueryCompletionExtraTextProvider() {
    }

    @NotNull
    @Override
    public String visitSubqueryAlias(@NotNull SQLRowsSourceAliasCompletionItem rowsSourceAlias) {
        return rowsSourceAlias.sourceInfo.tableOrNull != null ? " - Table alias" : " - Subquery alias";
    }

    @NotNull
    @Override
    public String visitColumnName(@NotNull SQLColumnNameCompletionItem columnName) {
        SQLQueryExprType type = columnName.columnInfo.type;
        String typeName = type == null || type == SQLQueryExprType.UNKNOWN ? null : type.getDisplayName();
        return typeName == null ? " - Column" : (" : " + typeName);
    }

    @NotNull
    @Override
    public String visitTableName(@NotNull SQLTableNameCompletionItem tableName) {
        return (DBUtils.isView(tableName.object) ? " - View " : " - Table ");
    }

    @Nullable
    @Override
    public String visitReservedWord(@Nullable SQLReservedWordCompletionItem reservedWord) {
        return null;
    }

    @NotNull
    @Override
    public String visitNamedObject(@NotNull SQLDbNamedObjectCompletionItem namedObject) {
        DBSObject o = namedObject.object;
        String typeName =  DBUtils.getObjectTypeName(o);
        if (typeName.equalsIgnoreCase("Object")) {
            if (o instanceof DBSSchema) {
                typeName = "Schema";
            } else if (o instanceof DBSCatalog) {
                typeName = "Catalog";
            } else {
                typeName = "";
            }
        }
        return CommonUtils.isEmpty(typeName) ? null : (" - " + typeName);
    }

    @Nullable
    @Override
    public String visitJoinCondition(@NotNull SQLJoinConditionCompletionItem joinCondition) {
        return " - Known foreign key relation";
    }
}
