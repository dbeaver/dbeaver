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
package org.jkiss.dbeaver.ext.postgresql.model;

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
        return query.getText();
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        // Set fetch size to 100k (maybe this should be configurable?)
        try {
            ((Statement)statement).setFetchSize(100000);
        } catch (SQLException e) {
            throw new DBCException(e, statement.getSession().getDataSource());
        }
    }
}