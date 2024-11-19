/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.bigquery.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;

/**
 * BigQuery SQL dialect
 */
public class BigQuerySQLDialect extends GenericSQLDialect {

    private static final String[] EXEC_KEYWORDS = {"CALL"};

    private static final String[][] BIGQUERY_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + SQLConstants.KEYWORD_CASE},
        {"IF", SQLConstants.BLOCK_END + " IF"},
        {"LOOP", SQLConstants.BLOCK_END + " LOOP"},
        {"WHILE", SQLConstants.BLOCK_END + " WHILE"},
        {"FOR", SQLConstants.BLOCK_END + " FOR"},
        {"REPEAT", SQLConstants.BLOCK_END + " REPEAT"}
    };

    public BigQuerySQLDialect() {
        super("BigQuery", "google_bigquery");
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public char getStringEscapeCharacter() {
        return '\\';
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return BIGQUERY_BEGIN_END_BLOCK;
    }
}
