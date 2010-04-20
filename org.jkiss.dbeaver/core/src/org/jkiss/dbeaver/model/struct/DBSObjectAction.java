/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSObjectAction
 */
public enum DBSObjectAction
{
    CREATED,    // Object created
    DROPED,     // Object delete
    ALTERED,    // Object altered
    CHANGED,    // Object's state changed
    REFRESHED,  // Object refreshed
}