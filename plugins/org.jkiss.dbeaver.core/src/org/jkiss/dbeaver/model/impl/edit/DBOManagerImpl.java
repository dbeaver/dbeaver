/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.edit.DBOManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBOManagerImpl
 */
public abstract class DBOManagerImpl<OBJECT_TYPE extends DBSObject> implements DBOManager<OBJECT_TYPE> {


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
