/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.folder;

import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * FolderEditorInput
 */
public class FolderEditorInput extends DatabaseEditorInput<DBNTreeFolder>
{
    public FolderEditorInput(DBNTreeFolder dbmTreeFolder)
    {
        super(dbmTreeFolder);
    }

}
