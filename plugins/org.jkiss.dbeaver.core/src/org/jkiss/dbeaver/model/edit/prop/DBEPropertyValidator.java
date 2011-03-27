/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object validator
 */
public interface DBEPropertyValidator<OBJECT_TYPE extends DBSObject> extends DBEPropertyHandler<OBJECT_TYPE> {

    void validate(OBJECT_TYPE object, Object value) throws DBException;

}