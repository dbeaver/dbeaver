/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object property updater
 */
public interface DatabaseObjectPropertyUpdater<OBJECT_TYPE extends DBSObject> extends DatabaseObjectPropertyHandler<OBJECT_TYPE> {

    void updateModel(OBJECT_TYPE object, Object value);

}