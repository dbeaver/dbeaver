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

package org.jkiss.dbeaver.ui.actions.datasource;

import org.jkiss.dbeaver.Log;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.jkiss.dbeaver.ui.actions.common.EmptyListAction;

import java.util.ArrayList;
import java.util.List;

public abstract class DataSourceMenuContributor extends CompoundContributionItem
{
    private static final Log log = Log.getLog(DataSourceMenuContributor.class);

    @Override
    protected IContributionItem[] getContributionItems()
    {
        List<IContributionItem> menuItems = new ArrayList<>();
        fillContributionItems(menuItems);
        return menuItems.isEmpty() ? makeEmptyList() : menuItems.toArray(new IContributionItem[menuItems.size()]);
    }

    protected abstract void fillContributionItems(
        List<IContributionItem> menuItems);

    private static IContributionItem[] makeEmptyList(){
        return new IContributionItem[] { new ActionContributionItem(new EmptyListAction())};
    }

}