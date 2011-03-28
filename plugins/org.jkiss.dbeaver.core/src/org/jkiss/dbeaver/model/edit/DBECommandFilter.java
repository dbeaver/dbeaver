/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Command filter.
 */
public interface DBECommandFilter<OBJECT_TYPE extends DBSObject> {

    void filterCommands(DBECommandQueue<OBJECT_TYPE> queue);

}