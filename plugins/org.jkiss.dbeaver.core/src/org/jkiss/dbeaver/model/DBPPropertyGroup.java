/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import java.util.List;

/**
 * DBPDriver
 */
public interface DBPPropertyGroup
{
    String getName();

    String getDescription();

    List<? extends DBPProperty> getProperties();

}