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
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.tabbed.AbstractTabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.registry.editor.EntityEditorDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.EditorWrapperSection;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;

import java.util.Collections;

/**
* EditorTabDescriptor
*/
class EditorTabDescriptor extends AbstractTabDescriptor {
    private IDatabaseEditor mainEditor;
    private EntityEditorDescriptor descriptor;

    public EditorTabDescriptor(IDatabaseEditor part, EntityEditorDescriptor descriptor)
    {
        this.mainEditor = part;
        this.descriptor = descriptor;
        setSectionDescriptors(Collections.singletonList(
            new SectionDescriptor(PropertiesContributor.SECTION_STANDARD, this.descriptor.getId()) {
                @Override
                public ISection getSectionClass()
                {
                    return new EditorWrapperSection(
                        EditorTabDescriptor.this.mainEditor,
                        EditorTabDescriptor.this.descriptor);
                }
            }));
    }

    @Override
    public String getCategory()
    {
        return PropertiesContributor.CATEGORY_STRUCT;
    }

    @Override
    public String getId()
    {
        return descriptor.getId();
    }

    @Override
    public String getLabel()
    {
        return descriptor.getName();
    }

    @Override
    public Image getImage()
    {
        return descriptor.getIcon();
    }

    @Override
    public boolean isIndented()
    {
        return !DBeaverCore.getInstance().getLocalSystem().isWindows();
    }
}
