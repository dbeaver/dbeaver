package org.jkiss.dbeaver.core.eclipse.search;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Search result
 */
public class SearchResultAdapter implements ISearchResult {

    private final SearchQueryAdapter queryAdapter;
    private final List<Object> objects = new ArrayList<Object>();

    public SearchResultAdapter(SearchQueryAdapter queryAdapter)
    {
        this.queryAdapter = queryAdapter;
    }

    @Override
    public void addListener(ISearchResultListener l)
    {

    }

    @Override
    public void removeListener(ISearchResultListener l)
    {

    }

    @Override
    public String getLabel()
    {
        return queryAdapter.getLabel();
    }

    @Override
    public String getTooltip()
    {
        return getLabel();
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        return null;
    }

    @Override
    public SearchQueryAdapter getQuery()
    {
        return queryAdapter;
    }

    public List<Object> getObjects()
    {
        return objects;
    }

    public void addObjects(Collection<?> objects)
    {
        this.objects.addAll(objects);
    }

}
