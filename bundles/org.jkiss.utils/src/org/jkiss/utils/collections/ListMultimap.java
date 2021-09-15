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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A multimap that remembers the order in which values were mapped to a given key.
 *
 * @param <K> keys
 * @param <V> values
 */
public class ListMultimap<K, V> implements Multimap<K, V> {
    private final Map<K, List<V>> map = new HashMap<>();

    @Override
    public void clear() {
        map.clear();
    }

    @Nullable
    @Override
    public List<V> get(@NotNull K key) {
        return map.get(key);
    }

    @Override
    public void put(@NotNull K key, @NotNull V value) {
        List<V> values = map.computeIfAbsent(key, k -> new ArrayList<>());
        values.add(value);
    }
}
