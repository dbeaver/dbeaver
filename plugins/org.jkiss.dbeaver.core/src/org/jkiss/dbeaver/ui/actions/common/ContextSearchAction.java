/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * Context search action
 */
public class ContextSearchAction extends Action {

    private final ISearchContextProvider contextProvider;
    private final ISearchContextProvider.SearchType searchType;

    public ContextSearchAction(ISearchContextProvider contextProvider, ISearchContextProvider.SearchType searchType)
    {
        super(CoreMessages.ui_actions_context_search_name, DBeaverIcons.getImageDescriptor(UIIcon.FIND));
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
