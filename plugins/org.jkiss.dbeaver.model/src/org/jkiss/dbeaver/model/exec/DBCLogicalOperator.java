/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
            final Object cmpValue = arguments == null ? null : arguments[0];
            return CommonUtils.equalObjects(srcValue, cmpValue);
        }
    },
    NOT_EQUALS("<>", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            final Object cmpValue = arguments == null ? null : arguments[0];
            return !CommonUtils.equalObjects(srcValue, cmpValue);
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
            return ArrayUtils.contains(arguments, srcValue);
        }
    },
    LIKE("LIKE", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return srcValue != null && !ArrayUtils.isEmpty(arguments) &&
                SQLUtils.matchesLike(srcValue.toString(), arguments[0].toString());
        }
    },
    NOT_LIKE("NOT LIKE", 1) {
        @Override
        public boolean evaluate(Object srcValue, Object[] arguments) {
            return srcValue != null && !ArrayUtils.isEmpty(arguments) &&
                !SQLUtils.matchesLike(srcValue.toString(), arguments[0].toString());
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

    /**
     * Operator string representation
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * Argument count.
     * Zero means no arguments.
     * Positive number means exact this number of arguments.
     * Negative number means variable number of arguments
     * @return argument count
     */
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
