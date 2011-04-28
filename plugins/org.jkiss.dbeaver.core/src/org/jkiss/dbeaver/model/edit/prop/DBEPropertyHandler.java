/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommandQueue;

/**
 * Database object property handler
 */
public interface DBEPropertyHandler<OBJECT_TYPE extends DBPObject> {

    DBECommandComposite<OBJECT_TYPE, ? extends DBEPropertyHandler<OBJECT_TYPE>> createCompositeCommand(DBECommandQueue<OBJECT_TYPE> commandQueue);
}
