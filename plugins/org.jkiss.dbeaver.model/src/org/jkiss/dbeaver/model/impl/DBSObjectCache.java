/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.List;

/**
 * Objects cache
 */
public interface DBSObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> {

    @NotNull
    Collection<OBJECT> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner)
        throws DBException;

    @NotNull
    List<OBJECT> getCachedObjects();

    @Nullable
    OBJECT getObject(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner, @NotNull String name)
        throws DBException;

    @Nullable
    OBJECT getCachedObject(@NotNull String name);

    /**
     * True if all available objects were cached
     */
    boolean isFullyCached();

    /**
     * Adds specified object to cache
     * @param object object to cache
     */
    void cacheObject(@NotNull OBJECT object);

    /**
     * Sets new cache contents. setCache(getCachedObjects()) will reset named cache.
     * Set fullyCache flag to true.
     * @param objects new cache contents
     */
    void setCache(List<OBJECT> objects);

    /**
     * Removes specified object from cache
     * @param object object to remove
     * @param resetFullCache if true resets fullyCached flag. May be used to refresh linked objects.
     */
    void removeObject(@NotNull OBJECT object, boolean resetFullCache);

    /**
     * Clears all cache
     */
    void clearCache();

}
