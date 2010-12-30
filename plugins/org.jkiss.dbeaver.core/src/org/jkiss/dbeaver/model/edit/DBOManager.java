/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBOManager
 */
public interface DBOManager<OBJECT_TYPE extends DBSObject> extends IDataSourceProvider {

    OBJECT_TYPE getObject();

    void setObject(OBJECT_TYPE object);

}