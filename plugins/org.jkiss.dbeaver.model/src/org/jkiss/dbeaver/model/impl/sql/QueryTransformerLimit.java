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
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryType;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.util.regex.Pattern;

/**
* Query transformer for LIMIT
*/
public class QueryTransformerLimit implements DBCQueryTransformer {

    //private static final Pattern SELECT_PATTERN = Pattern.compile("\\s*(?:select|update|delete|insert).+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final String KEYWORD_LIMIT = "LIMIT";
    public static final String KEYWORD_OFFSET = "OFFSET";

    public static final Pattern NON_LIMIT_QUERY_PATTERN = Pattern.compile("\\s+(LIMIT|OFFSET|INTO|UPDATE|PROCEDURE|FETCH)\\s+");

    private boolean supportsExtendedLimit;
    private boolean supportsOffsetKeyword;
    private Number offset;
    private Number length;
    private boolean limitSet;

    public QueryTransformerLimit() {
        this(true);
    }

    public QueryTransformerLimit(boolean supportsExtendedLimit) {
        this(supportsExtendedLimit, false);
    }

    public QueryTransformerLimit(boolean supportsExtendedLimit, boolean supportsOffsetKeyword) {
        this.supportsExtendedLimit = supportsExtendedLimit;
        this.supportsOffsetKeyword = supportsOffsetKeyword;
    }

    @Override
    public void setParameters(Object... parameters) {
        this.offset = (Number) parameters[0];
        this.length = (Number) parameters[1];
    }

    @Override
    public String transformQueryString(SQLQuery query) throws DBCException {
        String newQuery;
        String testQuery = query.getText().toUpperCase().trim();
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(query.getDataSource());
        boolean plainSelect = query.isPlainSelect();
        if (!plainSelect && query.getType() == SQLQueryType.UNKNOWN) {
            // Not parsed. Try to check with simple matcher
            plainSelect = "SELECT".equals(SQLUtils.getFirstKeyword(dialect, testQuery));
        }
        if (plainSelect) {
            plainSelect = !NON_LIMIT_QUERY_PATTERN.matcher(testQuery).find();
        }
        if (!plainSelect) {
            // Do not use limit if it is not a select or it already has LIMIT or it is SELECT INTO statement
            limitSet = false;
            newQuery = query.getText();
        } else {
            String normalizedQuery = SQLUtils.removeQueryDelimiter(dialect, query.getText());
            if (supportsExtendedLimit) {
                newQuery = normalizedQuery + "\n" + KEYWORD_LIMIT + " " + offset + ", " + length;
            } else if (supportsOffsetKeyword) {
                // LIMIT + OFFSET
                newQuery = normalizedQuery + "\n" + KEYWORD_LIMIT + " " + length.longValue();
                if (offset.longValue() > 0) {
                    newQuery += " " + KEYWORD_OFFSET + " " + offset.longValue();
                }
            } else {
                // We can limit only total row number
                newQuery = normalizedQuery + "\n" + KEYWORD_LIMIT + " " + (offset.longValue() + length.longValue());
            }
            limitSet = supportsExtendedLimit || supportsOffsetKeyword;
        }
        return newQuery;
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        if (!limitSet) {
            statement.setLimit(offset.longValue(), length.longValue());
        }
    }
}
