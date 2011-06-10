/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;

/**
* Query transformer for RS limit
*/
class QueryTransformerLimit implements DBCQueryTransformer {

    private Object offset;
    private Object length;
    private boolean limitSet;

    public void setParameters(Object... parameters) {
        this.offset = parameters[0];
        this.length = parameters[1];
    }

    public String transformQueryString(String query) throws DBCException {
        String testQuery = query.toUpperCase().trim();
        if (!testQuery.startsWith("SELECT") || testQuery.indexOf("LIMIT") != -1) {
            limitSet = false;
        } else {
            query = query + SQLUtils.TOKEN_TRANSFORM_START + " LIMIT " + offset + ", " + length + SQLUtils.TOKEN_TRANSFORM_END;
            limitSet = true;
        }
        return query;
    }

    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        if (!limitSet) {
            statement.setLimit(((Number)offset).longValue(), ((Number)length).longValue());
        }
    }
}
