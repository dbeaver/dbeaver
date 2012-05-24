/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;

import java.sql.SQLException;
import java.sql.Statement;

/**
* Query transformer for fetch-all selects
*/
class QueryTransformerFetchAll implements DBCQueryTransformer {

    @Override
    public void setParameters(Object... parameters)
    {
    }

    @Override
    public String transformQueryString(String query) throws DBCException {
        return query;
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        // Set fetch size to Integer.MIN_VALUE to enable result set streaming
        try {
            ((Statement)statement).setFetchSize(Integer.MIN_VALUE);
        } catch (SQLException e) {
            throw new DBCException(e);
        }
    }
}