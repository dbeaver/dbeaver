/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * DBEObjectManager
 */
public interface DBEObjectMaker<OBJECT_TYPE extends DBSObject, CONTAINER_TYPE> extends DBEObjectManager<OBJECT_TYPE> {

    public static final long FEATURE_SAVE_IMMEDIATELY = 1;
    public static final long FEATURE_CREATE_FROM_PASTE = 2;
    public static final long FEATURE_EDITOR_ON_CREATE = 4;

    long getMakerOptions();

    /**
     * Creates new object and sets it as manager's object.
     * New object shouldn't be persisted by this function - it just performs manager initialization.
     * Real object creation will be performed by saveChanges function.
     * Additionally implementation could add initial command(s) to this manager.
     * This function can be invoked only once per one manager.
     *
     *
     *
     * @param workbenchWindow workbench window
     * @param activeEditor active editor (may be null)
     * @param commandContext command context
     * @param parent parent object
     * @param copyFrom template for new object (usually result of "paste" operation)    @return null if no additional actions should be performed
     * */
    OBJECT_TYPE createNewObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor,
        DBECommandContext commandContext,
        CONTAINER_TYPE parent,
        Object copyFrom);

    /**
     * Deletes specified object.
     * Actually this function should not delete object but add command(s) to the manager.
     * Real object's delete will be performed by saveChanges function.
     * @param commandContext command context
     * @param object object
     * @param options delete options. Options are set by delete wizard.
     */
    void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options);

}