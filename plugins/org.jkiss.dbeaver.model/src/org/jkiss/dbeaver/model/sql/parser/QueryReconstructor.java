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

import net.sf.jsqlparser.parser.Token;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.Diff;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Carries information about the comments in the original SQL text to effectively reconstruct it on modifications,
 * combining both original text fragments with comments and newly introduced fragments of the new SQL text
 */
public class QueryReconstructor {

    private static final boolean DEBUG = false;

    private final SQLUtils.CommentsCollectionResult commentsCollectionResult;
    private final Token[] originalQueryTokens;
    private final SurroundingCommentsInfo originalQueryCommentsInfo;
    private final SQLDialect dialect;

    public QueryReconstructor(@NotNull SQLDialect dialect, @NotNull String queryOriginalText) {
        this.dialect = dialect;

        this.commentsCollectionResult = SQLUtils.collectComments(
            queryOriginalText,
            dialect.getMultiLineComments(),
            dialect.getSingleLineComments()
        );
        Token[] originalQueryTokens;
        try {
            originalQueryTokens = SQLSemanticProcessor.parseSqlTextForTokens(dialect, commentsCollectionResult.cleanSqlText());
        } catch (Exception e) {
            originalQueryTokens = null;
        }
        this.originalQueryTokens = originalQueryTokens;
        this.originalQueryCommentsInfo = originalQueryTokens != null && this.commentsCollectionResult.comments().length > 0
            ? findSurroundingComments(originalQueryTokens, this.commentsCollectionResult.comments())
            : null;
    }

