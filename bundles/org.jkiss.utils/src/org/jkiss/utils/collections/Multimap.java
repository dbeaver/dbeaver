/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.utils.collections;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collection;

/**
 * A map that allows mapping multiple values to a given key.
 *
 * @param <K> keys
 * @param <V> values
 */
public interface Multimap<K, V> {
    void clear();

    /**
     * Returns a collection with values mapped to a given key (if any).
     * The returned collection allows 'read-through' behavior, meaning the changes made to the collection are applied to the multimap and vice versa.
     *
     * Design note: the decision to return null is dictated by the fact that if we had decided to return an empty collection instead,
     * it would have been hard to prevent possible memory leaks.
     *
     * @param key key
     * @return a collection with values, of {@code null} if no mapping exists
     */
    @Nullable
    Collection<V> get(@NotNull K key);

    void put(@NotNull K key, @NotNull V value);
}
