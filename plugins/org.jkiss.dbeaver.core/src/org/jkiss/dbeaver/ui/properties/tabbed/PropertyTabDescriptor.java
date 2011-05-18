/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties.tabbed;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.tabbed.AbstractTabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ISectionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PropertyTabDescriptor
 */
public class PropertyTabDescriptor extends AbstractTabDescriptor {

    private String category;
    private String id;
    private String label;
    private Image image;
    private String afterTab;

    public PropertyTabDescriptor(
        String category,
        String id,
        String label,
        Image image,
        ISectionDescriptor ... sectionDescriptors)
    {
        this.category = category;
        this.id = id;
        this.label = label;
        this.image = image;
        if (sectionDescriptors != null) {
            List<ISectionDescriptor> sections = new ArrayList<ISectionDescriptor>(sectionDescriptors.length);
            Collections.addAll(sections, sectionDescriptors);
            setSectionDescriptors(sections);
        }
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

    @Override
    public String getAfterTab()
    {
        if (afterTab != null) {
            return afterTab;
        }
        return super.getAfterTab();
    }

    public void setAfterTab(String afterTab)
    {
        this.afterTab = afterTab;
    }

}