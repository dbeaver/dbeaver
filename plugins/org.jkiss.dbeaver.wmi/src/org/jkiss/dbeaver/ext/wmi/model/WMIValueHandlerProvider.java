/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.DBCDefaultValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * WMI data types provider
 */
public class WMIValueHandlerProvider implements DBDValueHandlerProvider {

    public Image getTypeImage(DBSTypedObject type)
    {
        return WMIClassProperty.getPropertyImage(type.getValueType());
    }

    public DBDValueHandler getHandler(DBDPreferences preferences, String typeName, int valueType)
    {
        return DBCDefaultValueHandler.INSTANCE;
    }

}