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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLTableAliasInsertMode;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItem.*;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemVisitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLQueryCompletionTextProvider implements SQLQueryCompletionItemVisitor<String> {

    private final SQLCompletionRequest request;
    private final SQLQueryCompletionContext queryCompletionContext;
    private final SQLTableAliasInsertMode aliasMode;
    private final char structSeparator;
    private final Set<String> localKnownColumnNames;

    private final DBRProgressMonitor monitor;

    public SQLQueryCompletionTextProvider(
        @NotNull SQLCompletionRequest request,
        @NotNull SQLQueryCompletionContext queryCompletionContext,
        @NotNull DBRProgressMonitor monitor
    ) {
        this.request = request;
        this.queryCompletionContext = queryCompletionContext;
        this.aliasMode = SQLTableAliasInsertMode.fromPreferences(request.getContext().getSyntaxManager().getPreferenceStore());
        this.structSeparator = request.getContext().getDataSource().getSQLDialect().getStructSeparator();
        this.localKnownColumnNames = queryCompletionContext.getDataContext() == null
            ? Collections.emptySet()
            : queryCompletionContext.getDataContext().getColumnsList().stream()
                .map(c -> c.symbol.getName())
                .collect(Collectors.toSet());
        this.monitor = monitor;
    }

    @NotNull
    @Override
    public String visitSubqueryAlias(@NotNull SQLSubqueryAliasCompletionItem subqueryAlias) {
        return subqueryAlias.symbol.getName();
    }

    @NotNull
    @Override
    public String visitColumnName(@NotNull SQLColumnNameCompletionItem columnName) {
        String preparedColumnName = this.convertCaseIfNeeded(columnName.columnInfo.symbol.getName());
        String suffix;

        if (this.queryCompletionContext.getInspectionResult().expectingColumnIntroduction &&
            this.aliasMode != SQLTableAliasInsertMode.NONE && this.localKnownColumnNames.contains(preparedColumnName) &&
            columnName.sourceInfo.aliasOrNull != null) {
            DBPDataSource ds = this.request.getContext().getDataSource();
            String alias = DBUtils.getUnQuotedIdentifier(ds, columnName.sourceInfo.aliasOrNull.getName())
                + DBUtils.getUnQuotedIdentifier(ds, preparedColumnName);;
            suffix = this.prepareAliasPrefix() + this.convertCaseIfNeeded(DBUtils.getQuotedIdentifier(ds, alias));
        } else {
            suffix = "";
        }

        if (columnName.absolute) {
            String prefix = columnName.sourceInfo.aliasOrNull != null
                ? columnName.sourceInfo.aliasOrNull.getName() + this.structSeparator : "";
            return prefix + preparedColumnName + suffix;
        } else {
            return preparedColumnName + suffix;
        }
    }

    @NotNull
    @Override
    public String visitTableName(@NotNull SQLTableNameCompletionItem tableName) {
        DBSEntity object = tableName.table;
        String suffix;

        if (this.queryCompletionContext.getInspectionResult().expectingTableSourceIntroduction &&
            this.aliasMode != SQLTableAliasInsertMode.NONE) {
            // It is table name completion after FROM. Auto-generate table alias
            SQLDialect sqlDialect = SQLUtils.getDialectFromObject(object);
            String alias = SQLUtils.generateEntityAlias(object,
                s -> sqlDialect.getKeywordType(s) != null ||
                    this.queryCompletionContext.getAliasesInUse().contains(s) ||
                    (this.queryCompletionContext.getDataContext() != null
                        && this.queryCompletionContext.getDataContext().resolveSource(monitor, List.of(s)) != null)
            );
            suffix = this.prepareAliasPrefix() + this.convertCaseIfNeeded(alias);
        } else {
            suffix = "";
        }

        // TODO if for table reference (after FROM), then generate alias
        return this.prepareObjectName(object) + suffix;
    }

    @NotNull
    private String prepareAliasPrefix() {
        return this.aliasMode == SQLTableAliasInsertMode.EXTENDED
            ? (" " + SQLCompletionAnalyzer.convertKeywordCase(this.request, "as", false) + " ") : " ";
    }

    @NotNull
    @Override
    public String visitReservedWord(@NotNull SQLReservedWordCompletionItem reservedWord) {
        return SQLCompletionAnalyzer.convertKeywordCase(this.request, reservedWord.text, false);
    }

    @NotNull
    @Override
    public String visitNamedObject(@NotNull SQLDbNamedObjectCompletionItem namedObject) {
        return this.prepareObjectName(namedObject.object);
    }

    @NotNull
    private String prepareObjectName(@NotNull DBPNamedObject namedObject) {
        String name;
        if (this.request.getContext().isUseShortNames()) {
            name = DBUtils.getQuotedIdentifier(namedObject);
        } else if (this.request.getContext().isUseFQNames()) {
            name = DBUtils.getObjectFullName(namedObject, DBPEvaluationContext.DML);
        } else {
            name = DBUtils.getQuotedIdentifier(namedObject);
        }
        return this.convertCaseIfNeeded(name);
    }

    @NotNull
    private String convertCaseIfNeeded(@NotNull String name) {
        String result;
        if (this.request.getWordDetector().isQuoted(name)) {
            result = name;
        } else {
            result = SQLCompletionAnalyzer.convertKeywordCase(this.request, name, true);
        }
        return result;
    }

}
