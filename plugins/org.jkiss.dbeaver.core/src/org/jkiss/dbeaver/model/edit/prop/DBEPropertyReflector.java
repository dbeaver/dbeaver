/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object property change reflector
 */
public interface DBEPropertyReflector<OBJECT_TYPE extends DBSObject> extends DBEPropertyHandler<OBJECT_TYPE> {

    void reflectValueChange(OBJECT_TYPE object, Object oldValue, Object newValue);

}