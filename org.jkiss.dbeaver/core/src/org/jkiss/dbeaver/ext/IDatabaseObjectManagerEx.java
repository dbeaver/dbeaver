/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.jface.window.IShellProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * IDatabaseObjectManager
 */
public interface IDatabaseObjectManagerEx<OBJECT_TYPE extends DBSObject> extends IDatabaseObjectManager<OBJECT_TYPE> {

    public static final long FEATURE_SAVE_IMMEDIATELY = 1;
    public static final long FEATURE_CREATE_FROM_PASTE = 2;

    void createNewObject(DBSObject parent, OBJECT_TYPE copyFrom);

    void deleteObject(OBJECT_TYPE object);

}