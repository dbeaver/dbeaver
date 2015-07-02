/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.exec;

/**
 * Logical operator
 */
public class DBCLogicalOperator {

    public static final DBCLogicalOperator EQUALS = new DBCLogicalOperator("=", 1);
    public static final DBCLogicalOperator NOT_EQUALS = new DBCLogicalOperator("<>", 1);
    public static final DBCLogicalOperator GREATER = new DBCLogicalOperator(">", 1);
    public static final DBCLogicalOperator GREATER_EQUALS = new DBCLogicalOperator(">=", 1);
    public static final DBCLogicalOperator LESS = new DBCLogicalOperator("<", 1);
    public static final DBCLogicalOperator LESS_EQUALS = new DBCLogicalOperator("<=", 1);
    public static final DBCLogicalOperator IS_NULL = new DBCLogicalOperator("IS NULL", 0);
    public static final DBCLogicalOperator IS_NOT_NULL = new DBCLogicalOperator("IS NOT NULL", 0);
    public static final DBCLogicalOperator BETWEEN = new DBCLogicalOperator("BETWEEN", 2);
    public static final DBCLogicalOperator IN = new DBCLogicalOperator("IN", -1);
    public static final DBCLogicalOperator LIKE = new DBCLogicalOperator("LIKE", 1);
    public static final DBCLogicalOperator REGEX = new DBCLogicalOperator("REGEX", 1);
    public static final DBCLogicalOperator SOUNDS = new DBCLogicalOperator("SOUNDS", 1);

    private final String stringValue;
    private final int argumentCount;

    DBCLogicalOperator(String stringValue, int argumentCount) {
        this.stringValue = stringValue;
        this.argumentCount = argumentCount;
    }

    public String getStringValue() {
        return stringValue;
    }

    public int getArgumentCount() {
        return argumentCount;
    }
}
