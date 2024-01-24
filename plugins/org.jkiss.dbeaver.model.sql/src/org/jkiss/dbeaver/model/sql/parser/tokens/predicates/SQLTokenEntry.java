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
package org.jkiss.dbeaver.model.sql.parser.tokens.predicates;


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.parser.TokenEntry;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;

import java.util.Objects;

/**
 * Implements representation of information about the SQL token in the text.
 * Any member can be null if represents partial information in case of placeholder in the predicate.
 */
public class SQLTokenEntry extends TokenPredicateNode implements TokenEntry {
    private final String string;
    private final SQLTokenType type;
    private final boolean isInverted;

    public SQLTokenEntry(@Nullable String string, @Nullable SQLTokenType type, boolean isInverted) {
        this.string = string;
        this.type = type;
        this.isInverted = isInverted;
    }

    @Override
    @Nullable
    public String getString() {
        return this.string;
    }

    @Override
    @Nullable
    public SQLTokenType getTokenType() {
        return this.type;
    }
    
    public boolean isInverted() {
        return this.isInverted;
    }

    @Override
    public boolean matches(@NotNull TokenEntry other) {
        boolean stringMatches = this.getString() == null || other.getString() == null || this.string.equalsIgnoreCase(other.getString());
        boolean typeMatches = this.getTokenType() == null || other.getTokenType() == null || this.type.equals(other.getTokenType());
        boolean result = stringMatches && typeMatches;
        if (this.isInverted) {
            result = !result;
        }
        return result; 
    }

    public boolean equals(@NotNull TokenEntry other) {
        boolean stringEquals = (this.getString() == null && other.getString() == null) || (this.getString() != null && other.getString() != null && this.getString().equals(other.getString()));
        boolean typeEquals = (this.getTokenType() == null && other.getTokenType() == null) || (this.getTokenType() != null && other.getTokenType() != null && this.getTokenType().equals(other.getTokenType()));
        boolean invertedEquals = this.isInverted() == other.isInverted();
        return stringEquals && typeEquals && invertedEquals;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof TokenEntry && this.equals((TokenEntry)o));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getString(), this.getTokenType(), this.isInverted());
    }

    @NotNull
    public StringBuilder format(@NotNull StringBuilder sb) {
        if (this.isInverted) {
            sb.append("!");
        }
        return sb.append("<").append(type != null ? type.name() : "?").append(">")
                .append(this.string != null ? "'" + this.string + "'" : "any");
    }

    @Override
    @NotNull
    protected <T, R> R applyImpl(@NotNull TokenPredicateNodeVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTokenEntry(this, arg);
    }
}
