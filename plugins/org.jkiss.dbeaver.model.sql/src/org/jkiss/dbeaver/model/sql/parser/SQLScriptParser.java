/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLControlToken;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.SQLTokenEntry;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.SQLTokenPredicateEvaluator;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.model.text.TextUtils;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;

/**
 * SQL parser
 */
public class SQLScriptParser {

    static protected final Log log = Log.getLog(SQLScriptParser.class);

    private static final String CLI_ARG_DEBUG_DISABLE_SKIP_TOKEN_EVALUATION = "dbeaver.debug.sql.disable-skip-token-evaluation";

    private static boolean isPredicateEvaluationEnabled() {
        String property = System.getProperty(CLI_ARG_DEBUG_DISABLE_SKIP_TOKEN_EVALUATION); // Turn off processor settings save.
        return CommonUtils.isEmpty(property);
    }

    /**
     * Parses sql query.
     *
     * @param context        the context
     * @param startPos       the start position
     * @param endPos         the end position
     * @param currentPos     the current position
     * @param scriptMode     the script mode
     * @param keepDelimiters the keep delimiters option
     * @return the sql script element
     */
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
        SQLTokenPredicateEvaluator predicateEvaluator = new SQLTokenPredicateEvaluator(dialect.getSkipTokenPredicates());
        boolean isPredicateEvaluationEnabled = isPredicateEvaluationEnabled();

        // Parse range
        TPRuleBasedScanner ruleScanner = context.getScanner();
        boolean useBlankLines = !scriptMode && context.getSyntaxManager().isBlankLineDelimiter();
        boolean lineFeedIsDelimiter = ArrayUtils.contains(context.getSyntaxManager().getStatementDelimiters(), "\n");
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

            boolean isControl = false;
            String delimiterText = null;
            try {
                if (isPredicateEvaluationEnabled && tokenLength > 0 && !token.isWhitespace()) {
                    String tokenText = document.get(tokenOffset, tokenLength);
                    predicateEvaluator.captureToken(new SQLTokenEntry(tokenText, tokenType));
                }

                boolean isDelimiter = (tokenType == SQLTokenType.T_DELIMITER) ||
                    (lineFeedIsDelimiter && token.isWhitespace() && document.get(tokenOffset, tokenLength).contains("\n"));
                if (isDelimiter) {
                    // Save delimiter text
                    delimiterText = document.get(tokenOffset, tokenLength);
                } else if (useBlankLines && token.isWhitespace() && tokenLength > 1) {
                    // Check for blank line delimiter
                    if (lastTokenLineFeeds + countLineFeeds(document, tokenOffset, tokenLength) >= 2) {
                        isDelimiter = true;
                    }
                }
                lastTokenLineFeeds = 0;
                if (tokenLength == 1) {
                    // Check for bracket block begin/end
                    char aChar = document.getChar(tokenOffset);
                    if (aChar == '(' || aChar == '{' || aChar == '[') {
                        curBlock = new ScriptBlockInfo(curBlock, false);
                    } else if (aChar == ')' || aChar == '}' || aChar == ']') {
                        if (curBlock != null) {
                            curBlock = curBlock.parent;
                        }
                    }
                }
                if (tokenType == SQLTokenType.T_BLOCK_BEGIN && prevNotEmptyTokenType == SQLTokenType.T_BLOCK_END) {
                    // This is a tricky thing.
                    // In some dialects block end looks like END CASE, END LOOP. It is parsed as
                    // Block end followed by block begin (as CASE and LOOP are block begin tokens)
                    // So let's ignore block begin if previos token was block end and there were no delimiters.
                    tokenType = SQLTokenType.T_UNKNOWN;
                }
                if (tokenType == SQLTokenType.T_DELIMITER && prevNotEmptyTokenType == SQLTokenType.T_BLOCK_BEGIN) {
                    // Another trick. If BEGIN follows with delimiter then it is not a block (#7821)
                    if (curBlock != null) curBlock = curBlock.parent;
                }

                if (tokenType == SQLTokenType.T_BLOCK_HEADER) {
                    curBlock = new ScriptBlockInfo(curBlock, true);
                    hasBlocks = true;
                } else if (tokenType == SQLTokenType.T_BLOCK_TOGGLE) {
                    String togglePattern;
                    try {
                        togglePattern = document.get(tokenOffset, tokenLength);
                    } catch (BadLocationException e) {
                        log.warn(e);
                        togglePattern = "";
                    }

                    if (curBlock != null && togglePattern.equals(curBlock.togglePattern)) {
                        curBlock = curBlock.parent;
                    } else {
                        curBlock = new ScriptBlockInfo(curBlock, togglePattern);
                        hasBlocks = true;
                    }
                } else if (tokenType == SQLTokenType.T_BLOCK_BEGIN) {
                    // Drop header block if it is followed by a regular block and
                    // that block is not preceded by the prefix e.g 'AS', because in many dialects
                    // there's no direct header block terminators
                    // like 'BEGIN ... END' but 'DECLARE ... BEGIN ... END'
                    if (curBlock != null && curBlock.isHeader && !ArrayUtils.containsIgnoreCase(dialect.getInnerBlockPrefixes(), lastKeyword)) {
                        curBlock = curBlock.parent;
                    }
                    curBlock = new ScriptBlockInfo(curBlock, false);
                    hasBlocks = true;
                } else if (tokenType == SQLTokenType.T_BLOCK_END) {
                    if (curBlock != null) {
                        curBlock = curBlock.parent;
                    }
                } else if (isDelimiter && curBlock != null) {
                    // Delimiter in some brackets or inside block. Ignore it.
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
                            lastKeyword = document.get(tokenOffset, tokenLength);
                            if (firstKeyword == null) {
                                firstKeyword = lastKeyword;
                            }
                            break;
                    }
                }

