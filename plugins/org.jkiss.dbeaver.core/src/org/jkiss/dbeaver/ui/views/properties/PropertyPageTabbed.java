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

    public PropertyPageTabbed()
    {
        this(true);
    }

    public PropertyPageTabbed(boolean showTitle)
    {
        super(DEFAULT_PROP_SHEET_CONTRIBUTOR, showTitle);
    }

    private static ITabbedPropertySheetPageContributor DEFAULT_PROP_SHEET_CONTRIBUTOR = new ITabbedPropertySheetPageContributor() {
        public String getContributorId()
        {
            return PropertiesContributor.CONTRIBUTOR_ID;
        }
    };

}
