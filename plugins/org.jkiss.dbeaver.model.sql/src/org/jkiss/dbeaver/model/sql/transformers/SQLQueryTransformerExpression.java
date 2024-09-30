/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.transformers;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryTransformer;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;

/**
 * SQLQueryTransformerExpression.
 * Transforms SQL expression into something like SELECT <expression> FROM DUAL.
*/
public class SQLQueryTransformerExpression implements SQLQueryTransformer {
    @Override
    public SQLQuery transformQuery(DBPDataSource dataSource, SQLSyntaxManager syntaxManager, SQLQuery query) throws DBException {
        String dualTableName = dataSource.getSQLDialect().getDualTableName();
        String newQuery = "SELECT " + query.getText();
        if (dualTableName != null) {
            newQuery += " FROM " + dualTableName;
        }
        return new SQLQuery(dataSource, newQuery, query, false);
    }
}
