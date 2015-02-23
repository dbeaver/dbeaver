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