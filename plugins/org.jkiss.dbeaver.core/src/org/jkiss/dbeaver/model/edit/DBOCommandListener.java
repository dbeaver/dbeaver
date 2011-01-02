/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

/**
 * DBOManager
 */
public interface DBOCommandListener {

    void onSave();

    void onReset();

    void onCommandDo(DBOCommand command);

    void onCommandUndo(DBOCommand command);

}