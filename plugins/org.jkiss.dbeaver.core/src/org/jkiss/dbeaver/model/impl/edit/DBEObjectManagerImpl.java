/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBEObjectManagerImpl
 */
public abstract class DBEObjectManagerImpl<OBJECT_TYPE extends DBSObject> implements DBEObjectManager<OBJECT_TYPE> {


    private OBJECT_TYPE object;

    public DBPDataSource getDataSource() {
        return object.getDataSource();
    }

    public OBJECT_TYPE getObject() {
        return object;
    }

    public void setObject(OBJECT_TYPE object) {
        if (this.object != object) {
            this.object = object;
        }
    }

}
