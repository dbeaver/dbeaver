/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.ByteArrayInputStream;

/**
 * SQL editor input factory.
 * It is left for backward compatibility (for old DBeaver versions and SQL editors saved with this factory).
 * TODO: remove it at some moment
 */
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
        DBWorkbench.getPlatform().getEditorsRegistry();

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
            return new FileEditorInput(file);
        }
        return null;
    }

    public static String getFactoryId()
    {
        return ID_FACTORY;
    }

    public static void saveState(IMemento memento, IFileEditorInput input)
    {
        IFile file = input.getFile();
        memento.putString(TAG_PATH, file.getFullPath().toString());
    }

}