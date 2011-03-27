/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object validator
 */
public interface DBEPropertyPersister<OBJECT_TYPE extends DBSObject> extends DBEPropertyHandler<OBJECT_TYPE> {

    IDatabasePersistAction[] getPersistActions(OBJECT_TYPE object, Object value);

}