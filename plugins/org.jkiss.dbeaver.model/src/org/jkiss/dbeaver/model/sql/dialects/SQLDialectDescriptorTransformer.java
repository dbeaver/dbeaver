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
package org.jkiss.dbeaver.model.sql.dialects;

import org.jkiss.code.NotNull;

import java.util.*;

/**
 * Includes additional keywords and excludes removed words
 * Due to using hashmap deletion is O(1) operation, so we can
 * avoid changing any data contained in the Descriptor
 */
public class SQLDialectDescriptorTransformer {

    private final Map<String, Map<String, Set<String>>> modifications;
    private final String id;

    public SQLDialectDescriptorTransformer(@NotNull String id) {
        this.id = id;
        modifications = new LinkedHashMap<>();
    }

    public void putExcludedWords(@NotNull String categoryID, @NotNull Set<String> excludedWords) {
        modifications.computeIfAbsent(categoryID, x -> new HashMap<>()).put(FilterType.EXCLUDES.name(), excludedWords);
    }

    public void putIncludedWords(@NotNull String categoryID, @NotNull Set<String> includedWords) {
        modifications.computeIfAbsent(categoryID, x -> new HashMap<>()).put(FilterType.INCLUDES.name(), includedWords);
    }

    public void putWords(@NotNull String categoryID, @NotNull Map<FilterType, Set<String>> words) {
        putIncludedWords(categoryID, words.get(FilterType.INCLUDES));
        putExcludedWords(categoryID, words.get(FilterType.EXCLUDES));
    }

    @NotNull
    public Map<String, Map<String, Set<String>>> getModifications() {
        return modifications;
    }

    @NotNull
    public String getId() {
        return id;
    }

    /**
     *
     * @param type sql keyword type
     * @param set set to modify
     * @return modified set
     */
    @NotNull
    public Set<String> transform(@NotNull SQLDialectDescriptor.WordType type, @NotNull Set<String> set) {
        HashSet<String> result = new HashSet<>(set);
        Map<String, Set<String>> typeListMap = modifications.get(type.getTypeName());
        if (typeListMap == null) {
            return result;
        }
        Set<String> exclude = typeListMap.get(FilterType.EXCLUDES.name());
        if (exclude != null) {
            exclude.forEach(result::remove);
        }
        Set<String> include = typeListMap.get(FilterType.INCLUDES.name());
        if (include != null) {
            result.addAll(include);
        }
        return result;
    }

    public enum FilterType {
        EXCLUDES,
        INCLUDES
    }

}
