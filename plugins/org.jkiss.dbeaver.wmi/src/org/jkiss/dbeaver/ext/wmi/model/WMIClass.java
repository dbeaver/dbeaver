/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIObject;

import java.util.Collection;

/**
 * WMI class
 */
public class WMIClass extends WMIContainer implements DBPCloseableObject
{

    private WMIObject classObject;
    private String name;

    public WMIClass(WMIContainer parent, WMIObject classObject)
    {
        super(parent);
        this.classObject = classObject;
    }


    public WMIObject getClassObject()
    {
        return classObject;
    }

    @Property(name = "Name", viewable = true)
    public String getName()
    {
        if (name == null) {
            try {
                name = CommonUtils.toString(
                    classObject.getValue("__CLASS"));
            } catch (WMIException e) {
                log.error(e);
                return e.getMessage();
            }
        }
        return name;
    }
    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return WMIClass.class;
    }

    public void close()
    {
        if (classObject != null) {
            classObject.release();
            classObject = null;
        }
    }
}
