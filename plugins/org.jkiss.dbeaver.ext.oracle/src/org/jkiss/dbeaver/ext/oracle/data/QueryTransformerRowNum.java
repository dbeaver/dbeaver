/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.data;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;

/**
* Query transformer for ROWNUM
*/
public class QueryTransformerRowNum implements DBCQueryTransformer {

    private static final Log log = Log.getLog(QueryTransformerRowNum.class);

    private Number offset;
    private Number length;

    @Override
    public void setParameters(Object... parameters) {
        this.offset = (Number) parameters[0];
        this.length = (Number) parameters[1];
    }

    @Override
    public String transformQueryString(SQLQuery query) throws DBCException {
        long totalRows = offset.longValue() + length.longValue();
        if (query.isPlainSelect()) {
            try {
                Statement statement = query.getStatement();
                if (statement instanceof Select) {
                    Select select = (Select) statement;
                    if (select.getSelectBody() instanceof PlainSelect) {
                        SQLSemanticProcessor.addWhereToSelect(
                            (PlainSelect) select.getSelectBody(),
                            "ROWNUM <= " + totalRows);
                        return statement.toString();
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
        statement.setLimit(offset.longValue(), length.longValue());
    }
}
