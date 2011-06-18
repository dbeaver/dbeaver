/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.jkiss.utils.CommonUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class ERDEditorInputFactory implements IElementFactory
{
    private static final String ID_FACTORY = ERDEditorInputFactory.class.getName(); //$NON-NLS-1$

    private static final String TAG_PATH = "path"; //$NON-NLS-1$

    public IAdaptable createElement(IMemento memento)
    {
        // Get the file name.
        String fileName = memento.getString(TAG_PATH);
        if (!CommonUtils.isEmpty(fileName)) {
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