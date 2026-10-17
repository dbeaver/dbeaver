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

package org.jkiss.dbeaver.ext.postgresql.model;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences.SQLScriptStatementDelimiterMode;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLPartitionScanner;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDollarQuoteRule;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.semantics.SQLDocumentScriptItemSyntaxContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLScriptItemAtOffset;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.function.Supplier;

/** Completion context for a PostgreSQL routine's Source tab. Never used for SQL scripts or other object editors. */
public class PostgreRoutineCompletion {
    private static final Log log = Log.getLog(PostgreRoutineCompletion.class);
    // Statement starters not included in the generic query, DDL or execution keyword groups.
    private static final String[] ADDITIONAL_QUERY_KEYWORDS = {"WITH", "LOCK", "TABLE"};
    private static final String[] QUERY_EXPRESSION_KEYWORDS = {SQLConstants.KEYWORD_SELECT, "WITH", "TABLE", "VALUES"};
    private static final String[] PROCEDURAL_PREFIX_KEYWORDS = {
        "DECLARE", "IF", "ELSIF", "WHILE", "CASE", "WHEN", "RETURN", "FOREACH",
        "PERFORM", "RAISE", "ASSERT", "EXIT", "CONTINUE", "GET", SQLConstants.BLOCK_END
    };

    private SQLParserContext parser;
    private IDocument sourceDocument;
    private long sourceStamp = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
    private boolean sourceBodyOnly;
    private int bodyStart;
    private int bodyEnd;
    private int preparedOffset = -1;
    private boolean preparedSqlQuery;
    @Nullable
    private SQLScriptElement preparedQuery;

