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

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Logical operator
 */
public enum DBCLogicalOperator {

    EQUALS("=", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return CommonUtils.equalObjects(srcValue, arguments[0]);
        }
    },
    NOT_EQUALS("<>", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return !CommonUtils.equalObjects(srcValue, arguments[0]);
        }
    },
    GREATER(">", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return compare(srcValue, arguments) > 0;
        }
    },
    GREATER_EQUALS(">=", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return compare(srcValue, arguments) >= 0;
        }
    },
    LESS("<", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return compare(srcValue, arguments) < 0;
        }
    },
    LESS_EQUALS("<=", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return compare(srcValue, arguments) <= 0;
        }
    },
    IS_NULL("IS NULL", 0) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return srcValue == null;
        }
    },
    IS_NOT_NULL("IS NOT NULL", 0) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return srcValue != null;
        }
    },
    BETWEEN("BETWEEN", 2) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return false;
        }
    },
    IN("IN", -1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return false;
        }
    },
    LIKE("LIKE", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return srcValue != null && !ArrayUtils.isEmpty(arguments) &&
                SQLUtils.matchesLike(srcValue.toString(), arguments[0].toString());
        }
    },
    REGEX("REGEX", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return false;
        }
    },
    SOUNDS("SOUNDS", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return false;
        }
    };

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

    public abstract boolean evaluate(Object srcValue, Object[] arguments);

    private static int compare(Object srcValue, Object[] arguments) {
        if (srcValue == null && (arguments == null || arguments.length == 0 || arguments[0] == null)) {
            return 0;
        }
        if (srcValue instanceof Comparable) {
            return ((Comparable<Object>)srcValue).compareTo(arguments[0]);
        }
        return 0;
    }

}
