/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Filter mapping
 */
public class FilterMapping {
    public final String typeName;
    DBSObjectFilter globalFilter;
    Map<String, DBSObjectFilter> customFilters = new HashMap<>();

    FilterMapping(String typeName) {
        this.typeName = typeName;
    }

    // Copy constructor
    FilterMapping(FilterMapping mapping) {
        this.typeName = mapping.typeName;
        this.globalFilter = mapping.globalFilter == null ? null : new DBSObjectFilter(mapping.globalFilter);
        for (Map.Entry<String, DBSObjectFilter> entry : mapping.customFilters.entrySet()) {
            this.customFilters.put(entry.getKey(), new DBSObjectFilter(entry.getValue()));
        }
    }

    /**
     * Resolves datasource mappings, checking interfaces before the class hierarchy.
     * Each type lookup only checks its immediate interfaces when it has no mapping of its own.
     */
    @Nullable
    static FilterMapping resolve(
        @NotNull Map<String, FilterMapping> filters,
        @NotNull Class<?> type,
        @Nullable DBSObject parentObject,
        boolean firstMatch
    ) {
        if (filters.isEmpty()) {
            return null;
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            FilterMapping mapping = resolveType(filters, interfaceType, parentObject, firstMatch);
            if (mapping != null) {
                return mapping;
            }
        }
        for (Class<?> testType = type; testType != null; testType = testType.getSuperclass()) {
            FilterMapping mapping = resolveType(filters, testType, parentObject, firstMatch);
            if (mapping != null) {
                return mapping;
            }
        }
        return null;
    }

    /**
     * User lookups prioritize an explicit mapping for this parent, including inherited interfaces,
     * before applying the datasource lookup rules.
     */
    @Nullable
    static FilterMapping resolveUser(
        @NotNull Map<String, FilterMapping> filters,
        @NotNull Class<?> type,
        @Nullable DBSObject parentObject,
        boolean firstMatch
    ) {
        if (filters.isEmpty()) {
            return null;
        }
        if (parentObject != null) {
            FilterMapping mapping = findCustomMapping(filters, type, getFilterContainerUniqueID(parentObject));
            if (mapping != null && hasMatchingFilter(mapping, parentObject, firstMatch)) {
                return mapping;
            }
        }
        return resolve(filters, type, parentObject, firstMatch);
    }

    @Nullable
    private static FilterMapping resolveType(
        @NotNull Map<String, FilterMapping> filters,
        @NotNull Class<?> type,
        @Nullable DBSObject parentObject,
        boolean firstMatch
    ) {
        FilterMapping mapping = filters.get(type.getName());
        if (mapping == null) {
            for (Class<?> interfaceType : type.getInterfaces()) {
                mapping = filters.get(interfaceType.getName());
                if (mapping != null && hasMatchingFilter(mapping, parentObject, firstMatch)) {
                    return mapping;
                }
            }
        }
        return mapping != null && hasMatchingFilter(mapping, parentObject, firstMatch) ? mapping : null;
    }

    private static boolean hasMatchingFilter(
        @NotNull FilterMapping mapping,
        @Nullable DBSObject parentObject,
        boolean firstMatch
    ) {
        DBSObjectFilter filter = mapping.getFilter(parentObject, firstMatch);
        return filter != null && (firstMatch || filter.isEnabled());
    }

    /** Selects an existing user mapping for writing, regardless of filter applicability. */
    @Nullable
    static FilterMapping findMapping(@NotNull Map<String, FilterMapping> filters, @NotNull Class<?> type) {
        return findMapping(filters, type, mapping -> true);
    }

    /** Selects an existing user mapping for the same parent before considering other mappings. */
    @Nullable
    static FilterMapping findCustomMapping(
        @NotNull Map<String, FilterMapping> filters,
        @NotNull Class<?> type,
        @NotNull String objectId
    ) {
        return findMapping(filters, type, mapping -> mapping.customFilters.containsKey(objectId));
    }

    @Nullable
    private static FilterMapping findMapping(
        @NotNull Map<String, FilterMapping> filters,
        @NotNull Class<?> type,
        @NotNull Predicate<FilterMapping> predicate
    ) {
        for (Class<?> interfaceType : type.getInterfaces()) {
            FilterMapping mapping = findTypeMapping(filters, interfaceType, predicate);
            if (mapping != null) {
                return mapping;
            }
        }
        for (Class<?> testType = type; testType != null; testType = testType.getSuperclass()) {
            FilterMapping mapping = findTypeMapping(filters, testType, predicate);
            if (mapping != null) {
                return mapping;
            }
        }
        return null;
    }

    @Nullable
    private static FilterMapping findTypeMapping(
        @NotNull Map<String, FilterMapping> filters,
        @NotNull Class<?> type,
        @NotNull Predicate<FilterMapping> predicate
    ) {
        FilterMapping mapping = filters.get(type.getName());
        if (mapping != null && predicate.test(mapping)) {
            return mapping;
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            mapping = findTypeMapping(filters, interfaceType, predicate);
            if (mapping != null) {
                return mapping;
            }
        }
        return null;
    }

    @Nullable
    DBSObjectFilter getFilter(@Nullable DBSObject parentObject, boolean firstMatch) {
        if (parentObject == null) {
            return globalFilter;
        }
        if (!customFilters.isEmpty()) {
            String objectID = getFilterContainerUniqueID(parentObject);
            DBSObjectFilter filter = customFilters.get(objectID);
            if ((filter != null && !filter.isNotApplicable()) || firstMatch) {
                return filter;
            }
        }

        return firstMatch ? null : globalFilter;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FilterMapping)) {
            return false;
        }
        FilterMapping source = (FilterMapping) obj;
        return CommonUtils.equalObjects(typeName, source.typeName)
            && CommonUtils.equalObjects(globalFilter, source.globalFilter)
            && CommonUtils.equalObjects(customFilters, source.customFilters);
    }

    @Override
    public int hashCode() {
        return CommonUtils.hashCode(typeName) +
            CommonUtils.hashCode(globalFilter) +
            CommonUtils.hashCode(customFilters);
    }

    public static String getFilterContainerUniqueID(@Nullable DBSObject parentObject) {
        String objectFullName = DBUtils.getObjectFullName(parentObject, DBPEvaluationContext.UI);
        DBSInstance ownerInstance = DBUtils.getObjectOwnerInstance(parentObject);
        if (!CommonUtils.equalObjects(ownerInstance.getName(), parentObject.getDataSource().getName())) {
            return ownerInstance.getName() + ":" + objectFullName;
        } else {
            return objectFullName;
        }
    }

}
