/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;

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
    }

}