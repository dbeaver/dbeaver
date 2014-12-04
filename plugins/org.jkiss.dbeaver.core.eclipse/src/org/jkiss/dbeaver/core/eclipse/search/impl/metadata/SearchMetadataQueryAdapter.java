package org.jkiss.dbeaver.core.eclipse.search.impl.metadata;

import org.jkiss.dbeaver.core.eclipse.search.SearchQueryAdapter;
import org.jkiss.dbeaver.core.eclipse.search.SearchResultAdapter;
import org.jkiss.dbeaver.ui.search.IObjectSearchQuery;

/**
 * Search query adapter
 */
public class SearchMetadataQueryAdapter extends SearchQueryAdapter {

    protected SearchMetadataQueryAdapter(IObjectSearchQuery source) {
        super(source);
    }

    @Override
    protected SearchResultAdapter createResult() {
        return new SearchMetadataResultAdapter(this);
    }

}
