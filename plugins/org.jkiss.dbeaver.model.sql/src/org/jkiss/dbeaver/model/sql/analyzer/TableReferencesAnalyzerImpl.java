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
package org.jkiss.dbeaver.model.sql.analyzer;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.dialect.LSMDialectRegistry;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.stm.*;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Internal implementation of references analyzer
 */
public class TableReferencesAnalyzerImpl implements TableReferencesAnalyzer {

    private static final Log log = Log.getLog(TableReferencesAnalyzerImpl.class);

    private Map<String, String> tableReferences;
    private final SQLCompletionRequest request;

    public TableReferencesAnalyzerImpl(@NotNull SQLCompletionRequest request) {
        this.request = request;
    }

    private void prepareTableReferences() {
        try {
            STMSource querySource = STMSource.fromReader(new StringReader(this.request.getActiveQuery().getText()));

            SQLDialect dialect = request.getContext().getDataSource().getSQLDialect();
            LSMAnalyzer analyzer = LSMDialectRegistry.getInstance().getAnalyzerFactoryForDialect(dialect)
                 .createAnalyzer(LSMAnalyzerParameters.forDialect(dialect, request.getContext().getSyntaxManager()));
            STMTreeRuleNode tree = analyzer.parseSqlQueryTree(querySource, new STMSkippingErrorListener());
            tableReferences = getTableAndAliasFromSources(tree);
        } catch (Exception e) {
            log.debug("Failed to extract table names from query", e);
            tableReferences = Collections.emptyMap();
        }
    }
    
    private boolean prepareTableReferencesIfNeeded() {
        if (tableReferences == null || tableReferences.isEmpty()) {
            final SQLScriptElement activeQuery = request.getActiveQuery();
            if (activeQuery == null) {
                return false;
            }
            this.prepareTableReferences();
        }
        return true;
    }

    @NotNull
    @Override
    public Map<String, String> getFilteredTableReferences(@NotNull String tableAlias, boolean allowPartialMatch) {
        Map<String, String> result;
        if (!this.prepareTableReferencesIfNeeded()) {
            return Collections.emptyMap();
        }
        if (CommonUtils.isNotEmpty(tableAlias) && tableReferences.size() > 0) {
            result = tableReferences.entrySet().stream()
                .filter(r -> allowPartialMatch
                    ? r.getValue() != null && CommonUtils.startsWithIgnoreCase(r.getValue(), tableAlias)
                    : r.getValue() != null && r.getValue().equalsIgnoreCase(tableAlias))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            result = tableReferences;
        }
        return result;
    }

    @NotNull
    @Override
    public Map<String, String> getTableAliasesFromQuery() {
        try {
            if (!this.prepareTableReferencesIfNeeded()) {
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return tableReferences;
    }

    /**
     * The method designed to get table and aliases from source query
     */
    @NotNull
    public Map<String, String> getTableAndAliasFromSources(@NotNull STMTreeRuleNode query) {
        Map<String, String> result = new TreeMap<>();
        List<STMTreeNode> tableExpandedReferences = STMUtils.expandSubtree(
            query,
            TableReferencesRules.expandRulesToTableRef,
            TableReferencesRules.extractRulesToTableRef);
        for (STMTreeNode tableRef : tableExpandedReferences) {
            List<STMTreeNode> names = STMUtils.expandSubtree(
                tableRef,
                TableReferencesRules.expandRulesToTableName,
                TableReferencesRules.extractRulesToTableName);
            String alias = null;
            String tableName = null;
            for (STMTreeNode part : names) {
                String nodeName = part.getNodeName();
                if (nodeName.equals(STMKnownRuleNames.tableName)) {
                    tableName = part.getText();
                } else if (nodeName.equals(STMKnownRuleNames.correlationName)) {
                    alias = part.getText();
                }
            }
            if (tableName != null) {
                result.put(tableName, alias);
            }
        }
        return result;
    }
}
