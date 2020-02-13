/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.text.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLControlToken;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.model.text.TextUtils;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * SQL parser
 */
public class SQLScriptParser
{
    static protected final Log log = Log.getLog(SQLScriptParser.class);

    public static SQLScriptElement parseQuery(
        final SQLParserContext context,
        final int startPos,
        final int endPos,
        final int currentPos,
        final boolean scriptMode,
        final boolean keepDelimiters)
    {
        int length = endPos - startPos;
        IDocument document = context.getDocument();
        if (length <= 0 || length > document.getLength()) {
            return null;
        }
        SQLDialect dialect = context.getDialect();

        // Parse range
        TPRuleBasedScanner ruleScanner = context.getScanner();
        boolean useBlankLines = !scriptMode && context.getSyntaxManager().isBlankLineDelimiter();
        ruleScanner.setRange(document, startPos, endPos - startPos);
        int statementStart = startPos;
        boolean hasValuableTokens = false;
        ScriptBlockInfo curBlock = null;
        boolean hasBlocks = false;
        int lastTokenLineFeeds = 0;
        SQLTokenType prevNotEmptyTokenType = SQLTokenType.T_UNKNOWN;
        String firstKeyword = null, lastKeyword = null;
        for (; ; ) {
            TPToken token = ruleScanner.nextToken();
            int tokenOffset = ruleScanner.getTokenOffset();
            int tokenLength = ruleScanner.getTokenLength();

            SQLTokenType tokenType = token instanceof TPTokenDefault ? (SQLTokenType) ((TPTokenDefault)token).getData() : SQLTokenType.T_OTHER;
            if (tokenOffset < startPos) {
                // This may happen with EOF tokens (bug in jface?)
                return null;
            }

            boolean isDelimiter = tokenType == SQLTokenType.T_DELIMITER;
            boolean isControl = false;
            String delimiterText = null;
            try {
                if (isDelimiter) {
                    // Save delimiter text
                    try {
                        delimiterText = document.get(tokenOffset, tokenLength);
                    } catch (BadLocationException e) {
                        log.debug(e);
                    }
                } else if (useBlankLines && token.isWhitespace() && tokenLength >= 1) {
                    // Check for blank line delimiter
                    if (lastTokenLineFeeds + countLineFeeds(document, tokenOffset, tokenLength) >= 2) {
                        isDelimiter = true;
                    }
                }
                lastTokenLineFeeds = 0;
                if (tokenLength == 1) {
                    // Check for bracket block begin/end
                    try {
                        char aChar = document.getChar(tokenOffset);
                        if (aChar == '(' || aChar == '{' || aChar == '[') {
                            curBlock = new ScriptBlockInfo(curBlock, false);
                        } else if (aChar == ')' || aChar == '}' || aChar == ']') {
                            if (curBlock != null) {
                                curBlock = curBlock.parent;
                            }
                        }
                    } catch (BadLocationException e) {
                        log.warn(e);
                    }
                }
                if (tokenType == SQLTokenType.T_BLOCK_BEGIN && prevNotEmptyTokenType == SQLTokenType.T_BLOCK_END) {
                    // This is a tricky thing.
                    // In some dialects block end looks like END CASE, END LOOP. It is parsed as
                    // Block end followed by block begin (as CASE and LOOP are block begin tokens)
                    // So let's ignore block begin if previos token was block end and there were no delimtiers.
                    tokenType = SQLTokenType.T_UNKNOWN;
                }

                if (tokenType == SQLTokenType.T_BLOCK_HEADER) {
                    if (curBlock == null) {
                        // Check for double block header, e.g. DO, DECLARE
                        curBlock = new ScriptBlockInfo(curBlock, true);
                    }
                    hasBlocks = true;
                } else if (tokenType == SQLTokenType.T_BLOCK_TOGGLE) {
                    String togglePattern;
                    try {
                        togglePattern = document.get(tokenOffset, tokenLength);
                    } catch (BadLocationException e) {
                        log.warn(e);
                        togglePattern = "";
                    }
                    // Second toggle pattern must be the same as first one.
                    // Toggles can be nested (PostgreSQL) and we need to count only outer
                    if (curBlock != null && curBlock.parent == null && togglePattern.equals(curBlock.togglePattern)) {
                        curBlock = curBlock.parent;
                    } else if (curBlock == null) {
                        curBlock = new ScriptBlockInfo(curBlock, togglePattern);
                    } else {
                        log.debug("Block toggle token inside another block. Can't process it");
                    }
                    hasBlocks = true;
                } else if (tokenType == SQLTokenType.T_BLOCK_BEGIN) {
                    if (curBlock == null || !curBlock.isHeader) {
                        curBlock = new ScriptBlockInfo(curBlock, false);
                    } else {
                        curBlock.isHeader = false;
                    }
                    hasBlocks = true;
                } else if (tokenType == SQLTokenType.T_BLOCK_END) {
                    // Sometimes query contains END clause without BEGIN. E.g. CASE, IF, etc.
                    // This END doesn't mean block
                    if (curBlock != null) {
                        if (!CommonUtils.isEmpty(curBlock.togglePattern)) {
                            // Block end inside of block toggle (#7460).
                            // Actually it is a result of some wrong SQL parse (e.g. we didn't recognize block begin correctly).
                            // However block toggle has higher priority. At the moment it is PostgreSQL specific.
                            try {
                                log.debug("Block end '" + document.get(tokenOffset, tokenLength) + "' inside of named block toggle '" + curBlock.togglePattern + "'. Ignore.");
                            } catch (Throwable e) {
                                log.debug(e);
                            }
                        } else {
                            curBlock = curBlock.parent;
                        }
                    }
                } else if (isDelimiter && curBlock != null) {
                    // Delimiter in some brackets - ignore it
                    continue;
                } else if (tokenType == SQLTokenType.T_SET_DELIMITER || tokenType == SQLTokenType.T_CONTROL) {
                    isDelimiter = true;
                    isControl = true;
                } else if (tokenType == SQLTokenType.T_COMMENT) {
                    lastTokenLineFeeds = tokenLength < 2 ? 0 : countLineFeeds(document, tokenOffset + tokenLength - 2, 2);
                }

                if (tokenLength > 0 && !token.isWhitespace()) {
                    switch (tokenType) {
                        case T_BLOCK_BEGIN:
                        case T_BLOCK_END:
                        case T_BLOCK_TOGGLE:
                        case T_BLOCK_HEADER:
                        case T_KEYWORD:
                        case T_UNKNOWN:
                            try {
                                lastKeyword = document.get(tokenOffset, tokenLength);
                                if (firstKeyword == null) {
                                    firstKeyword = lastKeyword;
                                }
                            } catch (BadLocationException e) {
                                log.error("Error getting last keyword", e);
                            }
                            break;
                    }
                }

                boolean cursorInsideToken = currentPos >= tokenOffset && currentPos < tokenOffset + tokenLength;
                if (isControl && (scriptMode || cursorInsideToken) && !hasValuableTokens) {
                    // Control query
                    try {
                        String controlText = document.get(tokenOffset, tokenLength);
                        String commandId = null;
                        if (token instanceof SQLControlToken) {
                            commandId = ((SQLControlToken) token).getCommandId();
                        }
                        SQLControlCommand command = new SQLControlCommand(
                            context.getDataSource(),
                            context.getSyntaxManager(),
                            controlText.trim(),
                            commandId,
                            tokenOffset,
                            tokenLength,
                            tokenType == SQLTokenType.T_SET_DELIMITER);
                        if (command.isEmptyCommand() ||
                            (command.getCommandId() != null &&
                                SQLCommandsRegistry.getInstance().getCommandHandler(command.getCommandId()) != null)) {
                            return command;
                        }
                        // This is not a valid command
                        isControl = false;
                    } catch (BadLocationException e) {
                        log.warn("Can't extract control statement", e); //$NON-NLS-1$
                        return null;
                    }
                }
                if (hasValuableTokens && (token.isEOF() || (isDelimiter && tokenOffset >= currentPos) || tokenOffset > endPos)) {
                    if (tokenOffset > endPos) {
                        tokenOffset = endPos;
                    }
                    if (tokenOffset >= document.getLength()) {
                        // Sometimes (e.g. when comment finishing script text)
                        // last token offset is beyond document range
                        tokenOffset = document.getLength();
                    }
                    assert (tokenOffset >= currentPos);
                    try {

                        // remove leading spaces
                        while (statementStart < tokenOffset && Character.isWhitespace(document.getChar(statementStart))) {
                            statementStart++;
                        }
                        // remove trailing spaces
/*
                        while (statementStart < tokenOffset && Character.isWhitespace(document.getChar(tokenOffset - 1))) {
                            tokenOffset--;
                            tokenLength++;
                        }
*/
                        if (tokenOffset == statementStart) {
                            // Empty statement
                            if (token.isEOF()) {
                                return null;
                            }
                            statementStart = tokenOffset + tokenLength;
                            continue;
                        }
                        String queryText = document.get(statementStart, tokenOffset - statementStart);
                        queryText = SQLUtils.fixLineFeeds(queryText);

                        boolean isDDLQuery = firstKeyword != null && ArrayUtils.contains(dialect.getDDLKeywords(), firstKeyword.toUpperCase(Locale.ENGLISH));
                        if (isDelimiter && (keepDelimiters ||
                            (hasBlocks && dialect.isDelimiterAfterQuery()) ||
                            (isDDLQuery && dialect.isDelimiterAfterBlock() && SQLConstants.BLOCK_END.equalsIgnoreCase(lastKeyword))))
                        {
                            if (delimiterText != null && delimiterText.equals(SQLConstants.DEFAULT_STATEMENT_DELIMITER)) {
                                // Add delimiter in the end of query. Do this only for semicolon delimiters.
                                // For SQL server add it in the end of query. For Oracle only after END clause
                                // Quite dirty workaround needed for Oracle and SQL Server.
                                // TODO: move this transformation into SQLDialect
                                queryText += delimiterText;
                            }
                        }
                        int queryEndPos = tokenOffset;
                        if (tokenType == SQLTokenType.T_DELIMITER) {
                            queryEndPos += tokenLength;
                        }
                        // make script line
                        return new SQLQuery(
                            context.getDataSource(),
                            queryText,
                            statementStart,
                            queryEndPos - statementStart);
                    } catch (BadLocationException ex) {
                        log.warn("Can't extract query", ex); //$NON-NLS-1$
                        return null;
                    }
                }
                if (isDelimiter) {
                    statementStart = tokenOffset + tokenLength;
                }
                if (token.isEOF()) {
                    return null;
                }
                if (!hasValuableTokens && !token.isWhitespace() && !isControl) {
                    if (tokenType == SQLTokenType.T_COMMENT) {
                        hasValuableTokens = dialect.supportsCommentQuery();
                    } else {
                        hasValuableTokens = true;
                    }
                }
            } finally {
                if (!token.isWhitespace() && !token.isEOF()) {
                    prevNotEmptyTokenType = tokenType;
                }
            }
        }
    }

