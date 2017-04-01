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
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryType;

/**
* Query transformer for LIMIT
*/
public class QueryTransformerLimit implements DBCQueryTransformer {

    //private static final Pattern SELECT_PATTERN = Pattern.compile("\\s*(?:select|update|delete|insert).+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final String KEYWORD_LIMIT = "LIMIT";

    private boolean supportsOffset;
    private Number offset;
    private Number length;
    private boolean limitSet;

    public QueryTransformerLimit() {
        this(true);
    }

    public QueryTransformerLimit(boolean supportsOffset) {
        this.supportsOffset = supportsOffset;
    }

    @Override
    public void setParameters(Object... parameters) {
        this.offset = (Number) parameters[0];
        this.length = (Number) parameters[1];
    }

    @Override
    public String transformQueryString(SQLQuery query) throws DBCException {
        String newQuery;
        boolean plainSelect = query.isPlainSelect();
        if (!plainSelect && query.getType() == SQLQueryType.UNKNOWN) {
            // Not parsed. Try to check with simple matcher
            String testQuery = query.getText().toUpperCase().trim();
            plainSelect = testQuery.startsWith("SELECT") &&
                !testQuery.contains("LIMIT") &&
                !testQuery.contains("INTO") &&
                !testQuery.contains("UPDATE") &&
                !testQuery.contains("PROCEDURE");
        }
        if (!plainSelect) {
            // Do not use limit if it is not a select or it already has LIMIT or it is SELECT INTO statement
            limitSet = false;
            newQuery = query.getText();
        } else {
            if (supportsOffset) {
                newQuery = query.getText() + "\n" + KEYWORD_LIMIT + " " + offset + ", " + length;
            } else {
                // We can limit only total row number
                newQuery = query.getText() + "\n" + KEYWORD_LIMIT + " " + (offset.longValue() + length.longValue());
            }
            limitSet = supportsOffset;
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
