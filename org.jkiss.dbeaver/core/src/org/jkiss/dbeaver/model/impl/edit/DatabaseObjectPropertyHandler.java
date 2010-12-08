/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database object modifier
 */
public interface DatabaseObjectPropertyHandler<OBJECT_TYPE extends DBSObject> {

    void modify(OBJECT_TYPE object, Object value);

}
