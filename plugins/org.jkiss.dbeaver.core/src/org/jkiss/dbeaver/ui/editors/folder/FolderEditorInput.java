/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.folder;

import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * FolderEditorInput
 */
public class FolderEditorInput extends DatabaseEditorInput<DBNDatabaseFolder>
{
    public FolderEditorInput(DBNDatabaseFolder dbmTreeFolder)
    {
        super(dbmTreeFolder);
    }

}
