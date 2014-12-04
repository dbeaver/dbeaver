package org.jkiss.dbeaver.core.eclipse.search.impl.data;

import org.jkiss.dbeaver.core.eclipse.search.SearchQueryAdapter;
import org.jkiss.dbeaver.core.eclipse.search.SearchResultAdapter;
import org.jkiss.dbeaver.ui.search.IObjectSearchQuery;

/**
 * Search query adapter
 */
public class SearchDataQueryAdapter extends SearchQueryAdapter {

    protected SearchDataQueryAdapter(IObjectSearchQuery source) {
        super(source);
    }

    @Override
    protected SearchResultAdapter createResult() {
        return new SearchDataResultAdapter(this);
    }

}
