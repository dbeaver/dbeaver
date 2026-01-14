/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

/**
 * A simple predicate tokens tree visitor
 *
 * @param <T> - argument type
 * @param <R> - return type
 */
public interface TokenPredicateNodeVisitor<T, R> {

    /**
     * Invoked for a node of token predicate describing a sequence of some tokens
     */
    R visitSequence(@NotNull SequenceTokenPredicateNode sequence, T arg);

    /**
     * Invoked for a node of token predicate describing one possible of alternatives of token subsequences
     */
    R visitAlternative(@NotNull AlternativeTokenPredicateNode alternative, T arg);

    /**
     * Invoked for a node of token predicate describing optional token subsequence
     */
    R visitOptional(@NotNull OptionalTokenPredicateNode optional, T arg);

    /**
     * Invoked for a node of token predicate capable of capturing matching text parts
     */
    R visitCapture(@NotNull CaptureTokenPredicateNode captureToken, T arg);

    /**
     * Invoked for a node representing SQL token in the text
     */
    R visitTokenEntry(@NotNull SQLTokenEntry token, T arg);

}
