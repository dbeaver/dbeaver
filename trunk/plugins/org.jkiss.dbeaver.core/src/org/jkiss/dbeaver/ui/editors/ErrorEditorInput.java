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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;

/**
 * ErrorEditorInput
 */
public class ErrorEditorInput extends DatabaseEditorInput<DBNDatabaseNode>
{
    private final IStatus error;

    public ErrorEditorInput(IStatus error, DBNDatabaseNode dataSourceNode) {
        super(dataSourceNode);
        this.error = error;
    }

    public IStatus getError() {
        return error;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
    }

    @Override
    public String getToolTipText() {
        return error.getMessage();
    }

}
