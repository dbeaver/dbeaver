/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
        public String getContributorId()
        {
            return PropertyTabDescriptorProvider.CONTRIBUTOR_ID;
        }
    };

}
