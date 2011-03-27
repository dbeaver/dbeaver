/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

/**
 * DBEObjectManager
 */
public interface DBECommandListener {

    void onSave();

    void onReset();

    void onCommandDo(DBECommand command);

    void onCommandUndo(DBECommand command);

}