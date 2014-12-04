package org.jkiss.dbeaver.core.eclipse.search.impl.metadata;

import org.jkiss.dbeaver.core.eclipse.search.SearchResultPageAdapter;
import org.jkiss.dbeaver.ui.search.metadata.SearchDataResultsPage;

/**
 * Metadata search page adapter
 */
public class SearchMetadataResultPageAdapter extends SearchResultPageAdapter {

    public SearchMetadataResultPageAdapter()
    {
        super(new SearchDataResultsPage());
    }

}
