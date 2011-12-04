/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
                public ISection getSectionClass()
                {
                    return new EditorWrapperSection(
                        EditorTabDescriptor.this.mainEditor,
                        EditorTabDescriptor.this.descriptor);
                }
            }));
    }

    public String getCategory()
    {
        return PropertiesContributor.CATEGORY_STRUCT;
    }

    public String getId()
    {
        return descriptor.getId();
    }

    public String getLabel()
    {
        return descriptor.getName();
    }

    public Image getImage()
    {
        return descriptor.getIcon();
    }

    public boolean isIndented()
    {
        return !DBeaverCore.getInstance().getLocalSystem().isWindows();
    }
}
