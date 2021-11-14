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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class CollectionUtils {
    private CollectionUtils() {
        throw new AssertionError("No instances of a utility class for you!");
    }

    // A substitution for static factory method Set.of() from Java 11. Can be safely replaced in favor of the aforementioned
    // method when the project is transitioned to Java 11+.
    @NotNull
    @SafeVarargs
    public static <T> Set<T> unmodifiableSet(@NotNull T... vararg) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(vararg)));
    }

    /**
     * Groups values into a map of their shared key and a list of matching values using that key.
     * <p>
     * <h3>Group strings by their first character</h3>
     * <pre>{@code
     * final List<String> values = Arrays.asList("aaa", "abb", "bbb", "bab", "ccc");
     * final Map<Character, List<String>> groups = group(values, x -> x.charAt(0));
     *
     * Assert.assertEquals(Arrays.asList("aaa", "abb"), groups.get('a'));
     * Assert.assertEquals(Arrays.asList("bbb", "bab"), groups.get('b'));
     * Assert.assertEquals(Arrays.asList("ccc"), groups.get('c'));
     * }</pre>
     * @param values values to group
     * @param keyExtractor a function that extracts key from value that is used to group values
     * @return map of a shared key and a list of matching values
     */
    @NotNull
    public static <K, V> Map<K, List<V>> group(@NotNull Iterable<? extends V> values, @NotNull Function<? super V, ? extends K> keyExtractor) {
        final Map<K, List<V>> grouped = new HashMap<>();
        for (V value : values) {
            final K key = keyExtractor.apply(value);
            final List<V> group = grouped.computeIfAbsent(key, k -> new ArrayList<>());
            group.add(value);
        }
        return grouped;
    }

    @NotNull
    public static <T> List<T> filterCollection(@NotNull Iterable<?> collection, @NotNull Class<? extends T> type) {
        List<T> result = new ArrayList<>();
        for (Object item : collection) {
            if (type.isInstance(item)) {
                result.add(type.cast(item));
            }
        }
        return result;
    }

    @NotNull
    public static <T> T getItem(@NotNull Iterable<T> collection, int index) {
        if (collection instanceof List) {
            return ((List<T>) collection).get(index);
        } else {
            Iterator<T> iter = collection.iterator();
            for (int i = 0; i < index; i++) {
                iter.next();
            }
            return iter.next();
        }
    }

    public static boolean equalsContents(@Nullable Collection<?> c1, @Nullable Collection<?> c2) {
        if (isEmpty(c1) && isEmpty(c2)) {
            return true;
        }
        if (c1 == null || c2 == null || c1.size() != c2.size()) {
            return false;
        }
        for (Object o : c1) {
            if (!c2.contains(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Swaps the element with its neighbor to the left in the specified list.
     * If the element is not present in the list, or it is the leftmost element in the list,
     * the list remains unchanged.
     *
     * @param list list
     * @param element element
     * @param <T> type of the list
     */
    public static <T> void shiftLeft(@NotNull List<? super T> list, @NotNull T element) {
        int idx = list.indexOf(element);
        if (idx > 0) {
            Collections.swap(list, idx - 1, idx);
        }
    }

    /**
     * Swaps the element with its neighbor to the right in the specified list.
     * If the element is not present in the list, or it is the rightmost element in the list,
     * the list remains unchanged.
     *
     * @param list list
     * @param element element
     * @param <T> type of the list
     */
    public static <T> void shiftRight(@NotNull List<? super T> list, @NotNull T element) {
        int idx = list.indexOf(element);
        if (idx != -1 && idx != list.size() - 1) {
            Collections.swap(list, idx, idx + 1);
        }
    }

    public static boolean isEmpty(@Nullable Collection<?> value) {
        return value == null || value.isEmpty();
    }

    public static boolean isEmpty(@Nullable Map<?, ?> value) {
        return value == null || value.isEmpty();
    }

    @NotNull
    public static <T> Collection<T> safeCollection(@Nullable Collection<T> theList) {
        if (theList == null) {
            theList = Collections.emptyList();
        }
        return theList;
    }

    @NotNull
    public static <T> List<T> safeList(@Nullable List<T> theList) {
        if (theList == null) {
            theList = Collections.emptyList();
        }
        return theList;
    }

    @NotNull
    public static <T> List<T> copyList(@Nullable Collection<? extends T> theList) {
        if (theList == null) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(theList);
        }
    }
}
