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
