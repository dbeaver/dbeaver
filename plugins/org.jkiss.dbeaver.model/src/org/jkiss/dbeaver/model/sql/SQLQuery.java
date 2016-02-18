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

package org.jkiss.dbeaver.model.sql;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Database;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLQuery
 */
public class SQLQuery {

    @NotNull
    private final String originalQuery;
    @NotNull
    private String query;
    private int offset;
    private int length;
    private Object data;
    @NotNull
    private SQLQueryType type;
    @Nullable
    private Statement statement;
    private List<SQLQueryParameter> parameters;
    private SingleTableMeta singleTableMeta;
    private Map<String, SQLSelectItem> selectItems;

    public SQLQuery(@NotNull String query)
    {
        this(query, 0, query.length());
    }

    public SQLQuery(@NotNull String query, int offset, int length)
    {
        this.originalQuery = this.query = query;
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
                        CommonUtils.isEmpty(plainSelect.getIntoTables()))
                    {
                        Table fromItem = (Table) plainSelect.getFromItem();
                        Database database = fromItem.getDatabase();
                        String catalogName = database == null ? null : database.getDatabaseName();
                        String schemaName = fromItem.getSchemaName();
                        String tableName = fromItem.getName();
                        singleTableMeta = new SingleTableMeta(
                            unquoteIdentifier(catalogName),
                            unquoteIdentifier(schemaName),
                            unquoteIdentifier(tableName));
                    }
                    // Extract select items info
                    final List<SelectItem> items = plainSelect.getSelectItems();
                    if (items != null && !items.isEmpty()) {
                        selectItems = new LinkedHashMap<>();
                        for (SelectItem item : items) {
                            SQLSelectItem si = new SQLSelectItem(item);
                            selectItems.put(si.getName(), si);
                        }
                    }
                }
            } else if (statement instanceof Insert) {
                type = SQLQueryType.INSERT;
            } else if (statement instanceof Update) {
                type = SQLQueryType.UPDATE;
            } else if (statement instanceof Delete) {
                type = SQLQueryType.DELETE;
            } else if (statement instanceof Alter ||
                statement instanceof CreateTable ||
                statement instanceof CreateView ||
                statement instanceof Drop ||
                statement instanceof CreateIndex)
            {
                type = SQLQueryType.DDL;
            } else {
                type = SQLQueryType.UNKNOWN;
            }
        } catch (Throwable e) {
            this.type = SQLQueryType.UNKNOWN;
            //log.debug("Error parsing SQL query [" + query + "]:" + CommonUtils.getRootCause(e).getMessage());
        }
    }

    private String unquoteIdentifier(String name) {
        if (name == null) {
            return null;
        }
        return DBUtils.getUnQuotedIdentifier(name, "\"");
    }

    /**
     * Plain select is a SELECT statement without INTO clause, without LIMIT or TOP modifiers
     * @return true is this query is a plain select
     */
    public boolean isPlainSelect() {
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

    public SQLSelectItem getSelectItem(String name) {
        return selectItems == null ? null : selectItems.get(name);
    }

    @NotNull
    public String getOriginalQuery() {
        return originalQuery;
    }

    @NotNull
    public String getQuery()
    {
        return query;
    }

    public void setQuery(@NotNull String query) {
        this.query = query;
    }

    @Nullable
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

    @NotNull
    public SQLQueryType getType()
    {
        return type;
    }

    public DBCEntityMetaData getSingleSource() {
        return singleTableMeta;
    }

    public void setParameters(List<SQLQueryParameter> parameters)
    {
        this.parameters = parameters;
        if (parameters != null) {
            // Replace parameter tokens with "?" symbol
            for (int i = parameters.size(); i > 0; i--) {
                SQLQueryParameter parameter = parameters.get(i - 1);
                query = query.substring(0, parameter.getTokenOffset()) + "?" + query.substring(parameter.getTokenOffset() + parameter.getTokenLength());
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

        private SingleTableMeta(String catalogName, String schemaName, @NotNull String tableName) {
            this.catalogName = catalogName;
            this.schemaName = schemaName;
            this.tableName = tableName;
        }

        @Nullable
        @Override
        public String getCatalogName() {
            return catalogName;
        }

        @Nullable
        @Override
        public String getSchemaName() {
            return schemaName;
        }

        @NotNull
        @Override
        public String getEntityName() {
            return tableName;
        }

        @NotNull
        @Override
        public List<? extends DBCAttributeMetaData> getAttributes() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return DBUtils.getSimpleQualifiedName(catalogName, schemaName, tableName);
        }

        @Override
        public int hashCode() {
            return (catalogName == null ? 1 : catalogName.hashCode()) *
                (schemaName == null ? 2 : schemaName.hashCode()) *
                tableName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SingleTableMeta)) {
                return false;
            }
            SingleTableMeta md2 = (SingleTableMeta) obj;
            return CommonUtils.equalObjects(catalogName, md2.catalogName) &&
                CommonUtils.equalObjects(schemaName, md2.schemaName) &&
                CommonUtils.equalObjects(tableName, md2.tableName);
        }
    }

}
