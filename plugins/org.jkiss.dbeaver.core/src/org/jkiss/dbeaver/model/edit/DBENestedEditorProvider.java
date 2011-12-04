/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBENestedEditorProvider
 */
public interface DBENestedEditorProvider<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    /**
     * Get tab descriptors for particular object.
     *
     * @param workbenchWindow workbench window
     * @param superEditor main editor */
    IDatabaseEditor[] getNestedEditors(
        IWorkbenchWindow workbenchWindow,
        IDatabaseEditor superEditor);

}