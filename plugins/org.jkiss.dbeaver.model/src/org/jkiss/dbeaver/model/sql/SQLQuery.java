/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
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
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQLQuery
 */
public class SQLQuery implements SQLScriptElement {

    private static final Pattern QUERY_TITLE_PATTERN = Pattern.compile("^\\s*(?:--|//|/\\*)\\s*(?:name|title)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

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
    @Nullable
    private List<SQLQueryParameter> parameters;

    private Throwable parseError;
    private boolean parsed = false;
    @NotNull
    private SQLQueryType type;
    private Statement statement;
    private SingleTableMeta singleTableMeta, rawSingleTableMetadata;
    private List<SQLSelectItem> selectItems;
    private String queryTitle;
    private String extraErrorMessage;
    private List<String> allSelectEntitiesNames = new ArrayList<>();

    public SQLQuery(@Nullable DBPDataSource dataSource, @NotNull String text) {
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

    public SQLQuery(@Nullable DBPDataSource dataSource, @NotNull String text, int offset, int length) {
        this.dataSource = dataSource;
        this.originalText = this.text = text;
        this.offset = offset;
        this.length = length;
        this.type = SQLQueryType.UNKNOWN;

        // Extract query title
        queryTitle = null;
        final Matcher matcher = QUERY_TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            queryTitle = matcher.group(1).trim();
        }
    }

    public DBPDataSource getDataSource() {
        return dataSource;
    }

    private void parseQuery() {
        if (parsed) {
            return;
        }
        parsed = true;
        try {
            if (CommonUtils.isEmpty(text)) {
                this.statement = null;
                this.parseError = new DBException("Empty query");
                return;
            }
            statement = SQLSemanticProcessor.parseQuery(dataSource == null ? null : dataSource.getSQLDialect(), text);
            if (statement instanceof Select) {
                type = SQLQueryType.SELECT;
                // Detect single source table (no joins, no group by, no sub-selects)
                SelectBody selectBody = ((Select) statement).getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    FromItem fromItem = plainSelect.getFromItem();

                    if (fromItem instanceof SubSelect &&
                        isPotentiallySingleSourceSelect(plainSelect) &&
                        ((SubSelect) fromItem).getSelectBody() instanceof PlainSelect &&
                        isPotentiallySingleSourceSelect((PlainSelect) ((SubSelect) fromItem).getSelectBody()))
                    {
                        // Real select is in sub-select
                        plainSelect = (PlainSelect) ((SubSelect) fromItem).getSelectBody();
                        fromItem = plainSelect.getFromItem();
                    }
                    if (fromItem instanceof Table &&
                        isPotentiallySingleSourceSelect(plainSelect))
                    {
                        boolean hasSubSelects = false, hasDirectSelects = false;
                        for (SelectItem si : plainSelect.getSelectItems()) {
                            if (si instanceof SelectExpressionItem && ((SelectExpressionItem) si).getExpression() instanceof SubSelect) {
                                hasSubSelects = true;
                            } else if (si instanceof SelectExpressionItem && ((SelectExpressionItem) si).getExpression() instanceof Column) {
                                hasDirectSelects = true;
                            }
                        }
                        if (hasDirectSelects || !hasSubSelects) {
                            fillSingleSource((Table) fromItem);
                        }
                    }
                    if (!CommonUtils.isEmpty(plainSelect.getJoins()) && fromItem instanceof Table) {
                        createTargetName(plainSelect, (Table) fromItem);
                    }
                    // Extract select items info
                    final List<SQLSelectItem> items = CommonUtils.safeList(plainSelect.getSelectItems()).stream()
                        .filter(this::isValidSelectItem)
                        .map(item -> new SQLSelectItem(this, item))
                        .collect(Collectors.toList());
                    if (!items.isEmpty()) {
                        selectItems = items;
                    }
                }
            } else if (statement instanceof Insert) {
                type = SQLQueryType.INSERT;
                fillSingleSource(((Insert) statement).getTable());
            } else if (statement instanceof Update) {
                type = SQLQueryType.UPDATE;
                Table table = ((Update) statement).getTable();
                if (table != null) {
                    fillSingleSource(table);
                }
            } else if (statement instanceof Delete) {
                type = SQLQueryType.DELETE;
                if (((Delete) statement).getTable() != null) {
                    fillSingleSource(((Delete) statement).getTable());
                } else {
                    List<Table> tables = ((Delete) statement).getTables();
                    if (tables != null && tables.size() == 1) {
                        fillSingleSource(tables.get(0));
                    }
                }
            } else if (statement instanceof Alter ||
                statement instanceof CreateTable ||
                statement instanceof CreateView ||
                statement instanceof Drop ||
                statement instanceof CreateIndex) {
                type = SQLQueryType.DDL;
            } else if (statement instanceof Merge) {
                type = SQLQueryType.MERGE;
            } else {
                type = SQLQueryType.UNKNOWN;
            }
        } catch (Throwable e) {
            this.type = SQLQueryType.UNKNOWN;
            this.parseError = e;
            //log.debug("Error parsing SQL query [" + query + "]:" + CommonUtils.getRootCause(e).getMessage());
        }
    }

