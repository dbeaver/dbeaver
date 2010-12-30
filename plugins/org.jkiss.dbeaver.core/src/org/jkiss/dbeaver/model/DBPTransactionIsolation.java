/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * DBPTransactionIsolation
 */
public interface DBPTransactionIsolation
{
    boolean isEnabled();

    String getName();
}
