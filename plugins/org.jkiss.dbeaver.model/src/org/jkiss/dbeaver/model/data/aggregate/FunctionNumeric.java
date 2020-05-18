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
package org.jkiss.dbeaver.model.data.aggregate;

/**
 * FunctionSum
 */
public abstract class FunctionNumeric implements IAggregateFunction {

    protected static Number getNumeric(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number)) {
            String strValue = value.toString();
            if (strValue == null) {
                return null;
            }
            try {
                value = Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                // Not a number. Its ok, do not warn  
            }
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        return null;
    }

    protected static Comparable getComparable(Object value, boolean aggregateAsStrings) {
        if (!aggregateAsStrings) {
            Number num = FunctionNumeric.getNumeric(value);
            if (num != null) {
                value = num;
            }
        }
        if (value instanceof Comparable) {
            return (Comparable)value;
        }
        return null;
    }
}
