/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.bundle.UIMessages;
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
        super(UIMessages.ui_actions_context_search_name, DBeaverIcons.getImageDescriptor(UIIcon.FIND));
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
