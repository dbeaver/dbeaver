/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * PropertyPageTabbed
 */
public class PropertyPageTabbed extends TabbedPropertySheetPage {

    private boolean allowContentScroll = false;

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

    private static ITabbedPropertySheetPageContributor DEFAULT_PROP_SHEET_CONTRIBUTOR = new ITabbedPropertySheetPageContributor() {
        public String getContributorId()
        {
            return PropertiesContributor.CONTRIBUTOR_ID;
        }
    };

}
