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

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * SQL translator
 */
final class SQLTranslateContext {

    @NotNull
    private final SQLDialect sourceDialect;
    @NotNull
    private final SQLDialect targetDialect;
    @NotNull
    private final DBPPreferenceStore preferenceStore;
    @NotNull
    private final SQLSyntaxManager syntaxManager;

    public SQLTranslateContext(
        @NotNull SQLDialect sourceDialect,
        @NotNull SQLDialect targetDialect,
        @NotNull DBPPreferenceStore preferenceStore)
    {
        this.sourceDialect = sourceDialect;
        this.targetDialect = targetDialect;
        this.preferenceStore = preferenceStore;

        syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(targetDialect, preferenceStore);
    }

    @NotNull
    public SQLDialect getSourceDialect() {
        return sourceDialect;
    }

    @NotNull
    public SQLDialect getTargetDialect() {
        return targetDialect;
    }

    @NotNull
    public DBPPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    @NotNull
    public SQLSyntaxManager getSyntaxManager() {
        return syntaxManager;
    }

    List<? extends SQLScriptElement> translateCommand(
        @NotNull SQLScriptElement element) throws DBException {

        if (element instanceof SQLQuery) {
            return translateQuery((SQLQuery)element);
        }

        return Collections.singletonList(element);
    }

    private List<? extends SQLScriptElement> translateQuery(@NotNull SQLQuery query) throws DBException
    {
        Statement statement = query.getStatement();
        if (statement != null) {
            return translateStatement(query, statement);
        }
        return Collections.singletonList(query);
    }

    private List<? extends SQLScriptElement> translateStatement(
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
                                }
                                break;
                        }
                    }
                }
            }
            if (defChanged) {
                String newQueryText = SQLFormatUtils.formatSQL(null, syntaxManager, createTable.toString());
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
