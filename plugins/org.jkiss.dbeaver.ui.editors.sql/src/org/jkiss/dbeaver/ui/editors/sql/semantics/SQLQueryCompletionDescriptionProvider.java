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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItem.*;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemVisitor;

public class SQLQueryCompletionDescriptionProvider implements SQLQueryCompletionItemVisitor<String> {

    @NotNull
    @Override
    public String visitSubqueryAlias(@NotNull SQLSubqueryAliasCompletionItem subqueryAlias) {
        return "Subquery alias for \n" + subqueryAlias.source.getSyntaxNode().getTextContent();
    }

    @NotNull
    @Override
    public String visitColumnName(@NotNull SQLColumnNameCompletionItem columnName) {
        @Nullable String originalColumnName = columnName.columnInfo.realAttr == null ? null
                : DBUtils.getObjectFullName(columnName.columnInfo.realAttr, DBPEvaluationContext.DML);

        if (columnName.columnInfo.symbol.getSymbolClass() == SQLQuerySymbolClass.COLUMN_DERIVED) {
            return "Derived column #" + columnName.columnInfo.index + " " + (originalColumnName != null ? "for real column " + originalColumnName : "") +
                    " from the subquery \n" + columnName.columnInfo.source.getSyntaxNode().getTextContent();
        } else {
            if (columnName.columnInfo.realAttr != null) {
                return columnName.columnInfo.realAttr.getDescription();
            } else if (columnName.columnInfo.realSource != null) {
                return "Column of the " +  DBUtils.getObjectFullName(columnName.columnInfo.realSource, DBPEvaluationContext.DML);
            } else {
                return "Computed column "; // TODO deliver the column expression to the model
            }
        }
    }

    @Nullable
    @Override
    public String visitTableName(@NotNull SQLTableNameCompletionItem tableName) {
        return tableName.table.getDescription();
    }

    @NotNull
    @Override
    public String visitReservedWord(@Nullable SQLReservedWordCompletionItem reservedWord) {
        return "Reserved word of the query language";
    }

    @NotNull
    @Override
    public String visitNamedObject(@NotNull SQLDbNamedObjectCompletionItem namedObject) {
        return DBUtils.getObjectFullName(namedObject.object, DBPEvaluationContext.DML);
    }
}
