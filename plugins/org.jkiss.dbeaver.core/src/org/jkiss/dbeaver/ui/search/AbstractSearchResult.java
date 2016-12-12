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

    protected void fireChange(SearchResultEvent e) {
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
}
