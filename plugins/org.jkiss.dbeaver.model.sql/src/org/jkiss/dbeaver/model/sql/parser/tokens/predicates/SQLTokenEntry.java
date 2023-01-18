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

    public SQLTokenEntry(@Nullable String string, @Nullable SQLTokenType type) {
        this.string = string;
        this.type = type;
    }

    @Override
    @Nullable
    public String getString() {
        return this.string;
    }

    @Override
    @Nullable
    public Enum getTokenType() {
        return this.type;
    }

    @Override
    public boolean matches(@NotNull TokenEntry other) {
        boolean stringMatches = this.getString() == null || other.getString() == null || this.string.equalsIgnoreCase(other.getString());
        boolean typeMatches = this.getTokenType() == null || other.getTokenType() == null || this.type.equals(other.getTokenType());
        return stringMatches && typeMatches;
    }

    public boolean equals(@NotNull TokenEntry other) {
        boolean stringEquals = (this.getString() == null && other.getString() == null) || (this.getString() != null && other.getString() != null && this.getString().equals(other.getString()));
        boolean typeEquals = (this.getTokenType() == null && other.getTokenType() == null) || (this.getTokenType() != null && other.getTokenType() != null && this.getTokenType().equals(other.getTokenType()));
        return stringEquals && typeEquals;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof TokenEntry && this.equals((TokenEntry)o));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getString(), this.getTokenType());
    }

    @NotNull
    public StringBuilder format(@NotNull StringBuilder sb) {
        return sb.append("<").append(type != null ? type.name() : "?").append(">")
                .append(this.string != null ? "'" + this.string + "'" : "any");
    }

    @Override
    @NotNull
    protected <T, R> R applyImpl(@NotNull TokenPredicateNodeVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTokenEntry(this, arg);
    }
}
