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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPOverloadedObject;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;

import java.util.function.Predicate;

/**
 * Provides a context-specific object filter for navigator children.
 */
@FunctionalInterface
public interface DBNObjectFilterProvider {
    /**
     * Creates an effective filter for children of the given node.
     *
     * @param connectionFilter filter from the shared connection configuration
     * @return effective filter, or {@code null} when filtering is not applicable
     */
    @Nullable
    Predicate<DBSObject> createFilter(
        @NotNull DBNDatabaseNode node,
        @NotNull DBXTreeItem meta,
        @Nullable DBSObjectFilter connectionFilter
    );

    @Nullable
    static Predicate<DBSObject> createFilterMatcher(@NotNull DBSObjectFilter... filters) {
        Predicate<DBSObject> result = null;
        for (DBSObjectFilter filter : filters) {
            if (filter == null || filter.isNotApplicable()) {
                continue;
            }
            Predicate<DBSObject> matcher = object -> object instanceof DBPOverloadedObject overloadedObject
                ? filter.matchesAny(object.getName(), overloadedObject.getOverloadedName())
                : filter.matches(object.getName());
            result = result == null ? matcher : result.and(matcher);
        }
        return result;
    }
}
