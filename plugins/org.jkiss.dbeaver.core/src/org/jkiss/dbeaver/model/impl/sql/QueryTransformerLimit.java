/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;

import java.util.regex.Pattern;

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
    private String limitKeyword;

    public QueryTransformerLimit() {
        this(true, KEYWORD_LIMIT);
    }

    public QueryTransformerLimit(boolean supportsOffset) {
        this(supportsOffset, KEYWORD_LIMIT);
    }

    public QueryTransformerLimit(boolean supportsOffset, String limitKeyword) {
        this.supportsOffset = supportsOffset;
        this.limitKeyword = limitKeyword;
    }

    @Override
    public void setParameters(Object... parameters) {
        this.offset = (Number) parameters[0];
        this.length = (Number) parameters[1];
    }

    @Override
    public String transformQueryString(String query) throws DBCException {
        String testQuery = query.toUpperCase().trim();
        if (!testQuery.startsWith("SELECT") || testQuery.contains(limitKeyword) || testQuery.contains("INTO")) {
            // Do not use limit if it is not a select or it already has LIMIT or it is SELECT INTO statement
            limitSet = false;
        } else {
            if (supportsOffset) {
                query = query + SQLUtils.TOKEN_TRANSFORM_START + " " + limitKeyword + " " + offset + ", " + length + SQLUtils.TOKEN_TRANSFORM_END;
            } else {
                // We can limit only total row number
                query = query + SQLUtils.TOKEN_TRANSFORM_START + " " + limitKeyword + " " + (offset.longValue() + length.longValue()) + SQLUtils.TOKEN_TRANSFORM_END;
            }
            limitSet = supportsOffset;
        }
        return query;
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        if (!limitSet) {
            statement.setLimit(offset.longValue(), length.longValue());
        }
    }
}
