/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.edit.DBOCommand;
import org.jkiss.dbeaver.model.edit.DBOCommandListener;

/**
 * Command adapter
 */
public abstract class DBOCommandAdapter implements DBOCommandListener {
    public void onSave()
    {
    }

    public void onReset()
    {
    }

    public void onCommandDo(DBOCommand command)
    {
    }

    public void onCommandUndo(DBOCommand command)
    {
    }
}
