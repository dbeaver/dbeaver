/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

/**
 * IRefreshablePart
 */
public interface IRefreshablePart
{
    void refreshPart(Object source, boolean force);
}