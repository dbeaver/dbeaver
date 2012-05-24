/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Entity abstract element
 */
public interface DBSEntityElement extends DBSObject
{
    @Override
    DBSEntity getParentObject();

    //boolean isInherited();
}
