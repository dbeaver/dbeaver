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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;

/**
 * AbstractDatabaseEditor
 */
public abstract class AbstractDatabaseEditor<INPUT_TYPE extends IDatabaseEditorInput> extends EditorPart implements IDatabaseEditor, IDataSourceProvider
{
    private DatabaseEditorListener listener;
    private Image editorImage;

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.setSite(site);
        super.setInput(input);
        this.setPartName(input.getName());
        editorImage = input.getImageDescriptor().createImage();
        this.setTitleImage(editorImage);

        listener = new DatabaseEditorListener(this);
    }

    @Override
    public void dispose()
    {
        if (editorImage != null) {
            editorImage.dispose();
            editorImage = null;
        }
        listener.dispose();
        super.dispose();
    }

    @Override
    @SuppressWarnings("unchecked")
    public INPUT_TYPE getEditorInput()
    {
        return (INPUT_TYPE)super.getEditorInput();
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isDirty()
    {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    public void setFocus() {

    }

}