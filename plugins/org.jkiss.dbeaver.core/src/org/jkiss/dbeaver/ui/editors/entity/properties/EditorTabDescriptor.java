/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.tabbed.AbstractTabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.registry.EntityEditorDescriptor;
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
