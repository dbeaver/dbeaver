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

package org.jkiss.dbeaver.model.sql.parser;

import org.antlr.v4.runtime.Token;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.sql.AbstractSQLDialect;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.dialect.SQLStandardAnalyzer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLControlToken;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.SQLTokenEntry;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.SQLTokenPredicateEvaluator;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.stm.STMSource;
import org.jkiss.dbeaver.model.text.TextUtils;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
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
        @NotNull final SQLParserContext context,
        final int startPos,
        final int endPos,
        final int currentPos,
        final boolean scriptMode,
        final boolean keepDelimiters
    ) {
        return tryExpandElement(parseQueryImpl(context, startPos, endPos, currentPos, scriptMode, keepDelimiters), context);
    }

    private static SQLScriptElement parseQueryImpl(
        @NotNull final SQLParserContext context,
        final int startPos,
        final int endPos,
        final int currentPos,
        final boolean scriptMode,
        final boolean keepDelimiters
    ) {
        int length = endPos - startPos;
        IDocument document = context.getDocument();
        if (length <= 0 || length > document.getLength()) {
            return null;
        }
        SQLDialect dialect = context.getDialect();
        SQLTokenPredicateEvaluator predicateEvaluator = new SQLTokenPredicateEvaluator(dialect.getSkipTokenPredicates());
        boolean isPredicateEvaluationEnabled = isPredicateEvaluationEnabled();
        boolean newTokenCaptured = false;

        // Parse range
        TPRuleBasedScanner ruleScanner = context.getScanner();
        boolean useBlankLines = !scriptMode && context.getSyntaxManager().getStatementDelimiterMode().useBlankLine;
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
                    predicateEvaluator.captureToken(new SQLTokenEntry(tokenText, tokenType, false));
                    newTokenCaptured = true;
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
                    // So let's ignore block begin if previous token was block end and there were no delimiters.
                    tokenType = SQLTokenType.T_UNKNOWN;
                }
                if (tokenType == SQLTokenType.T_DELIMITER && prevNotEmptyTokenType == SQLTokenType.T_BLOCK_BEGIN) {
                    // Another trick. If BEGIN follows with delimiter then it is not a block (#7821)
                    if (curBlock != null) curBlock = curBlock.parent;
                }
                if (dialect.isStripCommentsBeforeBlocks() && tokenType == SQLTokenType.T_BLOCK_HEADER && prevNotEmptyTokenType == SQLTokenType.T_COMMENT) {
                    statementStart = tokenOffset;
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
                        if (curBlock.togglePattern != null) {
                            log.trace("SQLScriptParser: blocks structure recognition inconsistency - trying to leave toggled block on non-togging token");
                        } else {
                            curBlock = curBlock.parent;
                        }
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

                if (isPredicateEvaluationEnabled && !token.isEOF() && newTokenCaptured) {
                    newTokenCaptured = false;
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
                        // if we are still inside the block, so statement definitely hasn't ended yet
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

                    int queryEndPos = tokenOffset;
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
                    if (tokenType == SQLTokenType.T_DELIMITER) {
                        queryEndPos += tokenLength;
                    }
                    if (curBlock != null) {
                        log.trace("Found leftover blocks in script after parsing");
                    }
                    // make script line
                    SQLQuery query = new SQLQuery(
                        context.getDataSource(),
                        queryText,
                        statementStart,
                        queryEndPos - statementStart);
                    query.setEndsWithDelimiter(tokenType == SQLTokenType.T_DELIMITER);
                    return query;
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
    public static SQLScriptElement parseQuery(
        DBPDataSource dataSource,
        SQLDialect dialect,
        DBPPreferenceStore preferenceStore,
        String sqlScriptContent,
        int cursorPosition
    ) {
        SQLParserContext parserContext = prepareSqlParserContext(dataSource, dialect, preferenceStore, sqlScriptContent);
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
        return tryExpandElement(extractQueryAtPosImpl(context, currentPos), context);
    }
    
    private static SQLScriptElement extractQueryAtPosImpl(SQLParserContext context, int currentPos) {
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
        boolean useBlankLines = syntaxManager.getStatementDelimiterMode().useBlankLine;
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
        return parseQueryImpl(context, startPos, document.getLength(), currentPos, false, false);
    }

    private static boolean isDefaultPartition(IDocumentPartitioner partitioner, int currentPos) {
        return partitioner == null || IDocument.DEFAULT_CONTENT_TYPE.equals(partitioner.getContentType(currentPos));
    }

    private static boolean isMultiCommentPartition(IDocumentPartitioner partitioner, int currentPos) {
        return partitioner != null && SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT.equals(partitioner.getContentType(currentPos));
    }

    public static SQLScriptElement extractNextQuery(@NotNull SQLParserContext context, int offset, boolean next) {
        SQLScriptElement curElement = extractQueryAtPos(context, offset);
        return tryExpandElement(extractNextQueryImpl(context, curElement, next), context);
    }

    public static SQLScriptElement extractNextQuery(
        @NotNull SQLParserContext context,
        @Nullable SQLScriptElement curElement,
        boolean next
    ) {
        return tryExpandElement(extractNextQueryImpl(context, curElement, next), context);
    }

    private static SQLScriptElement extractNextQueryImpl(
        @NotNull SQLParserContext context,
        @Nullable SQLScriptElement curElement,
        boolean next
    ) {
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
            return extractQueryAtPosImpl(context, curPos);
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
            query.setParameters(parseParametersAndVariables(context, query.getText()));
        }
        return element;
    }

    public static List<SQLQueryParameter> parseParametersAndVariables(SQLParserContext context, String selectedQueryText) {
        SQLParserContext ctx = new SQLParserContext(
                context.getDataSource(),
                context.getSyntaxManager(),
                context.getRuleManager(),
                new Document(selectedQueryText)
        );
        return  parseParametersAndVariables(ctx, 0, selectedQueryText.length());
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
                                preparedParamName = variableName;
                            }
                        }
                        if (preparedParamName == null) {
                            if (ArrayUtils.contains(syntaxManager.getNamedParameterPrefixes(), paramMark)) {
                                preparedParamName = paramName.substring(1);
                            } else {
                                preparedParamName = paramName;
                            }
                        }
                        
                        SQLQueryParameter parameter = new SQLQueryParameter(
                            syntaxManager,
                            parameters.size(),
                            preparedParamName,
                            paramName,
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
                            String paramName = matcher.group(SQLQueryParameter.VARIABLE_NAME_GROUP_NAME);
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

    public static List<SQLScriptElement> extractScriptQueries(
        @NotNull SQLParserContext parserContext,
        int startOffset,
        int length,
        boolean scriptMode,
        boolean keepDelimiters,
        boolean parseParameters
    ) {
        // LinkedList is crucial to prevent copy on expand and for many-to-one replacements efficiency
        List<SQLScriptElement> queryList = new LinkedList<>();

        IDocument document = parserContext.getDocument();
        if (document.getLength() == 0) {
            return queryList;
        }

        parserContext.startScriptEvaluation();
        try {
            for (int queryOffset = startOffset; ; ) {
                SQLScriptElement query = parseQueryImpl(parserContext, queryOffset, startOffset + length, queryOffset, scriptMode, keepDelimiters);
                if (query == null) {
                    break;
                }
                queryList.add(query);
                queryOffset = query.getOffset() + query.getLength();
            }
        } finally {
            parserContext.endScriptEvaluation();
        }

        if (parserContext.getSyntaxManager().getStatementDelimiterMode().useSmart) {
            expandQueries(parserContext, queryList);
        }

        if (parseParameters) {
            // Parse parameters
            for (SQLScriptElement element : queryList) {
                if (element instanceof SQLQuery query) {
                    query.setParameters(parseParametersAndVariables(parserContext, query.getOffset(), query.getLength()));
                }
            }
        }
        return queryList;
    }

    private static void expandQueries(@NotNull SQLParserContext parserContext, @NotNull List<SQLScriptElement> queryList) {
        var continuationDetector = new ScriptElementContinuationDetector(parserContext);
        var it = queryList.listIterator();
        while (it.hasNext()) {
            SQLScriptElement firstElement = it.next();
            if (firstElement instanceof SQLQuery queryStart && !queryStart.isEndsWithDelimiter() && it.hasNext()) {
                SQLQuery prevElement = queryStart;
                SQLScriptElement currElement = it.next();
                boolean captureCurrElement;
                while ((captureCurrElement = (
                        currElement instanceof SQLQuery queryElement && !continuationDetector.elementStartsProperly(queryElement) &&
                        !prevElement.isEndsWithDelimiter()
                    )) && it.hasNext()) {
                    it.remove(); // remove currElement while it is a continuation of the query started at the firstElement
                    prevElement = (SQLQuery) currElement;
                    currElement = it.next();
                }
                SQLQuery lastElement = captureCurrElement ? (SQLQuery) currElement : prevElement;
                if (lastElement != firstElement) {
                    if (captureCurrElement) {
                        it.remove();
                    }
                    SQLScriptElement prev = it.previous();
                    if (!captureCurrElement) {
                        // first previous() call returns currElement again, if we didn't remove it yet
                        assert prev == currElement;
                        prev = it.previous();
                    }
                    assert prev == firstElement;
                    // replace the original query head element with extended element
                    it.remove();
                    it.add(continuationDetector.prepareExtendedSQLScriptElement(queryStart, lastElement));
                } else {
                    // if there is nothing to capture, return the currElement back
                    // because it apparently is a head of the next query to handle on the next iteration
                    it.previous();
                }
            }
        }
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

    public static List<SQLScriptElement> parseScript(
        DBPDataSource dataSource,
        SQLDialect dialect,
        DBPPreferenceStore preferenceStore,
        String sqlScriptContent
    ) {
        SQLParserContext parserContext = prepareSqlParserContext(dataSource, dialect, preferenceStore, sqlScriptContent);
        return SQLScriptParser.extractScriptQueries(parserContext, 0, sqlScriptContent.length(), true, false, true);
    }

    @NotNull
    private static SQLParserContext prepareSqlParserContext(
        DBPDataSource dataSource,
        SQLDialect dialect,
        DBPPreferenceStore preferenceStore,
        String sqlScriptContent
    ) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, preferenceStore);
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules();

        Document sqlDocument = new Document(sqlScriptContent);

        FastPartitioner partitioner = new FastPartitioner(
            new SQLPartitionScanner(dataSource, dialect, ruleManager),
            SQLParserPartitions.SQL_CONTENT_TYPES);
        partitioner.connect(sqlDocument);

        sqlDocument.setDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING, partitioner);

        SQLParserContext parserContext = new SQLParserContext(dataSource, syntaxManager, ruleManager, sqlDocument);
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

    private static SQLScriptElement tryExpandElement(SQLScriptElement element, SQLParserContext context) {
        if (element instanceof SQLQuery queryElement && context.getSyntaxManager().getStatementDelimiterMode().useSmart) {
            var continuationDetector = new ScriptElementContinuationDetector(context);
            SQLScriptElement extendedElement = continuationDetector.tryPrepareExtendedElement(queryElement);
            if (extendedElement != null) {
                return extendedElement;
            }
        }
        return element;
    }
    
    private static class ScriptElementContinuationDetector {
        private static final Set<Integer> statementStartTokenIds = LSMInspections.prepareOffquerySyntaxInspection().predictedTokenIds();

        private static final Map<SQLDialect, Set<String>> statementStartKeywordsByDialect = Collections.synchronizedMap(new WeakHashMap<>());
        
        private final Set<String> statementStartKeywords;

        private final SQLParserContext context;
        private final LSMAnalyzerParameters analyzerParameters;
        
        public ScriptElementContinuationDetector(@NotNull SQLParserContext context) {
            this.context = context;
            this.statementStartKeywords = getStatementStartKeywords(this.context.getDialect());
            this.analyzerParameters = LSMAnalyzerParameters.forDialect(this.context.getDialect(), this.context.getSyntaxManager());
        }

        private static Set<String> getStatementStartKeywords(SQLDialect dialect) {
            return statementStartKeywordsByDialect.computeIfAbsent(dialect, d -> prepareStatementStartKeywordsSet(d));
        }

        private static Set<String> prepareStatementStartKeywordsSet(SQLDialect dialect) {
            Set<String> statementStartKeywords = new HashSet<>();

            if (dialect.getBlockHeaderStrings() != null) {
                Arrays.stream(dialect.getBlockHeaderStrings()).map(String::toUpperCase).forEach(statementStartKeywords::add);
            }
            String[][] blockBoundStrings = dialect.getBlockBoundStrings();
            if (blockBoundStrings != null) {
                for (String[] block : blockBoundStrings) {
                    statementStartKeywords.add(block[0]);
                }
            }
            if (dialect.getTransactionCommitKeywords() != null) {
                Arrays.stream(dialect.getTransactionCommitKeywords()).map(String::toUpperCase).forEach(statementStartKeywords::add);
            }
            if (dialect.getTransactionRollbackKeywords() != null) {
                Arrays.stream(dialect.getTransactionRollbackKeywords()).map(String::toUpperCase).forEach(statementStartKeywords::add);
            }
            if (dialect instanceof AbstractSQLDialect abstractSQLDialect) {
                Arrays.stream(abstractSQLDialect.getNonTransactionKeywords()).map(String::toUpperCase).forEach(statementStartKeywords::add);
            }
            Arrays.stream(dialect.getExecuteKeywords()).map(String::toUpperCase).forEach(statementStartKeywords::add);
            for (SQLCommandHandlerDescriptor controlCommand : SQLCommandsRegistry.getInstance().getCommandHandlers()) {
                statementStartKeywords.add("@" + controlCommand.getId().toUpperCase());
            }
            Arrays.stream(dialect.getQueryKeywords()).map(String::toUpperCase).forEach(statementStartKeywords::add);
            Arrays.stream(dialect.getDMLKeywords()).map(String::toUpperCase).forEach(statementStartKeywords::add);
            Arrays.stream(dialect.getDDLKeywords()).map(String::toUpperCase).forEach(statementStartKeywords::add);

            return statementStartKeywords;
        }

        private boolean elementStartsProperly(@NotNull SQLScriptElement element) {
            SQLStandardLexer lexer = SQLStandardAnalyzer.createLexer(
                STMSource.fromString(element.getOriginalText()),
                this.analyzerParameters
            );
            Token token = lexer.nextToken();
            while (token != null && token.getType() != -1 && token.getChannel() != Token.DEFAULT_CHANNEL) {
                token = lexer.nextToken();
            }
            return token != null && (
                statementStartTokenIds.contains(token.getType()) || statementStartKeywords.contains(token.getText().toUpperCase())
            );
        }

        private SQLQuery findSmartStatementBegginning(@NotNull SQLQuery element) {
            SQLQuery lastElement = element;
            SQLScriptElement prevElement = extractNextQueryImpl(this.context, element, false);
            boolean takePrev = true;
            while (
                prevElement instanceof SQLQuery prevQueryFragment &&
                    (takePrev = (
                        !Boolean.TRUE.equals(prevQueryFragment.isEndsWithDelimiter()) ||
                        prevElement.getOffset() + prevElement.getLength() >= lastElement.getOffset() + lastElement.getLength()
                    )) && !elementStartsProperly(prevElement) && prevElement.getOffset() < lastElement.getOffset()
            ) {
                lastElement = prevQueryFragment;
                prevElement = extractNextQueryImpl(this.context, lastElement, false);
            }
            SQLQuery boundaryElement = prevElement instanceof SQLQuery prevQueryElement && takePrev ? prevQueryElement : lastElement;
            return boundaryElement;
        }

        private SQLQuery findSmartStatementEnding(@NotNull SQLQuery element) {
            SQLQuery lastElement = element;
            SQLScriptElement nextElement = extractNextQueryImpl(this.context, element, true);
            while (nextElement instanceof SQLQuery nextQueryFragment &&
                !Boolean.TRUE.equals(lastElement.isEndsWithDelimiter()) &&
                !elementStartsProperly(nextElement) &&
                nextElement.getOffset() > lastElement.getOffset()
            ) {
                lastElement = nextQueryFragment;
                nextElement = extractNextQueryImpl(this.context, lastElement, true);
            }
            return lastElement;
        }

        @Nullable
        public SQLScriptElement tryPrepareExtendedElement(@NotNull SQLQuery element) {
            SQLQuery headElement = this.elementStartsProperly(element) ? element : this.findSmartStatementBegginning(element);
            SQLQuery extendedHead = headElement == element ? element : this.prepareExtendedSQLScriptElement(headElement, element);
            SQLQuery tailElement = this.findSmartStatementEnding(extendedHead);
            return prepareExtendedSQLScriptElement(extendedHead, tailElement);
        }

        public SQLQuery prepareExtendedSQLScriptElement(
            @NotNull SQLQuery headElement,
            @NotNull SQLQuery tailElement
        ) {
            try {
                int start = headElement.getOffset();
                int headEnd = headElement.getOffset() + headElement.getLength();
                int tailEnd = tailElement.getOffset() + tailElement.getLength();
                int realEnd;
                int extractionEnd;
                if (headEnd > tailEnd) {
                    realEnd = headEnd;
                    extractionEnd = headElement.getOffset() + headElement.getOriginalText().length();
                } else {
                    realEnd = tailEnd;
                    extractionEnd = tailElement.getOffset() + tailElement.getOriginalText().length();
                }
                String text = this.context.getDocument().get(start, extractionEnd - start);
                SQLQuery query = new SQLQuery(this.context.getDataSource(), text, start, realEnd - start);
                query.setEndsWithDelimiter(tailElement.isEndsWithDelimiter());
                return query;
            } catch (BadLocationException ex) {
                return headElement;
            }
        }
    }

}