    private boolean isValidSelectItem(@NotNull SelectItem item) {
        // Workaround for JSQLParser not respecting the `#` comment in MySQL and treating them as valid values
        if (item instanceof SelectExpressionItem && dataSource != null) {
            final Expression expr = ((SelectExpressionItem) item).getExpression();
            if (expr instanceof Column) {
                final String name = CommonUtils.trim(((Column) expr).getColumnName());
                if (CommonUtils.isNotEmpty(name)) {
                    for (String comment : dataSource.getSQLDialect().getSingleLineComments()) {
                        if (name.startsWith(comment)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean isPotentiallySingleSourceSelect(PlainSelect plainSelect) {
        return CommonUtils.isEmpty(plainSelect.getJoins()) &&
            (plainSelect.getGroupBy() == null || CommonUtils.isEmpty(plainSelect.getGroupBy().getGroupByExpressionList().getExpressions())) &&
            CommonUtils.isEmpty(plainSelect.getIntoTables());
    }

    private void fillSingleSource(Table fromItem) {
        rawSingleTableMetadata = createOriginalSourceTableMetaData(fromItem);
        singleTableMeta = createUnquotedTableMetaData(rawSingleTableMetadata);
    }

    SingleTableMeta createTableMetaData(Table fromItem) {
        return createUnquotedTableMetaData(createOriginalSourceTableMetaData(fromItem));
    }

    private SingleTableMeta createOriginalSourceTableMetaData(Table fromItem) {
        Database database = fromItem.getDatabase();
        String catalogName = database == null ? null : database.getDatabaseName();
        String schemaName = fromItem.getSchemaName();
        String tableName = fromItem.getName();
        return new SingleTableMeta(catalogName, schemaName, tableName);
    }

    private SingleTableMeta createUnquotedTableMetaData(SingleTableMeta tableMeta) {
        return new SingleTableMeta(
            unquoteIdentifier(tableMeta.getCatalogName()),
            unquoteIdentifier(tableMeta.getSchemaName()),
            unquoteIdentifier(tableMeta.getEntityName()));
    }

    private String unquoteIdentifier(String name) {
        if (name == null) {
            return null;
        }
        return dataSource == null ?
            DBUtils.getUnQuotedIdentifier(name, SQLConstants.DEFAULT_IDENTIFIER_QUOTE) :
            DBUtils.getUnQuotedIdentifier(dataSource, name);
    }

    /**
     * Plain select is a SELECT statement without INTO clause, without LIMIT or TOP modifiers
     *
     * @return true is this query is a plain select
     */
    public boolean isPlainSelect() {
        parseQuery();
        if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
            PlainSelect selectBody = (PlainSelect) ((Select) statement).getSelectBody();
            return CommonUtils.isEmpty(selectBody.getIntoTables()) &&
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

    public int getSelectItemCount() {
        return selectItems == null ? 0 : selectItems.size();
    }

    public SQLSelectItem getSelectItem(int index) {
        return selectItems == null || selectItems.size() <= index ? null : selectItems.get(index);
    }

    public int getSelectItemAsteriskIndex() {
        if (selectItems != null) {
            for (int i = 0; i < selectItems.size(); i++) {
                SQLSelectItem item = selectItems.get(i);
                if (item.getName().contains("*")) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Sometime we want to know all source containers names from the query if it is Select statement and it has JOINs.
     * For the data transfer target file name, as example.
     * Contains only entities names without schema/catalog identifiers.
     *
     * @param plainSelect plain Select class
     */
    private void createTargetName(@NotNull PlainSelect plainSelect, @NotNull Table fromItem) {
        String fromItemName = fromItem.getName();
        if (CommonUtils.isNotEmpty(fromItemName)) {
            allSelectEntitiesNames.add(fromItemName);
        }
        List<Join> joins = plainSelect.getJoins();
        for (Join join : joins) {
            FromItem rightItem = join.getRightItem();
            if (rightItem instanceof Table) {
                String name = ((Table) rightItem).getName();
                if (CommonUtils.isNotEmpty(name)) {
                    allSelectEntitiesNames.add(name);
                }
            }
        }
    }

    @NotNull
    public List<String> getAllSelectEntitiesNames() {
        return allSelectEntitiesNames;
    }

    @NotNull
    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(@NotNull String originalText) {
        this.originalText = originalText;
    }

    @NotNull
    public String getText() {
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
        parseQuery();
        return statement;
    }

    public Throwable getParseError() {
        return parseError;
    }

    public List<SQLQueryParameter> getParameters() {
        return parameters;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /**
     * User defined data object. May be used to identify statements.
     *
     * @return data or null
     */
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @NotNull
    public SQLQueryType getType() {
        parseQuery();
        return type;
    }

    public DBCEntityMetaData getEntityMetadata(boolean raw) {
        parseQuery();
        return raw? rawSingleTableMetadata : singleTableMeta;
    }

    public void setParameters(List<SQLQueryParameter> parameters) {
        this.parameters = parameters;
    }

    public void reset() {
        this.text = this.originalText;
        if (this.parameters != null) {
            setParameters(this.parameters);
        }
    }

    @Override
    public String toString() {
        return text;
    }

    public String getExtraErrorMessage() {
        return extraErrorMessage;
    }

    public void addExtraErrorMessage(String extraErrorMessage) {
        if (CommonUtils.isEmpty(this.extraErrorMessage)) {
            this.extraErrorMessage = extraErrorMessage;
        } else {
            this.extraErrorMessage = this.extraErrorMessage + System.getProperty(StandardConstants.ENV_LINE_SEPARATOR) + extraErrorMessage;
        }
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

    public boolean isDeleteUpdateDangerous() {
        parseQuery();
        if (statement == null) {
            return false;
        }
        if (statement instanceof Delete) {
            if (((Delete) statement).getWhere() == null) {
                return true;
            }
        } else if (statement instanceof Update) {
            if (((Update) statement).getWhere() == null) {
                return true;
            }
        }
        return false;
    }

    public boolean isDropTableDangerous() {
        parseQuery();
        return statement != null && statement instanceof Drop &&
            ((Drop) statement).getName() != null && ((Drop) statement).getType().equalsIgnoreCase("table");
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

    public boolean isModifiyng() {
        if (getType() == SQLQueryType.UNKNOWN) {
            return false;
        }
        if (statement instanceof Select) {
            SelectBody selectBody = ((Select) statement).getSelectBody();
            if (selectBody instanceof PlainSelect) {
                if (((PlainSelect) selectBody).isForUpdate() ||
                    ((PlainSelect) selectBody).getIntoTables() != null)
                {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SQLQuery && text.equals(((SQLQuery) obj).text);
    }

}
