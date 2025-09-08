/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.exec.DBCQueryTransformerExt;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.sql.SQLQuery;

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
        if (statement instanceof PlainSelect select) {
            return query.isPlainSelect() && select.getFromItem() != null;
        }
        return false;
    }
    
}
