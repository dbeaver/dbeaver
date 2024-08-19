/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.translate;

import net.sf.jsqlparser.statement.ReferentialAction;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The type Sql query translator.
 */
public class SQLQueryTranslator implements SQLTranslator {

    @NotNull
    private SQLTranslateContext sqlTranslateContext;

    /**
     * Instantiates sql query translator.
     *
     * @param sqlTranslateContext the sql translate context
     */
    public SQLQueryTranslator(@NotNull SQLTranslateContext sqlTranslateContext) {
        this.sqlTranslateContext = sqlTranslateContext;
    }

    /**
     * Translates script to target dialect.
     *
     * @param sourceDialect   the source dialect
     * @param targetDialect   the target dialect
     * @param preferenceStore the preference store
     * @param script          the script
     * @return the string
     * @throws DBException the db exception
     */
    @NotNull
    public static String translateScript(
        @NotNull SQLDialect sourceDialect,
        @NotNull SQLDialect targetDialect,
        @NotNull DBPPreferenceStore preferenceStore,
        @NotNull String script
    ) throws DBException {

        SQLTranslateContext context = new SQLTranslateContext(sourceDialect, targetDialect, preferenceStore);

        List<SQLScriptElement> sqlScriptElements = SQLScriptParser.parseScript(null, sourceDialect, preferenceStore, script);
        List<SQLScriptElement> result = new ArrayList<>();

        SQLQueryTranslator defaultSQLQueryTranslator = new SQLQueryTranslator(context);

        for (SQLScriptElement element : sqlScriptElements) {
            result.addAll(defaultSQLQueryTranslator.translate(element));
        }
        String scriptDelimiter = targetDialect.getScriptDelimiters()[0];

        StringBuilder sql = new StringBuilder();
        for (SQLScriptElement element : result) {
            sql.append(element.getText());
            sql.append(scriptDelimiter).append("\n");
        }
        return sql.toString();
    }

    /**
     * Translates sql script element.
     *
     * @param element the element
     * @return the list
     * @throws DBException the db exception
     */
    @NotNull
    public List<? extends SQLScriptElement> translate(@NotNull SQLScriptElement element) throws DBException {

        if (element instanceof SQLQuery) {
            return translateQuery((SQLQuery) element);
        }

        return Collections.singletonList(element);
    }

    @NotNull
    private List<? extends SQLScriptElement> translateQuery(@NotNull SQLQuery query) {
        Statement statement = query.getStatement();
        if (statement != null) {
            return translateStatement(query, statement);
        }
        return Collections.singletonList(query);
    }

