/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

interface TokenPredicateNodeVisitor<T, TRet> {

    TRet visitSequence(@NotNull SequenceTokenPredicateNode sequence, T arg);

    TRet visitAlternative(@NotNull AlternativeTokenPredicateNode alternative, T arg);

    TRet visitOptional(@NotNull OptionalTokenPredicateNode optional, T arg);

    TRet visitTokenEntry(@NotNull SQLTokenEntry token, T arg);

}

/**
 * Represents node of token predicate tree
 */
public abstract class TokenPredicateNode {

    protected TokenPredicateNode() {
    }

    public final <T, TRet> TRet apply(@NotNull TokenPredicateNodeVisitor<T, TRet> visitor, T arg) {
        return this.applyImpl(visitor, arg);
    }

    protected abstract <T, TRet> TRet applyImpl(@NotNull TokenPredicateNodeVisitor<T, TRet> visitor, T arg);

    /**
     * Expands the tree into the list of all possible token entry sequences matching the predicate
     * @return
     */
    @NotNull
    public List<List<TokenEntry>> expand() {
        return TokenPredicateExpander.expand(this);
    }

    @Override
    public final String toString() {
        return TokenPredicateFormatter.format(this);
    }
}

/**
 * Represents any node of token predicate tree carrying only one child node
 */
abstract class UnaryTokenPredicateNode extends TokenPredicateNode {
    public final TokenPredicateNode child;

    protected UnaryTokenPredicateNode(@NotNull TokenPredicateNode child) {
        this.child = child;
    }
}

/**
 * Represents any node of token predicate tree carrying a number of children nodes
 */
abstract class GroupTokenPredicatesNode extends TokenPredicateNode {
    public final List<TokenPredicateNode> childs;

    protected GroupTokenPredicatesNode(TokenPredicateNode ... childs) {
        this.childs = Collections.unmodifiableList(List.of(childs));
    }
}

/**
 * Represents node of token predicate describing a sequence of some tokens
 */
class SequenceTokenPredicateNode extends GroupTokenPredicatesNode {
    public SequenceTokenPredicateNode(TokenPredicateNode ... childs) {
        super(childs);
    }

    @Override
    protected <T, TRet> TRet applyImpl(TokenPredicateNodeVisitor<T, TRet> visitor, T arg) {
        return visitor.visitSequence(this, arg);
    }
}

/**
 * Represents node of token predicate describing one possible of alternatives of token subsequences
 */
class AlternativeTokenPredicateNode extends GroupTokenPredicatesNode {
    public AlternativeTokenPredicateNode(TokenPredicateNode ... childs) {
        super(childs);
    }

    @Override
    protected <T, TRet> TRet applyImpl(TokenPredicateNodeVisitor<T, TRet> visitor, T arg) {
        return visitor.visitAlternative(this, arg);
    }
}

/**
 * Represents node of token predicate describing optional token subsequence
 */
class OptionalTokenPredicateNode extends UnaryTokenPredicateNode {
    public OptionalTokenPredicateNode(@NotNull TokenPredicateNode child) {
        super(child);
    }

    @Override
    protected <T, TRet> TRet applyImpl(TokenPredicateNodeVisitor<T, TRet> visitor, T arg) {
        return visitor.visitOptional(this, arg);
    }
}
