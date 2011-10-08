/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

/**
 * Database persist action
 */
public interface IDatabasePersistAction {

    public enum ActionType {
        INITIALIZER,
        NORMAL,
        FINALIZER
    }

    String getTitle();

    String getScript();

    void handleExecute(Throwable error);

    ActionType getType();
}
