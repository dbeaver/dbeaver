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

import java.util.Comparator;

/**
 * Describes a way to compare trie keys and terms during lookup operations.
 * Establishes partial ordering on partially comparable items.
 * Complete ordering could be established with another strong comparer on strongly comparable items.
 * @param <T> type of the items to compare
 */
public interface TrieLookupComparator<T> extends Comparator<T> {
    /**
     * @param term used as a path item key during trie lookup operations
     * @return true if the term contains enough information to be sure that it can be ordered relatively to the other terms containing not less amount of information
     */
    boolean isPartiallyComparable(@NotNull T term);
    /**
     * @param term used as a path item key during trie lookup operations
     * @return true if the term contains enough information to be sure that it can be ordered relatively to the any other terms
     */
    boolean isStronglyComparable(@NotNull T term);

    /**
     * @param other
     * @return true if entries could  describe the same concrete token
     */

    /**
     * Checks if given term corresponds to the key according to the information being carried by them.
     * @param key
     * @param term
     * @return
     */
    boolean match(@NotNull T key, @NotNull T term);
}

