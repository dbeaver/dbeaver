/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
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

    public Collection<OBJECT> getCachedObjects()
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

    public OBJECT getCachedObject(String name)
    {
        synchronized (this) {
            return objectList == null ? null : getObjectMap().get(caseSensitive ? name : name.toUpperCase());
        }
    }

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

    private void detectCaseSensitivity(DBSObject object) {
        if (this.caseSensitive && object.getDataSource().getInfo().storesUnquotedCase() == DBPIdentifierCase.MIXED) {
            this.caseSensitive = false;
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

        public boolean hasNext()
        {
            return listIterator.hasNext();
        }

        public OBJECT next()
        {
            return (curObject = listIterator.next());
        }

        public void remove()
        {
            listIterator.remove();
            if (objectMap != null) {
                objectMap.remove(caseSensitive ? curObject.getName() : curObject.getName().toUpperCase());
            }
        }
    }
}
