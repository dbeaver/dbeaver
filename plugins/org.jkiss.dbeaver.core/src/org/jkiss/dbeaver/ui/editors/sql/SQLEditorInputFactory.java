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
package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.core.DBeaverCore;

import java.io.ByteArrayInputStream;

public class SQLEditorInputFactory implements IElementFactory
{
    static final Log log = Log.getLog(SQLEditorInputFactory.class);

    private static final String ID_FACTORY = SQLEditorInputFactory.class.getName(); //$NON-NLS-1$

    private static final String TAG_PATH = "path"; //$NON-NLS-1$

    public SQLEditorInputFactory()
    {
    }

    @Override
    public IAdaptable createElement(IMemento memento)
    {
        // Get the file name.
        String fileName = memento.getString(TAG_PATH);
        if (fileName == null) {
            return null;
        }
        // Make sure that core is initialized
        DBeaverCore.getInstance();

        // Get a handle to the IFile...which can be a handle
        // to a resource that does not exist in workspace
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fileName));
        if (file != null) {
            if (!file.exists()) {
                try {
                    file.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
                } catch (CoreException e) {
                    log.error("Can't create new file", e);
                    return null;
                }
            }
            return new SQLEditorInput(file);
        }
        return null;
    }

    public static String getFactoryId()
    {
        return ID_FACTORY;
    }

    public static void saveState(IMemento memento, SQLEditorInput input)
    {
        IFile file = input.getFile();
        memento.putString(TAG_PATH, file.getFullPath().toString());
    }

}