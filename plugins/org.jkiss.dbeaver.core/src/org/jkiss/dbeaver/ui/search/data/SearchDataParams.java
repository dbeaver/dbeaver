/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.search.data;

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
    int maxResults;

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

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}
