/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
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
     * Provides access to objects cache.
     * Editor will reflect object create/delete in commands model update method
     * @param object contained object
     * @return objects cache or null
     */
    @Nullable
    DBSObjectCache<? extends DBSObject, OBJECT_TYPE> getObjectsCache(OBJECT_TYPE object);

    boolean canCreateObject(CONTAINER_TYPE parent);

    boolean canDeleteObject(OBJECT_TYPE object);

    /**
     * Creates new object and sets it as manager's object.
     * New object shouldn't be persisted by this function - it just performs manager initialization.
     * Real object creation will be performed by saveChanges function.
     * Additionally implementation could add initial command(s) to this manager.
     * This function can be invoked only once per one manager.
     *
     * @param workbenchWindow workbench window
     * @param commandContext command context
     * @param parent parent object
     * @param copyFrom template for new object (usually result of "paste" operation)    @return null if no additional actions should be performed   */
    OBJECT_TYPE createNewObject(
        IWorkbenchWindow workbenchWindow,
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