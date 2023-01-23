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
import org.jkiss.dbeaver.model.sql.parser.SQLParserActionKind;
import org.jkiss.dbeaver.model.sql.parser.SQLTokenPredicate;
import org.jkiss.dbeaver.model.sql.parser.TokenEntry;

import java.util.Collections;
import java.util.List;

/**
 * A condition about sequence of terms expressed in form of predicate pair about term sequence prefix and suffix
 */
public class TokenPredicatesCondition implements SQLTokenPredicate {
    /**
     * Action to perform during parse on condition match
     */
    private final SQLParserActionKind actionKind;
    /**
     * Predicate trees representing conditions on possible structure of sequence of terms
     */
    private final TokenPredicateNode prefixPredicate;
    private final TokenPredicateNode suffixPredicate;
    /**
     * Complete list of all possible prefixes and suffixes matching the condition an any combination
     */
    private final List<List<TokenEntry>> prefixes;
    private final List<List<TokenEntry>> suffixes;
    /**
     * Maximum lengths of corresponding prefixes and suffixes under condition
     */
    public final int maxPrefixLength;
    public final int maxSuffixLength;

    public TokenPredicatesCondition(@NotNull SQLParserActionKind actionKind, @NotNull TokenPredicateNode prefixPredicate, @NotNull TokenPredicateNode suffixPredicate) {
        this.actionKind = actionKind;
        this.prefixPredicate = prefixPredicate;
        this.suffixPredicate = suffixPredicate;
        this.prefixes = Collections.unmodifiableList(prefixPredicate.expand());
        this.suffixes = Collections.unmodifiableList(suffixPredicate.expand());
        this.maxPrefixLength = this.prefixes.stream().mapToInt(c -> c.size()).max().orElse(0);
        this.maxSuffixLength = this.suffixes.stream().mapToInt(c -> c.size()).max().orElse(0);
    }

    @Override
    public int getMaxSuffixLength() {
        return maxSuffixLength;
    }

    @NotNull
    public List<List<TokenEntry>> getPrefixes() {
        return prefixes;
    }

    @NotNull
    public List<List<TokenEntry>> getSuffixes() {
        return suffixes;
    }

    @Override
    @NotNull
    public SQLParserActionKind getActionKind() {
        return this.actionKind;
    }

    @Override
    public String toString() {
        return "TokenEnvironmentCondition[" +
                "action: [" + this.actionKind + "], " +
                "prefix: [" + this.prefixPredicate + "], " +
                "suffix: [" + this.suffixPredicate + "]" +
                "]";
    }
}

