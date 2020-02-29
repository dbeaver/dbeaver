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
package org.jkiss.dbeaver.model.sql;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

/**
 * SQL query select item.
 * Presents in SELECT statements.
 */
public class SQLSelectItem {
    private SelectItem source;
    private Table table;
    private String name;
    private boolean plainColumn;

    SQLSelectItem(SelectItem item) {
        this.source = item;
        if (item instanceof SelectExpressionItem) {
            final Expression itemExpression = ((SelectExpressionItem) item).getExpression();
            if (itemExpression instanceof Column) {
                table = ((Column) itemExpression).getTable();
                name = ((Column) itemExpression).getColumnName();
                plainColumn = true;
            } else {
                table = null;
                final Alias alias = ((SelectExpressionItem) item).getAlias();
                if (alias != null) {
                    name = alias.getName();
                } else {
                    name = item.toString();
                }
            }
        } else if (item instanceof AllColumns) {
            table = null;
            name = "*";
        } else if (item instanceof AllTableColumns) {
            table = ((AllTableColumns) item).getTable();
            name = "*";
        } else {
            table = null;
            name = "?";
        }
    }

    public String getName() {
        return name;
    }

    public boolean isPlainColumn() {
        return plainColumn;
    }

    @Override
    public String toString() {
        return source.toString();
    }
}
