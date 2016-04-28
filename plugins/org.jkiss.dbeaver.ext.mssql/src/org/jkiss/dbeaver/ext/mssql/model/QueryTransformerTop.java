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
package org.jkiss.dbeaver.ext.mssql.model;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.Top;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.utils.CommonUtils;

/**
* Query transformer for TOP
*/
public class QueryTransformerTop implements DBCQueryTransformer {

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
                            top.setRowCount(offset.longValue() + length.longValue());
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
        return query.getQuery();
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        if (!limitSet) {
            statement.setLimit(offset.longValue(), length.longValue());
        }
    }
}
