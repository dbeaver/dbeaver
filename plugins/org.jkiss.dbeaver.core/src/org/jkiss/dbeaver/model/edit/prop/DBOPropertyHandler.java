/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object property handler
 */
public interface DBOPropertyHandler<OBJECT_TYPE extends DBSObject> {

    DBOCommandComposite<OBJECT_TYPE, ? extends DBOPropertyHandler<OBJECT_TYPE>> createCompositeCommand();
}
