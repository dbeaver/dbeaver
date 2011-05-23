/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

/**
 * DBNDatabaseFolder
 */
public interface DBNContainer //extends DBSFolder
{
    Object getValueObject();

    String getChildrenType();

    Class<?> getChildrenClass();

}