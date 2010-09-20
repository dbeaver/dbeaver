/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCQueryTransformer;
import org.jkiss.dbeaver.model.dbc.DBCStatement;

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
        if (query.toUpperCase().indexOf("LIMIT") != -1) {
            limitSet = false;
        } else {
            query = query + " LIMIT " + offset + ", " + length;
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