    private static int countLineFeeds(final IDocument document, final int offset, final int length) {
        int lfCount = 0;
        try {
            for (int i = offset; i < offset + length; i++) {
                if (document.getChar(i) == '\n') {
                    lfCount++;
                }
            }
        } catch (BadLocationException e) {
            log.error(e);
        }
        return lfCount;
    }

    public static SQLScriptElement extractQueryAtPos(SQLParserContext context, int currentPos) {
        IDocument document = context.getDocument();
        if (document.getLength() == 0) {
            return null;
        }
        SQLSyntaxManager syntaxManager = context.getSyntaxManager();
        final int docLength = document.getLength();
        IDocumentPartitioner partitioner = document instanceof IDocumentExtension3 ? ((IDocumentExtension3)document).getDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING) : null;
        if (partitioner != null) {
            // Move to default partition. We don't want to be in the middle of multi-line comment or string
            while (currentPos < docLength && isMultiCommentPartition(partitioner, currentPos)) {
                currentPos++;
            }
        }
        // Extract part of document between empty lines
        int startPos = 0;
        boolean useBlankLines = syntaxManager.isBlankLineDelimiter();
        final String[] statementDelimiters = syntaxManager.getStatementDelimiters();
        int lastPos = currentPos >= docLength ? docLength - 1 : currentPos;

