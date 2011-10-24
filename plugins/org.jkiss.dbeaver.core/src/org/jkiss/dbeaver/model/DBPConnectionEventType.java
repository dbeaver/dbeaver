/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.core.CoreMessages;

/**
* Connection event type
*/
public enum DBPConnectionEventType {
    BEFORE_CONNECT(CoreMessages.dialog_connection_events_event_before_connect),
    AFTER_CONNECT(CoreMessages.dialog_connection_events_event_after_connect),
    BEFORE_DISCONNECT(CoreMessages.dialog_connection_events_event_before_disconnect),
    AFTER_DISCONNECT(CoreMessages.dialog_connection_events_event_after_disconnect);

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
