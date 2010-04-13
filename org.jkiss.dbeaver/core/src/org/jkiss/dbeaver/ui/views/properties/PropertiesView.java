/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.PageBook;

/**
 * PropertiesView
 */
public class PropertiesView extends PropertySheet
{
    public PropertiesView()
    {
    }

    protected IPage createDefaultPage(PageBook book) {
        PropertySheetPage page = new PropertiesPage();
        initPage(page);
        page.createControl(book);
        return page;
    }

    @Override
    public PropertiesPage getCurrentPage()
    {
        return (PropertiesPage)super.getCurrentPage();
    }

}