        try {
            int currentLine = document.getLineOfOffset(currentPos);
            if (useBlankLines) {
                if (TextUtils.isEmptyLine(document, currentLine)) {
                    if (currentLine == 0) {
                        return null;
                    }
                    currentLine--;
                    if (TextUtils.isEmptyLine(document, currentLine)) {
                        // Prev line empty too. No chance.
                        return null;
                    }
                }
            }

            int lineOffset = document.getLineOffset(currentLine);
            int firstLine = currentLine;
            while (firstLine > 0) {
                if (useBlankLines) {
                    if (TextUtils.isEmptyLine(document, firstLine) &&
                        isDefaultPartition(partitioner, document.getLineOffset(firstLine))) {
                        break;
                    }
                }
                if (currentLine == firstLine) {
                    for (String delim : statementDelimiters) {
                        if (Character.isLetterOrDigit(delim.charAt(0))) {
                            // Skip literal delimiters
                            continue;
                        }
                        final int offset = TextUtils.getOffsetOf(document, firstLine, delim);
                        if (offset >= 0 ) {
                            int delimOffset = document.getLineOffset(firstLine) + offset + delim.length();
                            if (isDefaultPartition(partitioner, delimOffset)) {
                                if (currentPos > startPos) {
                                    if (docLength > delimOffset) {
                                        boolean hasValuableChars = false;
                                        for (int i = delimOffset; i <= lastPos; i++) {
                                            if (!Character.isWhitespace(document.getChar(i))) {
                                                hasValuableChars = true;
                                                break;
                                            }
                                        }
                                        if (hasValuableChars) {
                                            startPos = delimOffset;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                firstLine--;
            }
            if (startPos == 0) {
                startPos = document.getLineOffset(firstLine);
            }

            // Move currentPos at line begin
            currentPos = lineOffset;
        } catch (BadLocationException e) {
            log.warn(e);
        }
        return parseQuery(context,
            startPos, document.getLength(), currentPos, false, false);
    }

    private static boolean isDefaultPartition(IDocumentPartitioner partitioner, int currentPos) {
        return partitioner == null || IDocument.DEFAULT_CONTENT_TYPE.equals(partitioner.getContentType(currentPos));
    }

    private static boolean isMultiCommentPartition(IDocumentPartitioner partitioner, int currentPos) {
        return partitioner != null && SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT.equals(partitioner.getContentType(currentPos));
    }

    public static SQLScriptElement extractNextQuery(SQLParserContext context, int offset, boolean next) {
        SQLScriptElement curElement = extractQueryAtPos(context, offset);
        if (curElement == null) {
            return null;
        }

        IDocument document = context.getDocument();
        try {
            int docLength = document.getLength();
            int curPos;
            if (next) {
                final String[] statementDelimiters = context.getSyntaxManager().getStatementDelimiters();
                curPos = curElement.getOffset() + curElement.getLength();
                while (curPos < docLength) {
                    char c = document.getChar(curPos);
                    if (!Character.isWhitespace(c)) {
                        boolean isDelimiter = false;
                        for (String delim : statementDelimiters) {
                            if (delim.indexOf(c) != -1) {
                                isDelimiter = true;
                            }
                        }
                        if (!isDelimiter) {
                            break;
                        }
                    }
                    curPos++;
                }
            } else {
                curPos = curElement.getOffset() - 1;
                while (curPos >= 0) {
                    char c = document.getChar(curPos);
                    if (Character.isLetter(c)) {
                        break;
                    }
                    curPos--;
                }
            }
            if (curPos <= 0 || curPos >= docLength) {
                return null;
            }
            return extractQueryAtPos(context, curPos);
        } catch (BadLocationException e) {
            log.warn(e);
            return null;
        }
    }

    @Nullable
    public static SQLScriptElement extractActiveQuery(SQLParserContext context, int selOffset, int selLength) {
        String selText = null;
        if (selOffset >= 0 && selLength > 0) {
            try {
                selText = context.getDocument().get(selOffset, selLength);
            } catch (BadLocationException e) {
                log.debug(e);
            }
        }

        if (selText != null && context.getPreferenceStore().getBoolean(ModelPreferences.QUERY_REMOVE_TRAILING_DELIMITER)) {
            SQLSyntaxManager syntaxManager = context.getSyntaxManager();
            selText = SQLUtils.trimQueryStatement(syntaxManager, selText, !syntaxManager.getDialect().isDelimiterAfterQuery());
        }

        SQLScriptElement element;
        if (!CommonUtils.isEmpty(selText)) {
            SQLScriptElement parsedElement = SQLScriptParser.parseQuery(
                context,
                selOffset, selOffset + selLength, selOffset, false, false);
            if (parsedElement instanceof SQLControlCommand) {
                // This is a command
                element = parsedElement;
            } else {
                // Use selected query as is
                selText = SQLUtils.fixLineFeeds(selText);
                element = new SQLQuery(context.getDataSource(), selText, selOffset, selLength);
            }
        } else if (selOffset >= 0) {
            element = extractQueryAtPos(context, selOffset);
        } else {
            element = null;
        }
        // Check query do not ends with delimiter
        // (this may occur if user selected statement including delimiter)
        if (element == null || CommonUtils.isEmpty(element.getText())) {
            return null;
        }
        if (element instanceof SQLQuery && context.getPreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED)) {
            SQLQuery query = (SQLQuery) element;
            query.setParameters(parseParameters(context, query.getOffset(), query.getLength()));
        }
        return element;
    }

    public static List<SQLQueryParameter> parseParameters(SQLParserContext context, int queryOffset, int queryLength) {
        final SQLDialect sqlDialect = context.getDialect();
        IDocument document = context.getDocument();
        if (queryOffset + queryLength > document.getLength()) {
            // This may happen during parameters parsing. Query may be trimmed or modified
            queryLength = document.getLength() - queryOffset;
        }
        SQLSyntaxManager syntaxManager = context.getSyntaxManager();
        boolean supportParamsInDDL = context.getPreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_IN_DDL_ENABLED);
        boolean execQuery = false;
        boolean ddlQuery = false;
        List<SQLQueryParameter> parameters = null;
        TPRuleBasedScanner ruleScanner = context.getScanner();
        ruleScanner.setRange(document, queryOffset, queryLength);

        boolean firstKeyword = true;
        for (; ; ) {
            TPToken token = ruleScanner.nextToken();
            final int tokenOffset = ruleScanner.getTokenOffset();
            final int tokenLength = ruleScanner.getTokenLength();
            if (token.isEOF() || tokenOffset > queryOffset + queryLength) {
                break;
            }
            // Handle only parameters which are not in SQL blocks
            SQLTokenType tokenType = token instanceof TPTokenDefault ? (SQLTokenType) ((TPTokenDefault)token).getData() : null;
            if (token.isWhitespace() || tokenType == SQLTokenType.T_COMMENT) {
                continue;
            }
            if (firstKeyword) {
                // Detect query type
                try {
                    String tokenText = document.get(tokenOffset, tokenLength);
                    if (ArrayUtils.containsIgnoreCase(sqlDialect.getDDLKeywords(), tokenText)) {
                        // DDL doesn't support parameters
                        ddlQuery = true;
                    } else {
                        execQuery = ArrayUtils.containsIgnoreCase(sqlDialect.getExecuteKeywords(), tokenText);
                    }
                } catch (BadLocationException e) {
                    log.warn(e);
                }
                firstKeyword = false;
            }

            if (tokenType == SQLTokenType.T_PARAMETER && tokenLength > 0) {
                try {
                    String paramName = document.get(tokenOffset, tokenLength);
                    if (!supportParamsInDDL && ddlQuery) {
                        continue;
                    }
                    if (execQuery && paramName.equals(String.valueOf(syntaxManager.getAnonymousParameterMark()))) {
                        // Skip ? parameters for stored procedures (they have special meaning? [DB2])
                        continue;
                    }

                    if (parameters == null) {
                        parameters = new ArrayList<>();
                    }

                    SQLQueryParameter parameter = new SQLQueryParameter(
                        syntaxManager,
                        parameters.size(),
                        paramName,
                        tokenOffset - queryOffset,
                        tokenLength);

                    parameter.setPrevious(getPreviousParameter(parameters, parameter));
                    parameters.add(parameter);
                } catch (BadLocationException e) {
                    log.warn("Can't extract query parameter", e);
                }
            }
        }

        if (syntaxManager.isVariablesEnabled()) {
            try {
                // Find variables in strings, comments, etc
                // Use regex
                String query = document.get(queryOffset, queryLength);

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
                            param = new SQLQueryParameter(syntaxManager, orderPos, matcher.group(0), start, matcher.end() - matcher.start());
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

    private static SQLQueryParameter getPreviousParameter(List<SQLQueryParameter> parameters, SQLQueryParameter parameter) {
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

    public static List<SQLScriptElement> extractScriptQueries(SQLParserContext parserContext, int startOffset, int length, boolean scriptMode, boolean keepDelimiters, boolean parseParameters) {
        List<SQLScriptElement> queryList = new ArrayList<>();

        IDocument document = parserContext.getDocument();
        if (document.getLength() == 0) {
            return queryList;
        }

        parserContext.startScriptEvaluation();
        try {
            for (int queryOffset = startOffset; ; ) {
                SQLScriptElement query = parseQuery(
                    parserContext, queryOffset, startOffset + length, queryOffset, scriptMode, keepDelimiters);
                if (query == null) {
                    break;
                }
                queryList.add(query);
                queryOffset = query.getOffset() + query.getLength();
            }
        } finally {
            parserContext.endScriptEvaluation();
        }

        if (parseParameters && parserContext.getPreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED)) {
            // Parse parameters
            for (SQLScriptElement element : queryList) {
                if (element instanceof SQLQuery) {
                    SQLQuery query = (SQLQuery) element;
                    (query).setParameters(parseParameters(parserContext, query.getOffset(), query.getLength()));
                }
            }
        }
        return queryList;
    }

    public static List<SQLScriptElement> parseScript(DBCExecutionContext executionContext, String sqlScriptContent) {
        DBPContextProvider contextProvider = () -> executionContext;

        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(executionContext.getDataSource());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(executionContext.getDataSource(), false);

        Document sqlDocument = new Document(sqlScriptContent);

        SQLParserContext parserContext = new SQLParserContext(contextProvider, syntaxManager, ruleManager, sqlDocument);
        return SQLScriptParser.extractScriptQueries(parserContext, 0, sqlScriptContent.length(), true, false, true);
    }

    private static class ScriptBlockInfo {
        final ScriptBlockInfo parent;
        final String togglePattern;
        boolean isHeader; // block started by DECLARE, FUNCTION, etc

        ScriptBlockInfo(ScriptBlockInfo parent, boolean isHeader) {
            this.parent = parent;
            this.togglePattern = null;
            this.isHeader = isHeader;
        }

        ScriptBlockInfo(ScriptBlockInfo parent, String togglePattern) {
            this.parent = parent;
            this.togglePattern = togglePattern;
        }
    }

}