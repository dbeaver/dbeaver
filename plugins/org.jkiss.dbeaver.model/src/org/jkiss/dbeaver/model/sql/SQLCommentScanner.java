package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.Pair;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterates over the SQL text and yields pure comment bodies
 * (without comment delimiters or line terminators).
 */
final class SQLCommentScanner implements Iterator<String> {
    private final String sql;
    private final String mlStart;
    private final String mlEnd;
    private final String[] slTokens;

    private int pos = 0;
    private String nextComment;

    SQLCommentScanner(
        @Nullable Pair<String, String> mlComments,
        @Nullable String[] slTokens,
        @NotNull String sql
    ) {
        this.mlStart = mlComments != null ? mlComments.getFirst() : null;
        this.mlEnd = mlComments != null ? mlComments.getSecond() : null;
        this.slTokens = slTokens != null ? slTokens : new String[0];
        this.sql = sql;
        advance();
    }

    @Override
    public boolean hasNext() {
        return nextComment != null;
    }

    @Override
    public String next() {
        if (nextComment == null) {
            throw new NoSuchElementException();
        }
        String result = nextComment;
        advance();
        return result;
    }

    private void advance() {
        nextComment = null;
        int len = sql.length();

        while (pos < len) {
            /* -------- Multi-line comment ---------- */
            if (mlStart != null && sql.startsWith(mlStart, pos)) {
                pos += mlStart.length();
                int end = mlEnd == null ? -1 : sql.indexOf(mlEnd, pos);
                if (end < 0) {
                    nextComment = sql.substring(pos).trim();
                    pos = len;
                } else {
                    nextComment = sql.substring(pos, end).trim();
                    pos = end + mlEnd.length();
                }
                return;
            }

            /* -------- Single-line comment ---------- */
            String slToken = findSingleLineToken(sql, pos, slTokens);
            if (slToken != null) {
                pos += slToken.length();
                int eol = findEol(sql, pos);
                nextComment = sql.substring(pos, eol).trim();
                pos = (eol < len) ? eol + 1 : len; // skip '\n' if present
                return;
            }

            /* -------- Ordinary character ----------- */
            pos++;
        }
    }

    private static String findSingleLineToken(String sql, int offset, String[] tokens) {
        for (String token : tokens) {
            if (!token.isEmpty() && sql.startsWith(token, offset)) {
                return token;
            }
        }
        return null;
    }

    private static int findEol(String sql, int offset) {
        int nl = sql.indexOf('\n', offset);
        return nl == -1 ? sql.length() : nl;
    }
}
