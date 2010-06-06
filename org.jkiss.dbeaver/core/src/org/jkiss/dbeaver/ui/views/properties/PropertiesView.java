/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * PropertiesView
 */
public class PropertiesView extends PropertySheet
{
    public PropertiesView()
    {
    }

    protected IPage createDefaultPage(PageBook book) {
        PropertySheetPage page = new PropertyPageStandard();
        initPage(page);
        page.createControl(book);
        return page;
    }

    @Override
    public PropertyPageStandard getCurrentPage()
    {
        return (PropertyPageStandard)super.getCurrentPage();
    }

}
