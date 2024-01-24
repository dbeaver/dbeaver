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
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;

import java.util.Arrays;
import java.util.stream.Collectors;


/**
 * The producer of all predicate nodes responsible for exact string token entries classification according to dialect.
 * <p>
 * Producing methods accept different possible arguments in any combinations:
 * <ul>
 *     <li>{@literal null} - entry corresponding to any token
 *     <li>{@link String} - entry corresponding to the exact token
 *     <li>{@link SQLTokenType} - entry corresponding to the any token of given token type
 *     <li>{@link TokenPredicateNode} - entry corresponding to the given subsequence of tokens
 * </ul>
 * </p>
 */
public abstract class TokenPredicateFactory {

    /**
     * Create dialect-agnostinc {@link TokenPredicateFactory}
     */
    @NotNull
    public static TokenPredicateFactory makeDefaultFactory() {
        return new DefaultTokenPredicateFactory();
    }

    /**
     * Create dialect-specific {@link TokenPredicateFactory}
     */
    @NotNull
    public static TokenPredicateFactory makeDialectSpecificFactory(@NotNull SQLRuleManager ruleManager) {
        return new SQLTokenPredicateFactory(ruleManager);
    }

    protected TokenPredicateFactory() {

    }

    /**
     * Materialize token predicate node describing given token string with a dialect-specific token type classification
     * @param tokenString to classify
     * @return predicate node carrying information about the token entry
     */
    @NotNull
    protected abstract SQLTokenEntry classifyToken(@NotNull String tokenString);

    /**
     * Materialize token predicate node carrying information about token entry described in a certain way.
     * @param obj some information about the token entry (see {@link TokenPredicateFactory} for the details)
     * @return predicate node carrying information about the token entry
     */
    @NotNull
    private TokenPredicateNode makeNode(@Nullable Object obj) {
        if (obj == null) {
            return new SQLTokenEntry(null, null, false);
        } else if (obj instanceof TokenPredicateNode) {
            return (TokenPredicateNode)obj;
        } else if (obj instanceof String) {
            return this.classifyToken((String)obj);
        } else if (obj instanceof SQLTokenType) {
            return new SQLTokenEntry(null, (SQLTokenType) obj, false);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @NotNull
    private TokenPredicateNode[] makeGroup(@NotNull Object ... objs) {
        return Arrays.stream(objs).map(o -> makeNode(o)).collect(Collectors.toList()).toArray(new TokenPredicateNode[0]);
    }

    @NotNull
    public TokenPredicateNode token(@NotNull Object obj) {
        return this.makeNode(obj);
    }

    @NotNull
    public TokenPredicateNode sequence(@NotNull TokenPredicateNode ... nodes) {
        return new SequenceTokenPredicateNode(nodes);
    }

    @NotNull
    public TokenPredicateNode sequence(@NotNull Object ... objs) {
        return new SequenceTokenPredicateNode(this.makeGroup(objs));
    }

    @NotNull
    public TokenPredicateNode alternative(@NotNull TokenPredicateNode ... nodes) {
        return new AlternativeTokenPredicateNode(nodes);
    }

    @NotNull
    public TokenPredicateNode alternative(@NotNull Object ... objs) {
        return new AlternativeTokenPredicateNode(this.makeGroup(objs));
    }

    @NotNull
    public TokenPredicateNode optional(@NotNull TokenPredicateNode node) {
        return new OptionalTokenPredicateNode(node);
    }

    @NotNull
    public TokenPredicateNode optional(@NotNull Object ... obj) {
        return new OptionalTokenPredicateNode(obj.length == 1 ? this.makeNode(obj[0]) : this.sequence(obj));
    }
    
    @NotNull
    public TokenPredicateNode not(@NotNull String str) {
        SQLTokenEntry entry = this.classifyToken(str);
        return new SQLTokenEntry(entry.getString(), entry.getTokenType(), true);
    }

    @NotNull
    public TokenPredicateNode not(@NotNull SQLTokenType token) {
        return new SQLTokenEntry(null, token, true);
    }
}