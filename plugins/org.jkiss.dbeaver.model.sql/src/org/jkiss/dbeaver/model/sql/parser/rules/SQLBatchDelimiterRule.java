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
package org.jkiss.dbeaver.model.sql.parser.rules;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLBatchDelimiterToken;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

import java.util.List;

/**
 * Matches line-isolated delimiters that separate script batches.
 */
public class SQLBatchDelimiterRule extends SQLDelimiterRule {
    private List<TPRule> lineCommentRules = List.of();

    public SQLBatchDelimiterRule(@NotNull String... delimiters) {
        super(delimiters, new SQLBatchDelimiterToken());
    }

    public final void setLineCommentRules(@NotNull List<TPRule> lineCommentRules) {
        this.lineCommentRules = List.copyOf(lineCommentRules);
    }

    public final boolean matchesAny(@NotNull String[] delimiters) {
        for (char[] ownDelimiter : getDelimiters()) {
            for (String delimiter : delimiters) {
                if (matches(delimiter, ownDelimiter)) {
                    return true;
                }
            }
        }
        return false;
    }

    public final boolean matches(@NotNull String delimiter) {
        for (char[] ownDelimiter : getDelimiters()) {
            if (matches(delimiter, ownDelimiter)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(@NotNull String delimiter, @NotNull char[] ownDelimiter) {
        return delimiter.equalsIgnoreCase(String.valueOf(ownDelimiter));
    }

    @Override
    @NotNull
    public TPToken evaluate(@NotNull TPCharacterScanner scanner) {
        if (!hasOnlyWhitespaceBefore(scanner)) {
            return TPTokenAbstract.UNDEFINED;
        }

        int startOffset = scanner.getOffset();
        TPToken token = super.evaluate(scanner);
        if (token.isUndefined()) {
            return token;
        }

        consumeSuffix(scanner);
        int delimiterEndOffset = scanner.getOffset();
        boolean valid = hasValidLineEnding(scanner);
        restoreOffset(scanner, valid ? delimiterEndOffset : startOffset);
        return valid ? token : TPTokenAbstract.UNDEFINED;
    }

    /**
     * Consumes a dialect-specific suffix, leaving the scanner immediately after it.
     */
    protected void consumeSuffix(@NotNull TPCharacterScanner scanner) {
    }

    protected static boolean isInlineWhitespace(int ch) {
        return ch != '\r' && ch != '\n' && ch != TPCharacterScanner.EOF && Character.isWhitespace(ch);
    }

    protected static void restoreOffset(@NotNull TPCharacterScanner scanner, int offset) {
        while (scanner.getOffset() > offset) {
            scanner.unread();
        }
        while (scanner.getOffset() < offset) {
            scanner.read();
        }
    }

    private static boolean hasOnlyWhitespaceBefore(@NotNull TPCharacterScanner scanner) {
        int column;
        try {
            column = scanner.getColumn();
        } catch (UnsupportedOperationException e) {
            column = 0;
        }
        for (int i = 0; i < column; i++) {
            scanner.unread();
        }
        boolean whitespace = true;
        for (int i = 0; i < column; i++) {
            whitespace &= Character.isWhitespace(scanner.read());
        }
        return whitespace;
    }

    private boolean hasValidLineEnding(@NotNull TPCharacterScanner scanner) {
        int ch;
        do {
            ch = scanner.read();
        } while (isInlineWhitespace(ch));

        if (ch == '\r' || ch == '\n' || ch == TPCharacterScanner.EOF && (
            !(scanner instanceof TPRuleBasedScanner ruleBasedScanner) || ruleBasedScanner.isRangeEndAtLineEnd()
        )) {
            return true;
        }
        scanner.unread();
        int commentOffset = scanner.getOffset();
        for (TPRule commentRule : lineCommentRules) {
            if (!commentRule.evaluate(scanner).isUndefined()) {
                return true;
            }
            restoreOffset(scanner, commentOffset);
        }
        return false;
    }
}
