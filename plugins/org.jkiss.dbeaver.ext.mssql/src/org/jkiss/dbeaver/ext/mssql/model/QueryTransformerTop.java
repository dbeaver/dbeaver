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
package org.jkiss.dbeaver.ext.mssql.model;

import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.Top;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformerExt;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.utils.CommonUtils;

/**
* Query transformer for TOP
*/
public class QueryTransformerTop implements DBCQueryTransformer, DBCQueryTransformerExt {

    private static final Log log = Log.getLog(QueryTransformerTop.class);

    private Number offset;
    private Number length;
    private boolean limitSet;

    @Override
    public void setParameters(Object... parameters) {
        this.offset = (Number) parameters[0];
        this.length = (Number) parameters[1];
    }

    @Override
    public String transformQueryString(SQLQuery query) throws DBCException {
        limitSet = false;
        if (query.isPlainSelect()) {
            try {
                Statement statement = query.getStatement();
                if (statement instanceof Select) {
                    Select select = (Select) statement;
                    if (select.getSelectBody() instanceof PlainSelect) {
                        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
                        if (selectBody.getTop() == null && CommonUtils.isEmpty(selectBody.getIntoTables())) {
                            Top top = new Top();
                            top.setPercentage(false);
                            top.setExpression(new LongValue(offset.longValue() + length.longValue()));
                            selectBody.setTop(top);

                            limitSet = true;
                            return statement.toString();
                        }
                    }
                }
            } catch (Throwable e) {
                // ignore
                log.debug(e);
            }
        }
        return query.getText();
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        if (!limitSet) {
            statement.setLimit(offset.longValue(), length.longValue());
        }
    }

    @Override
    public boolean isApplicableTo(SQLQuery query) {
        // TOP cannot be used with OFFSET. See #13594
        if (query.isPlainSelect()) {
            final Statement statement = query.getStatement();
            if (statement instanceof Select) {
                final SelectBody body = ((Select) statement).getSelectBody();
                if (body instanceof PlainSelect) {
                    return ((PlainSelect) body).getOffset() == null;
                }
            }
        }
        return false;
    }
}
