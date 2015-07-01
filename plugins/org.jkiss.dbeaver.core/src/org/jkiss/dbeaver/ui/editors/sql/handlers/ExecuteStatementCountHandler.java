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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.jkiss.dbeaver.Log;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryTransformer;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecuteStatementCountHandler extends AbstractHandler implements SQLQueryTransformer {

    static final Log log = Log.getLog(ExecuteStatementCountHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor != null) {
            editor.processSQL(false, false, this);
        }
        return null;
    }

    @Override
    public void transformQuery(SQLQuery query) throws DBException {
        try {
            Statement statement = CCJSqlParserUtil.parse(query.getQuery());
            if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
                PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
                List<SelectItem> selectItems = new ArrayList<SelectItem>();
                Function countFunc = new Function();
                countFunc.setName("count");
                countFunc.setParameters(new ExpressionList(Collections.<Expression>singletonList(new Column("*"))));
                SelectItem countItem = new SelectExpressionItem(countFunc);
                selectItems.add(countItem);
                select.setSelectItems(selectItems);
                query.setQuery(select.toString());
            } else {
                throw new DBException("Query [" + query.getQuery() + "] can't be modified");
            }
        } catch (JSQLParserException e) {
            throw new DBException("Can't transform query to SELECT count(*)", e);
        }
    }

}