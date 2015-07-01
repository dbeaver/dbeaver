/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.utils.CommonUtils;

public class ERDEditorInputFactory implements IElementFactory
{
    private static final String ID_FACTORY = ERDEditorInputFactory.class.getName(); //$NON-NLS-1$

    private static final String TAG_PATH = "path"; //$NON-NLS-1$

    @Override
    public IAdaptable createElement(IMemento memento)
    {
        // Get the file name.
        String fileName = memento.getString(TAG_PATH);
        if (!CommonUtils.isEmpty(fileName)) {
            // Make sure that core is initialized
            DBeaverCore.getInstance();

            IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fileName));
            if (file != null) {
                return new ERDEditorInput(file);
            }
        }
        return null;
    }

    public static String getFactoryId()
    {
        return ID_FACTORY;
    }

    public static void saveState(IMemento memento, ERDEditorInput input)
    {
        memento.putString(TAG_PATH, input.getFile().getFullPath().toString());
    }

}