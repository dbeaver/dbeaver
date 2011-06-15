/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * DBDValueHandlerProvider
 */
public interface DBDValueHandlerProvider
{
    Image getTypeImage(DBSTypedObject type);

    DBDValueHandler getHandler(
        DBCExecutionContext context,
        DBSTypedObject type);

}