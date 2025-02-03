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
package org.jkiss.dbeaver.ext.cubrid.model;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformerExt;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.sql.SQLQuery;

public class QueryTransformerLimitCubrid extends QueryTransformerLimit
        implements DBCQueryTransformerExt {

    public QueryTransformerLimitCubrid() {
        super(true);
    }

    @Override
    public boolean isApplicableTo(SQLQuery query) {
        Statement statement = query.getStatement();
        return statement != null && isLimitApplicable(statement);
    }

    public boolean isLimitApplicable(Statement statement) {
        if (statement instanceof PlainSelect select) {
            String where = String.valueOf(select.getWhere()).toUpperCase();
            if (where.contains("ROWNUM") || where.contains("INST_NUM")) {
                return false;
            }

            String having = String.valueOf(select.getHaving()).toUpperCase();
            if (having.contains("GROUPBY_NUM")) {
                return false;
            }
        }

        return true;
    }
}
