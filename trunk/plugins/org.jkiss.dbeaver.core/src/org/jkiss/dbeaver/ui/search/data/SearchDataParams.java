/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.search.data;

import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Search parameters
 */
public class SearchDataParams {

    List<DBSObject> sources = new ArrayList<DBSObject>();
    String searchString;
    boolean caseSensitive;
    boolean fastSearch; // Indexed
    boolean searchNumbers;
    boolean searchLOBs;
    int maxResults;

    public SearchDataParams()
    {
    }

    public List<DBSObject> getSources() {
        return sources;
    }

    public void setSources(List<DBSObject> sources) {
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
