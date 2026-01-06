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
package org.jkiss.dbeaver.model.sql.semantics.context;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class UnmodifiableMap<K, V> {

    private static final UnmodifiableMap<?, ?> EMPTY = new UnmodifiableMap<>(Collections.emptyMap());

    @SuppressWarnings("unchecked")
    public static <K, V> UnmodifiableMap<K, V> emptyMap() {
        return (UnmodifiableMap<K, V>) EMPTY;
    }

    private final Map<K, V> map;

    private UnmodifiableMap(Map<K, V> map) {
        this.map = map;
    }

    public UnmodifiableMap<K, V> combine(UnmodifiableMap<K, V> other) {
        if (this.map.isEmpty()) {
            return other;
        } else if (other.map.isEmpty()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            HashMap<K, V> result = new HashMap<>(this.map.size() + other.map.size());
            result.putAll(this.map);
            result.putAll(other.map);
            return new UnmodifiableMap<>(result);
        }
    }

    public V get(K key) {
        return this.map.get(key);
    }

    public UnmodifiableMap<K, V> put(Collection<Map.Entry<K, V>> entries) {
        if (entries.isEmpty()) {
            return this;
        } else {
            HashMap<K, V> result = new HashMap<>(this.map);
            for (Map.Entry<? extends K, ? extends V> entry : entries) {
                result.put(entry.getKey(), entry.getValue());
            }
            return new UnmodifiableMap<>(result);
        }
    }

    public UnmodifiableMap<K, V> put(K key, V value) {
        HashMap<K, V> result = new HashMap<>(this.map);
        result.put(key, value);
        return new UnmodifiableMap<>(result);
    }

    public UnmodifiableMap<K, V> remove(Collection<Map.Entry<K, V>> entries) {
        if (entries.isEmpty()) {
            return this;
        } else {
            HashMap<K, V> result = new HashMap<>(this.map);
            for (Map.Entry<? extends K, ? extends V> entry : entries) {
                result.remove(entry.getKey(), entry.getValue());
            }
            return new UnmodifiableMap<>(result);
        }
    }

    public Collection<V> values() {
        return this.map.values();
    }

    public Collection<Map.Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }
}
