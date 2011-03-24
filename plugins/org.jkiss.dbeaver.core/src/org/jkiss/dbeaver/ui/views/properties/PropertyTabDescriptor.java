/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.views.properties.tabbed.AbstractTabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ISectionDescriptor;

import java.util.List;

/**
 * PropertyTabDescriptor
 */
public class PropertyTabDescriptor extends AbstractTabDescriptor {

    private String category;
    private String id;
    private String label;

    public PropertyTabDescriptor(
        String category,
        String id,
        String label,
        List<ISectionDescriptor> sectionDescriptors)
    {
        this.category = category;
        this.id = id;
        this.label = label;
        setSectionDescriptors(sectionDescriptors);
    }

    public String getCategory()
    {
        return category;
    }

    public String getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

}