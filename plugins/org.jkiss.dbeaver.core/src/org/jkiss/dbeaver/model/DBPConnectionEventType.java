/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
* Connection event type
*/
public enum DBPConnectionEventType {
    BEFORE_CONNECT("Before Connect"),
    AFTER_CONNECT("After Connect"),
    BEFORE_DISCONNECT("Before Disconnect"),
    AFTER_DISCONNECT("After Disconnect");

    private final String title;

    DBPConnectionEventType(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }
}
