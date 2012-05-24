/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties.tabbed;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractSectionDescriptor;

/**
 * Section descriptor
 */
public abstract class SectionDescriptor extends AbstractSectionDescriptor {

    private String id;
    private String targetTab;

    protected SectionDescriptor(String id, String targetTab)
    {
        this.id = id;
        this.targetTab = targetTab;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getTargetTab()
    {
        return targetTab;
    }

    @Override
    public boolean appliesTo(IWorkbenchPart part, ISelection selection)
    {
        return true;
    }
}
