/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.sql.SQLQuery;

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
    public String transformQueryString(SQLQuery query) throws DBCException {
        return query.getQuery();
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        // Set fetch size to Integer.MIN_VALUE to enable result set streaming
        try {
            ((Statement)statement).setFetchSize(Integer.MIN_VALUE);
        } catch (SQLException e) {
            throw new DBCException(e, statement.getSession().getDataSource());
        }
    }
}