/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
    Collection<OBJECT> getCachedObjects();

    @Nullable
    OBJECT getObject(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner, @NotNull String name)
        throws DBException;

    @Nullable
    OBJECT getCachedObject(@NotNull String name);

    boolean isCached();

    /**
     * Adds specified object to cache
     * @param object object to cache
     */
    void cacheObject(@NotNull OBJECT object);

    /**
     * Sets new cache contents. setCache(getCachedObjects()) will reset named cache
     * @param objects new cache contents
     */
    void setCache(List<OBJECT> objects);

    /**
     * Removes specified object from cache
     * @param object object to remove
     */
    void removeObject(@NotNull OBJECT object);

    void clearCache();

}
