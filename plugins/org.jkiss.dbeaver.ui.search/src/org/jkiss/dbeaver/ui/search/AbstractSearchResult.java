/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.search;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.SearchResultEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSearchResult<OBJECT_TYPE> implements ISearchResult {


    private ISearchQuery searchQuery;
    private List<OBJECT_TYPE> objects = new ArrayList<>();
    private final List<ISearchResultListener> listeners = new ArrayList<>();

    public AbstractSearchResult(ISearchQuery searchQuery) {
        this.searchQuery = searchQuery;
    }

    @Override
    public void addListener(ISearchResultListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListener(ISearchResultListener l) {
        listeners.remove(l);
    }

    @Override
    public String getLabel() {
        return searchQuery.getLabel();
    }

    @Override
    public String getTooltip() {
        return searchQuery.getLabel();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public ISearchQuery getQuery() {
        return searchQuery;
    }

    public List<OBJECT_TYPE> getObjects() {
        return objects;
    }

    public void addObjects(List<OBJECT_TYPE> objects) {
        this.objects.addAll(objects);
        fireChange(new DatabaseSearchResultEvent(objects));
    }

    public void fireChange(SearchResultEvent e) {
        ISearchResultListener[] copiedListeners;
        synchronized (listeners) {
            copiedListeners = listeners.toArray(new ISearchResultListener[listeners.size()]);
        }
        for (ISearchResultListener listener : copiedListeners) {
            listener.searchResultChanged(e);
        }
    }

    public class DatabaseSearchResultEvent extends SearchResultEvent {
        private final List<OBJECT_TYPE> objects;
        public DatabaseSearchResultEvent(List<OBJECT_TYPE> objects)
        {
            super(AbstractSearchResult.this);
            this.objects = objects;
        }

        public List<OBJECT_TYPE> getObjects() {
            return objects;
        }
    }

    public static class DatabaseSearchFinishEvent extends SearchResultEvent {
        private final int totalObjects;

        public DatabaseSearchFinishEvent(ISearchResult searchResult, int totalObjects) {
            super(searchResult);
            this.totalObjects = totalObjects;
        }

        public int getTotalObjects() {
            return totalObjects;
        }
    }

}
