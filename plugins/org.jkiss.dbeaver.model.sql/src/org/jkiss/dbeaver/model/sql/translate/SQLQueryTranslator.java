/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectDDLExtension;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The type Sql query translator.
 */
public class SQLQueryTranslator implements SQLTranslator {

    private SQLTranslateContext sqlTranslateContext;

    public SQLQueryTranslator(SQLTranslateContext sqlTranslateContext) {
        this.sqlTranslateContext = sqlTranslateContext;
    }

    public List<? extends SQLScriptElement> translate(@NotNull SQLScriptElement element) throws DBException {

        if (element instanceof SQLQuery) {
            return translateQuery((SQLQuery) element);
        }

        return Collections.singletonList(element);
    }

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
    protected List<? extends SQLScriptElement> translateStatement(@NotNull SQLQuery query,
                                                                  @NotNull Statement statement) {

        List<SQLScriptElement> extraQueries = null;

        if (statement instanceof CreateTable) {
            boolean defChanged = false;
            CreateTable createTable = (CreateTable) statement;
            SQLDialect targetDialect = sqlTranslateContext.getTargetDialect();
            SQLDialectDDLExtension extendedDialect = null;
            if (targetDialect instanceof SQLDialectDDLExtension) {
                extendedDialect = (SQLDialectDDLExtension) targetDialect;
            }

            if (extendedDialect != null && extendedDialect.supportsCreateIfExists()) {
                createTable.setIfNotExists(false);
                defChanged = true;
            }

            if (!targetDialect.supportsTableDropCascade() && createTable.getIndexes() != null) {
                for (Index index : createTable.getIndexes()) {
                    if (index instanceof ForeignKeyIndex) {
                        ForeignKeyIndex foreignKeyIndex = (ForeignKeyIndex) index;
                        ReferentialAction referentialAction = foreignKeyIndex.getReferentialAction(ReferentialAction.Type.DELETE);
                        if (referentialAction != null && referentialAction.getAction().equals(ReferentialAction.Action.CASCADE)) {
                            referentialAction.setAction(ReferentialAction.Action.NO_ACTION);
                            defChanged = true;
                        }
                    }
                }
            }

            for (ColumnDefinition cd : createTable.getColumnDefinitions()) {
                String newDataType = null;
                switch (cd.getColDataType().getDataType().toUpperCase(Locale.ENGLISH)) {
                    case "CLOB":
                    case "TEXT":
                        newDataType = (extendedDialect != null) ? extendedDialect.getLargeCharacterType() : "varchar";
                        break;
                    case "TIMESTAMP":
                        if (extendedDialect != null && extendedDialect.timestampAsDatetime()) {
                            newDataType = "datetime";
                        }
                        break;
                    case "BIGINT":
                        if (extendedDialect != null) {
                            newDataType = extendedDialect.getLargeNumericType();
                        }
                        break;
                }
                if (newDataType != null) {
                    cd.getColDataType().setDataType(newDataType);
                    defChanged = true;
                }

                if (!CommonUtils.isEmpty(cd.getColumnSpecs())) {
                    for (String cSpec : new ArrayList<>(cd.getColumnSpecs())) {
                        switch (cSpec.toUpperCase(Locale.ENGLISH)) {
                            case "AUTO_INCREMENT":
                            case "IDENTITY":
                                if (!targetDialect.supportsColumnAutoIncrement()) {
                                    String sequenceName = CommonUtils.escapeIdentifier(createTable.getTable().getName()) +
                                        "_" + CommonUtils.escapeIdentifier(cd.getColumnName());

                                    cd.getColumnSpecs().remove(cSpec);
                                    cd.getColumnSpecs().add("DEFAULT");
                                    cd.getColumnSpecs().add("NEXTVAL('" + sequenceName + "')");
                                    defChanged = true;

                                    String createSeqQuery = "CREATE SEQUENCE " + sequenceName;

                                    if (extraQueries == null) {
                                        extraQueries = new ArrayList<>();
                                    }
                                    extraQueries.add(new SQLQuery(null, createSeqQuery));
                                } else if (extendedDialect != null) {
                                    int indexOf = cd.getColumnSpecs().indexOf(cSpec);
                                    defChanged = true;
                                    cd.getColumnSpecs().set(indexOf, extendedDialect.getAutoIncrementKeyword());
                                }
                                break;
                        }
                    }
                }
            }
            if (defChanged) {
                String newQueryText = SQLFormatUtils.formatSQL(null,
                    sqlTranslateContext.getSyntaxManager(),
                    createTable.toString());

                query.setText(newQueryText);

                if (extraQueries == null) {
                    extraQueries = new ArrayList<>();
                }
                extraQueries.add(query);
            }
        }
        if (extraQueries == null) {
            return Collections.singletonList(query);
        }
        return extraQueries;
    }

    /**
     * Gets sql translate context.
     *
     * @return the sql translate context
     */
    public SQLTranslateContext getSqlTranslateContext() {
        return sqlTranslateContext;
    }

    /**
     * Sets sql translate context.
     *
     * @param sqlTranslateContext the sql translate context
     */
    public void setSqlTranslateContext(SQLTranslateContext sqlTranslateContext) {
        this.sqlTranslateContext = sqlTranslateContext;
    }
}
