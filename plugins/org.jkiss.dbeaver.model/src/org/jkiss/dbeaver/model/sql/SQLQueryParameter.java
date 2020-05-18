/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;

import java.util.regex.Pattern;

/**
 * SQL statement parameter info
 */
public class SQLQueryParameter {


    private static final Pattern VARIABLE_PATTERN_SIMPLE = Pattern.compile("\\$\\{[a-z0-9_]+\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern VARIABLE_PATTERN_FULL = Pattern.compile("\\$P?!?\\{[a-z0-9_]+\\}", Pattern.CASE_INSENSITIVE);


    private final SQLSyntaxManager syntaxManager;
    private int ordinalPosition;
    private String name;
    private String value;
    private boolean variableSet;
    private final int tokenOffset;
    private final int tokenLength;
    private SQLQueryParameter previous;

    public SQLQueryParameter(SQLSyntaxManager syntaxManager, int ordinalPosition, String name)
    {
        this(syntaxManager, ordinalPosition, name, 0, 0);
    }

    public SQLQueryParameter(SQLSyntaxManager syntaxManager, int ordinalPosition, String name, int tokenOffset, int tokenLength)
    {
        this.syntaxManager = syntaxManager;
        if (tokenOffset < 0) {
            throw new IndexOutOfBoundsException("Bad parameter offset: " + tokenOffset);
        }
        if (tokenLength < 0) {
            throw new IndexOutOfBoundsException("Bad parameter length: " + tokenLength);
        }
        this.ordinalPosition = ordinalPosition;
        this.name = name.trim();
        this.tokenOffset = tokenOffset;
        this.tokenLength = tokenLength;
    }

    public boolean isNamed() {
        return !String.valueOf(syntaxManager.getAnonymousParameterMark()).equals(name);
    }

    public int getTokenOffset() {
        return tokenOffset;
    }

    public int getTokenLength() {
        return tokenLength;
    }

    public SQLQueryParameter getPrevious() {
        return previous;
    }

    public void setPrevious(SQLQueryParameter previous) {
        this.previous = previous;
    }

    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public boolean isVariableSet() {
        return variableSet;
    }

    public void setVariableSet(boolean variableSet) {
        this.variableSet = variableSet;
    }

    public String getVarName() {
        String varName = stripVariablePattern(name);
        if (!varName.equals(name)) {
            return varName;
        }
        for (String prefix : syntaxManager.getNamedParameterPrefixes()) {
            if (name.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }

    @Override
    public String toString() {
        return getVarName() + "=" + value;
    }

    public static Pattern getVariablePattern() {
        if (supportsJasperSyntax()) {
            return VARIABLE_PATTERN_FULL;
        } else {
            return VARIABLE_PATTERN_SIMPLE;
        }
    }

    public static boolean supportsJasperSyntax() {
        return true;
    }

    @NotNull
    public static String stripVariablePattern(String pattern) {
        if (supportsJasperSyntax()) {
            if (pattern.startsWith("$P{") && pattern.endsWith("}")) {
                return pattern.substring(3, pattern.length() - 1);
            } else if (pattern.startsWith("$P!{") && pattern.endsWith("}")) {
                return pattern.substring(4, pattern.length() - 1);
            }
        }
        if (pattern.startsWith("${") && pattern.endsWith("}")) {
            return pattern.substring(2, pattern.length() - 1);
        }

        return pattern;
    }

}
