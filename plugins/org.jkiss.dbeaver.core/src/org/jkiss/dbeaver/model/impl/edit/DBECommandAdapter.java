/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandListener;

/**
 * Command adapter
 */
public abstract class DBECommandAdapter implements DBECommandListener {

    @Override
    public void onCommandChange(DBECommand command)
    {
    }

    @Override
    public void onSave()
    {
    }

    @Override
    public void onReset()
    {
    }

    @Override
    public void onCommandDo(DBECommand command)
    {
    }

    @Override
    public void onCommandUndo(DBECommand command)
    {
    }
}
