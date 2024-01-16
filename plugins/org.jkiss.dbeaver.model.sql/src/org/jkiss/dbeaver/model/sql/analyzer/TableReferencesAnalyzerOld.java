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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class TableReferencesAnalyzerOld implements TableReferencesAnalyzer {
    
    private SQLCompletionRequest request;
    private Log log = Log.getLog(TableReferencesAnalyzerOld.class);

    public TableReferencesAnalyzerOld(SQLCompletionRequest request) {
        this.request = request;
    }

    @Override
    @NotNull
    public Map<String, String> getFilteredTableReferences(@NotNull String tableAlias, boolean allowPartialMatch) {

        final SQLScriptElement activeQuery = request.getActiveQuery();
        if (activeQuery == null) {
            return Collections.emptyMap();
        }
        final IDocument document = request.getDocument();
        final SQLRuleManager ruleManager = request.getContext().getRuleManager();
        final TPRuleBasedScanner scanner = new TPRuleBasedScanner();
        scanner.setRules(ruleManager.getAllRules());
        scanner.setRange(document, activeQuery.getOffset(), activeQuery.getLength());

        /*
         * When we search for table name knowing its alias, we want to match the
         * following sequence: [FROM|UPDATE|JOIN|INTO] <table-name> [AS]?
         * <known-alias-name>
         * 
         * If we don't know the alias, the following sequence must be used instead:
         * [FROM|UPDATE|JOIN|INTO] <table-name>
         * 
         * We use "state machine" to process such sequences. The transition table is
         * listed below: UNMATCHED -> TABLE_NAME ; if found starting token (FROM,
         * UPDATE, JOIN, INTO, etc.). TABLE_NAME -> TABLE_DOT ; if found string token.
         * TABLE_DOT -> TABLE_NAME ; if found structure separator (dot). TABLE_DOT ->
         * MATCHED ; if found space, and the alias is unknown. TABLE_DOT -> ALIAS_AS ;
         * if found space, and the alias is known. ALIAS_AS -> ALIAS_NAME ; if found
         * 'as' token. ALIAS_NAME -> MATCHED ; if found string token.
         */

        Map<String, String> tableRefs = new TreeMap<>();
        try {
            InlineState state = InlineState.UNMATCHED;
            String matchedTableName = null;
            String matchedTableAlias = null;

            final char structSeparator = request.getContext().getSyntaxManager().getStructSeparator();
            boolean prevTokenWasMatchAttempt = false;

            while (true) {
                final TPToken tok = scanner.nextToken();
                if (tok.isEOF()) {
                    break;
                }
                if (!(tok instanceof TPTokenAbstract) || tok.isWhitespace()) {
                    continue;
                }

                final String value = document.get(scanner.getTokenOffset(), scanner.getTokenLength());
                if (state == InlineState.UNMATCHED && (isTableQueryToken(tok, value) || (prevTokenWasMatchAttempt && ",".equals(value)))) {
                    state = InlineState.TABLE_NAME;
                    continue;
                }
                if ((state == InlineState.TABLE_DOT || state == InlineState.ALIAS_AS)
                    && (/* tok.getData() == SQLTokenType.T_KEYWORD || */",".equals(value))) {
                    // Coma after table name
                    // Possible partial match
                    if (!CommonUtils.isEmpty(matchedTableName)
                        && (CommonUtils.isEmpty(tableAlias) || CommonUtils.equalObjects(tableAlias, matchedTableAlias))) {
                        if (matchedTableAlias == null) {
                            matchedTableAlias = matchedTableName;
                        }
                        tableRefs.put(matchedTableName, matchedTableAlias);
                    }
                    matchedTableName = null;
                    state = InlineState.TABLE_NAME;
                    continue;
                }
                if (state == InlineState.TABLE_NAME && TableReferencesAnalyzer.isNamePartToken(tok)) {
                    matchedTableName = CommonUtils.notEmpty(matchedTableName) + value;
                    state = InlineState.TABLE_DOT;
                    continue;
                }
                if (state == InlineState.TABLE_DOT && value.indexOf(structSeparator) >= 0) {
                    matchedTableName += value;
                    state = InlineState.TABLE_NAME;
                    continue;
                }
                if (state == InlineState.TABLE_DOT) {
                    if (CommonUtils.isEmpty(tableAlias) && !isTableQueryToken(tok, value)) {
                        state = InlineState.MATCHED;
                    } else if (isTableQueryToken(tok, value)) {
                        /*
                         * Sometimes we can have table without alias, it will reset state to table_name
                         * because there is no alias to check See #12335
                         */
                        matchedTableName = null;
                        state = InlineState.TABLE_NAME;
                        continue;
                    } else {
                        state = InlineState.ALIAS_AS;
                    }
                }
                if (state == InlineState.ALIAS_AS && tok.getData() == SQLTokenType.T_KEYWORD && "AS".equalsIgnoreCase(value)) {
                    state = InlineState.ALIAS_NAME;
                    continue;
                }
                if (tok.getData() == SQLTokenType.T_KEYWORD) {
                    // Any keyword but AS resets state to
                    state = CommonUtils.isEmpty(matchedTableName) ? InlineState.UNMATCHED : InlineState.MATCHED;
                }
                if ((state == InlineState.ALIAS_AS || state == InlineState.ALIAS_NAME) && TableReferencesAnalyzer.isNamePartToken(tok)) {
                    matchedTableAlias = value;
                    state = InlineState.MATCHED;
                }
                if (state == InlineState.MATCHED) {
                    prevTokenWasMatchAttempt = true;
                    final boolean fullMatch = CommonUtils.isEmpty(tableAlias) || tableAlias.equalsIgnoreCase(matchedTableAlias);
                    final boolean partialMatch = fullMatch
                        || (allowPartialMatch && CommonUtils.startsWithIgnoreCase(matchedTableAlias, tableAlias));
                    if (fullMatch || partialMatch) {
                        if (matchedTableAlias == null) {
                            matchedTableAlias = matchedTableName;
                        }
                        tableRefs.put(matchedTableName, matchedTableAlias);
                    }
                    state = InlineState.UNMATCHED;
                    matchedTableName = null;
                    matchedTableAlias = null;
                } else {
                    prevTokenWasMatchAttempt = false;
                }
            }
            if (!CommonUtils.isEmpty(matchedTableName)
                && (CommonUtils.isEmpty(tableAlias) || CommonUtils.equalObjects(tableAlias, matchedTableAlias))) {
                if (matchedTableAlias == null) {
                    matchedTableAlias = "";
                }
                tableRefs.put(matchedTableName, matchedTableAlias);
            }
        } catch (BadLocationException e) {
            log.debug(e);
        }
        return tableRefs;
    }

    @Override
    public Map<String, String> getTableAliasesFromQuery(String query) {
        return Collections.emptyMap();
    }

    private static boolean isTableQueryToken(TPToken tok, String value) {
        return tok.getData() == SQLTokenType.T_KEYWORD &&
            (value.equalsIgnoreCase(SQLConstants.KEYWORD_FROM) ||
                value.equalsIgnoreCase(SQLConstants.KEYWORD_UPDATE) ||
                value.equalsIgnoreCase(SQLConstants.KEYWORD_JOIN) ||
                value.equalsIgnoreCase(SQLConstants.KEYWORD_INTO));
    }

    private enum InlineState {
        UNMATCHED,
        TABLE_NAME,
        TABLE_DOT,
        ALIAS_AS,
        ALIAS_NAME,
        MATCHED
    }
}