    @NotNull
    public String reconstructFromOriginalFragments(@NotNull String newSqlText) throws Exception {
        SQLUtils.CommentEntry[] comments = this.commentsCollectionResult.comments();
        if (comments.length == 0 || this.originalQueryTokens == null || this.originalQueryCommentsInfo == null) {
            // when failed to parse or find comments at all - no comments, take new text variant  as is
            return newSqlText;
        }

        String originalSqlText = this.commentsCollectionResult.originalSqlText();
        if (this.originalQueryCommentsInfo.hasOnlySurroundingComments) {
            // original text has only leading and trailing comments, but no comments inside the SQL text,
            // so just combine the new one with leading and trailing comment fragments
            String leadingComment = originalSqlText.substring(0, this.originalQueryCommentsInfo.leadingCommentEnd);
            String trailingComment = originalSqlText.substring(this.originalQueryCommentsInfo.trailingCommentStart);
            return leadingComment + newSqlText + trailingComment;
        }

        // there are comments inside the original SQL text, so compute the by-tokens diff between the original text and the new text, so
        // reconstruct new text version by cutting out original comment-containing fragments and gluing them with newly introduced fragments

        Token[] originalTokens = this.originalQueryTokens;
        Token[] newTokens = SQLSemanticProcessor.parseSqlTextForTokens(this.dialect, newSqlText);

        // compare new and old text (called image in jsqlparser) by tokens
        List<Diff.Range> diff = Diff.prepareDiff(originalTokens, newTokens, (x, y) -> x.image.equals(y.image));

        if (DEBUG) {
            printDiffDebugView(originalTokens, newTokens, diff);
        }

        StringBuilder result = new StringBuilder();
        {
            // start cutting out original text at the beginning
            int offset = 0;
            int expectedCommentIndex = 0;
            int pos = 1;
            for (Diff.Range range : diff) {
                switch (range.operation) {
                    case MATCH_AB -> { // take original fragment with comments
                        int cleanStart = pos;
                        int nextTokenAfterEnd = range.start + range.length;
                        int cleanEnd = nextTokenAfterEnd < originalTokens.length
                            ? originalTokens[nextTokenAfterEnd].absoluteBegin
                            :  originalTokens[originalTokens.length - 1].absoluteEnd;
                        int originalStart = cleanStart + offset;
                        int originalEnd = cleanEnd + offset;

                        if (expectedCommentIndex < comments.length) {
                            do { // iterate through the comments until the matching end's token position reached to obtain cutout offsets
                                SQLUtils.CommentEntry c = comments[expectedCommentIndex];
                                if (originalStart > c.start() + 1) {
                                    offset = c.accumulatedOffset();
                                    expectedCommentIndex++;
                                    originalStart = cleanStart + offset;
                                    originalEnd = cleanEnd + offset;
                                } else if (originalEnd > c.start() + 1) {
                                    offset = c.accumulatedOffset();
                                    expectedCommentIndex++;
                                    originalEnd = cleanEnd + offset;
                                } else {
                                    break;
                                }
                            } while (expectedCommentIndex < comments.length);
                        }

                        // jsqlparser's token absolute positions always have +1, so take -1 each time using them with text
                        String originalFragment = originalSqlText.substring(originalStart - 1, originalEnd - 1);
                        appendFragment(result, originalFragment);
                        pos = cleanEnd;
                    }
                    case DELETE_A -> { // skip original fragment
                        // the next cutout begins where the removed fragment ends
                        int x = range.start + range.length;
                        pos = originalTokens[x - 1].absoluteEnd;
                    }
                    case INSERT_B -> { // take generated fragment
                        if (range.length > 0) {
                            int prevToken = range.start - 1;
                            int insertionStart = prevToken >= 0 ? newTokens[prevToken].absoluteEnd : newTokens[0].absoluteBegin;
                            int nextToken = range.start + range.length;
                            int insertionEnd = nextToken < newTokens.length
                                ? newTokens[nextToken].absoluteBegin
                                : newTokens[newTokens.length - 1].absoluteEnd;

                            // jsqlparser's token absolute positions always have +1, so take -1 each time using them with text
                            String insertedFragment = newSqlText.substring(insertionStart - 1, insertionEnd - 1);
                            appendFragment(result, insertedFragment);
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected diff range operation : " + range.operation);
                }
            }

            // glue-up the original text tail, which is not covered by diff tokens
            String trailingComment = originalSqlText.substring(this.originalQueryCommentsInfo.trailingCommentStart);
            appendFragment(result, trailingComment);
            return result.toString();
        }

    }

    /**
     * Append text fragment to the StringBuilder excluding newline duplication near the joint position
     */
    private static void appendFragment(@NotNull StringBuilder result, @NotNull String fragment) {
        String preparedFragment = null;

        Matcher rm = Pattern.compile("([\\r\\n]++[\\t\\f\\v ]*+)++$").matcher(result.toString());
        if (rm.find() && rm.end() == result.length()) { // if presented text ends with newline
            Matcher fm = Pattern.compile("^([\\t\\f\\v ]*+[\\r\\n]++)++").matcher(fragment);
            if (fm.find() && fm.start() == 0) { // and appendance starts with newline
                preparedFragment = fragment.substring(fm.end()); // truncate leading newlines of the appendance
            }
        }

        if (preparedFragment == null) {
            preparedFragment = fragment;
        }

        result.append(preparedFragment);
    }

    /**
     * Generic information about the comments position
     *
     * @param leadingCommentEnd where the meaningful SQL text starts
     * @param trailingCommentStart where the meaningful SQL text ends
     * @param hasOnlySurroundingComments true when there are no comments inside the meaningful SQL text
     */
    private record SurroundingCommentsInfo(
        int leadingCommentEnd,
        int trailingCommentStart,
        boolean hasOnlySurroundingComments
    ) {
    }

    /**
     * Test if given comments are positioned only before and/or after the meaningful tokens in the original text
     *
     * @param originalTokens of the comments-free cleaned-up version of the SQL text
     * @param comments of the original SQL text
     */
    @NotNull
    private static SurroundingCommentsInfo findSurroundingComments(
        @NotNull Token[] originalTokens,
        @NotNull SQLUtils.CommentEntry[] comments
    ) {
        Token firstToken = originalTokens[0];
        int firstTokenOffset = 0;
        int leadingCommentIndex = 0;
        while (leadingCommentIndex < comments.length && firstToken.absoluteBegin > comments[leadingCommentIndex].cleanPosition()) {
            firstTokenOffset = comments[leadingCommentIndex].accumulatedOffset();
            leadingCommentIndex++;
        }
        int start = firstToken.absoluteBegin + firstTokenOffset;

        Token lastToken = originalTokens[originalTokens.length - 1];
        int trailingCommentIndex = comments.length - 1;
        while (trailingCommentIndex >= 0 && lastToken.absoluteBegin < comments[trailingCommentIndex].cleanPosition()) {
            trailingCommentIndex--;
        }
        int end = comments[trailingCommentIndex].accumulatedOffset() + lastToken.absoluteEnd;

        boolean hasOnlySurroundingComments = (trailingCommentIndex + 1) - (leadingCommentIndex - 1) <= 1;

        // jsqlparser's token absolute positions always have +1, so take -1 each time using them with text
        return new SurroundingCommentsInfo(start - 1, end - 1, hasOnlySurroundingComments);
    }

    /**
     * Collect and print debug view of token's diff, which is copy-pasteable into a table processor like MS Excel
     */
    private static void printDiffDebugView(
        @NotNull Token[] originalTokens,
        @NotNull Token[] newTokens,
        @NotNull List<Diff.Range> diff
    ) {
        String[][] arr = new String[newTokens.length + 2][originalTokens.length + 2];
        for (int i = 0; i < originalTokens.length; i++) {
            arr[0][i + 2] = Integer.toString(i);
            arr[1][i + 2] = originalTokens[i].image;
        }
        for (int i = 0; i < newTokens.length; i++) {
            arr[i + 2][0] = Integer.toString(i);
            arr[i + 2][1] = newTokens[i].image;
        }
        var x = 0;
        var y = 0;
        for (var d : diff) {
            switch (d.operation) {
                case MATCH_AB -> {
                    x = d.start;
                    for (int i = 0; i < d.length; i++, x++, y++) {
                        arr[y + 2][x + 2] = "\\";
                    }
                }
                case DELETE_A -> {
                    x = d.start;
                    for (int i = 0; i < d.length; i++, x++) {
                        arr[y + 2][x + 2] = "d";
                    }
                }
                case INSERT_B -> {
                    y = d.start;
                    for (int i = 0; i < d.length; i++, y++) {
                        arr[y + 2][x + 2] = "i";
                    }
                }
                default -> throw new IllegalStateException("Unexpected diff range operation : " + d.operation);
            }
        }
        String splitLine = "-".repeat(Math.max(
            Stream.of(arr[0]).filter(Objects::nonNull).mapToInt(String::length).sum(),
            Stream.of(arr[1]).filter(Objects::nonNull).mapToInt(String::length).sum()
        ));
        var sb = new StringBuilder();
        sb.append(splitLine);
        for (String[] line : arr) {
            for (String cell : line) {
                sb.append(CommonUtils.notNull(cell, "")).append("\t");
            }
            sb.append("\n");
        }
        sb.append(splitLine);
        System.out.println(sb);
    }
}
