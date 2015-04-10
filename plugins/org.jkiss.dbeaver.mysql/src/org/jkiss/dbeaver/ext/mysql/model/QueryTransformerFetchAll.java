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