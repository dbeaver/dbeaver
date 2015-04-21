/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.ext.IAutoSaveEditorInput;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;

/**
 * ERDEditorInput
 */
public class ERDEditorInput extends ProjectFileEditorInput implements IPersistableElement, IAutoSaveEditorInput
{
    private String diagramName;

    public ERDEditorInput(IFile file)
    {
        super(file);
        this.diagramName = file.getFullPath().removeFileExtension().lastSegment();
    }

    @Override
    public String getName()
    {
        return diagramName;
    }

    @Override
    public String getToolTipText()
    {
        return diagramName;
    }

    @Override
    public IPersistableElement getPersistable()
    {
        return this;
    }

    @Override
    public String getFactoryId()
    {
        return ERDEditorInputFactory.getFactoryId();
    }

    @Override
    public void saveState(IMemento memento)
    {
        ERDEditorInputFactory.saveState(memento, this);
    }

    @Override
    public boolean isAutoSaveEnabled()
    {
        return false;
    }

}