                if (isPredicateEvaluationEnabled && !token.isEOF()) {
                    SQLParserActionKind actionKind = predicateEvaluator.evaluatePredicates();
                    if (actionKind == SQLParserActionKind.BEGIN_BLOCK) {
                        // header blocks seems optional and we are in the block either way
                        while (curBlock != null && curBlock.isHeader) {
                            curBlock = curBlock.parent;
                        }
                        curBlock = new ScriptBlockInfo(curBlock, false);
                        hasBlocks = true;
                    }

                    if (curBlock != null && !token.isEOF()) {
                        // if we are still inside of the block, so statement definitely hasn't ended yet
                        // and will not be ended until we leave the block at least
                        continue;
                    }

                    if (actionKind == SQLParserActionKind.SKIP_SUFFIX_TERM) {
                        continue;
                    }
                }

                boolean cursorInsideToken = currentPos >= tokenOffset && currentPos < tokenOffset + tokenLength;
                if (isControl && (
                        ((scriptMode || cursorInsideToken) && !hasValuableTokens)
                        || (token.isEOF() || (isDelimiter && tokenOffset + tokenLength >= currentPos))
                )) {
                    // Control query
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
                }
                if (hasValuableTokens && (token.isEOF() || (isDelimiter && tokenOffset + tokenLength >= currentPos) || tokenOffset > endPos)) {
                    if (tokenOffset > endPos) {
                        tokenOffset = endPos;
                    }
                    if (tokenOffset >= document.getLength()) {
                        // Sometimes (e.g. when comment finishing script text)
                        // last token offset is beyond document range
                        tokenOffset = document.getLength();
                    }
                    assert (tokenOffset >= currentPos);

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

                    if (isDelimiter &&
                        (keepDelimiters ||
                        (hasBlocks && dialect.isDelimiterAfterQuery()) ||
                        (needsDelimiterAfterBlock(firstKeyword, lastKeyword, dialect))))
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
                    if (curBlock != null) {
                        log.trace("Found leftover blocks in script after parsing");
                    }
                    // make script line
                    return new SQLQuery(
                        context.getDataSource(),
                        queryText,
                        statementStart,
                        queryEndPos - statementStart);
                }
                if (isDelimiter) {
                    statementStart = tokenOffset + tokenLength;
                    if (isPredicateEvaluationEnabled) {
                        firstKeyword = null;
                        predicateEvaluator.reset();
                        isControl = false;
                        hasBlocks = false;
                        hasValuableTokens = false;
                    }
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
            } catch (BadLocationException e) {
                log.warn("Error parsing query", e);
                StringWriter buf = new StringWriter();
                e.printStackTrace(new PrintWriter(buf, true));
                return new SQLQuery(context.getDataSource(), buf.toString());
            } finally {
                if (!token.isWhitespace() && !token.isEOF()) {
                    prevNotEmptyTokenType = tokenType;
                }
            }
        }
    }

    /**
     * Parses sql query.
     *
     * @param dialect          the dialect
     * @param preferenceStore  the preference store
     * @param sqlScriptContent the sql script content
     * @param cursorPosition   the cursor position
     * @return the sql script element
     */
    public static SQLScriptElement parseQuery(SQLDialect dialect, DBPPreferenceStore preferenceStore, String sqlScriptContent,
                                              int cursorPosition) {
        SQLParserContext parserContext = prepareSqlParserContext(dialect, preferenceStore, sqlScriptContent);
        return SQLScriptParser.extractQueryAtPos(parserContext, cursorPosition);
    }

    private static boolean needsDelimiterAfterBlock(String firstKeyword, String lastKeyword, SQLDialect dialect) {
        if (dialect.needsDelimiterFor(firstKeyword, lastKeyword)) {
            // SQL Server needs delimiters after MERGE
            return true;
        }
        // FIXME: special workaround for Oracle
        if (!dialect.isDelimiterAfterBlock()) {
            return false;
        }
        if (firstKeyword == null) {
            return false;
        }
        if (!SQLConstants.BLOCK_END.equalsIgnoreCase(lastKeyword)) {
            return false;
        }
        firstKeyword = firstKeyword.toUpperCase(Locale.ENGLISH);

        String[][] blockBoundStrings = dialect.getBlockBoundStrings();
        if (blockBoundStrings != null) {
            for (String[] bb : blockBoundStrings) {
                if (bb[0].equals(firstKeyword)) {
                    return true;
                }
            }
        }
        return ArrayUtils.contains(dialect.getBlockHeaderStrings(), firstKeyword) ||
            ArrayUtils.contains(dialect.getDDLKeywords(), firstKeyword);
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
            try {
                int currLineIndex = document.getLineOfOffset(currentPos);
                IRegion currLine = document.getLineInformation(currLineIndex);
                int currLineEnd = currLine.getOffset() + currLine.getLength(); 
                boolean hasNextLine = currLineIndex + 1 < document.getNumberOfLines();
                boolean inTailComment = SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT.equals(partitioner.getContentType(currentPos)); 
                if (!inTailComment && currentPos >= currLineEnd &&
                    (!hasNextLine || (hasNextLine && currentPos < document.getLineInformation(currLineIndex + 1).getOffset()))
                ) { 
                    inTailComment = SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT.equals(partitioner.getContentType(currLineEnd - 1));
                }
                if (inTailComment) {
                    int observablePosition = currentPos < document.getLength() ? currentPos : currentPos - 1;
                    int letterBeforeComment = skipCommentsBackTillLetter(document, partitioner, observablePosition, currLine.getOffset());
                    if (letterBeforeComment >= currLine.getOffset()) {
                        // if we are in the single-line comment and there are letters before the comment, then extract  
                        currentPos = letterBeforeComment;
                    }
                }
            } catch (BadLocationException ex) { 
                return null; 
            }
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
        boolean lineFeedIsDelimiter = ArrayUtils.contains(statementDelimiters, "\n");

        try {
            int originalPosLine = document.getLineOfOffset(currentPos);
            int currentLine = originalPosLine;
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

            if (!lineFeedIsDelimiter) {
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
                            if (offset >= 0) {
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
            }

            /*if (currentLine != originalPosLine) {
                // Move currentPos before last delimiter
                currentPos = lineOffset;
            } else */
            {
                // Move currentPos at line begin
                IRegion region = document.getLineInformation(currentLine);
                if (lineFeedIsDelimiter) {
                    startPos = currentPos = region.getOffset();
                } else if (region.getLength() > 0) {
                    int offsetFromLineStart = currentPos - region.getOffset();
                    String lineStr = document.get(region.getOffset(), offsetFromLineStart);
                    for (String delim : statementDelimiters) {
                        int delimIndex = lineStr.lastIndexOf(delim);
                        if (delimIndex != -1) {
                            // There is a delimiter in current line
                            // Move pos before it if there are no valuable chars between delimiter and cursor position
                            boolean hasValuableChars = false;
                            for (int i = region.getOffset() + delimIndex + delim.length(); i < currentPos; i++) {
                                if (!Character.isWhitespace(document.getChar(i))) {
                                    hasValuableChars = true;
                                    break;
                                }
                            }
                            if (!hasValuableChars) {
                                currentPos = region.getOffset() + delimIndex - 1;
                                break;
                            }
                        }
                    }
                }
            }

        } catch (BadLocationException e) {
            log.warn(e);
        }
        return parseQuery(context, startPos, document.getLength(), currentPos, false, false);
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
        IDocumentPartitioner partitioner = document instanceof IDocumentExtension3
            ? ((IDocumentExtension3) document).getDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING)
            : null;
        try {
            int docLength = document.getLength();
            int curPos;
            if (next) {
                final String[] statementDelimiters = context.getSyntaxManager().getStatementDelimiters();
                curPos = curElement.getOffset() + curElement.getLength();
                while (curPos < docLength) {
                    if (partitioner != null) { 
                        ITypedRegion region = partitioner.getPartition(curPos);
                        switch (region.getType()) {
                            case SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT:
                            case SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT: {
                                curPos = region.getOffset() + region.getLength();
                                continue;
                            }   
                        }
                    }
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
                curPos = skipCommentsBackTillLetter(document, partitioner, curPos, 0);
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
    
    private static int skipCommentsBackTillLetter(
        @NotNull IDocument document,
        @Nullable IDocumentPartitioner partitioner,
        int pos,
        int limit
    ) throws BadLocationException {
        int curPos = pos;
        while (curPos >= limit) {
            if (partitioner != null) { 
                ITypedRegion region = partitioner.getPartition(curPos);
                switch (region.getType()) {
                    case SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT:
                    case SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT: {
                        curPos = region.getOffset() - 1;
                        continue;
                    }   
                }
            }
            char c = document.getChar(curPos);
            if (Character.isLetter(c)) {
                return curPos;
            }
            curPos--;
        }
        return -1;
    }
    
    @Nullable
    public static SQLScriptElement extractActiveQuery(SQLParserContext context, int selOffset, int selLength) {
        return extractActiveQuery(context, new IRegion[]{new Region(selOffset, selLength)});
    }

    @Nullable
    public static SQLScriptElement extractActiveQuery(@NotNull SQLParserContext context, @NotNull IRegion[] regions) {
        String selText = null;

        try {
            final StringJoiner text = new StringJoiner(CommonUtils.getLineSeparator());
            for (IRegion region : regions) {
                if (region.getOffset() >= 0 && region.getLength() > 0) {
                    text.add(context.getDocument().get(region.getOffset(), region.getLength()));
                }
            }
            if (text.length() > 0) {
                selText = text.toString();
            }
        } catch (BadLocationException e) {
            log.debug(e);
        }

        if (selText != null && context.getPreferenceStore().getBoolean(ModelPreferences.QUERY_REMOVE_TRAILING_DELIMITER)) {
            SQLSyntaxManager syntaxManager = context.getSyntaxManager();
            selText = SQLUtils.trimQueryStatement(syntaxManager, selText, !syntaxManager.getDialect().isDelimiterAfterQuery());
        }

        final IRegion region = regions[0];
        final SQLScriptElement element;
        if (!CommonUtils.isEmpty(selText)) {
            SQLScriptElement parsedElement = SQLScriptParser.parseQuery(
                context,
                region.getOffset(), region.getOffset() + region.getLength(), region.getOffset(), false, false);
            if (parsedElement instanceof SQLControlCommand) {
                // This is a command
                element = parsedElement;
            } else {
                // Use selected query as is
                selText = SQLUtils.fixLineFeeds(selText);
                element = new SQLQuery(context.getDataSource(), selText, region.getOffset(), region.getLength());
            }
        } else if (region.getOffset() >= 0) {
            element = extractQueryAtPos(context, region.getOffset());
        } else {
            element = null;
        }
        // Check query do not ends with delimiter
        // (this may occur if user selected statement including delimiter)
        if (element == null || CommonUtils.isEmpty(element.getText())) {
            return null;
        }
        if (element instanceof SQLQuery) {
            SQLQuery query = (SQLQuery) element;
            query.setParameters(parseParametersAndVariables(context, query.getOffset(), query.getLength()));
        }
        return element;
    }

    public static List<SQLQueryParameter> parseParametersAndVariables(SQLParserContext context, int queryOffset, int queryLength) {
        final SQLDialect sqlDialect = context.getDialect();
        IDocument document = context.getDocument();
        if (queryOffset + queryLength > document.getLength()) {
            // This may happen during parameters parsing. Query may be trimmed or modified
            queryLength = document.getLength() - queryOffset;
        }
        SQLSyntaxManager syntaxManager = context.getSyntaxManager();
        boolean supportParamsInEmbeddedCode =
            context.getPreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED);
        boolean execQuery = false;
        boolean ddlQuery = false;
        boolean insideDollarQuote = false;
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
                            ddlQuery = true;
                        } else {
                            execQuery = ArrayUtils.containsIgnoreCase(sqlDialect.getExecuteKeywords(), tokenText);
                        }
                    } catch (BadLocationException e) {
                        log.warn(e);
                    }
                    firstKeyword = false;
                }


                if (tokenType == SQLTokenType.T_BLOCK_TOGGLE) {
                    insideDollarQuote = !insideDollarQuote;
                }

                if (tokenType == SQLTokenType.T_PARAMETER && tokenLength > 0) {
                    try {
                        String paramName = document.get(tokenOffset, tokenLength);
                        if (!supportParamsInEmbeddedCode && (ddlQuery || insideDollarQuote)) {
                            continue;
                        }
                        if (execQuery && paramName.equals(String.valueOf(syntaxManager.getAnonymousParameterMark()))) {
                            // Skip ? parameters for stored procedures (they have special meaning? [DB2])
                            continue;
                        }

                        if (parameters == null) {
                            parameters = new ArrayList<>();
                        }

                        String preparedParamName = null;
                        String paramMark = paramName.substring(0, 1);
                        if (paramMark.equals("$")) {
                            String variableName = SQLQueryParameter.stripVariablePattern(paramName);
                            if (!variableName.equals(paramName)) {
                                preparedParamName = variableName.toUpperCase(Locale.ENGLISH);
                            }
                        } 
                        if (preparedParamName == null) {
                            if (ArrayUtils.contains(syntaxManager.getNamedParameterPrefixes(), paramMark)) {
                                String rawParamName = paramName.substring(1);
                                if (sqlDialect.isQuotedIdentifier(rawParamName)) {
                                    preparedParamName = sqlDialect.getUnquotedIdentifier(rawParamName);
                                } else {
                                    preparedParamName = rawParamName.toUpperCase(Locale.ENGLISH);
                                }
                            } else {
                                preparedParamName = paramName;
                            }
                        }
                        
                        SQLQueryParameter parameter = new SQLQueryParameter(
                            syntaxManager,
                            parameters.size(),
                            preparedParamName,
                            tokenOffset - queryOffset,
                            tokenLength
                        );

                        parameter.setPrevious(getPreviousParameter(parameters, parameter));
                        parameters.add(parameter);
                    } catch (BadLocationException e) {
                        log.warn("Can't extract query parameter", e);
                    }
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
                            param = new SQLQueryParameter(
                                syntaxManager,
                                orderPos,
                                matcher.group(SQLQueryParameter.VARIABLE_NAME_GROUP_NAME).toUpperCase(Locale.ENGLISH),
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

        if (parseParameters) {
            // Parse parameters
            for (SQLScriptElement element : queryList) {
                if (element instanceof SQLQuery) {
                    SQLQuery query = (SQLQuery) element;
                    (query).setParameters(parseParametersAndVariables(parserContext, query.getOffset(), query.getLength()));
                }
            }
        }
        return queryList;
    }

    public static List<SQLScriptElement> parseScript(DBPDataSource dataSource, String sqlScriptContent) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dataSource.getSQLDialect(), dataSource.getContainer().getPreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);

        Document sqlDocument = new Document(sqlScriptContent);

        SQLParserContext parserContext = new SQLParserContext(dataSource, syntaxManager, ruleManager, sqlDocument);
        return SQLScriptParser.extractScriptQueries(parserContext, 0, sqlScriptContent.length(), true, false, true);
    }

    public static List<SQLScriptElement> parseScript(SQLDialect dialect, DBPPreferenceStore preferenceStore, String sqlScriptContent) {
        SQLParserContext parserContext = prepareSqlParserContext(dialect, preferenceStore, sqlScriptContent);
        return SQLScriptParser.extractScriptQueries(parserContext, 0, sqlScriptContent.length(), true, false, true);
    }

    @NotNull
    private static SQLParserContext prepareSqlParserContext(SQLDialect dialect, DBPPreferenceStore preferenceStore,
                                                            String sqlScriptContent) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, preferenceStore);
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules();

        Document sqlDocument = new Document(sqlScriptContent);

        SQLParserContext parserContext = new SQLParserContext(null, syntaxManager, ruleManager, sqlDocument);
        parserContext.setPreferenceStore(preferenceStore);
        return parserContext;
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