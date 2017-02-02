/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.sqlite.model;

import java.sql.Types;

public enum SQLiteAffinity {

    INTEGER(Types.BIGINT, 19, 0),
    REAL(Types.DOUBLE, 17, 17),
    NUMERIC(Types.NUMERIC, 17, 17),
    TEXT(Types.VARCHAR, Integer.MAX_VALUE, 0),
    BLOB(Types.BINARY, Integer.MAX_VALUE, 0);

    private final int valueType;
    private final int precision;
    private final int scale;

    SQLiteAffinity(int valueType, int precision, int scale) {
        this.valueType = valueType;
        this.precision = precision;
        this.scale = scale;
    }

    public int getValueType() {
        return valueType;
    }

    public int getPrecision() {
        return precision;
    }

    public int getScale() {
        return scale;
    }
}
