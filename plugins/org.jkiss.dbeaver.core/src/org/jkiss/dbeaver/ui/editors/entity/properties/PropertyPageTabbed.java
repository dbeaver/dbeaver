/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.ui.views.properties.tabbed.*;

import java.util.HashMap;
import java.util.Map;

/**
 * PropertyPageTabbed
 */
class PropertyPageTabbed extends TabbedPropertySheetPage {

    private boolean allowContentScroll = false;
    private Map<ITabDescriptor, TabContents> tabContentsMap = new HashMap<ITabDescriptor, TabContents>();

    public PropertyPageTabbed()
    {
        this(true);
    }

    public PropertyPageTabbed(boolean showTitle)
    {
        super(DEFAULT_PROP_SHEET_CONTRIBUTOR, showTitle);
    }

    public boolean isAllowContentScroll()
    {
        return allowContentScroll;
    }

    public void setAllowContentScroll(boolean allowContentScroll)
    {
        this.allowContentScroll = allowContentScroll;
    }

    /**
     * This is empty implementation of resizeScrolledComposite()
     * to avoid scrolled composite scroll-bars
     */
    @Override
    public void resizeScrolledComposite()
    {
        if (allowContentScroll) {
            super.resizeScrolledComposite();
        }
    }

    public ISection[] getTabSections(ITabDescriptor tabDescriptor)
    {
        final TabContents tabContents = tabContentsMap.get(tabDescriptor);
        return tabContents == null ? null : tabContents.getSections();
    }

    @Override
    protected TabContents createTab(ITabDescriptor tabDescriptor)
    {
        final TabContents tabContents = super.createTab(tabDescriptor);
        tabContentsMap.put(tabDescriptor, tabContents);
        return tabContents;
    }

    @Override
    protected void updateTabs(ITabDescriptor[] descriptors)
    {
        super.updateTabs(descriptors);
    }

    private static ITabbedPropertySheetPageContributor DEFAULT_PROP_SHEET_CONTRIBUTOR = new ITabbedPropertySheetPageContributor() {
        @Override
        public String getContributorId()
        {
            return PropertyTabDescriptorProvider.CONTRIBUTOR_ID;
        }
    };

}
