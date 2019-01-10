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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.dbeaver.model.DBPDataKind;

import java.sql.Types;

public enum SQLiteAffinity {

    INTEGER(DBPDataKind.NUMERIC, Types.BIGINT, 19, 0),
    REAL(DBPDataKind.NUMERIC, Types.DOUBLE, 17, 17),
    NUMERIC(DBPDataKind.NUMERIC, Types.NUMERIC, 17, 17),
    TEXT(DBPDataKind.STRING, Types.VARCHAR, Integer.MAX_VALUE, 0),
    BLOB(DBPDataKind.BINARY, Types.BINARY, Integer.MAX_VALUE, 0);

    private final DBPDataKind dataKind;
    private final int valueType;
    private final int precision;
    private final int scale;

    SQLiteAffinity(DBPDataKind dataKind, int valueType, int precision, int scale) {
        this.dataKind = dataKind;
        this.valueType = valueType;
        this.precision = precision;
        this.scale = scale;
    }

    public DBPDataKind getDataKind() {
        return dataKind;
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
