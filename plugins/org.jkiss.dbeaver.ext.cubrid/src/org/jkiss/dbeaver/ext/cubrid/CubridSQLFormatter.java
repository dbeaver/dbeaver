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
package org.jkiss.dbeaver.ext.cubrid;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;

import org.jkiss.code.NotNull;

public class CubridSQLFormatter
{
    private static final Set<String> BEGIN_CLAUSES = new HashSet<String>();
    private static final Set<String> END_CLAUSES = new HashSet<String>();
    private static final Set<String> LOGICAL = new HashSet<>();
    private static final Set<String> QUANTIFIERS = new HashSet<>();
    private static final Set<String> DML = new HashSet<>();
    private static final Set<String> MISC = new HashSet<>();
    String indentString = "    ";
    String initial = "\n    ";
    boolean beginLine = true;
    boolean afterBeginBeforeEnd = false;
    boolean afterByOrSetOrFromOrSelect = false;
    boolean afterValues = false;
    boolean afterOn = false;
    boolean afterBetween = false;
    boolean afterInsert = false;
    int inFunction = 0;
    int parensSinceSelect = 0;
    private LinkedList<Integer> parenCounts = new LinkedList<Integer>();
    private LinkedList<Boolean> afterByOrFromOrSelects = new LinkedList<Boolean>();
    int indent = 1;
    StringBuffer result = new StringBuffer();
    StringTokenizer tokens;
    String lastToken;
    String token;
    String lcToken;

    public CubridSQLFormatter(@NotNull String sql) {
        this.tokens = new StringTokenizer(sql, "()+*/-=<>'`\"[], \n\r\f\t", true);
    }

    @NotNull
    public CubridSQLFormatter setInitialString(@NotNull String initial) {
        this.initial = initial;
        return this;
    }

    @NotNull
    public CubridSQLFormatter setIndentString(@NotNull String indent) {
        this.indentString = indent;
        return this;
    }

    @NotNull
    public String format() {
        this.result.append(this.initial);

        while (this.tokens.hasMoreTokens()) {
            this.token = this.tokens.nextToken();
            this.lcToken = this.token.toLowerCase();
            String t;
            if ("'".equals(this.token)) {
                do {
                    t = this.tokens.nextToken();
                    this.token = this.token + t;
                } while (!"'".equals(t) && this.tokens.hasMoreTokens());
            } else if ("\"".equals(this.token)) {
                do {
                    t = this.tokens.nextToken();
                    this.token = this.token + t;
                } while (!"\"".equals(t));
            }

            if (this.afterByOrSetOrFromOrSelect && ",".equals(this.token)) {
                this.commaAfterByOrFromOrSelect();
            } else if (this.afterOn && ",".equals(this.token)) {
                this.commaAfterOn();
            } else if ("(".equals(this.token)) {
                this.openParen();
            } else if (")".equals(this.token)) {
                this.closeParen();
            } else if (BEGIN_CLAUSES.contains(this.lcToken)) {
                this.beginNewClause();
            } else if (END_CLAUSES.contains(this.lcToken)) {
                this.endNewClause();
            } else if ("select".equals(this.lcToken)) {
                this.select();
            } else if (DML.contains(this.lcToken)) {
                this.updateOrInsertOrDelete();
            } else if ("values".equals(this.lcToken)) {
                this.values();
            } else if ("on".equals(this.lcToken)) {
                this.on();
            } else if (this.afterBetween && this.lcToken.equals("and")) {
                this.misc();
                this.afterBetween = false;
            } else if (LOGICAL.contains(this.lcToken)) {
                this.logical();
            } else if (isWhitespace(this.token)) {
                this.white();
            } else {
                this.misc();
            }

            if (!isWhitespace(this.token)) {
                this.lastToken = this.lcToken;
            }
        }

        return this.result.toString();
    }

    @NotNull
    public static String join(@NotNull String separator, @NotNull String[] strings) {
        int length = strings.length;
        if (length == 0) {
            return "";
        } else {
            StringBuffer buf = (new StringBuffer(length * strings[0].length())).append(strings[0]);

            for (int i = 1; i < length; ++i) {
                buf.append(separator).append(strings[i]);
            }

            return buf.toString();
        }
    }

    private void commaAfterOn() {
        this.out();
        --this.indent;
        this.newline();
        this.afterOn = false;
        this.afterByOrSetOrFromOrSelect = true;
    }

    private void commaAfterByOrFromOrSelect() {
        this.out();
        this.newline();
    }

    private void logical() {
        if ("end".equals(this.lcToken)) {
            --this.indent;
        }

        this.newline();
        this.out();
        this.beginLine = false;
    }

    private void on() {
        ++this.indent;
        this.afterOn = true;
        this.newline();
        this.out();
        this.beginLine = false;
    }

