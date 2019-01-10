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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * List wrapper cache
 */
public class ListCache<OWNER extends DBSObject, OBJECT extends DBSObject>
    implements DBSObjectCache<OWNER, OBJECT>
{

    @NotNull
    private final List<OBJECT> objectList;

    public ListCache(List<OBJECT> objectList) {
        this.objectList = (objectList == null ? new ArrayList<>() : objectList);
    }

    @Override
    public Collection<OBJECT> getAllObjects(DBRProgressMonitor monitor, OWNER owner) throws DBException {
        return objectList;
    }

    @NotNull
    @Override
    public List<OBJECT> getCachedObjects()
    {
        return objectList;
    }

    @Override
    public OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, String name) throws DBException {
        return DBUtils.findObject(objectList, name);
    }

    @Nullable
    @Override
    public OBJECT getCachedObject(@NotNull String name)
    {
        return DBUtils.findObject(objectList, name);
    }

    @Override
    public void cacheObject(@NotNull OBJECT object)
    {
        objectList.add(object);
    }

    @Override
    public void removeObject(@NotNull OBJECT object, boolean resetFullCache)
    {
        objectList.remove(object);
    }

    public boolean isFullyCached()
    {
        return true;
    }

    @Override
    public void clearCache()
    {
        this.objectList.clear();
    }

    public void setCache(List<OBJECT> objects)
    {
        this.objectList.clear();
        this.objectList.addAll(objects);
    }

}
