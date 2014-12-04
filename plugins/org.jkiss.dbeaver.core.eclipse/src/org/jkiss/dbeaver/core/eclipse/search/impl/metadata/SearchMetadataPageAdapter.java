package org.jkiss.dbeaver.core.eclipse.search.impl.metadata;

import org.eclipse.search.ui.ISearchQuery;
import org.jkiss.dbeaver.core.eclipse.search.SearchPageAdapter;
import org.jkiss.dbeaver.ui.search.IObjectSearchQuery;
import org.jkiss.dbeaver.ui.search.metadata.SearchMetadataPage;

/**
 * Metadata search page adapter
 */
public class SearchMetadataPageAdapter extends SearchPageAdapter {

    public SearchMetadataPageAdapter()
    {
        super(new SearchMetadataPage());
    }

    @Override
    protected ISearchQuery createQueryAdapter(IObjectSearchQuery query) {
        return new SearchMetadataQueryAdapter(query);
    }
}
