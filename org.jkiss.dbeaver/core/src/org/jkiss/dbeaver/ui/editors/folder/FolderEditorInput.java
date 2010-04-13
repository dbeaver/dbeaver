/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.folder;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.model.meta.DBMTreeFolder;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * FolderEditorInput
 */
public class FolderEditorInput implements IDatabaseEditorInput
{
    private DBMTreeFolder folder;

    public FolderEditorInput(DBMTreeFolder folder)
    {
        this.folder = folder;
    }

    public DBMTreeFolder getFolder()
    {
        return folder;
    }

    public boolean exists()
    {
        return false;
    }

    public ImageDescriptor getImageDescriptor()
    {
        return ImageDescriptor.createFromImage(folder.getNodeIconDefault());
    }

    public String getName()
    {
        return folder.getName();
    }

    public IPersistableElement getPersistable()
    {
        return null;
    }

    public String getToolTipText()
    {
        return folder.getDescription();
    }

    public Object getAdapter(Class adapter)
    {
        return null;
    }

    public DBMModel getModel()
    {
        return folder.getModel();
    }

    public DBSObject getDatabaseObject()
    {
        return folder.getObject();
    }

}
