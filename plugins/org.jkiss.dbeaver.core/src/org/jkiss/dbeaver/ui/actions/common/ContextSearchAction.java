/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Context search action
 */
public class ContextSearchAction extends Action {

    private final ISearchContextProvider contextProvider;
    private final ISearchContextProvider.SearchType searchType;

    public ContextSearchAction(ISearchContextProvider contextProvider, ISearchContextProvider.SearchType searchType)
    {
        super(CoreMessages.ui_actions_context_search_name, DBIcon.FIND.getImageDescriptor());
        this.contextProvider = contextProvider;
        this.searchType = searchType;
    }

    @Override
    public void run()
    {
        if (contextProvider.isSearchEnabled()) {
            contextProvider.performSearch(searchType);
        }
    }
}
