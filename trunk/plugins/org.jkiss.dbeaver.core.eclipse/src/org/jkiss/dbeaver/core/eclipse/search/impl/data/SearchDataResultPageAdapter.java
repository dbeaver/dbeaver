package org.jkiss.dbeaver.core.eclipse.search.impl.data;

import org.jkiss.dbeaver.core.eclipse.search.SearchResultPageAdapter;
import org.jkiss.dbeaver.ui.search.data.SearchDataResultsPage;

/**
 * Data search results page adapter
 */
public class SearchDataResultPageAdapter extends SearchResultPageAdapter {

    public SearchDataResultPageAdapter()
    {
        super(new SearchDataResultsPage());
    }

}
