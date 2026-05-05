/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License")),
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

package org.jkiss.dbeaver.model.sql.parser.tokens;

import org.jkiss.dbeaver.model.text.parser.TPTokenType;

/**
 * SQL token type
 */
public enum SQLTokenType implements TPTokenType {

    T_KEYWORD(500),
    T_STRING(501),
    T_QUOTED(502),
    T_TYPE(503),
    T_NUMBER(504),
    T_TABLE(505),
    T_TABLE_ALIAS(506),
    T_COLUMN(507),
    T_COLUMN_DERIVED(508),
    T_SCHEMA(509),
    T_COMPOSITE_FIELD(510),
    T_SQL_VARIABLE(511),
    T_FUNCTION(512),
    T_SEMANTIC_ERROR(900),
    
    T_UNKNOWN(1000),
    T_BLOCK_BEGIN(1001),
    T_BLOCK_END(1002),
    T_BLOCK_TOGGLE(1003),
    T_BLOCK_HEADER(1004),

    T_COMMENT(1005),
    T_CONTROL(1006),
    T_DELIMITER(1007),
    T_SET_DELIMITER(1008),
    T_PARAMETER(1009),
    T_VARIABLE(1010),

    T_OTHER(2000);

    private final int type;

    SQLTokenType(int type) {
        this.type = type;
    }

    @Override
    public int getTokenType() {
        return type;
    }

    @Override
    public String getTypeId() {
        return null;
    }

}
