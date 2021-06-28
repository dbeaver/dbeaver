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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * DBSStructureAssistant
 */
public interface DBSStructureAssistant<CONTEXT extends DBCExecutionContext> {
    DBSObjectType[] getSupportedObjectTypes();

    DBSObjectType[] getSearchObjectTypes();

    DBSObjectType[] getHyperlinkObjectTypes();

    DBSObjectType[] getAutoCompleteObjectTypes();

    @NotNull
    List<DBSObjectReference> findObjectsByMask(@NotNull DBRProgressMonitor monitor, @NotNull CONTEXT executionContext,
                                               @NotNull ObjectsSearchParams params) throws DBException;

    default boolean supportsSearchInCommentsFor(@NotNull DBSObjectType objectType) {
        return false;
    }

    default boolean supportsSearchInDefinitionsFor(@NotNull DBSObjectType objectType) {
        return false;
    }

    /**
     * A data class with search parameters.
     *
     * These include:
     * <ul>
     *     <li>{@code parentObject}: parent (schema or catalog)</li>
     *     <li>{@code objectTypes}: type of objects to search</li>
     *     <li>{@code mask}: name mask</li>
     *     <li>{@code caseSensitive}: case sensitive search (ignored by some implementations)</li>
     *     <li>{@code globalSearch}: search in all available schemas/catalogs. If {@code false} then search with respect of active schema/catalog</li>
     *     <li>{@code maxResults}: maximum number of results</li>
     *     <li>{@code searchInComments}: perform additional search in comments (ignored by some implementations)</li>
     *     <li>{@code searchInDefinitions}: perform additional search in definitions (ignored by some implementations)</li>
     * </ul>
     */
    class ObjectsSearchParams {
        @NotNull
        private final DBSObjectType[] objectTypes;
        @NotNull
        private String mask;
        @Nullable
        private DBSObject parentObject;
        private int maxResults = Integer.MAX_VALUE;
        private boolean caseSensitive;
        private boolean searchInComments;
        private boolean searchInDefinitions;
        private boolean globalSearch;

        public ObjectsSearchParams(@NotNull DBSObjectType[] objectTypes, @NotNull String mask) {
            this.objectTypes = objectTypes;
            this.mask = mask;
        }

        @Nullable
        public DBSObject getParentObject() {
            return parentObject;
        }

        public void setParentObject(@Nullable DBSObject parentObject) {
            this.parentObject = parentObject;
        }

        @NotNull
        public DBSObjectType[] getObjectTypes() {
            return objectTypes;
        }

        @NotNull
        public String getMask() {
            return mask;
        }

        public void setMask(@NotNull String mask) {
            this.mask = mask;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public boolean isSearchInComments() {
            return searchInComments;
        }

        public void setSearchInComments(boolean searchInComments) {
            this.searchInComments = searchInComments;
        }

        public boolean isSearchInDefinitions() {
            return searchInDefinitions;
        }

        public void setSearchInDefinitions(boolean searchInDefinitions) {
            this.searchInDefinitions = searchInDefinitions;
        }

        public boolean isGlobalSearch() {
            return globalSearch;
        }

        public void setGlobalSearch(boolean globalSearch) {
            this.globalSearch = globalSearch;
        }
    }
}
