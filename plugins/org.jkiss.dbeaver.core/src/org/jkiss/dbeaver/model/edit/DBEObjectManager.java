/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBEObjectManager
 */
public interface DBEObjectManager<OBJECT_TYPE extends DBSObject> extends IDataSourceProvider {

    OBJECT_TYPE getObject();

    void setObject(OBJECT_TYPE object);

}