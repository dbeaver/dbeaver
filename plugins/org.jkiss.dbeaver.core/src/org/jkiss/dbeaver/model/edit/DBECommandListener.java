/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

/**
 * DBEObjectManager
 */
public interface DBECommandListener {

    void onCommandChange(DBECommand command);

    void onSave();

    void onReset();

    void onCommandDo(DBECommand command);

    void onCommandUndo(DBECommand command);

}