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
package org.jkiss.dbeaver.model.sql.parser;

import net.sf.jsqlparser.statement.ParenthesedStatement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

/**
 * Detects data-modifying statements nested in SELECT statements.
 */
public class SelectMutationVisitor extends TablesNamesFinder<Void> {

    private boolean mutating = false;

    @Override
    @Nullable
    public <S> Void visit(@NotNull WithItem<?> withItem, @Nullable S context) {
        ParenthesedStatement statement = withItem.getParenthesedStatement();
        if (!(statement instanceof ParenthesedSelect)) {
            mutating = true;
            return null;
        }
        return super.visit(withItem, context);
    }

    @Override
    @Nullable
    public <S> Void visit(@NotNull PlainSelect plainSelect, @Nullable S context) {
        if (!CommonUtils.isEmpty(plainSelect.getIntoTables()) || plainSelect.getIntoTempTable() != null) {
            mutating = true;
        }
        visitSelectModifiers(plainSelect, context);
        if (plainSelect.getDistinct() != null && plainSelect.getDistinct().getOnSelectItems() != null) {
            for (var item : plainSelect.getDistinct().getOnSelectItems()) {
                item.accept(this, context);
            }
        }
        if (plainSelect.getGroupBy() != null) {
            if (plainSelect.getGroupBy().getGroupByExpressionList() != null) {
                plainSelect.getGroupBy().getGroupByExpressionList().accept(this, context);
            }
            if (plainSelect.getGroupBy().getGroupingSets() != null) {
                for (var groupingSet : plainSelect.getGroupBy().getGroupingSets()) {
                    groupingSet.accept(this, context);
                }
            }
        }
        if (plainSelect.getQualify() != null) {
            plainSelect.getQualify().accept(this, context);
        }
        if (plainSelect.getWindowDefinitions() != null) {
            for (var window : plainSelect.getWindowDefinitions()) {
                if (window.getPartitionExpressionList() != null) {
                    window.getPartitionExpressionList().accept(this, context);
                }
                if (window.getOrderByElements() != null) {
                    for (var orderBy : window.getOrderByElements()) {
                        orderBy.getExpression().accept(this, context);
                    }
                }
            }
        }
        return super.visit(plainSelect, context);
    }

    @Override
    @Nullable
    public <S> Void visit(@NotNull SetOperationList setOperationList, @Nullable S context) {
        visitSelectModifiers(setOperationList, context);
        return super.visit(setOperationList, context);
    }

    @Override
    @Nullable
    public <S> Void visit(@NotNull ParenthesedSelect parenthesedSelect, @Nullable S context) {
        visitSelectModifiers(parenthesedSelect, context);
        return super.visit(parenthesedSelect, context);
    }

    @Override
    @Nullable
    public <S> Void visit(@NotNull Delete delete, @Nullable S context) {
        mutating = true;
        return null;
    }

    @Override
    @Nullable
    public <S> Void visit(@NotNull Insert insert, @Nullable S context) {
        mutating = true;
        return null;
    }

    @Override
    @Nullable
    public <S> Void visit(@NotNull Update update, @Nullable S context) {
        mutating = true;
        return null;
    }

    private <S> void visitSelectModifiers(@NotNull Select select, @Nullable S context) {
        if (select.getOrderByElements() != null) {
            for (var orderBy : select.getOrderByElements()) {
                orderBy.getExpression().accept(this, context);
            }
        }
        if (select.getLimit() != null) {
            if (select.getLimit().getOffset() != null) {
                select.getLimit().getOffset().accept(this, context);
            }
            if (select.getLimit().getRowCount() != null) {
                select.getLimit().getRowCount().accept(this, context);
            }
            if (select.getLimit().getByExpressions() != null) {
                select.getLimit().getByExpressions().accept(this, context);
            }
        }
        if (select.getOffset() != null && select.getOffset().getOffset() != null) {
            select.getOffset().getOffset().accept(this, context);
        }
        if (select.getFetch() != null && select.getFetch().getExpression() != null) {
            select.getFetch().getExpression().accept(this, context);
        }
    }

    public boolean isMutating() {
        return mutating;
    }
}
