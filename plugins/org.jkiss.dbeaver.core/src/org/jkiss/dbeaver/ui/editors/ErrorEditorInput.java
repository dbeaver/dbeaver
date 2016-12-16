/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ErrorEditorInput
 */
public class ErrorEditorInput extends DatabaseEditorInput<DBNDatabaseNode>
{
    private final IStatus error;

    public ErrorEditorInput(@NotNull IStatus error, @Nullable DBNDatabaseNode dataSourceNode) {
        super(dataSourceNode);
        this.error = error;
    }

    public IStatus getError() {
        return error;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return UIUtils.getShardImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
    }

    @Override
    public String getToolTipText() {
        return error.getMessage();
    }

}
