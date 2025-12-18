/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.starrocks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDialect;
import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.Arrays;

/**
 * StarRocks SQL Dialect - extends MySQL dialect with StarRocks-specific features.
 *
 * StarRocks is MySQL-compatible but has additional features like:
 * - Multi-level catalog support (SET CATALOG)
 * - Different storage engines (OLAP, etc.)
 * - Materialized views
 * - External catalog support (Hive, Iceberg, etc.)
 */
public class StarRocksDialect extends MySQLDialect {

    private static final String[] STARROCKS_KEYWORDS = {
        // Catalog keywords
        "CATALOG",
        "CATALOGS",
        // StarRocks-specific
        "OLAP",
        "DUPLICATE",
        "AGGREGATE",
        "UNIQUE",
        "PRIMARY",
        "DISTRIBUTED",
        "BUCKETS",
        "PROPERTIES",
        "BROKER",
        "ROUTINE",
        "LOAD",
        "LABEL",
        "SYNC",
        "ASYNC",
        "REFRESH",
        "MATERIALIZED",
        // External tables
        "EXTERNAL",
        "ICEBERG",
        "HIVE",
        "HUDI",
        "JDBC",
        "ELASTICSEARCH",
        "FILE"
    };

    private static final String[] STARROCKS_FUNCTIONS = {
        // Array functions
        "ARRAY_AGG",
        "ARRAY_CONTAINS",
        "ARRAY_LENGTH",
        "ARRAY_POSITION",
        "ARRAY_REMOVE",
        "ARRAY_SORT",
        "ARRAY_DISTINCT",
        "ARRAY_JOIN",
        "ARRAY_SLICE",
        "CARDINALITY",
        // Bitmap functions
        "BITMAP_UNION",
        "BITMAP_INTERSECT",
        "BITMAP_COUNT",
        "BITMAP_AND",
        "BITMAP_OR",
        "BITMAP_XOR",
        "BITMAP_CONTAINS",
        "TO_BITMAP",
        "BITMAP_FROM_STRING",
        // HLL functions
        "HLL_UNION_AGG",
        "HLL_CARDINALITY",
        "HLL_HASH",
        // JSON functions
        "JSON_QUERY",
        "JSON_VALUE",
        "JSON_EXISTS",
        "JSON_OBJECT",
        "JSON_ARRAY",
        "PARSE_JSON",
        "GET_JSON_DOUBLE",
        "GET_JSON_INT",
        "GET_JSON_STRING",
        // Window functions
        "LEAD",
        "LAG",
        "FIRST_VALUE",
        "LAST_VALUE",
        "RANK",
        "DENSE_RANK",
        "ROW_NUMBER",
        "NTILE",
        "PERCENT_RANK",
        "CUME_DIST",
        // Aggregate functions
        "APPROX_COUNT_DISTINCT",
        "PERCENTILE_APPROX",
        "PERCENTILE_CONT",
        "PERCENTILE_DISC",
        "RETENTION",
        "WINDOW_FUNNEL",
        // String functions
        "SPLIT_PART",
        "REGEXP_EXTRACT",
        "REGEXP_REPLACE",
        "PARSE_URL",
        "URL_EXTRACT_HOST",
        "URL_EXTRACT_PATH",
        // Date/Time functions
        "DATE_TRUNC",
        "TIME_SLICE",
        "MONTHS_DIFF",
        "YEARS_DIFF",
        "WEEKS_DIFF",
        "DAYS_DIFF",
        "HOURS_DIFF",
        "MINUTES_DIFF",
        "SECONDS_DIFF",
        // Utility functions
        "UUID",
        "MURMUR_HASH3_32",
        "MD5_SUM",
        "SM3"
    };

    public StarRocksDialect() {
        super();
        // Add StarRocks-specific keywords
        addSQLKeywords(Arrays.asList(STARROCKS_KEYWORDS));
        addFunctions(Arrays.asList(STARROCKS_FUNCTIONS));
    }

    /**
     * Override to handle StarRocksDataSource instead of MySQLDataSource.
     * This prevents ClassCastException when parent="mysql" is set in plugin.xml.
     */
    @Override
    public void afterDataSourceInitialization(@NotNull DBPDataSource dataSource) {
        if (dataSource instanceof StarRocksDataSource) {
            // Handle StarRocksDataSource
            int lowerCaseTableNames = ((StarRocksDataSource) dataSource).getLowerCaseTableNames();
            this.setSupportsUnquotedMixedCase(lowerCaseTableNames != 2);
        } else {
            // Fall back to parent implementation for MySQLDataSource
            super.afterDataSourceInitialization(dataSource);
        }
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "StarRocks";
    }

    @NotNull
    @Override
    public String[][] getIdentifierQuoteStrings() {
        // StarRocks uses backticks like MySQL
        return new String[][]{
            {"`", "`"},
            {"\"", "\""}
        };
    }

    @Override
    public boolean supportsSubqueries() {
        return true;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return true;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return false;
    }

    @Override
    public boolean supportsOrderByIndex() {
        return true;
    }

    @Override
    public boolean supportsNestedComments() {
        return false;
    }

    @Override
    public boolean supportsCommentQuery() {
        return true;
    }

    @Override
    public boolean supportsNullability() {
        return true;
    }
}
