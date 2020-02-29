/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.jkiss.dbeaver.ui.actions.EmptyListAction;

import java.util.ArrayList;
import java.util.List;

public abstract class DataSourceMenuContributor extends CompoundContributionItem
{
    @Override
    protected IContributionItem[] getContributionItems()
    {
        List<IContributionItem> menuItems = new ArrayList<>();
        fillContributionItems(menuItems);
        return menuItems.isEmpty() ? makeEmptyList() : menuItems.toArray(new IContributionItem[0]);
    }

    protected abstract void fillContributionItems(
        List<IContributionItem> menuItems);

    private static IContributionItem[] makeEmptyList(){
        return new IContributionItem[] { new ActionContributionItem(new EmptyListAction())};
    }

}