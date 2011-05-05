/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.eclipse.swt.graphics.Image;
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
    private Image image;

    public PropertyTabDescriptor(
        String category,
        String id,
        String label,
        Image image,
        List<ISectionDescriptor> sectionDescriptors)
    {
        this.category = category;
        this.id = id;
        this.label = label;
        this.image = image;
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

    public Image getImage()
    {
        return image;
    }
}