/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object property handler
 */
public interface DBEPropertyHandler<OBJECT_TYPE extends DBSObject> {

    DBECommandComposite<OBJECT_TYPE, ? extends DBEPropertyHandler<OBJECT_TYPE>> createCompositeCommand();
}
