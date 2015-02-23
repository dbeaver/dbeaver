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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.*;

/**
 * Various objects cache
 */
public abstract class AbstractObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> implements DBSObjectCache<OWNER, OBJECT>
{

    private List<OBJECT> objectList;
    private Map<String, OBJECT> objectMap;
    protected boolean caseSensitive = true;
    protected Comparator<OBJECT> listOrderComparator;

    protected AbstractObjectCache() {
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    public Comparator<OBJECT> getListOrderComparator()
    {
        return listOrderComparator;
    }

    public void setListOrderComparator(Comparator<OBJECT> listOrderComparator)
    {
        this.listOrderComparator = listOrderComparator;
    }

    @Override
    public List<OBJECT> getCachedObjects()
    {
        synchronized (this) {
            return objectList == null ? Collections.<OBJECT>emptyList() : objectList;
        }
    }

    public <SUB_TYPE> Collection<SUB_TYPE> getTypedObjects(DBRProgressMonitor monitor, OWNER owner, Class<SUB_TYPE> type)
        throws DBException
    {
        List<SUB_TYPE> result = new ArrayList<SUB_TYPE>();
        for (OBJECT object : getObjects(monitor, owner)) {
            if (type.isInstance(object)) {
                result.add(type.cast(object));
            }
        }
        return result;
    }

    @Nullable
    @Override
    public OBJECT getCachedObject(String name)
    {
        synchronized (this) {
            return objectList == null || name == null ? null : getObjectMap().get(caseSensitive ? name : name.toUpperCase());
        }
    }

    @Override
    public void cacheObject(OBJECT object)
    {
        synchronized (this) {
            if (this.objectList != null) {
                detectCaseSensitivity(object);
                this.objectList.add(object);
                if (this.objectMap != null) {
                    this.objectMap.put(
                        caseSensitive ?
                            object.getName() :
                            object.getName().toUpperCase(),
                        object);
                }
            }
        }
    }

    @Override
    public void removeObject(OBJECT object)
    {
        synchronized (this) {
            if (this.objectList != null) {
                detectCaseSensitivity(object);
                this.objectList.remove(object);
                if (this.objectMap != null) {
                    this.objectMap.remove(caseSensitive ?
                        object.getName() :
                        object.getName().toUpperCase());
                }
            }
        }
    }

    @Nullable
    public <SUB_TYPE> SUB_TYPE getObject(DBRProgressMonitor monitor, OWNER owner, String name, Class<SUB_TYPE> type)
        throws DBException
    {
        final OBJECT object = getObject(monitor, owner, name);
        return type.isInstance(object) ? type.cast(object) : null;
    }

    public boolean isCached()
    {
        synchronized (this) {
            return objectList != null;
        }
    }

    @Override
    public void clearCache()
    {
        synchronized (this) {
            this.objectList = null;
            this.objectMap = null;
        }
    }

    public void setCache(List<OBJECT> objects)
    {
        synchronized (this) {
            objectList = objects;
            objectMap = null;
        }
    }

    private synchronized Map<String, OBJECT> getObjectMap()
    {
        if (objectMap == null) {
            this.objectMap = new HashMap<String, OBJECT>();
            for (OBJECT object : objectList) {
                this.objectMap.put(
                    caseSensitive ?
                        object.getName() :
                        object.getName().toUpperCase(),
                    object);
            }
        }
        return objectMap;
    }

    protected void detectCaseSensitivity(DBSObject object) {
        if (this.caseSensitive) {
            DBPDataSource dataSource = object.getDataSource();
            if (dataSource instanceof SQLDataSource &&
                ((SQLDataSource) dataSource).getSQLDialect().storesUnquotedCase() == DBPIdentifierCase.MIXED)
            {
                this.caseSensitive = false;
            }
        }
    }

    protected void invalidateObjects(DBRProgressMonitor monitor, OWNER owner, Iterator<OBJECT> objectIter)
    {

    }

    protected class CacheIterator implements Iterator<OBJECT> {
        private Iterator<OBJECT> listIterator = objectList.iterator();
        private OBJECT curObject;
        public CacheIterator()
        {
        }

        @Override
        public boolean hasNext()
        {
            return listIterator.hasNext();
        }

        @Override
        public OBJECT next()
        {
            return (curObject = listIterator.next());
        }

        @Override
        public void remove()
        {
            listIterator.remove();
            if (objectMap != null) {
                objectMap.remove(caseSensitive ? curObject.getName() : curObject.getName().toUpperCase());
            }
        }
    }
}
