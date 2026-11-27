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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/** Shared SQL parameter and variable parser for a single query. */
public class DefaultSQLQueryParameterParser {
    private static final Log log = Log.getLog(DefaultSQLQueryParameterParser.class);
    @NotNull
    protected final SQLParserContext context;
    protected final int queryOffset;
    protected final int queryLength;
    protected final boolean embeddedCode;
    protected boolean ddl;
    protected boolean dollarQuote;
    protected boolean execute;

    public DefaultSQLQueryParameterParser(@NotNull SQLParserContext context, int queryOffset, int queryLength) {
        this.context = context;
        this.queryOffset = queryOffset;
        int documentLength = context.getDocument().getLength();
        // This may happen during parameters parsing. Query may be trimmed or modified
        this.queryLength = queryOffset + queryLength > documentLength ? documentLength - queryOffset : queryLength;
        this.embeddedCode = context.getPreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED);
    }

    @Nullable
    public List<SQLQueryParameter> parseParametersAndVariables() {
        final SQLDialect sqlDialect = context.getDialect();
        IDocument document = context.getDocument();
        SQLSyntaxManager syntaxManager = context.getSyntaxManager();
        char anonymousMark = getAnonymousParameterMark();
        // The scanner uses user preferences; a dialect may require a different marker or enable it independently.
        boolean scanRawMarkers = isAnonymousParametersEnabled()
            && (!syntaxManager.isAnonymousParametersEnabled() || anonymousMark != syntaxManager.getAnonymousParameterMark());
        List<SQLQueryParameter> parameters = null;
        TPRuleBasedScanner ruleScanner = context.getScanner();
        ruleScanner.setRange(document, queryOffset, queryLength);

        boolean firstKeyword = true;
        if (syntaxManager.isParametersEnabled()) {
            for (; ; ) {
                TPToken token = ruleScanner.nextToken();
                final int tokenOffset = ruleScanner.getTokenOffset();
                final int tokenLength = ruleScanner.getTokenLength();
                if (token.isEOF() || tokenOffset > queryOffset + queryLength) {
                    break;
                }
                // Handle only parameters which are not in SQL blocks
                SQLTokenType tokenType = token instanceof TPTokenDefault
                                         ? (SQLTokenType) ((TPTokenDefault) token).getData()
                                         : null;
                if (token.isWhitespace() || tokenType == SQLTokenType.T_COMMENT) {
                    continue;
                }
                if (firstKeyword) {
                    // Detect query type
                    try {
                        String tokenText = document.get(tokenOffset, tokenLength);
                        if (ArrayUtils.containsIgnoreCase(sqlDialect.getDDLKeywords(), tokenText)) {
                            // DDL doesn't support parameters
                            ddl = true;
                        } else {
                            execute = ArrayUtils.containsIgnoreCase(sqlDialect.getExecuteKeywords(), tokenText);
                        }
                    } catch (BadLocationException e) {
                        log.warn(e);
                    }
                    firstKeyword = false;
                }

                if (tokenType == SQLTokenType.T_BLOCK_TOGGLE) {
                    dollarQuote = !dollarQuote;
                }

                if (tokenType == SQLTokenType.T_PARAMETER && tokenLength > 0) {
                    try {
                        String paramName = document.get(tokenOffset, tokenLength);
                        int relativeOffset = tokenOffset - queryOffset;
                        int ordinal = parameters == null ? 0 : parameters.size();
                        SQLQueryParameter parameter = paramName.equals(String.valueOf(anonymousMark)) ?
                            parseAnonymousParameter(paramName, relativeOffset, ordinal) :
                            parseNamedParameter(paramName, relativeOffset, tokenLength, ordinal);
                        parameters = addParameter(parameters, parameter);
                    } catch (BadLocationException e) {
                        log.warn("Can't extract query parameter", e);
                    }
                } else if (scanRawMarkers && tokenType != SQLTokenType.T_STRING
                    && tokenType != SQLTokenType.T_QUOTED && tokenLength > 0) {
                    // Anonymous markers may be ordinary tokens when the anonymous-parameters preference is disabled.
                    try {
                        String tokenText = document.get(tokenOffset, tokenLength);
                        for (int i = tokenText.indexOf(anonymousMark); i >= 0; i = tokenText.indexOf(anonymousMark, i + 1)) {
                            SQLQueryParameter parameter = parseAnonymousParameter(
                                String.valueOf(anonymousMark), tokenOffset + i - queryOffset,
                                parameters == null ? 0 : parameters.size());
                            parameters = addParameter(parameters, parameter);
                        }
                    } catch (BadLocationException e) {
                        log.warn("Can't extract native query parameter", e);
                    }
                }
            }
        }

        return parseVariables(parameters);
    }

    @Nullable
    private List<SQLQueryParameter> parseVariables(@Nullable List<SQLQueryParameter> parameters) {
        SQLParserContext context = this.context;
        SQLSyntaxManager syntaxManager = context.getSyntaxManager();
        if (syntaxManager.isVariablesEnabled()) {
            try {
                // Find variables in strings, comments, etc
                // Use regex
                String query = context.getDocument().get(queryOffset, queryLength);

                Matcher matcher = SQLQueryParameter.getVariablePattern().matcher(query);
                int position = 0;
                while (matcher.find(position)) {
                    {
                        int start = matcher.start();
                        int orderPos = 0;
                        SQLQueryParameter param = null;
                        if (parameters != null) {
                            for (SQLQueryParameter p : parameters) {
                                if (p.getTokenOffset() == start) {
                                    param = p;
                                    break;
                                } else if (p.getTokenOffset() < start) {
                                    orderPos++;
                                }
                            }
                        }

                        if (param == null) {
                            String paramName = SQLQueryParameter.getVariableName(matcher);
                            param = new SQLQueryParameter(
                                syntaxManager,
                                orderPos,
                                paramName,
                                paramName,
                                start,
                                matcher.end() - matcher.start()
                            );
                            if (parameters == null) {
                                parameters = new ArrayList<>();
                            }
                            param.setPrevious(getPreviousParameter(parameters, param));
                            parameters.add(param.getOrdinalPosition(), param);
                        }
                    }
                    position = matcher.end();
                }
            } catch (BadLocationException e) {
                log.warn("Error parsing variables", e);
            }
        }

        return parameters;
    }

    @Nullable
    protected SQLQueryParameter parseNamedParameter(@NotNull String name, int offset, int length, int ordinal) {
        if (!embeddedCode && (ddl || dollarQuote)) {
            return null;
        }
        String preparedName = SQLQueryParameter.stripVariablePattern(name);
        if (preparedName.equals(name)
            && ArrayUtils.contains(context.getSyntaxManager().getNamedParameterPrefixes(), name.substring(0, 1))) {
            preparedName = name.substring(1);
        }
        return new SQLQueryParameter(context.getSyntaxManager(), ordinal, preparedName, name, offset, length);
    }

    @Nullable
    protected SQLQueryParameter parseAnonymousParameter(@NotNull String mark, int offset, int ordinal) {
        if (!isAnonymousParametersEnabled() || isAnonymousParameterIgnored()) {
            return null;
        }
        return new SQLQueryParameter(context.getSyntaxManager(), ordinal, mark, mark, offset, 1);
    }

    protected boolean isAnonymousParametersEnabled() {
        return context.getSyntaxManager().isAnonymousParametersEnabled();
    }

    protected char getAnonymousParameterMark() {
        return context.getSyntaxManager().getAnonymousParameterMark();
    }

    protected boolean isAnonymousParameterIgnored() {
        return (!embeddedCode && (ddl || dollarQuote)) || execute;
    }

    @Nullable
    private List<SQLQueryParameter> addParameter(@Nullable List<SQLQueryParameter> parameters,
                                                 @Nullable SQLQueryParameter parameter) {
        if (parameter != null) {
            if (parameters == null) {
                parameters = new ArrayList<>();
            }
            parameter.setPrevious(getPreviousParameter(parameters, parameter));
            parameters.add(parameter);
        }
        return parameters;
    }

    @Nullable
    private SQLQueryParameter getPreviousParameter(@NotNull List<SQLQueryParameter> parameters,
                                                   @NotNull SQLQueryParameter parameter) {
        String varName = parameter.getVarName();
        if (parameter.isNamed()) {
            for (int i = parameters.size(); i > 0; i--) {
                if (parameters.get(i - 1).getVarName().equals(varName)) {
                    return parameters.get(i - 1);
                }
            }
        }
        return null;
    }
}