    @Nullable
    public SQLCompletionRequest createRequest(
        @NotNull SQLCompletionContext context,
        @NotNull IDocument document,
        int offset,
        boolean simpleMode,
        boolean bodyOnly
    ) throws BadLocationException {
        if (parser == null) {
            SQLSyntaxManager syntax = new SQLSyntaxManager() {
                @NotNull
                @Override
                public String[] getStatementDelimiters() {
                    return SQLConstants.DEFAULT_SCRIPT_DELIMITER;
                }
            };
            // Inner dollar literals remain strings, regardless of the script execution settings.
            PostgreDialect dialect = new PostgreDialect() {
                @NotNull
                @Override
                public TPRule[] extendRules(@Nullable DBPDataSourceContainer container, @NotNull RulePosition position) {
                    TPRule[] rules = super.extendRules(container, position);
                    for (int i = 0; i < rules.length; i++) {
                        if (rules[i] instanceof SQLDollarQuoteRule) {
                            rules[i] = new SQLDollarQuoteRule(position == RulePosition.PARTITION, true, true, true);
                        }
                    }
                    return rules;
                }
            };
            syntax.init(dialect, context.getSyntaxManager().getPreferenceStore());
            syntax.setStatementDelimiterMode(SQLScriptStatementDelimiterMode.ONLY_SEPARATOR);
            SQLRuleManager rules = new SQLRuleManager(syntax);
            rules.loadRules(context.getDataSource(), false);
            parser = new SQLParserContext(context.getDataSource(), syntax, rules, new Document());
        }
        long stamp = document instanceof IDocumentExtension4 extension
            ? extension.getModificationStamp() : IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
        if (sourceDocument != document || stamp == IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP
            || sourceStamp != stamp || sourceBodyOnly != bodyOnly
        ) {
            int start = 0;
            int end = document.getLength();
            if (!bodyOnly) {
                ITypedRegion region = TextUtilities.getPartition(document, SQLParserPartitions.SQL_PARTITIONING, offset, true);
                if (!SQLParserPartitions.CONTENT_TYPE_SQL_STRING.equals(region.getType())
                    || document.getChar(region.getOffset()) != '$') {
                    return null;
                }
                // An argument default can also contain a dollar literal. Only the literal following AS is the body.
                var scanner = parser.getScanner();
                scanner.setRange(document, 0, region.getOffset());
                boolean afterAS = false;
                for (TPToken token = scanner.nextToken(); !token.isEOF(); token = scanner.nextToken()) {
                    if (!token.isWhitespace() && token.getData() != SQLTokenType.T_COMMENT) {
                        afterAS = SQLConstants.KEYWORD_AS.equalsIgnoreCase(
                            document.get(scanner.getTokenOffset(), scanner.getTokenLength())
                        );
                    }
                }
                if (!afterAS) {
                    return null;
                }
                start = region.getOffset() + 1;
                while (start < region.getOffset() + region.getLength() && document.getChar(start) != '$') {
                    start++;
                }
                int delimiterLength = start + 1 - region.getOffset();
                start++;
                end = region.getOffset() + region.getLength() - delimiterLength;
                if (end < start || !document.get(region.getOffset(), delimiterLength).equals(document.get(end, delimiterLength))) {
                    return null;
                }
            }
            bodyStart = start;
            bodyEnd = end;
            sourceDocument = document;
            sourceStamp = stamp;
            sourceBodyOnly = bodyOnly;
            preparedOffset = -1;
            preparedQuery = null;
            // A changed source invalidates the immutable snapshot used by earlier requests.
            parser = new SQLParserContext(context.getDataSource(), parser.getSyntaxManager(), parser.getRuleManager(), new Document());
        }
        if (offset < bodyStart || offset > bodyEnd) {
            return null;
        }
        // All proposals are validated for the same document version and caret position. Reuse their
        // SQL context, but give each request its own mutable word detector and analysis state.
        if (preparedOffset != offset) {
            QueryRange range = findQuery(document, offset);
            int snapshotEnd = Math.max(offset, range.end());
            if (parser.getDocument().getLength() < snapshotEnd) {
                // Preserve absolute offsets, but do not copy or partition the unused tail of a large routine.
                char[] text = document.get(0, snapshotEnd).toCharArray();
                Arrays.fill(text, 0, bodyStart, ' ');
                Document bodyDocument = new Document(new String(text));
                FastPartitioner partitioner = new FastPartitioner(
                    new SQLPartitionScanner(context.getDataSource(), parser.getSyntaxManager().getDialect(), parser.getRuleManager()),
                    SQLParserPartitions.SQL_CONTENT_TYPES
                );
                bodyDocument.setDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING, partitioner);
                partitioner.connect(bodyDocument);
                parser = new SQLParserContext(context.getDataSource(), parser.getSyntaxManager(), parser.getRuleManager(), bodyDocument);
            }
            preparedQuery = range.start() < 0 ? null
                : SQLScriptParser.parseQuery(parser, range.start(), range.end(), offset, false, false);
            if (preparedQuery instanceof SQLQuery query) {
                // parseQuery normalizes lone CR line endings. Semantic positions must refer to the original document.
                query.setOriginalText(parser.getDocument().get(query.getOffset(), query.getLength()));
            }
            preparedOffset = offset;
            preparedSqlQuery = range.sql();
        }
        boolean sqlQuery = preparedSqlQuery;
        return new SQLCompletionRequest(context, parser.getDocument(), offset, preparedQuery, simpleMode) {
            @Override
            public boolean supportsSemanticCompletion() {
                // Incomplete statement keywords and procedural expressions still use lexical completion.
                return sqlQuery && getActiveQuery() != null && getWordDetector().getStartOffset() > getActiveQuery().getOffset();
            }

            @Override
            public boolean requiresLegacyCompletion() {
                // Preserve SQL constructs not covered by the semantic grammar, such as UPDATE ... RETURNING.
                return true;
            }

            @NotNull
            @Override
            public SQLQueryCompletionContext obtainCompletionContext(
                @NotNull DBRProgressMonitor monitor,
                @NotNull Supplier<SQLQueryCompletionContext> editorContext
            ) {
                // Run in the completion job against this immutable request, never the helper's next snapshot.
                SQLScriptElement query = getActiveQuery();
                if (query == null) {
                    return SQLQueryCompletionContext.prepareOffquery(offset, offset);
                }
                SQLQueryRecognitionContext recognition = new SQLQueryRecognitionContext(
                    monitor, context.getExecutionContext(), true, false,
                    context.getSyntaxManager(), context.getSyntaxManager().getDialect()
                );
                var model = SQLQueryModelRecognizer.recognizeQuery(recognition, query.getOriginalText());
                if (model == null) {
                    return SQLQueryCompletionContext.prepareOffquery(query.getOffset(), offset);
                }
                var item = new SQLDocumentScriptItemSyntaxContext(
                    query.getOffset(), query.getOriginalText(), model, query.getLength()
                );
                item.setHasContextBoundaryAtLength(false);
                return SQLQueryCompletionContext.prepareCompletionContext(
                    new SQLScriptItemAtOffset(query.getOffset(), item), offset,
                    context.getExecutionContext(), context.getSyntaxManager().getDialect()
                );
            }

            @NotNull
            @Override
            public SQLWordPartDetector createWordDetector(@NotNull IDocument currentDocument, int currentOffset) {
                try {
                    SQLCompletionRequest updated = createRequest(context, currentDocument, currentOffset, simpleMode, bodyOnly);
                    if (updated != null) {
                        return updated.getWordDetector();
                    }
                } catch (BadLocationException e) {
                    log.debug(e);
                }
                return super.createWordDetector(currentDocument, currentOffset);
            }
        };
    }

    /**
     * Locate the SQL statement containing the caret using the existing PostgreSQL lexer. Once SQL starts,
     * its keywords (including CASE and identifiers such as "loop") belong to that query until its end.
     * Parentheses keep SELECTs in procedural conditions separate from the statements in their branches.
     */
    @NotNull
    private QueryRange findQuery(@NotNull IDocument document, int offset) throws BadLocationException {
        var scanner = parser.getScanner();
        scanner.setRange(document, bodyStart, bodyEnd - bodyStart);
        int depth = 0;
        int queryDepth = 0;
        int expressionCaseDepth = 0;
        int queryStart = -1;
        int statementStart = -1;
        String statementKeyword = "";
        boolean proceduralStatement = false;
        boolean declarations = false;
        boolean queryPrefix = false;
        boolean afterOpeningParenthesis = false;
        boolean afterQueryKeyword = false;
        boolean afterValuesKeyword = false;
        boolean afterStructSeparator = false;
        boolean forHeader = false;
        boolean cursorStatement = false;
        for (TPToken token = scanner.nextToken(); !token.isEOF(); token = scanner.nextToken()) {
            int start = scanner.getTokenOffset();
            int end = start + scanner.getTokenLength();
            if (queryStart < 0 && statementStart < 0 && start > offset) {
                return new QueryRange(-1, start, false);
            }
            if (token.isWhitespace() || token.getData() == SQLTokenType.T_COMMENT
                || token.getData() == SQLTokenType.T_STRING || token.getData() == SQLTokenType.T_QUOTED
            ) {
                if (token.getData() == SQLTokenType.T_STRING || token.getData() == SQLTokenType.T_QUOTED) {
                    afterStructSeparator = false;
                    afterOpeningParenthesis = false;
                    afterQueryKeyword = false;
                    afterValuesKeyword = false;
                }
                if (queryStart < 0 && statementStart < 0 && end > offset) {
                    return new QueryRange(-1, end, false);
                }
                continue;
            }
            String text = document.get(start, end - start);
            // The lexer can return the two characters of := as separate tokens.
            boolean assignment = text.equals(":=") || text.equals("=")
                || text.equals(":") && end < bodyEnd && document.getChar(end) == '=';
            // A statement keyword can instead be a variable on the left side of an assignment.
            if (afterQueryKeyword && (assignment
                || text.equals("[") || text.equals(String.valueOf(SQLConstants.STRUCT_SEPARATOR)))
                // A VALUES query starts with a parenthesized row; otherwise "values" can be a variable.
                || afterValuesKeyword && !text.equals("(")
            ) {
                queryStart = -1;
                proceduralStatement = true;
            }
            boolean queryExpression = ArrayUtils.containsIgnoreCase(QUERY_EXPRESSION_KEYWORDS, text);
            boolean rowQuery = queryExpression || ArrayUtils.containsIgnoreCase(SQLConstants.QUERY_KEYWORDS, text);
            boolean closingParenthesis = text.equals(")") && queryStart >= 0 && depth == queryDepth;
            // A field after a dot is an identifier, even when its name is a statement or block keyword.
            boolean endOfForQuery = forHeader && queryStart >= 0 && depth == queryDepth
                && !afterStructSeparator && text.equalsIgnoreCase("LOOP")
                // While completing loop_column or loop_table, this word is still part of the query.
                && !(start < offset && offset <= end);
            if (text.equals(SQLConstants.DEFAULT_STATEMENT_DELIMITER) || closingParenthesis || endOfForQuery) {
                if (offset <= start) {
                    return new QueryRange(queryStart < 0 ? statementStart : queryStart, start, queryStart >= 0);
                }
                queryStart = -1;
                // After a subquery, resume the enclosing condition or assignment.
                if (!closingParenthesis) {
                    expressionCaseDepth = 0;
                    statementStart = -1;
                    queryPrefix = false;
                    forHeader = false;
                    cursorStatement = false;
                }
            } else if (queryStart < 0 && !afterStructSeparator) {
                // Keep a fallback for SQL commands absent from the keyword groups. Recognized queries
                // still take precedence inside procedural prefixes such as RETURN QUERY or IF EXISTS.
                if (statementStart < 0) {
                    statementStart = start;
                    statementKeyword = text;
                    if (text.equalsIgnoreCase("DECLARE")) {
                        declarations = true;
                    }
                    proceduralStatement = declarations || ArrayUtils.containsIgnoreCase(PROCEDURAL_PREFIX_KEYWORDS, text)
                        || !Character.isJavaIdentifierStart(text.charAt(0));
                }
                if (assignment) {
                    proceduralStatement = true;
                }
                // CASE at the start of a statement is procedural; elsewhere it is an expression.
                // Its THEN/ELSE arms must not reset the surrounding assignment, condition or RETURN.
                if (text.equalsIgnoreCase("CASE") && start != statementStart
                    && !statementKeyword.equalsIgnoreCase(SQLConstants.BLOCK_END)
                ) {
                    expressionCaseDepth++;
                } else if (expressionCaseDepth > 0 && text.equalsIgnoreCase(SQLConstants.BLOCK_END)) {
                    expressionCaseDepth--;
                }
                boolean proceduralToken = expressionCaseDepth == 0;
                if (proceduralToken && (text.equalsIgnoreCase("OPEN") || text.equalsIgnoreCase("CURSOR"))) {
                    // OPEN ... FOR and CURSOR ... FOR introduce queries, not loop headers.
                    cursorStatement = true;
                    proceduralStatement = true;
                } else if (proceduralToken && text.equalsIgnoreCase("FOR")) {
                    forHeader = !cursorStatement;
                    queryPrefix = cursorStatement;
                    proceduralStatement = true;
                } else if (proceduralToken && (forHeader && text.equalsIgnoreCase("IN")
                    || statementKeyword.equalsIgnoreCase("RETURN") && text.equalsIgnoreCase("QUERY"))
                ) {
                    queryPrefix = !forHeader || !hasForRange(document, end);
                } else if (proceduralToken && (text.equalsIgnoreCase("LOOP") || text.equalsIgnoreCase(SQLConstants.BLOCK_BEGIN)
                    || text.equalsIgnoreCase("THEN") || text.equalsIgnoreCase("ELSE"))
                ) {
                    if (offset < start) {
                        return new QueryRange(statementStart, start, false);
                    }
                    if (text.equalsIgnoreCase(SQLConstants.BLOCK_BEGIN)) {
                        declarations = false;
                    }
                    statementStart = -1;
                    queryPrefix = false;
                    forHeader = false;
                    cursorStatement = false;
                } else if (start <= offset && (
                    proceduralToken && !proceduralStatement && (rowQuery || ArrayUtils.containsIgnoreCase(SQLConstants.DDL_KEYWORDS, text)
                        || SQLUtils.isExecKeyword(parser.getDialect(), text)
                        || ArrayUtils.containsIgnoreCase(ADDITIONAL_QUERY_KEYWORDS, text))
                    || proceduralToken && queryPrefix && rowQuery
                    || afterOpeningParenthesis && queryExpression)
                ) {
                    queryStart = start;
                    queryDepth = depth;
                } else if (start <= offset && proceduralToken && !proceduralStatement
                    && parser.getDialect().isEntityQueryWord(text)
                ) {
                    // In a fallback command, an object introducer such as VIEW establishes SQL context.
                    // The following identifier (even a prefix like "loop" or "lock") cannot start a new statement.
                    queryStart = statementStart;
                    queryDepth = depth;
                }
            }
            if (text.equals("(") || text.equals("[")) {
                depth++;
            } else if (text.equals(")") || text.equals("]")) {
                depth = Math.max(0, depth - 1);
            }
            // The lexer can emit a range's two dots separately; neither qualifies the following identifier.
            afterStructSeparator = text.length() == 1 && text.charAt(0) == SQLConstants.STRUCT_SEPARATOR
                && (start == bodyStart || document.getChar(start - 1) != SQLConstants.STRUCT_SEPARATOR)
                && (end == bodyEnd || document.getChar(end) != SQLConstants.STRUCT_SEPARATOR);
            afterOpeningParenthesis = text.equals("(");
            afterQueryKeyword = start == queryStart;
            afterValuesKeyword = afterQueryKeyword && text.equalsIgnoreCase("VALUES");
        }
        return new QueryRange(queryStart < 0 ? statementStart : queryStart, bodyEnd, queryStart >= 0);
    }

    /** Distinguish integer FOR bounds from a row query before interpreting any bound as SQL. */
    private boolean hasForRange(@NotNull IDocument document, int start) throws BadLocationException {
        TPRuleBasedScanner scanner = new TPRuleBasedScanner();
        scanner.setRules(parser.getRuleManager().getAllRules());
        scanner.setRange(document, start, bodyEnd - start);
        int depth = 0;
        boolean afterStructSeparator = false;
        for (TPToken token = scanner.nextToken(); !token.isEOF(); token = scanner.nextToken()) {
            if (token.isWhitespace() || token.getData() == SQLTokenType.T_COMMENT) {
                continue;
            }
            if (token.getData() == SQLTokenType.T_STRING || token.getData() == SQLTokenType.T_QUOTED) {
                afterStructSeparator = false;
                continue;
            }
            int end = scanner.getTokenOffset() + scanner.getTokenLength();
            String text = document.get(scanner.getTokenOffset(), scanner.getTokenLength());
            if (depth == 0) {
                if (text.contains("..") || text.endsWith(".") && end < bodyEnd && document.getChar(end) == '.') {
                    return true;
                }
                if (text.equals(SQLConstants.DEFAULT_STATEMENT_DELIMITER)
                    || !afterStructSeparator && text.equalsIgnoreCase("LOOP")
                ) {
                    return false;
                }
            }
            if (text.equals("(") || text.equals("[")) {
                depth++;
            } else if (text.equals(")") || text.equals("]")) {
                depth--;
            }
            afterStructSeparator = text.equals(String.valueOf(SQLConstants.STRUCT_SEPARATOR));
        }
        return false;
    }

    private record QueryRange(int start, int end, boolean sql) {
    }
}
