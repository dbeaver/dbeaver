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
import org.jkiss.dbeaver.model.sql.parser.TokenEntry;

import java.util.Collections;
import java.util.List;

/**
 * Represents node of token predicate tree
 */
public abstract class TokenPredicateNode {

    protected TokenPredicateNode() {

    }

    @NotNull
    public final <T, R> R apply(@NotNull TokenPredicateNodeVisitor<T, R> visitor, @NotNull T arg) {
        return this.applyImpl(visitor, arg);
    }

    @NotNull
    protected abstract <T, R> R applyImpl(@NotNull TokenPredicateNodeVisitor<T, R> visitor, @NotNull T arg);

    /**
     * Expands the tree into the list of all possible token entry sequences matching the predicate
     * @return
     */
    @NotNull
    public List<List<TokenEntry>> expand() {
        List<List<TokenEntry>> result = TokenPredicateExpander.expand(this);
        return result.isEmpty() ? List.of(Collections.emptyList()) : result;
    }

    @Override
    public final String toString() {
        return TokenPredicateFormatter.format(this);
    }
}

