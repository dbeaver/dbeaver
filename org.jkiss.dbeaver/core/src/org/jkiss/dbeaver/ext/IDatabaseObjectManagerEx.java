/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.jface.window.IShellProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.Map;

/**
 * IDatabaseObjectManager
 */
public interface IDatabaseObjectManagerEx<OBJECT_TYPE extends DBSObject> extends IDatabaseObjectManager<OBJECT_TYPE> {

    public static final long FEATURE_SAVE_IMMEDIATELY = 1;
    public static final long FEATURE_CREATE_FROM_PASTE = 2;

    /**
     * Creates new object and sets it as manager's object.
     * New object shouldn't be persisted by this function - it just performs manager initialization.
     * Real object creation will be performed by saveChanges function.
     * Additionally implementation could add initial command(s) to this manager.
     * This function can be invoked only once per one manager.
     * @param parent parent object
     * @param copyFrom template for new object (usually result of "paste" operation)
     */
    void createNewObject(DBSObject parent, OBJECT_TYPE copyFrom);

    /**
     * Deletes specified object.
     * Actually this function should not delete object but add command(s) to the manager.
     * Real object's delete will be performed by saveChanges function.
     * @param object object to delete
     * @param options delete options. Options are set by delete wizard.
     */
    void deleteObject(OBJECT_TYPE object, Map<String, Object> options);

}