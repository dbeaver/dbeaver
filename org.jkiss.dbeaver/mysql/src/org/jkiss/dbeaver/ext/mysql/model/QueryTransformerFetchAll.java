/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCQueryTransformer;
import org.jkiss.dbeaver.model.dbc.DBCStatement;

import java.sql.SQLException;
import java.sql.Statement;

/**
* Query transformer for fetch-all selects
*/
class QueryTransformerFetchAll implements DBCQueryTransformer {

    public void setParameters(Object... parameters)
    {
    }

    public String transformQueryString(String query) throws DBCException {
        return query;
    }

    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        // Set fetch size to Integer.MIN_VALUE to enable result set streaming
        try {
            ((Statement)statement).setFetchSize(Integer.MIN_VALUE);
        } catch (SQLException e) {
            throw new DBCException(e);
        }
    }
}