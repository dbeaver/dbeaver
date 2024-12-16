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
import org.jkiss.dbeaver.model.DBPObjectWithDescription;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItem.*;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemVisitor;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultPseudoColumn;
import org.jkiss.utils.CommonUtils;

public class SQLQueryCompletionDescriptionProvider implements SQLQueryCompletionItemVisitor<String> {

    public static final SQLQueryCompletionDescriptionProvider INSTANCE = new SQLQueryCompletionDescriptionProvider();

    private SQLQueryCompletionDescriptionProvider() {
    }

    @NotNull
    @Override
    public String visitSubqueryAlias(@NotNull SQLRowsSourceAliasCompletionItem rowsSourceAlias) {
        String prefix = rowsSourceAlias.sourceInfo.tableOrNull != null ? "Table alias for \n" : "Subquery alias for \n";
        return prefix + rowsSourceAlias.sourceInfo.source.getSyntaxNode().getTextContent();
    }

    @Nullable
    @Override
    public String visitColumnName(@NotNull SQLColumnNameCompletionItem columnName) {
        @Nullable String originalColumnName = columnName.columnInfo.realAttr == null ? null
            : DBUtils.getObjectFullName(columnName.columnInfo.realAttr, DBPEvaluationContext.DML);

        if (columnName.columnInfo.symbol.getSymbolClass() == SQLQuerySymbolClass.COLUMN_DERIVED) {
            return "Derived column #" + columnName.columnInfo.index + " " + (originalColumnName != null ? "for real column " + originalColumnName : "") +
                    " from the subquery \n" + columnName.columnInfo.source.getSyntaxNode().getTextContent();
        } else {
            if (columnName.columnInfo.realAttr != null) {
                String attrDescription = columnName.columnInfo.realAttr.getDescription();
                return CommonUtils.isNotEmpty(attrDescription) ? attrDescription
                    : "Column " + columnName.columnInfo.realAttr.getName() + " of " + DBUtils.getObjectFullName(columnName.columnInfo.realAttr.getParentObject(), DBPEvaluationContext.DML);
            } else if (columnName.columnInfo.realSource != null) {
                return "Column " + columnName.columnInfo.symbol.getName() + " of " + DBUtils.getObjectFullName(columnName.columnInfo.realSource, DBPEvaluationContext.DML);
            } else if (columnName.columnInfo.symbol.getDefinition() instanceof SQLQueryResultPseudoColumn pseudoColumn) {
                return pseudoColumn.description;
            } else {
                return "Computed column #" + columnName.columnInfo.index + // TODO deliver the column expression from the SQLQuerySelectionResultColumnSpec?
                    " from the subquery \n" + columnName.columnInfo.source.getSyntaxNode().getTextContent();
            }
        }
    }

    @Nullable
    @Override
    public String visitTableName(@NotNull SQLTableNameCompletionItem tableName) {
        return tableName.object.getDescription();
    }

    @Nullable
    @Override
    public String visitReservedWord(@Nullable SQLReservedWordCompletionItem reservedWord) {
        return "Reserved word of the query language";
    }

    @Nullable
    @Override
    public String visitNamedObject(@NotNull SQLDbNamedObjectCompletionItem namedObject) {
        return namedObject instanceof DBPObjectWithDescription owd
            ? owd.getDescription()
            : DBUtils.getObjectFullName(namedObject.object, DBPEvaluationContext.DML);
    }

    @Nullable
    @Override
    public String visitJoinCondition(@NotNull SQLJoinConditionCompletionItem joinCondition) {
        return "Join condition on the foreign key known from the database schema: " +
            joinCondition.left.apply(this) + " vs " + joinCondition.right.apply(this);
    }
}
