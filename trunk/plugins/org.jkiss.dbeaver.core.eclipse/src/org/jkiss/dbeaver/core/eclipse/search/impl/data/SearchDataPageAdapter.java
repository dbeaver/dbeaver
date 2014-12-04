package org.jkiss.dbeaver.core.eclipse.search.impl.data;

import org.eclipse.search.ui.ISearchQuery;
import org.jkiss.dbeaver.core.eclipse.search.SearchPageAdapter;
import org.jkiss.dbeaver.core.eclipse.search.SearchQueryAdapter;
import org.jkiss.dbeaver.ui.search.IObjectSearchQuery;
import org.jkiss.dbeaver.ui.search.data.SearchDataPage;

/**
 * Data search page adapter
 */
public class SearchDataPageAdapter extends SearchPageAdapter {

    public SearchDataPageAdapter()
    {
        super(new SearchDataPage());
    }

    @Override
    protected ISearchQuery createQueryAdapter(IObjectSearchQuery query) {
        return new SearchDataQueryAdapter(query);
    }
}
