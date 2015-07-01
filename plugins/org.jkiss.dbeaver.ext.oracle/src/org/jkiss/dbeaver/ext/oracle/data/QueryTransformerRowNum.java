/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

    static final Log log = Log.getLog(QueryTransformerRowNum.class);

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
        return query.getQuery();
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        statement.setLimit(offset.longValue(), length.longValue());
    }
}
