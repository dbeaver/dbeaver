/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ui.properties.tabbed;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.tabbed.AbstractTabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ISectionDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;

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

    @Override
    public String getCategory()
    {
        return category;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getLabel()
    {
        return label;
    }

    @Override
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

    @Override
    /**
     * Tabs are always intended because non-intended tabs looks ugly in some look-and-feel themes
     * (because they use shadow color as background)
     */
    public boolean isIndented()
    {
        return !DBeaverCore.getInstance().getLocalSystem().isWindows();//true;//PropertiesContributor.TAB_STANDARD.equals(id);
    }
}