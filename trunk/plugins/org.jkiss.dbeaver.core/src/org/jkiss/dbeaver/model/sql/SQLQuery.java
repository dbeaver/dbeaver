/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.sql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Database;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLBlockBeginToken;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLBlockEndToken;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLParameterToken;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLQuery
 */
public class SQLQuery {

    static final Log log = LogFactory.getLog(SQLQuery.class);

    private String query;
    private List<SQLQueryParameter> parameters;
    private int offset;
    private int length;
    private Object data;
    private SQLQueryType type;
    private Statement statement;
    private SingleTableMeta singleTableMeta;

    public SQLQuery(String query, int offset, int length)
    {
        this.query = query;
        this.offset = offset;
        this.length = length;
        try {
            statement = CCJSqlParserUtil.parse(query);
            if (statement instanceof Select) {
                type = SQLQueryType.SELECT;
                // Detect single source table
                SelectBody selectBody = ((Select) statement).getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    if (plainSelect.getFromItem() instanceof Table &&
                        CommonUtils.isEmpty(plainSelect.getJoins()) &&
                        CommonUtils.isEmpty(plainSelect.getGroupByColumnReferences()) &&
                        plainSelect.getInto() == null)
                    {
                        Table fromItem = (Table) plainSelect.getFromItem();
                        Database database = fromItem.getDatabase();
                        String schemaName = fromItem.getSchemaName();
                        String tableName = fromItem.getName();
                        singleTableMeta = new SingleTableMeta(
                            database == null ? null : database.getDatabaseName(),
                            schemaName,
                            tableName);
                    }
                }
            } else if (statement instanceof Insert) {
                type = SQLQueryType.INSERT;
            } else if (statement instanceof Update) {
                type = SQLQueryType.UPDATE;
            } else if (statement instanceof Delete) {
                type = SQLQueryType.DELETE;
            } else {
                type = SQLQueryType.DDL;
            }
        } catch (JSQLParserException e) {
            this.type = SQLQueryType.UNLKNOWN;
            log.debug("Error parsing SQL query [" + query + "]:" + CommonUtils.getRootCause(e).getMessage());
        }
    }

    public String getQuery()
    {
        return query;
    }

    public Statement getStatement() {
        return statement;
    }

    public List<SQLQueryParameter> getParameters() {
        return parameters;
    }

    public int getOffset()
    {
        return offset;
    }

    public int getLength()
    {
        return length;
    }

    /**
     * User defined data object. May be used to identify statements.
     * @return data or null
     */
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public SQLQueryType getType()
    {
        return type;
    }

    public DBCEntityMetaData getSingleSource() {
        return singleTableMeta;
    }

    public void parseParameters(IDocument document, SQLSyntaxManager syntaxManager)
    {
        syntaxManager.setRange(document, offset, length);
        int blockDepth = 0;
        for (;;) {
            IToken token = syntaxManager.nextToken();
            int tokenOffset = syntaxManager.getTokenOffset();
            final int tokenLength = syntaxManager.getTokenLength();
            if (token.isEOF() || tokenOffset > offset + length) {
                break;
            }
            // Handle only parameters which are not in SQL blocks
            if (token instanceof SQLBlockBeginToken) {
                blockDepth++;
            }else if (token instanceof SQLBlockEndToken) {
                blockDepth--;
            }
            if (token instanceof SQLParameterToken && tokenLength > 0 && blockDepth <= 0) {
                try {
                    String paramName = document.get(tokenOffset, tokenLength);
                    if (parameters == null) {
                        parameters = new ArrayList<SQLQueryParameter>();
                    }
                    parameters.add(
                        new SQLQueryParameter(
                            parameters.size(),
                            paramName));
                } catch (BadLocationException e) {
                    log.warn("Can't extract query parameter", e);
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return query;
    }

    private static class SingleTableMeta implements DBCEntityMetaData {

        private final String catalogName;
        private final String schemaName;
        private final String tableName;

        private SingleTableMeta(String catalogName, String schemaName, String tableName) {
            this.catalogName = catalogName;
            this.schemaName = schemaName;
            this.tableName = tableName;
        }

        @Override
        public String getCatalogName() {
            return catalogName;
        }

        @Override
        public String getSchemaName() {
            return schemaName;
        }

        @Override
        public String getEntityName() {
            return tableName;
        }

        @Override
        public List<? extends DBCAttributeMetaData> getAttributes() {
            return Collections.emptyList();
        }
    }

}
