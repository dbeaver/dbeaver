/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.exec;

/**
 * Logical operator
 */
public enum DBCLogicalOperator {

    EQUALS("=", 1),
    NOT_EQUALS("<>", 1),
    GREATER(">", 1),
    GREATER_EQUALS(">=", 1),
    LESS("<", 1),
    LESS_EQUALS(">=", 1),
    IS_NULL("IS NULL", 0),
    IS_NOT_NULL("IS NOT NULL", 0),
    BETWEEN("BETWEEN", 2),
    IN("IN", -1),
    LIKE("LIKE", 1),
    REGEX("REGEX", 1),
    SOUNDS("SOUNDS", 1),
    ;

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
