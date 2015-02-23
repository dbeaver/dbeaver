/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
