/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.folder;

import org.jkiss.dbeaver.model.meta.DBMTreeFolder;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * FolderEditorInput
 */
public class FolderEditorInput extends DatabaseEditorInput<DBMTreeFolder>
{
    public FolderEditorInput(DBMTreeFolder dbmTreeFolder)
    {
        super(dbmTreeFolder);
    }

}
