/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.search.data;

import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Search parameters
 */
public class SearchDataParams {

    List<DBSDataContainer> sources = new ArrayList<>();
    String searchString;
    boolean caseSensitive;
    boolean fastSearch; // Indexed
    boolean searchNumbers;
    boolean searchLOBs;
    boolean searchForeignObjects;
    int maxResults;
    List<DBNNode> selectedNodes = new ArrayList<>();

    public SearchDataParams()
    {
    }

    public List<DBSDataContainer> getSources() {
        return sources;
    }

    public void setSources(List<DBSDataContainer> sources) {
        this.sources = sources;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isFastSearch() {
        return fastSearch;
    }

    public void setFastSearch(boolean fastSearch) {
        this.fastSearch = fastSearch;
    }

    public boolean isSearchNumbers() {
        return searchNumbers;
    }

    public void setSearchNumbers(boolean searchNumbers) {
        this.searchNumbers = searchNumbers;
    }

    public boolean isSearchLOBs() {
        return searchLOBs;
    }

    public void setSearchLOBs(boolean searchLOBs) {
        this.searchLOBs = searchLOBs;
    }

    public boolean isSearchForeignObjects() {
        return searchForeignObjects;
    }

    public void setSearchForeignObjects(boolean searchForeignObjects) {
        this.searchForeignObjects = searchForeignObjects;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public List<DBNNode> getSelectedNodes() {
        return selectedNodes;
    }
}
