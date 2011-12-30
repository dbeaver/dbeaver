/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIObject;

import java.util.Collection;

/**
 * WMI class
 */
public class WMIClass extends WMIContainer {

    private WMIObject classObject;

    public WMIClass(WMIContainer parent, WMIObject classObject)
    {
        super(parent);
        this.classObject = classObject;
    }


    public WMIObject getClassObject()
    {
        return classObject;
    }

    public String getName()
    {
        try {
            return CommonUtils.toString(
                classObject.getProperty("Name"));
        } catch (WMIException e) {
            log.error(e);
            return e.getMessage();
        }
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return WMIClass.class;
    }

}
