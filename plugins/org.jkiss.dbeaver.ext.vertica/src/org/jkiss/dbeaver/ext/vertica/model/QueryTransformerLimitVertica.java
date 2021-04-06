/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.vertica.model;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformerExt;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.utils.CommonUtils;

/**
* Query transformer for LIMIT.
 * No applicable to queries without FROM (see #8167)
*/
public class QueryTransformerLimitVertica extends QueryTransformerLimit implements DBCQueryTransformerExt {
    public QueryTransformerLimitVertica() {
        super(false);
    }

    @Override
    public boolean isApplicableTo(SQLQuery query) {
        Statement statement = query.getStatement();
        return statement != null && isLimitApplicable(statement);
    }

    public boolean isLimitApplicable(Statement statement) {
        if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
            PlainSelect selectBody = (PlainSelect) ((Select) statement).getSelectBody();
            return selectBody.getFromItem() != null &&
                CommonUtils.isEmpty(selectBody.getIntoTables()) &&
                selectBody.getLimit() == null &&
                selectBody.getTop() == null &&
                !selectBody.isForUpdate();
        }
        return false;
    }
}
