/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.*;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyTabDescriptorProvider
 */
public class PropertyTabDescriptorProvider implements ITabDescriptorProvider {

    public ITabDescriptor[] getTabDescriptors(IWorkbenchPart part, ISelection selection)
    {
        List<ISectionDescriptor> standardSections = new ArrayList<ISectionDescriptor>();
        standardSections.add(new AbstractSectionDescriptor() {
            public String getId()
            {
                return PropertiesContributor.SECTION_STANDARD;
            }

            public ISection getSectionClass()
            {
                return new PropertySectionStandard();
            }

            public String getTargetTab()
            {
                return PropertiesContributor.TAB_STANDARD;
            }

            @Override
            public boolean appliesTo(IWorkbenchPart part, ISelection selection)
            {
                return true;
            }
        });
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_MAIN,
                PropertiesContributor.TAB_STANDARD,
                "Edit",
                standardSections),
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_MAIN,
                PropertiesContributor.TAB_STANDARD,
                "Information",
                standardSections)
        };
    }

}
