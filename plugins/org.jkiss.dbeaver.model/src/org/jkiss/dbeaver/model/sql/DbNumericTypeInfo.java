/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

public class DbNumericTypeInfo<T extends Comparable<T>> {
    private final T minValue;
    private final T maxValue;
    private final String typeName;
 
    public DbNumericTypeInfo(T minValue, T maxValue, String typeName) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.typeName = typeName;
    }

    public T getMinValue() {
        return minValue;
    }

    public T getMaxValue() {
        return maxValue;
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean fitsIn(DbNumericTypeInfo<T> other) {
        return this.minValue.compareTo(other.minValue) >= 0 && this.maxValue.compareTo(other.maxValue) <= 0;
    }
}
