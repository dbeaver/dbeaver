/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectManager;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * AbstractObjectManager
 */
public abstract class AbstractObjectManager<OBJECT_TYPE extends DBPObject> implements IObjectManager {

    private OBJECT_TYPE object;

    public OBJECT_TYPE getObject() {
        return object;
    }

    @SuppressWarnings("unchecked")
    public void init(DBPObject object) throws DBException {
        if (object == null) {
            throw new IllegalArgumentException("Object can't be NULL");
        }
        try {
            this.object = (OBJECT_TYPE) object;
        } catch (ClassCastException e) {
            throw new DBException("Bad object type: " + object.getClass().getName());
        }
    }

    public boolean supportsEdit() {
        return false;
    }

    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        // do nothing
    }

    public void resetChanges(DBRProgressMonitor monitor) {
        // do nothing
    }
}