    private void misc() {
        this.out();
        if ("between".equals(this.lcToken)) {
            this.afterBetween = true;
        }

        if (this.afterInsert) {
            this.newline();
            this.afterInsert = false;
        } else {
            this.beginLine = false;
            if ("case".equals(this.lcToken)) {
                ++this.indent;
            }
        }
    }

    private void white() {
        if (!this.beginLine) {
            this.result.append(" ");
        }
    }

    private void updateOrInsertOrDelete() {
        this.out();
        ++this.indent;
        this.beginLine = false;
        if ("update".equals(this.lcToken)) {
            this.newline();
        }

        if ("insert".equals(this.lcToken)) {
            this.afterInsert = true;
        }
    }

    private void select() {
        this.out();
        ++this.indent;
        this.newline();
        this.parenCounts.addLast(Integer.valueOf(this.parensSinceSelect));
        this.afterByOrFromOrSelects.addLast(Boolean.valueOf(this.afterByOrSetOrFromOrSelect));
        this.parensSinceSelect = 0;
        this.afterByOrSetOrFromOrSelect = true;
    }

    private void out() {
        this.result.append(this.token);
    }

    private void endNewClause() {
        if (!this.afterBeginBeforeEnd) {
            --this.indent;
            if (this.afterOn) {
                --this.indent;
                this.afterOn = false;
            }

            this.newline();
        }

        this.out();
        if (!"union".equals(this.lcToken)) {
            ++this.indent;
        }

        this.newline();
        this.afterBeginBeforeEnd = false;
        this.afterByOrSetOrFromOrSelect = "by".equals(this.lcToken) || "set".equals(this.lcToken) || "from".equals(this.lcToken);
    }

    private void beginNewClause() {
        if (!this.afterBeginBeforeEnd) {
            if (this.afterOn) {
                --this.indent;
                this.afterOn = false;
            }

            --this.indent;
            this.newline();
        }

        this.out();
        this.beginLine = false;
        this.afterBeginBeforeEnd = true;
    }

    private void values() {
        --this.indent;
        this.newline();
        this.out();
        ++this.indent;
        this.newline();
        this.afterValues = true;
    }

    private void closeParen() {
        --this.parensSinceSelect;
        if (this.parensSinceSelect < 0) {
            --this.indent;
            this.parensSinceSelect = this.parenCounts.removeLast();
            this.afterByOrSetOrFromOrSelect = this.afterByOrFromOrSelects.removeLast();
        }

        if (this.inFunction > 0) {
            --this.inFunction;
            this.out();
        } else {
            if (!this.afterByOrSetOrFromOrSelect) {
                --this.indent;
                this.newline();
            }

            this.out();
        }

        this.beginLine = false;
    }

    private void openParen() {
        if (isFunctionName(this.lastToken) || this.inFunction > 0) {
            ++this.inFunction;
        }

        this.beginLine = false;
        if (this.inFunction > 0) {
            this.out();
        } else {
            this.out();
            if (!this.afterByOrSetOrFromOrSelect) {
                ++this.indent;
                this.newline();
                this.beginLine = true;
            }
        }

        ++this.parensSinceSelect;
    }

    @NotNull
    private static boolean isFunctionName(@NotNull String tok) {
        char begin = tok.charAt(0);
        boolean isIdentifier = Character.isJavaIdentifierStart(begin) || '"' == begin;
        return isIdentifier
                && !LOGICAL.contains(tok)
                && !END_CLAUSES.contains(tok)
                && !QUANTIFIERS.contains(tok)
                && !DML.contains(tok)
                && !MISC.contains(tok);
    }

    @NotNull
    private static boolean isWhitespace(@NotNull String token) {
        return " \n\r\f\t".indexOf(token) >= 0;
    }

    private void newline() {
        this.result.append("\n");

        for (int i = 0; i < this.indent; ++i) {
            this.result.append(this.indentString);
        }

        this.beginLine = true;
    }

    static {
        BEGIN_CLAUSES.add("left");
        BEGIN_CLAUSES.add("right");
        BEGIN_CLAUSES.add("inner");
        BEGIN_CLAUSES.add("outer");
        BEGIN_CLAUSES.add("group");
        BEGIN_CLAUSES.add("order");
        END_CLAUSES.add("where");
        END_CLAUSES.add("set");
        END_CLAUSES.add("having");
        END_CLAUSES.add("join");
        END_CLAUSES.add("from");
        END_CLAUSES.add("by");
        END_CLAUSES.add("join");
        END_CLAUSES.add("into");
        END_CLAUSES.add("union");
        LOGICAL.add("and");
        LOGICAL.add("view");
        LOGICAL.add("when");
        LOGICAL.add("else");
        LOGICAL.add("end");
        QUANTIFIERS.add("in");
        QUANTIFIERS.add("all");
        QUANTIFIERS.add("exists");
        QUANTIFIERS.add("some");
        QUANTIFIERS.add("any");
        DML.add("insert");
        DML.add("update");
        DML.add("delete");
        MISC.add("select");
        MISC.add("on");
    }
}