/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.model.DBPObject;

/**
 * Structured objects editor
 */
public interface DBEStructEditor<OBJECT_TYPE extends DBPObject> extends DBEObjectEditor<OBJECT_TYPE> {

    Class<?>[] getChildTypes();
}
