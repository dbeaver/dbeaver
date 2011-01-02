/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.runtime.qm.meta.QMMObject;

/**
 * QM meta event
 */
public class QMMetaEvent {

    public enum Action {
        BEGIN,
        END,
        UPDATE,
    }

    private final QMMObject object;
    private final Action action;

    public QMMetaEvent(QMMObject object, Action action)
    {
        this.object = object;
        this.action = action;
    }

    public QMMObject getObject()
    {
        return object;
    }

    public Action getAction()
    {
        return action;
    }

    @Override
    public String toString()
    {
        return action + " " + object;
    }
}
