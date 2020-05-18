/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * DBEObjectManager
 */
public interface DBEObjectMaker<OBJECT_TYPE extends DBSObject, CONTAINER_TYPE> extends DBEObjectManager<OBJECT_TYPE> {

    long FEATURE_SAVE_IMMEDIATELY       = 1;
    long FEATURE_CREATE_FROM_PASTE      = 2;
    long FEATURE_EDITOR_ON_CREATE       = 4;
    long FEATURE_DELETE_CASCADE         = 8;

    /**
     * New object container.
     * Usually it is a navigator node (DBNNode).
     */
    String OPTION_CONTAINER = "container";
    /**
     * Object type (class)
     */
    String OPTION_OBJECT_TYPE = "objectType";

    String OPTION_DELETE_CASCADE = "deleteCascade";

    long getMakerOptions(DBPDataSource dataSource);

    /**
     * Provides access to objects cache.
     * Editor will reflect object create/delete in commands model update method
     * @param object contained object
     * @return objects cache or null
     */
    @Nullable
    DBSObjectCache<? extends DBSObject, OBJECT_TYPE> getObjectsCache(OBJECT_TYPE object);

    boolean canCreateObject(Object container);

    boolean canDeleteObject(OBJECT_TYPE object);

    /**
     * Creates new object and sets it as manager's object.
     * New object shouldn't be persisted by this function - it just performs manager initialization.
     * Real object creation will be performed by saveChanges function.
     * Additionally implementation could add initial command(s) to this manager.
     * This function can be invoked only once per one manager.
     *
     * @param monitor progress monitor
     * @param commandContext command context
     * @param container parent object
     * @param copyFrom template for new object (usually result of "paste" operation)
     * @param options options
     * @return null if no additional actions should be performed
     */
    OBJECT_TYPE createNewObject(
        DBRProgressMonitor monitor,
        DBECommandContext commandContext,
        Object container,
        Object copyFrom,
        Map<String, Object> options) throws DBException;

    /**
     * Deletes specified object.
     * Actually this function should not delete object but add command(s) to the manager.
     * Real object's delete will be performed by saveChanges function.
     * @param commandContext command context
     * @param object object
     * @param options delete options. Options are set by delete wizard.
     */
    void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options) throws DBException;

}