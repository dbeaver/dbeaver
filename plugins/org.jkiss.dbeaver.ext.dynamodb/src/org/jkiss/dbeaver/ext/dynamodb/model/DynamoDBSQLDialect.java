/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dynamodb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;

import java.util.Collections;

public class DynamoDBSQLDialect extends BasicSQLDialect {

    private static final String[] PARTIQL_KEYWORDS = {
        "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "BETWEEN",
        "INSERT", "INTO", "VALUE", "VALUES", "UPDATE", "SET", "REMOVE",
        "DELETE", "EXISTS", "IS", "NULL", "TRUE", "FALSE", "MISSING",
        "BY", "ORDER", "ASC", "DESC"
    };

    private static final String[] PARTIQL_FUNCTIONS = {
        "attribute_exists", "attribute_not_exists", "attribute_type",
        "begins_with", "contains", "size", "list_append", "if_not_exists"
    };

    public DynamoDBSQLDialect() {
        super();
        for (String kw : PARTIQL_KEYWORDS) {
            addSQLKeyword(kw);
        }
        for (String fn : PARTIQL_FUNCTIONS) {
            addFunctions(Collections.singletonList(fn));
        }
    }

    @NotNull
    @Override
    public String getDialectId() {
        return "dynamodb";
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "DynamoDB PartiQL";
    }

    @Nullable
    @Override
    public String[][] getIdentifierQuoteStrings() {
        return new String[][]{{"\"", "\""}};
    }

    @NotNull
    @Override
    public String[][] getStringQuoteStrings() {
        return new String[][]{{"'", "'"}};
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return new String[0];
    }

    @NotNull
    @Override
    public String[] getDDLKeywords() {
        return new String[0];
    }

    @NotNull
    @Override
    public String[] getDMLKeywords() {
        return new String[]{"SELECT", "INSERT", "UPDATE", "DELETE"};
    }

    @NotNull
    @Override
    public String[] getNonTransactionKeywords() {
        return new String[]{"SELECT"};
    }

    @Override
    public boolean supportsSubqueries() {
        return false;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return false;
    }

    @Override
    public boolean supportsGroupBy() {
        return false;
    }

    @Override
    public boolean supportsOrderBy() {
        return false;
    }

    @Override
    public int getCatalogUsage() {
        return USAGE_NONE;
    }

    @Override
    public int getSchemaUsage() {
        return USAGE_NONE;
    }
}
