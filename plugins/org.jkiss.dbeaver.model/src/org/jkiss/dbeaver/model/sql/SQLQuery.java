/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQLQuery
 */
public class SQLQuery implements SQLScriptElement {

    private static final Pattern QUERY_TITLE_PATTERN = Pattern.compile("(?:--|/\\*)\\s*(?:NAME|TITLE)\\s*:\\s*(.+)\\s*", Pattern.CASE_INSENSITIVE);

    @Nullable
    private final DBPDataSource dataSource;
    @NotNull
    private String originalText;
    @NotNull
    private String text;
    private int offset;
    private int length;
    private Object data;
    private int resultsOffset = -1;
    private int resultsMaxRows = -1;
    @NotNull
    private SQLQueryType type;
    @Nullable
    private Statement statement;
    private List<SQLQueryParameter> parameters;
    private SingleTableMeta singleTableMeta;
    private List<SQLSelectItem> selectItems;
    private String queryTitle;

    public SQLQuery(@Nullable DBPDataSource dataSource, @NotNull String text)
    {
        this(dataSource, text, 0, text.length());
    }

    /**
     * Copy constructor.
     * Copies query state but sets new query string.
     */
    public SQLQuery(@Nullable DBPDataSource dataSource, @NotNull String text, @NotNull SQLQuery sourceQuery) {
        this(dataSource, text, sourceQuery, true);
    }

    public SQLQuery(@Nullable DBPDataSource dataSource, @NotNull String text, @NotNull SQLQuery sourceQuery, boolean preserveOriginal) {
        this(dataSource, text, sourceQuery.offset, sourceQuery.length);
        if (preserveOriginal) {
            this.originalText = sourceQuery.originalText;
        }
        this.parameters = sourceQuery.parameters;
        this.data = sourceQuery.data;
    }

    public SQLQuery(@Nullable DBPDataSource dataSource, @NotNull String text, int offset, int length)
    {
        this.dataSource = dataSource;
        this.originalText = this.text = text;
        this.offset = offset;
        this.length = length;

        try {
            statement = CCJSqlParserUtil.parse(text);
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
                        selectItems = new ArrayList<>();
                        for (SelectItem item : items) {
                            selectItems.add(new SQLSelectItem(item));
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
        // Extract query title
        queryTitle = null;
        final Matcher matcher = QUERY_TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            queryTitle = matcher.group(1);
        }
    }

    private String unquoteIdentifier(String name) {
        if (name == null) {
            return null;
        }
        return DBUtils.getUnQuotedIdentifier(dataSource, name);
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
        if (selectItems == null) {
            return null;
        }
        for (SQLSelectItem item : selectItems) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public SQLSelectItem getSelectItem(int index) {
        return selectItems == null || selectItems.size() <= index ? null : selectItems.get(index);
    }

    @NotNull
    public String getOriginalText() {
        return originalText;
    }

    @NotNull
    public String getText()
    {
        return text;
    }

    public void setText(@NotNull String text) {
        this.text = text;
    }

    public String getQueryTitle() {
        return queryTitle;
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
    }

    public void reset() {
        this.text = this.originalText;
        if (this.parameters != null) {
            setParameters(this.parameters);
        }
    }

    @Override
    public String toString()
    {
        return text;
    }

    /**
     * Overrides results offset/limit for this particular query
     */
    public void setResultSetLimit(int rowOffset, int maxRows) {
        this.resultsOffset = rowOffset;
        this.resultsMaxRows = maxRows;
    }

    public int getResultsOffset() {
        return resultsOffset;
    }

    public int getResultsMaxRows() {
        return resultsMaxRows;
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SQLQuery && text.equals(((SQLQuery) obj).text);
    }
}
