/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object property change reflector
 */
public interface DatabaseObjectPropertyReflector<OBJECT_TYPE extends DBSObject> extends DatabaseObjectPropertyHandler<OBJECT_TYPE> {

    void reflectValueChange(OBJECT_TYPE object, Object oldValue, Object newValue);

}