/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

/**
 * IRefreshablePart
 */
public interface IRefreshableContainer
{
    void addRefreshClient(IRefreshablePart part);

    void removeRefreshClient(IRefreshablePart part);

}