package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Context search action
 */
public class ContextSearchAction extends Action {

    private final ISearchContextProvider contextProvider;

    public ContextSearchAction(ISearchContextProvider contextProvider)
    {
        super("Context search", DBIcon.FIND.getImageDescriptor());
        this.contextProvider = contextProvider;
    }

    @Override
    public void run()
    {
        if (contextProvider.isSearchEnabled()) {
            contextProvider.performSearch(ISearchContextProvider.SearchType.NONE);
        }
    }
}
