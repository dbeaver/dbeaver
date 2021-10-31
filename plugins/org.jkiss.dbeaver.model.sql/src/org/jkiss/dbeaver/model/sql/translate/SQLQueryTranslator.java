/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * SQL translator
 */
public final class SQLQueryTranslator {

    public static String translateScript(
        @NotNull SQLDialect sourceDialect,
        @NotNull SQLDialect targetDialect,
        @NotNull DBPPreferenceStore preferenceStore,
        @NotNull String script) throws DBException
    {
        SQLTranslateContext context = new SQLTranslateContext(sourceDialect, targetDialect, preferenceStore);
        List<SQLScriptElement> sqlScriptElements = SQLScriptParser.parseScript(sourceDialect, preferenceStore, script);
        List<SQLScriptElement> result = new ArrayList<>();
        for (SQLScriptElement element : sqlScriptElements) {
            result.addAll(
                translateCommand(context, element));
        }
        String scriptDelimiter = targetDialect.getScriptDelimiters()[0];

        StringBuilder sql = new StringBuilder();
        for (SQLScriptElement element : result) {
            sql.append(element.getText());
            sql.append(scriptDelimiter).append("\n");
        }
        return sql.toString();
    }

    public static List<? extends SQLScriptElement> translateCommand(
        @NotNull SQLTranslateContext context,
        @NotNull SQLScriptElement element) throws DBException {

        if (element instanceof SQLQuery) {
            return translateQuery(context, (SQLQuery)element);
        }

        return Collections.singletonList(element);
    }

    private static List<? extends SQLScriptElement> translateQuery(
        @NotNull SQLTranslateContext context,
        @NotNull SQLQuery query) throws DBException
    {
        Statement statement = query.getStatement();
        if (statement != null) {
            return translateStatement(context, query, statement);
        }
        return Collections.singletonList(query);
    }

    private static List<? extends SQLScriptElement> translateStatement(
        @NotNull SQLTranslateContext context,
        @NotNull SQLQuery query,
        @NotNull Statement statement) throws DBException
    {
        // FIXME: currently it is a dummy translator to PostgreSQL dialect
        List<SQLScriptElement> extraQueries = null;

        if (statement instanceof CreateTable) {
            boolean defChanged = false;
            CreateTable createTable = (CreateTable)statement;
            for (ColumnDefinition cd : createTable.getColumnDefinitions()) {

                String newDataType = null;
                switch (cd.getColDataType().getDataType().toUpperCase(Locale.ENGLISH)) {
                    case "CLOB":
                        newDataType = "varchar";
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
                                break;
                        }
                    }
                }
            }
            if (defChanged) {
                String newQueryText = SQLFormatUtils.formatSQL(null, context.getSyntaxManager(), createTable.toString());
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

}
