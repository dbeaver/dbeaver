/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
