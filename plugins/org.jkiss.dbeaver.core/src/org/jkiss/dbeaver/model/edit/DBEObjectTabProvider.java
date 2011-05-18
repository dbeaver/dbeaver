/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBEObjectTabProvider
 */
public interface DBEObjectTabProvider<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    /**
     * Get tab descriptors for particular object.
     *
     * @param workbenchWindow workbench window
     * @param activeEditor active editor (may be null)
     * */
    ITabDescriptor[] getTabDescriptors(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor,
        OBJECT_TYPE object);

}