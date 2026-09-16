/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import net.sf.jsqlparser.statement.Commit;
import net.sf.jsqlparser.statement.CreateFunctionalStatement;
import net.sf.jsqlparser.statement.ParenthesedStatement;
import net.sf.jsqlparser.statement.RollbackStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.sequence.AlterSequence;
import net.sf.jsqlparser.statement.create.function.CreateFunction;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.procedure.CreateProcedure;
import net.sf.jsqlparser.statement.create.schema.CreateSchema;
import net.sf.jsqlparser.statement.create.sequence.CreateSequence;
import net.sf.jsqlparser.statement.create.synonym.CreateSynonym;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.AlterView;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.utils.CommonUtils;

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

    private Boolean isEndsWithDelimiter = null;
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
    private SQLDdlChange ddlChange;
    private SingleTableMeta singleTableMeta, rawSingleTableMetadata;
    private List<SQLSelectItem> selectItems;
    private String queryTitle;
    private String extraErrorMessage;
    private final List<String> allSelectEntitiesNames = new ArrayList<>();

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

        queryTitle = extractQueryTitle(text);
    }

    @Nullable
    private static String extractQueryTitle(@NotNull String text) {
        final Matcher matcher = QUERY_TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    public List<SQLScriptElement> getScriptElements() {
        return List.of(this);
    }

    private void parseQuery() {
        if (parsed) {
            return;
        }
        parsed = true;
        SQLDialect dialect = dataSource == null ? BasicSQLDialect.INSTANCE : dataSource.getSQLDialect();
        SQLDdlChange fallbackDdlChange;
        Throwable fallbackParseError = null;
        try {
            fallbackDdlChange = dialect.parseDdlChange(text);
        } catch (Throwable e) {
            fallbackParseError = e;
            fallbackDdlChange = null;
        }
        if (fallbackDdlChange == null) {
            try {
                fallbackDdlChange = SQLDdlParser.parse(dialect, text);
            } catch (Throwable e) {
                if (fallbackParseError == null) {
                    fallbackParseError = e;
                }
            }
        }
        try {
            if (CommonUtils.isEmpty(text)) {
                this.statement = null;
                this.parseError = new DBException("Empty query");
                return;
            }
            statement = SQLSemanticProcessor.parseQuery(dataSource == null ? null : dataSource.getSQLDialect(), text);
            if (statement instanceof Select) {
                type = SQLQueryType.SELECT;
                if (statement instanceof PlainSelect plainSelect) {
                    // Detect single source table (no joins, no group by, no sub-selects)
                    FromItem fromItem = plainSelect.getFromItem();
                    if (fromItem instanceof Table fromTable && isPotentiallySingleSourceSelect(plainSelect)) {
                        boolean hasSubSelects = false;
                        boolean hasDirectSelects = false;
                        for (SelectItem<?> si : plainSelect.getSelectItems()) {
                            if (si.getExpression() instanceof ParenthesedSelect) {
                                hasSubSelects = true;
                            } else if (si.getExpression() instanceof Column) {
                                hasDirectSelects = true;
                            }
                        }
                        if (hasDirectSelects || !hasSubSelects) {
                            fillSingleSource(fromTable);
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
            } else if (statement instanceof Insert insert) {
                type = SQLQueryType.INSERT;
                fillSingleSource(insert.getTable());
            } else if (statement instanceof Update update) {
                type = SQLQueryType.UPDATE;
                Table table = update.getTable();
                if (table != null) {
                    fillSingleSource(table);
                }
            } else if (statement instanceof Delete delete) {
                type = SQLQueryType.DELETE;
                if (delete.getTable() != null) {
                    fillSingleSource(delete.getTable());
                } else {
                    List<Table> tables = delete.getTables();
                    if (tables != null && tables.size() == 1) {
                        fillSingleSource(tables.get(0));
                    }
                }
            } else if (statement instanceof Alter alter) {
                type = SQLQueryType.DDL;
                fillSingleSource(alter.getTable());
                setDdlChange(SQLDdlChange.Operation.ALTER, SQLDdlChange.ObjectKind.TABLE, alter.getTable());
            } else if (statement instanceof CreateTable createTable) {
                type = SQLQueryType.DDL;
                fillSingleSource(createTable.getTable());
                setDdlChange(SQLDdlChange.Operation.CREATE, SQLDdlChange.ObjectKind.TABLE, createTable.getTable());
            } else if (statement instanceof CreateView createView) {
                type = SQLQueryType.DDL;
                fillSingleSource(createView.getView());
                setDdlChange(SQLDdlChange.Operation.CREATE, SQLDdlChange.ObjectKind.VIEW, createView.getView());
            } else if (statement instanceof CreateIndex createIndex) {
                type = SQLQueryType.DDL;
                setDdlChange(
                    SQLDdlChange.Operation.CREATE,
                    SQLDdlChange.ObjectKind.INDEX,
                    getIndexNameParts(createIndex)
                );
            } else if (statement instanceof Drop drop) {
                type = SQLQueryType.DDL;
                SQLDdlChange.ObjectKind objectKind = getDdlObjectKind(drop.getType());
                if (objectKind == SQLDdlChange.ObjectKind.TABLE || objectKind == SQLDdlChange.ObjectKind.VIEW) {
                    fillSingleSource(drop.getName());
                }
                setDdlChange(SQLDdlChange.Operation.DROP, objectKind, getNameParts(drop.getName()));
            } else if (statement instanceof CreateFunction createFunction) {
                type = SQLQueryType.DDL;
                setDdlChange(
                    SQLDdlChange.Operation.CREATE,
                    SQLDdlChange.ObjectKind.FUNCTION,
                    getFunctionalNameParts(createFunction)
                );
            } else if (statement instanceof CreateProcedure createProcedure) {
                type = SQLQueryType.DDL;
                setDdlChange(
                    SQLDdlChange.Operation.CREATE,
                    SQLDdlChange.ObjectKind.PROCEDURE,
                    getFunctionalNameParts(createProcedure)
                );
            } else if (statement instanceof CreateSequence createSequence) {
                type = SQLQueryType.DDL;
                var sequence = createSequence.getSequence();
                if (sequence != null) {
                    setDdlChange(
                        SQLDdlChange.Operation.CREATE,
                        SQLDdlChange.ObjectKind.SEQUENCE,
                        getNameParts(sequence.getDatabase(), sequence.getSchemaName(), sequence.getName())
                    );
                }
            } else if (statement instanceof CreateSynonym createSynonym) {
                type = SQLQueryType.DDL;
                var synonym = createSynonym.getSynonym();
                if (synonym != null) {
                    setDdlChange(
                        SQLDdlChange.Operation.CREATE,
                        SQLDdlChange.ObjectKind.SYNONYM,
                        getNameParts(synonym.getDatabase(), synonym.getSchemaName(), synonym.getName())
                    );
                }
            } else if (statement instanceof AlterView alterView) {
                type = SQLQueryType.DDL;
                fillSingleSource(alterView.getView());
                setDdlChange(SQLDdlChange.Operation.ALTER, SQLDdlChange.ObjectKind.VIEW, alterView.getView());
            } else if (statement instanceof AlterSequence alterSequence) {
                type = SQLQueryType.DDL;
                var sequence = alterSequence.getSequence();
                if (sequence != null) {
                    setDdlChange(
                        SQLDdlChange.Operation.ALTER,
                        SQLDdlChange.ObjectKind.SEQUENCE,
                        getNameParts(sequence.getDatabase(), sequence.getSchemaName(), sequence.getName())
                    );
                }
            } else if (statement instanceof CreateSchema createSchema) {
                type = SQLQueryType.DDL;
                setDdlChange(
                    SQLDdlChange.Operation.CREATE,
                    SQLDdlChange.ObjectKind.SCHEMA,
                    getNameParts(createSchema.getSchemaName())
                );
            } else if (statement instanceof Merge) {
                type = SQLQueryType.MERGE;
            } else if (statement instanceof Commit) {
                type = SQLQueryType.COMMIT;
            } else if (statement instanceof RollbackStatement) {
                type = SQLQueryType.ROLLBACK;
            } else {
                ddlChange = fallbackDdlChange;
                if (fallbackDdlChange == null) {
                    type = SQLQueryType.UNKNOWN;
                    parseError = fallbackParseError;
                } else {
                    type = SQLQueryType.DDL;
                }
            }
        } catch (Throwable e) {
            if (ddlChange == null) {
                ddlChange = fallbackDdlChange;
            }
            if (ddlChange == null) {
                this.type = SQLQueryType.UNKNOWN;
            } else {
                this.type = SQLQueryType.DDL;
            }
            this.parseError = e;
            //log.debug("Error parsing SQL query [" + query + "]:" + CommonUtils.getRootCause(e).getMessage());
        }
    }

    private boolean isValidSelectItem(@NotNull SelectItem<?> item) {
        // Workaround for JSQLParser not respecting the `#` comment in MySQL and treating them as valid values
        if (dataSource != null) {
            final Expression expr = item.getExpression();
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
            (plainSelect.getGroupBy() == null || CommonUtils.isEmpty(plainSelect.getGroupBy().getGroupByExpressionList())) &&
            CommonUtils.isEmpty(plainSelect.getIntoTables());
    }

    private void fillSingleSource(@Nullable Table fromItem) {
        if (fromItem != null) {
            fillSingleSource(fromItem.getDatabase(), fromItem.getSchemaName(), fromItem.getName());
        }
    }

    private void fillSingleSource(@Nullable Database database, @Nullable String schemaName, @Nullable String entityName) {
        if (entityName != null) {
            rawSingleTableMetadata = new SingleTableMeta(
                database == null ? null : database.getDatabaseName(),
                schemaName,
                entityName
            );
            singleTableMeta = createUnquotedTableMetaData(rawSingleTableMetadata);
        }
    }

    private void setDdlChange(
        @NotNull SQLDdlChange.Operation operation,
        @NotNull SQLDdlChange.ObjectKind objectKind,
        @Nullable Table table
    ) {
        setDdlChange(operation, objectKind, getNameParts(table));
    }

    private void setDdlChange(
        @NotNull SQLDdlChange.Operation operation,
        @NotNull SQLDdlChange.ObjectKind objectKind,
        @NotNull List<String> nameParts
    ) {
        if (!nameParts.isEmpty()) {
            ddlChange = new SQLDdlChange(operation, objectKind, nameParts);
        }
    }

    @NotNull
    private List<String> getIndexNameParts(@NotNull CreateIndex createIndex) {
        List<String> indexNameParts = createIndex.getIndex() == null ?
            List.of() : CommonUtils.safeList(createIndex.getIndex().getNameParts());
        if (indexNameParts.size() != 1) {
            return indexNameParts;
        }
        List<String> tableNameParts = getNameParts(createIndex.getTable());
        if (tableNameParts.size() <= 1) {
            return indexNameParts;
        }
        List<String> qualifiedIndexName = new ArrayList<>(tableNameParts.size());
        qualifiedIndexName.addAll(tableNameParts.subList(0, tableNameParts.size() - 1));
        qualifiedIndexName.add(indexNameParts.getFirst());
        return List.copyOf(qualifiedIndexName);
    }

    @NotNull
    private List<String> getFunctionalNameParts(@NotNull CreateFunctionalStatement statement) {
        List<String> declarationParts = statement.getFunctionDeclarationParts();
        return CommonUtils.isEmpty(declarationParts) ? List.of() : getNameParts(declarationParts.get(0));
    }

    @NotNull
    private List<String> getNameParts(@Nullable Table table) {
        if (table == null) {
            return List.of();
        }
        return getNameParts(table.getDatabase(), table.getSchemaName(), table.getName());
    }

    @NotNull
    private static List<String> getNameParts(
        @Nullable Database database,
        @Nullable String schemaName,
        @Nullable String entityName
    ) {
        List<String> result = new ArrayList<>(3);
        if (database != null && database.getDatabaseName() != null) {
            result.add(database.getDatabaseName());
        }
        if (schemaName != null) {
            result.add(schemaName);
        }
        if (entityName != null) {
            result.add(entityName);
        }
        return List.copyOf(result);
    }

    @NotNull
    private List<String> getNameParts(@Nullable String qualifiedName) {
        if (qualifiedName == null) {
            return List.of();
        }
        SQLDialect dialect = dataSource == null ? BasicSQLDialect.INSTANCE : dataSource.getSQLDialect();
        String[][] quoteStrings = dialect.getIdentifierQuoteStrings();
        if (quoteStrings == null) {
            quoteStrings = BasicSQLDialect.DEFAULT_IDENTIFIER_QUOTES;
        }
        return List.of(SQLUtils.splitFullIdentifier(
            qualifiedName,
            String.valueOf(dialect.getStructSeparator()),
            quoteStrings,
            true
        ));
    }

    @NotNull
    private static SQLDdlChange.ObjectKind getDdlObjectKind(@Nullable String type) {
        if (type != null) {
            try {
                return SQLDdlChange.ObjectKind.valueOf(type.toUpperCase(java.util.Locale.ENGLISH));
            } catch (IllegalArgumentException ignored) {
                // Vendor-specific kinds remain valid DDL changes.
            }
        }
        return SQLDdlChange.ObjectKind.OTHER;
    }

    SingleTableMeta createTableMetaData(Table fromItem) {
        return createUnquotedTableMetaData(createOriginalSourceTableMetaData(fromItem));
    }

    @NotNull
    private SingleTableMeta createOriginalSourceTableMetaData(@NotNull Table fromItem) {
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
        SQLDialect dialect = dataSource == null ? BasicSQLDialect.INSTANCE : dataSource.getSQLDialect();
        return dialect.getUnquotedIdentifier(name, true);
    }

    /**
     * Plain select is a SELECT statement without INTO clause, without LIMIT or TOP modifiers
     *
     * @return true is this query is a plain select
     */
    public boolean isPlainSelect() {
        parseQuery();
        if (statement instanceof PlainSelect plainSelect) {
            return CommonUtils.isEmpty(plainSelect.getIntoTables()) &&
                plainSelect.getLimit() == null &&
                plainSelect.getTop() == null &&
                plainSelect.getForMode() == null;
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
        this.queryTitle = extractQueryTitle(text);
        resetParsedState();
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

    @Nullable
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

    public Boolean isEndsWithDelimiter() {
        return this.isEndsWithDelimiter;
    }

    public void setEndsWithDelimiter(boolean value) {
        this.isEndsWithDelimiter = value;
    }

    @NotNull
    public SQLQueryType getType() {
        parseQuery();
        return type;
    }

    public DBCEntityMetaData getEntityMetadata(boolean raw) {
        parseQuery();
        return raw ? rawSingleTableMetadata : singleTableMeta;
    }

    @Nullable
    public SQLDdlChange getDdlChange() {
        parseQuery();
        return ddlChange;
    }

    public void setParameters(@Nullable List<SQLQueryParameter> parameters) {
        this.parameters = parameters;
    }

    public void reset() {
        this.text = this.originalText;
        this.queryTitle = extractQueryTitle(originalText);
        resetParsedState();
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
            this.extraErrorMessage = this.extraErrorMessage + System.lineSeparator() + extraErrorMessage;
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
        if (statement instanceof Delete delete) {
            return delete.getWhere() == null;
        } else if (statement instanceof Update update) {
            return update.getWhere() == null;
        }
        return false;
    }

    public boolean isDropDangerous() {
        parseQuery();
        return ddlChange != null && ddlChange.operation() == SQLDdlChange.Operation.DROP || statement != null &&
            statement instanceof Drop dropStatement &&
            dropStatement.getName() != null
            && dropStatement.getType() != null;
    }

    public boolean isModifying() {
        if (getType() == SQLQueryType.UNKNOWN) {
            return false;
        }
        if (statement instanceof Select) {
            return isMutatingStatement() ||
                statement instanceof PlainSelect plainSelect && plainSelect.getForMode() != null;
        }
        return true;
    }

    public boolean isMutatingStatement() {
        parseQuery();
        if (statement instanceof Select) {
            SelectMutationVisitor visitor = new SelectMutationVisitor();
            visitor.getTables(statement);
            return visitor.isMutating();
        }
        return ddlChange != null || statement != null &&
            (statement instanceof Drop || statement instanceof Delete || statement instanceof Update ||
            statement instanceof Insert || statement instanceof CreateTable || statement instanceof CreateIndex ||
            statement instanceof CreateView || statement instanceof CreateFunction || statement instanceof CreateProcedure ||
            statement instanceof CreateSchema || statement instanceof CreateSequence || statement instanceof CreateSynonym ||
            statement instanceof Alter || statement instanceof AlterView || statement instanceof AlterSequence ||
            statement instanceof Merge);
    }

    private void resetParsedState() {
        parsed = false;
        parseError = null;
        type = SQLQueryType.UNKNOWN;
        statement = null;
        ddlChange = null;
        singleTableMeta = null;
        rawSingleTableMetadata = null;
        selectItems = null;
        allSelectEntitiesNames.clear();
    }

    private static class SelectMutationVisitor extends TablesNamesFinder<Void> {
        private boolean mutating;

        @Override
        @Nullable
        public <S> Void visit(@NotNull WithItem<?> withItem, @Nullable S context) {
            ParenthesedStatement statement = withItem.getParenthesedStatement();
            if (!(statement instanceof ParenthesedSelect)) {
                mutating = true;
                return null;
            }
            return super.visit(withItem, context);
        }

        @Override
        @Nullable
        public <S> Void visit(@NotNull PlainSelect plainSelect, @Nullable S context) {
            if (!CommonUtils.isEmpty(plainSelect.getIntoTables()) || plainSelect.getIntoTempTable() != null) {
                mutating = true;
            }
            visitSelectModifiers(plainSelect, context);
            if (plainSelect.getDistinct() != null && plainSelect.getDistinct().getOnSelectItems() != null) {
                for (var item : plainSelect.getDistinct().getOnSelectItems()) {
                    item.accept(this, context);
                }
            }
            if (plainSelect.getGroupBy() != null) {
                if (plainSelect.getGroupBy().getGroupByExpressionList() != null) {
                    plainSelect.getGroupBy().getGroupByExpressionList().accept(this, context);
                }
                if (plainSelect.getGroupBy().getGroupingSets() != null) {
                    for (var groupingSet : plainSelect.getGroupBy().getGroupingSets()) {
                        groupingSet.accept(this, context);
                    }
                }
            }
            if (plainSelect.getQualify() != null) {
                plainSelect.getQualify().accept(this, context);
            }
            if (plainSelect.getWindowDefinitions() != null) {
                for (var window : plainSelect.getWindowDefinitions()) {
                    if (window.getPartitionExpressionList() != null) {
                        window.getPartitionExpressionList().accept(this, context);
                    }
                    if (window.getOrderByElements() != null) {
                        for (var orderBy : window.getOrderByElements()) {
                            orderBy.getExpression().accept(this, context);
                        }
                    }
                }
            }
            return super.visit(plainSelect, context);
        }

        @Override
        @Nullable
        public <S> Void visit(@NotNull SetOperationList setOperationList, @Nullable S context) {
            visitSelectModifiers(setOperationList, context);
            return super.visit(setOperationList, context);
        }

        @Override
        @Nullable
        public <S> Void visit(@NotNull ParenthesedSelect parenthesedSelect, @Nullable S context) {
            visitSelectModifiers(parenthesedSelect, context);
            return super.visit(parenthesedSelect, context);
        }

        @Override
        @Nullable
        public <S> Void visit(@NotNull Delete delete, @Nullable S context) {
            mutating = true;
            return null;
        }

        @Override
        @Nullable
        public <S> Void visit(@NotNull Insert insert, @Nullable S context) {
            mutating = true;
            return null;
        }

        @Override
        @Nullable
        public <S> Void visit(@NotNull Update update, @Nullable S context) {
            mutating = true;
            return null;
        }

        private <S> void visitSelectModifiers(@NotNull Select select, @Nullable S context) {
            if (select.getOrderByElements() != null) {
                for (var orderBy : select.getOrderByElements()) {
                    orderBy.getExpression().accept(this, context);
                }
            }
            if (select.getLimit() != null) {
                if (select.getLimit().getOffset() != null) {
                    select.getLimit().getOffset().accept(this, context);
                }
                if (select.getLimit().getRowCount() != null) {
                    select.getLimit().getRowCount().accept(this, context);
                }
                if (select.getLimit().getByExpressions() != null) {
                    select.getLimit().getByExpressions().accept(this, context);
                }
            }
            if (select.getOffset() != null && select.getOffset().getOffset() != null) {
                select.getOffset().getOffset().accept(this, context);
            }
            if (select.getFetch() != null && select.getFetch().getExpression() != null) {
                select.getFetch().getExpression().accept(this, context);
            }
        }

        private boolean isMutating() {
            return mutating;
        }
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
            if (!(obj instanceof SingleTableMeta md2)) {
                return false;
            }
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