    /**
     * Translates statement to target dialect.
     *
     * @param query     the query
     * @param statement the statement
     * @return the list
     */
    @NotNull
    protected List<? extends SQLScriptElement> translateStatement(
        @NotNull SQLQuery query,
        @NotNull Statement statement
    ) {

        List<SQLScriptElement> extraQueries = null;
        List<SQLScriptElement> postExtraQueries = new ArrayList<>();

        boolean defChanged = false;
        SQLDialect targetDialect = sqlTranslateContext.getTargetDialect();
        SQLDialectDDLExtension extendedDialect = null;
        if (targetDialect instanceof SQLDialectDDLExtension) {
            extendedDialect = (SQLDialectDDLExtension) targetDialect;
        }
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;

            if (extendedDialect != null && extendedDialect.supportsCreateIfExists()) {
                createTable.setIfNotExists(false);
                defChanged = true;
            }

            var columnDefinitions = createTable.getColumnDefinitions();
            for (ColumnDefinition cd : columnDefinitions) {
                defChanged |= translateColumnDataType(cd, extendedDialect, targetDialect);

                if (!CommonUtils.isEmpty(cd.getColumnSpecs())) {
                    for (String columnSpec : new ArrayList<>(cd.getColumnSpecs())) {
                        switch (columnSpec.toUpperCase(Locale.ENGLISH)) {
                            case "AUTO_INCREMENT":
                            case "IDENTITY":
                                if (!targetDialect.supportsColumnAutoIncrement()) {
                                    String schemaName = createTable.getTable().getSchemaName();
                                    String sequenceWithoutSchemaName = CommonUtils.escapeIdentifier(createTable.getTable().getName()) +
                                        "_" + CommonUtils.escapeIdentifier(cd.getColumnName());
                                    String sequenceName = schemaName == null ? sequenceWithoutSchemaName :
                                        schemaName + "." + sequenceWithoutSchemaName;

                                    cd.getColumnSpecs().remove(columnSpec);
                                    cd.getColumnSpecs().add("DEFAULT");
                                    cd.getColumnSpecs().add("NEXTVAL('" + sequenceName + "')");
                                    defChanged = true;

                                    String createSeqQuery = "CREATE SEQUENCE " + sequenceName;

                                    if (extraQueries == null) {
                                        extraQueries = new ArrayList<>();
                                    }
                                    extraQueries.add(new SQLQuery(null, createSeqQuery));

                                    String linkSeqWithTable =
                                        "ALTER SEQUENCE " + sequenceName + " OWNED BY " + createTable.getTable()
                                            .getFullyQualifiedName() + "." + cd.getColumnName();
                                    postExtraQueries.add(new SQLQuery(null, linkSeqWithTable));
                                } else if (extendedDialect != null) {
                                    int indexOf = cd.getColumnSpecs().indexOf(columnSpec);
                                    defChanged = true;
                                    cd.getColumnSpecs().set(indexOf, extendedDialect.getAutoIncrementKeyword());
                                }
                                break;
                            default:
                                //no action
                                break;
                        }
                    }
                }
            }
            if (extendedDialect != null &&
                !extendedDialect.supportsNoActionIndex() &&
                !CommonUtils.isEmpty(createTable.getIndexes())
            ) {
                for (var index : createTable.getIndexes()) {
                    if (index instanceof ForeignKeyIndex) {
                        ForeignKeyIndex fkIndex = (ForeignKeyIndex) index;
                        ReferentialAction ra = fkIndex.getReferentialAction(ReferentialAction.Type.DELETE);
                        if (ra != null && ReferentialAction.Action.NO_ACTION.equals(ra.getAction())) {
                            fkIndex.removeReferentialAction(ReferentialAction.Type.DELETE);
                            defChanged = true;
                        }
                    }
                }
            }
        } else if (statement instanceof Alter alter) {
            if (alter.getAlterExpressions() != null) {
                for (AlterExpression expr : alter.getAlterExpressions()) {
                    var columnDataTypeList = expr.getColDataTypeList();
                    if (columnDataTypeList == null) {
                        continue;
                    }
                    if (extendedDialect != null && expr.getOperation().equals(AlterOperation.ALTER)) {
                        expr.setOperation(AlterOperation.valueOf(extendedDialect.getAlterColumnOperation().toUpperCase()));
                        expr.hasColumn(extendedDialect.supportsAlterHasColumn());
                        defChanged = true;
                    }

                    for (ColumnDefinition columnDataType : columnDataTypeList) {
                        defChanged |= translateColumnDataType(columnDataType, extendedDialect, targetDialect);
                    }
                }
            }
        }
        if (defChanged) {
            String newQueryText = SQLFormatUtils.formatSQL(null,
                    sqlTranslateContext.getSyntaxManager(),
                    statement.toString());

            query.setText(newQueryText);

            if (extraQueries == null) {
                extraQueries = new ArrayList<>();
            }
            extraQueries.add(query);
            extraQueries.addAll(postExtraQueries);
        }
        if (extraQueries == null) {
            return Collections.singletonList(query);
        }
        return extraQueries;
    }

    private boolean translateColumnDataType(ColumnDefinition cd, SQLDialectDDLExtension extendedDialect, SQLDialect targetDialect) {
        String newDataType = null;
        var colDataType = cd.getColDataType() != null
            ? cd.getColDataType().getDataType().toUpperCase(Locale.ENGLISH)
            : "";
        switch (colDataType) {
            case "CLOB":
                newDataType = (extendedDialect != null) ? extendedDialect.getClobDataType() : "varchar";
                break;
            case "BLOB":
                newDataType = (extendedDialect != null) ? extendedDialect.getBlobDataType() : "blob";
                break;
            case "TEXT":
                String dialectName = targetDialect.getDialectName().toLowerCase();
                if (extendedDialect != null && (dialectName.equals("oracle") || dialectName.equals("sqlserver"))) {
                    newDataType = extendedDialect.getClobDataType();
                }
                break;
            case "TIMESTAMP":
                if (extendedDialect != null) {
                    newDataType = extendedDialect.getTimestampDataType();
                }
                break;
            case SQLConstants.DATA_TYPE_BIGINT:
                if (extendedDialect != null) {
                    newDataType = extendedDialect.getBigIntegerType();
                }
                break;
            case "UUID":
                if (extendedDialect != null) {
                    newDataType = extendedDialect.getUuidDataType();
                }
                break;
            case "BOOLEAN":
                if (extendedDialect != null) {
                    newDataType = extendedDialect.getBooleanDataType();
                }
                break;
            case "SET":
                if (extendedDialect != null && !extendedDialect.supportsAlterColumnSet()) {
                    newDataType = "";
                }
                break;
            default:
                //no action
                break;
        }
        if (newDataType != null) {
            cd.getColDataType().setDataType(newDataType);
            return true;
        }
        return false;
    }

    /**
     * Gets sql translate context.
     *
     * @return the sql translate context
     */
    @NotNull
    public SQLTranslateContext getSqlTranslateContext() {
        return sqlTranslateContext;
    }

    /**
     * Sets sql translate context.
     *
     * @param sqlTranslateContext the sql translate context
     */
    public void setSqlTranslateContext(@NotNull SQLTranslateContext sqlTranslateContext) {
        this.sqlTranslateContext = sqlTranslateContext;
    }

    @NotNull
    public static DBPPreferenceStore getDefaultPreferenceStore() {
        DBPPreferenceStore prefStore = new SimplePreferenceStore() {
            @Override
            public void save() throws IOException {

            }
        };
        prefStore.setValue(SQLModelPreferences.SQL_FORMAT_FORMATTER, "default");
        return prefStore;
    }
}
