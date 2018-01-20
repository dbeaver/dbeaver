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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;

/**
* PostgreSQL dialect
*/
class PostgreDialect extends JDBCSQLDialect {

    public static final String[] POSTGRE_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "SHOW", "SET"
        }
    );

    public PostgreDialect() {
        super("PostgreSQL");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);

        addSQLKeywords(
            Arrays.asList(
                "SHOW",
                "TYPE",
                "USER",
                "COMMENT",
                "MATERIALIZED",
                "ILIKE",
                "ELSIF",
                "ELSEIF",
                "ANALYSE",
                "ANALYZE",
                "CONCURRENTLY",
                "FREEZE",
                "LANGUAGE",
                "MODULE",
                "OFFSET",
                "PUBLIC",
                "RETURNING",
                "VARIADIC",
                "PERFORM",
                "FOREACH",
                "LOOP",
                "PERFORM",
                "RAISE",
                "NOTICE"
            ));

        addFunctions(
            Arrays.asList(
                "CURRENT_DATABASE",
                "ARRAY_AGG",
                "BIT_AND",
                "BIT_OR",
                "BOOL_AND",
                "BOOL_OR",
                "JSON_AGG",
                "JSONB_AGG",
                "JSON_OBJECT_AGG",
                "JSONB_OBJECT_AGG",
                "STRING_AGG",
                "XMLAGG",
                "BIT_LENGTH",
                "CURRENT_CATALOG",
                "CURRENT_SCHEMA",
                "SQLCODE",
                "LENGTH",
                "SQLERROR"
            ));

        removeSQLKeyword("LENGTH");
    }

    @Override
    public int getCatalogUsage() {
        return SQLDialect.USAGE_NONE;
    }

    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }

    @Nullable
    @Override
    public String getBlockToggleString() {
        return "$" + SQLConstants.KEYWORD_PATTERN_CHARS + "$";
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        // PostgreSQL-specific blocks ($$) should be used everywhere
        return null;//super.getBlockBoundStrings();
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return true;
    }

    @Override
    public boolean supportsCommentQuery() {
        return true;
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return PostgreBinaryFormatter.INSTANCE;
    }

    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(PostgreConstants.DATA_TYPE_ALIASES.keySet());
    }

    @NotNull
    @Override
    protected String[] getNonTransactionKeywords() {
        return POSTGRE_NON_TRANSACTIONAL_KEYWORDS;
    }
}
