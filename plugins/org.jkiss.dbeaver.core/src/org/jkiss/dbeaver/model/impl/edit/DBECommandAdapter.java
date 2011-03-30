/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandListener;

/**
 * Command adapter
 */
public abstract class DBECommandAdapter implements DBECommandListener {

    public void onCommandChange(DBECommand command)
    {
    }

    public void onSave()
    {
    }

    public void onReset()
    {
    }

    public void onCommandDo(DBECommand command)
    {
    }

    public void onCommandUndo(DBECommand command)
    {
    }
}
