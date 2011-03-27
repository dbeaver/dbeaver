/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object property updater
 */
public interface DBEPropertyUpdater<OBJECT_TYPE extends DBSObject> extends DBEPropertyHandler<OBJECT_TYPE> {

    void updateModel(OBJECT_TYPE object, Object value);

}